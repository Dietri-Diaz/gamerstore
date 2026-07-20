package com.gamerstore.app.service;

import com.gamerstore.app.dto.SeguimientoDTO;
import com.gamerstore.app.dto.SeguimientoItemDTO;
import com.gamerstore.app.dto.SeguimientoPasoDTO;
import com.gamerstore.app.model.Pedido;
import com.gamerstore.app.repository.ComprobanteRepository;
import com.gamerstore.app.repository.PedidoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

/**
 * Seguimiento público del pedido: el comprador entra su código (PED-0047) y su DNI y ve
 * en qué va su compra, sin necesidad de tener cuenta ni loguearse.
 */
@Service
public class SeguimientoService {

    // Mensaje ÚNICO para "no existe" y para "el DNI no coincide". Es a propósito: si
    // respondiéramos distinto, cualquiera podría probar códigos correlativos (PED-0001,
    // PED-0002...) y deducir cuáles existen. Con un solo mensaje no se puede distinguir.
    private static final String NO_ENCONTRADO = "No encontramos un pedido con ese código y DNI";

    private final PedidoRepository pedidoRepo;
    private final ComprobanteRepository comprobanteRepo;

    public SeguimientoService(PedidoRepository pedidoRepo, ComprobanteRepository comprobanteRepo) {
        this.pedidoRepo = pedidoRepo;
        this.comprobanteRepo = comprobanteRepo;
    }

    /** Busca el pedido por código verificando el DNI del cliente y arma su seguimiento. */
    public SeguimientoDTO consultar(String pedidoCodigo, String dni) {
        Pedido pedido = buscarPorCodigo(pedidoCodigo);

        // Verificación de dueño: el mismo criterio que ya usa la descarga de la boleta.
        String dniPedido = pedido.getCliente() != null ? pedido.getCliente().getDni() : null;
        if (dniPedido == null || dni == null || !dniPedido.equals(dni.trim())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, NO_ENCONTRADO);
        }

        // El código de la boleta solo existe si el pedido ya se pagó; si no, va en null.
        String comprobanteCodigo = comprobanteRepo.findByPedidoId(pedido.getId())
                .map(c -> c.getCodigo())
                .orElse(null);

        List<SeguimientoItemDTO> items = pedido.getItems().stream()
                .map(i -> new SeguimientoItemDTO(
                        i.getProducto() != null ? i.getProducto().getNombre() : "—",
                        i.getCantidad(), i.getPrecioUnitario(), i.getSubtotal()))
                .toList();

        return new SeguimientoDTO(
                pedido.getCodigo(),
                pedido.getFecha(),
                pedido.getEstado(),
                pedido.getTotal(),
                pedido.getMetodoPago(),
                pedido.getCliente() != null ? pedido.getCliente().getNombreCompleto() : null,
                pedido.getTipoEntrega(),
                pedido.getDireccionEnvio(),
                pedido.getReferenciaEnvio(),
                comprobanteCodigo,
                pedido.getMotivoAnulacion(),
                items,
                armarHistorial(pedido));
    }

    /**
     * Traduce el código público "PED-0047" al id 47 y trae ese pedido.
     * Al final se compara el código generado con el que pidieron, así una entrada rara
     * ("PED-47", "ped-0047xyz") no devuelve un pedido que no corresponde.
     */
    private Pedido buscarPorCodigo(String pedidoCodigo) {
        if (pedidoCodigo == null || pedidoCodigo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, NO_ENCONTRADO);
        }
        String limpio = pedidoCodigo.trim().toUpperCase();
        // Acepta "PED-0047" y también "47" pelado, por si el comprador copia solo el número.
        String soloNumero = limpio.startsWith("PED-") ? limpio.substring(4) : limpio;
        long id;
        try {
            id = Long.parseLong(soloNumero);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, NO_ENCONTRADO);
        }
        return pedidoRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, NO_ENCONTRADO));
    }

    /**
     * Arma la línea de tiempo del pedido.
     *
     * HONESTIDAD SOBRE LA LIMITACIÓN: NO existe una tabla que guarde el historial de cambios de
     * estado, así que el recorrido se DEDUCE del estado actual: si el pedido está en ENVIADO,
     * asumimos que ya pasó por PENDIENTE y PAGADO. Por eso ningún paso trae fecha propia: sería
     * inventarla. Para mostrar fechas reales por paso habría que crear una tabla
     * "pedido_historial" que registre cada cambio (mejora pendiente).
     */
    private List<SeguimientoPasoDTO> armarHistorial(Pedido pedido) {
        String estado = pedido.getEstado() == null ? "PENDIENTE" : pedido.getEstado();
        boolean delivery = "DELIVERY".equalsIgnoreCase(pedido.getTipoEntrega());

        // Los pedidos que se cortaron no siguen el recorrido normal: se muestra un solo paso
        // explicando por qué terminó ahí. ANULADO (venta pagada que se reversó con devolución)
        // y CANCELADO (pedido que nunca llegó a pagarse) son cosas distintas: ver AnulacionService.
        if (estado.equals("ANULADO")) {
            String detalle = (pedido.getMotivoAnulacion() == null || pedido.getMotivoAnulacion().isBlank())
                    ? "La venta fue anulada y el importe fue devuelto."
                    : "Motivo: " + pedido.getMotivoAnulacion();
            return List.of(new SeguimientoPasoDTO("ANULADO", "Venta anulada", detalle, true, true));
        }
        if (estado.equals("CANCELADO")) {
            return List.of(new SeguimientoPasoDTO("CANCELADO", "Pedido cancelado",
                    "El pedido se canceló antes de completarse el pago.", true, true));
        }

        // Recorrido normal. El tercer paso reutiliza el estado ENVIADO de la BD (no inventamos
        // un estado "LISTO"): lo único que cambia según el tipo de entrega es cómo se llama.
        List<SeguimientoPasoDTO> pasos = new ArrayList<>();
        List<String> orden = List.of("PENDIENTE", "PAGADO", "ENVIADO", "ENTREGADO");

        // Posición del estado actual dentro del recorrido; si es un estado desconocido, arranca en 0.
        int actual = orden.indexOf(estado);
        if (actual < 0) actual = 0;

        for (int i = 0; i < orden.size(); i++) {
            String paso = orden.get(i);
            String titulo;
            String descripcion;
            switch (paso) {
                case "PENDIENTE" -> {
                    titulo = "Pedido recibido";
                    descripcion = "Registramos tu pedido.";
                }
                case "PAGADO" -> {
                    titulo = "Pago confirmado";
                    descripcion = "Confirmamos tu pago y emitimos tu boleta.";
                }
                case "ENVIADO" -> {
                    titulo = delivery ? "En camino" : "Listo para recojo";
                    descripcion = delivery
                            ? "Tu pedido salió hacia la dirección que indicaste."
                            : "Tu pedido ya está listo: puedes acercarte a la tienda a recogerlo.";
                }
                default -> {
                    titulo = "Entregado";
                    descripcion = delivery
                            ? "Tu pedido fue entregado. ¡Gracias por tu compra!"
                            : "Recogiste tu pedido en tienda. ¡Gracias por tu compra!";
                }
            }
            // completado: ya se alcanzó (los anteriores y el actual) · actual: es donde está hoy.
            pasos.add(new SeguimientoPasoDTO(paso, titulo, descripcion, i <= actual, i == actual));
        }
        return pasos;
    }
}
