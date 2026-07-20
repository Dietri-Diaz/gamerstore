package com.gamerstore.app.service;

import com.gamerstore.app.model.Comprobante;
import com.gamerstore.app.model.Pago;
import com.gamerstore.app.model.Pedido;
import com.gamerstore.app.model.PedidoItem;
import com.gamerstore.app.model.Producto;
import com.gamerstore.app.repository.ComprobanteRepository;
import com.gamerstore.app.repository.PagoRepository;
import com.gamerstore.app.repository.PedidoRepository;
import com.gamerstore.app.repository.ProductoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Anulación de una venta: reversa completa de una compra que YA se pagó.
 *
 * CUIDADO, no confundir CANCELADO con ANULADO (son estados distintos y ambos existen):
 *  - CANCELADO: el pedido nunca llegó a pagarse (se abandonó, el pago se rechazó...).
 *               No hay plata que devolver ni boleta emitida.
 *  - ANULADO:   la venta se concretó (hay pago aprobado y boleta) y se reversa: se devuelve
 *               el dinero, se repone el stock y la boleta queda marcada como anulada.
 *
 * Todo el proceso corre dentro de UNA sola transacción: si algo falla a mitad de camino
 * (sobre todo la devolución en Stripe), no queda nada a medias.
 */
@Service
public class AnulacionService {

    private static final Logger log = LoggerFactory.getLogger(AnulacionService.class);

    private final PedidoRepository pedidoRepo;
    private final PagoRepository pagoRepo;
    private final ComprobanteRepository comprobanteRepo;
    private final ProductoRepository productoRepo;
    private final StripeService stripeService;

    public AnulacionService(PedidoRepository pedidoRepo, PagoRepository pagoRepo,
                            ComprobanteRepository comprobanteRepo, ProductoRepository productoRepo,
                            StripeService stripeService) {
        this.pedidoRepo = pedidoRepo;
        this.pagoRepo = pagoRepo;
        this.comprobanteRepo = comprobanteRepo;
        this.productoRepo = productoRepo;
        this.stripeService = stripeService;
    }

    /**
     * Anula la venta de un pedido: repone stock, devuelve el dinero, anula la boleta y
     * deja el pedido en ANULADO con el motivo.
     */
    @Transactional
    public Pedido anular(Long pedidoId, String motivo) {
        // 1. El pedido tiene que existir y no estar ya anulado (anular dos veces devolvería
        //    el dinero dos veces y repondría el stock de más).
        Pedido pedido = pedidoRepo.findById(pedidoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));

        if ("ANULADO".equals(pedido.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El pedido ya está anulado");
        }

        // 2. Reponer el stock: los productos vuelven a estar disponibles para vender.
        for (PedidoItem item : pedido.getItems()) {
            Producto prod = item.getProducto();
            if (prod != null) {
                prod.setStock(prod.getStock() + item.getCantidad());
                productoRepo.save(prod);
            }
        }

        // 3. Devolver el dinero, si es que hubo un cobro aprobado.
        devolverPagos(pedido, motivo);

        // 4. Anular la boleta (si el pedido llegó a tener una). No se borra ni se reutiliza su
        //    correlativo: sigue en el registro de ventas marcada como ANULADA.
        Comprobante comprobante = comprobanteRepo.findByPedidoId(pedido.getId()).orElse(null);
        if (comprobante != null) {
            comprobante.setEstado("ANULADO");
            comprobante.setMotivoAnulacion(motivo);
            comprobante.setFechaAnulacion(LocalDateTime.now());
            comprobanteRepo.save(comprobante);
        }

        // 5. Y recién ahora el pedido queda anulado, con el motivo a la vista.
        pedido.setEstado("ANULADO");
        pedido.setMotivoAnulacion(motivo);
        return pedidoRepo.save(pedido);
    }

    /**
     * Devuelve el dinero de los pagos APROBADOS del pedido y los marca como DEVUELTO.
     *
     * Un pedido sin pago aprobado (nunca se cobró) simplemente no entra al bucle: se anula
     * igual, sin devolución, porque no hay nada que devolver.
     */
    private void devolverPagos(Pedido pedido, String motivo) {
        List<Pago> aprobados = pagoRepo.findByPedidoIdAndEstado(pedido.getId(), "APROBADO");

        for (Pago pago : aprobados) {
            boolean esTarjetaStripe = "TARJETA".equalsIgnoreCase(pago.getMetodo())
                    && pago.getReferencia() != null
                    && pago.getReferencia().startsWith("pi_")   // solo los cobros REALES de Stripe
                    && stripeService.estaActivo();

            if (esTarjetaStripe) {
                // Devolución REAL contra Stripe (en modo prueba, pero es la llamada de verdad).
                // Si Stripe falla, reembolsar(...) lanza 502 y, al estar todo en la misma
                // transacción, se revierte la anulación entera: preferimos NO anular antes que
                // anular sin haberle devuelto el dinero al cliente.
                StripeService.ResultadoReembolso r = stripeService.reembolsar(pago.getReferencia());
                if (!r.ok()) {
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                            "No se pudo devolver el dinero: " + r.mensaje());
                }
                pago.setReferenciaDevolucion(r.refundId());
                log.info("Pedido {} anulado: devolucion Stripe {} ({})",
                        pedido.getCodigo(), r.refundId(), motivo);
            } else {
                // Devolución MANUAL. Pasa en dos casos:
                //  - Yape: la app NO tiene API de devolución, así que el dinero se devuelve a
                //    mano (otro yapeo, transferencia o efectivo en tienda). El sistema solo deja
                //    constancia de que corresponde devolver; el traspaso lo hace una persona.
                //  - Pagos con la tarjeta simulada (referencia sin "pi_"), donde nunca se movió
                //    plata real, así que no hay nada que pedirle a la pasarela.
                pago.setReferenciaDevolucion("MANUAL");
                log.info("Pedido {} anulado: devolucion MANUAL del pago {} (metodo {})",
                        pedido.getCodigo(), pago.getCodigo(), pago.getMetodo());
            }

            pago.setEstado("DEVUELTO");
            pago.setFechaDevolucion(LocalDateTime.now());
            pagoRepo.save(pago);
        }
    }
}
