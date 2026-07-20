package com.gamerstore.app.service;

import com.gamerstore.app.model.Pago;
import com.gamerstore.app.model.Pedido;
import com.gamerstore.app.repository.PagoRepository;
import com.gamerstore.app.repository.PedidoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/** Reglas de negocio de la pasarela de pagos: cobro simulado con tarjeta (Luhn) o Yape. */
@Service
public class PagoService {

    private static final String ALFANUMERICOS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private final SecureRandom random = new SecureRandom();

    private final PagoRepository pagoRepo;
    private final PedidoRepository pedidoRepo;
    private final StripeService stripeService;
    private final ComprobanteService comprobanteService;
    private final double yapeMontoMaximo;

    public PagoService(PagoRepository pagoRepo, PedidoRepository pedidoRepo, StripeService stripeService,
                       ComprobanteService comprobanteService,
                       @Value("${app.yape.monto-maximo:500}") double yapeMontoMaximo) {
        this.pagoRepo = pagoRepo;
        this.pedidoRepo = pedidoRepo;
        this.stripeService = stripeService;
        this.comprobanteService = comprobanteService;
        this.yapeMontoMaximo = yapeMontoMaximo;
    }

    // Lista todos los pagos, del más reciente al más antiguo.
    public List<Pago> listar() {
        return pagoRepo.findAllByOrderByFechaDesc();
    }

    // Busca un pago por id.
    public Pago porId(Long id) {
        return pagoRepo.findById(id).orElseThrow();
    }

    /**
     * Cobra un pedido con tarjeta: valida número (Luhn), vencimiento y CVV, simula la
     * respuesta del banco (rechaza si el número termina en 0002) y, si aprueba, marca
     * el pedido como PAGADO.
     */
    @Transactional
    public Pago pagarConTarjeta(Long pedidoId, String numero, String titular, String vencimiento, String cvv) {
        Pedido pedido = validarPedido(pedidoId);

        String limpio = numero == null ? "" : numero.replaceAll("[\\s-]", "");
        if (!limpio.matches("\\d{13,19}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Número de tarjeta inválido");
        }
        if (!luhn(limpio)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Número de tarjeta inválido");
        }

        if (vencimiento == null || !vencimiento.matches("\\d{2}/\\d{2}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vencimiento inválido (usa MM/AA)");
        }
        int mes = Integer.parseInt(vencimiento.substring(0, 2));
        int anioCorto = Integer.parseInt(vencimiento.substring(3, 5));
        if (mes < 1 || mes > 12) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vencimiento inválido (usa MM/AA)");
        }
        int anio = 2000 + anioCorto;
        LocalDate ultimoDia = YearMonth.of(anio, mes).atEndOfMonth();
        if (ultimoDia.isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La tarjeta está vencida");
        }

        if (cvv == null || !cvv.matches("\\d{3,4}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CVV inválido");
        }

        // Simulación del banco: los números que terminan en 0002 siempre se rechazan.
        boolean rechazado = limpio.endsWith("0002");

        Pago pago = new Pago();
        pago.setPedido(pedido);
        pago.setMetodo("TARJETA");
        pago.setMonto(pedido.getTotal());
        pago.setReferencia(generarCodigoAutorizacion());
        pago.setTarjetaUlt4(limpio.substring(limpio.length() - 4));
        pago.setTitular(titular);
        pago.setEstado(rechazado ? "RECHAZADO" : "APROBADO");

        Pago guardado = pagoRepo.save(pago);

        if (!rechazado) {
            aprobarPedido(pedido, guardado);
        }

        return guardado;
    }

    /** Cobra el pedido con la pasarela REAL (Stripe). El navegador ya tokenizó la tarjeta: aquí solo llega el paymentMethodId. */
    @Transactional
    public Pago pagarConStripe(Long pedidoId, String paymentMethodId) {
        Pedido pedido = validarPedido(pedidoId);

        if (paymentMethodId == null || paymentMethodId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta el token de la tarjeta");
        }

        StripeService.ResultadoStripe resultado = stripeService.cobrar(
                paymentMethodId, pedido.getTotal(), "Pedido " + pedido.getCodigo() + " - GamerStore");

        Pago pago = new Pago();
        pago.setPedido(pedido);
        pago.setMetodo("TARJETA");
        pago.setMonto(pedido.getTotal());
        pago.setReferencia(resultado.referencia());
        pago.setTarjetaUlt4(resultado.ult4());
        pago.setTitular(resultado.titular());
        pago.setEstado(resultado.aprobado() ? "APROBADO" : "RECHAZADO");

        Pago guardado = pagoRepo.save(pago);

        if (resultado.aprobado()) {
            aprobarPedido(pedido, guardado);
        }

        return guardado;
    }

    /** Cobra un pedido con Yape: valida el número de operación y evita registrarlo dos veces. */
    @Transactional
    public Pago pagarConYape(Long pedidoId, String numeroOperacion, String voucher) {
        Pedido pedido = validarPedido(pedidoId);

        // Yape (la app) tiene tope de monto: si el pedido lo supera, hay que cobrar con tarjeta.
        if (pedido.getTotal() > yapeMontoMaximo) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, String.format(
                    "Yape solo permite pagos de hasta S/ %,.2f. Usa tarjeta para montos mayores.", yapeMontoMaximo));
        }

        String limpio = numeroOperacion == null ? "" : numeroOperacion.trim();
        if (!limpio.matches("\\d{6,20}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Número de operación inválido");
        }
        if (pagoRepo.existsByReferenciaAndMetodo(limpio, "YAPE")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ese número de operación ya fue registrado");
        }

        Pago pago = new Pago();
        pago.setPedido(pedido);
        pago.setMetodo("YAPE");
        pago.setMonto(pedido.getTotal());
        pago.setEstado("APROBADO");
        pago.setReferencia(limpio);
        pago.setVoucher(voucher);

        Pago guardado = pagoRepo.save(pago);

        aprobarPedido(pedido, guardado);

        return guardado;
    }

    // Cuando el pago se aprueba: marcamos el pedido como pagado y emitimos su boleta.
    private void aprobarPedido(Pedido pedido, Pago pago) {
        pedido.setEstado("PAGADO");
        pedido.setMetodoPago(pago.getMetodo());
        pedidoRepo.save(pedido);
        comprobanteService.emitir(pedido, pago);
    }

    // Trae el pedido y valida que se pueda cobrar (debe existir y no estar ya pagado/cancelado).
    private Pedido validarPedido(Long pedidoId) {
        Pedido pedido = pedidoRepo.findById(pedidoId).orElseThrow();
        if ("PAGADO".equals(pedido.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El pedido ya está pagado");
        }
        if ("CANCELADO".equals(pedido.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El pedido está cancelado");
        }
        // Una venta ya anulada NO se puede volver a cobrar: al anularla se devolvió el dinero
        // y se repuso el stock, así que cobrarla de nuevo vendería un stock que ya volvió al
        // catálogo y dejaría la boleta anulada apuntando a un pedido "pagado". Si el cliente
        // se arrepiente de arrepentirse, hace una compra nueva.
        if ("ANULADO".equals(pedido.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El pedido está anulado");
        }
        return pedido;
    }

    // Algoritmo de Luhn: valida que el número de tarjeta sea matemáticamente consistente.
    private boolean luhn(String numero) {
        int suma = 0;
        boolean alternar = false;
        for (int i = numero.length() - 1; i >= 0; i--) {
            int digito = numero.charAt(i) - '0';
            if (alternar) {
                digito *= 2;
                if (digito > 9) digito -= 9;
            }
            suma += digito;
            alternar = !alternar;
        }
        return suma % 10 == 0;
    }

    // Genera un código de autorización aleatorio de 6 caracteres alfanuméricos en mayúsculas.
    private String generarCodigoAutorizacion() {
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(ALFANUMERICOS.charAt(random.nextInt(ALFANUMERICOS.length())));
        }
        return sb.toString();
    }
}
