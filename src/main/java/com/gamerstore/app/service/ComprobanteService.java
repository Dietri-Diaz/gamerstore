package com.gamerstore.app.service;

import com.gamerstore.app.dto.ResumenVentasDTO;
import com.gamerstore.app.model.Cliente;
import com.gamerstore.app.model.Comprobante;
import com.gamerstore.app.model.Pago;
import com.gamerstore.app.model.Pedido;
import com.gamerstore.app.repository.ComprobanteRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

/** Reglas de negocio de comprobantes: emisión de la boleta al aprobarse un pago y registro de ventas. */
@Service
public class ComprobanteService {

    private final ComprobanteRepository comprobanteRepo;
    private final String serie;
    private final double igvTasa;

    public ComprobanteService(ComprobanteRepository comprobanteRepo,
                              @Value("${app.comprobante.serie:B001}") String serie,
                              @Value("${app.comprobante.igv:0.18}") double igvTasa) {
        this.comprobanteRepo = comprobanteRepo;
        this.serie = serie;
        this.igvTasa = igvTasa;
    }

    /**
     * Emite la boleta de un pedido ya pagado. Es idempotente: si el pedido ya tiene
     * comprobante, lo devuelve tal cual en vez de emitir uno nuevo.
     */
    @Transactional
    public Comprobante emitir(Pedido pedido, Pago pago) {
        var existente = comprobanteRepo.findByPedidoId(pedido.getId());
        if (existente.isPresent()) {
            return existente.get();
        }

        // NOTA: en producción, el correlativo por serie debe calcularse con un bloqueo
        // (SELECT ... FOR UPDATE) o una secuencia dedicada, para que dos pagos aprobados
        // en simultáneo no puedan calcular el mismo "siguiente número". Aquí, al ser una
        // demo académica sin ese nivel de concurrencia, se calcula directo.
        int numero = comprobanteRepo.ultimoNumero(serie) + 1;

        // Los precios del catálogo YA incluyen IGV (18%), como en el retail peruano:
        // el total del pedido es el importe con IGV, y de ahí se desglosa la operación gravada.
        double total = pedido.getTotal();
        double subtotal = redondear(total / (1 + igvTasa));
        double igv = redondear(total - subtotal);

        Comprobante c = new Comprobante();
        c.setTipo("BOLETA");
        c.setSerie(serie);
        c.setNumero(numero);
        c.setPedido(pedido);

        Cliente cliente = pedido.getCliente();
        if (cliente != null) {
            c.setClienteNombre(cliente.getNombreCompleto());
            c.setClienteDni(cliente.getDni());
            c.setClienteDireccion(cliente.getDireccion());
        }

        c.setSubtotal(subtotal);
        c.setIgv(igv);
        c.setTotal(total);
        c.setMoneda("PEN");
        c.setMetodoPago(pago.getMetodo());
        c.setReferenciaPago(pago.getReferencia());
        c.setEstado("EMITIDO");

        return comprobanteRepo.save(c);
    }

    // Lista todas las boletas emitidas, de la más reciente a la más antigua (registro de ventas).
    public List<Comprobante> listar() {
        return comprobanteRepo.findAllByOrderByFechaEmisionDesc();
    }

    // Busca una boleta por id.
    public Comprobante porId(Long id) {
        return comprobanteRepo.findById(id).orElseThrow();
    }

    // Busca la boleta de un pedido puntual (para el detalle del pedido y la descarga del comprador).
    public Comprobante porPedido(Long pedidoId) {
        return comprobanteRepo.findByPedidoId(pedidoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Este pedido aún no tiene boleta"));
    }

    // Busca la boleta por el código del pedido (p. ej. "PED-0004"). Sin índice dedicado: al ser
    // pocas boletas por demo, se resuelve recorriendo el listado en memoria.
    public Comprobante porPedidoCodigo(String pedidoCodigo) {
        return listar().stream()
                .filter(c -> c.getPedido() != null && c.getPedido().getCodigo().equalsIgnoreCase(pedidoCodigo))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Este pedido aún no tiene boleta"));
    }

    /** Filtra las boletas emitidas por rango de fecha (inclusive), para el registro de ventas. */
    public List<Comprobante> filtrar(LocalDate desde, LocalDate hasta) {
        return listar().stream().filter(c -> {
            LocalDate f = c.getFechaEmision().toLocalDate();
            if (desde != null && f.isBefore(desde)) return false;
            if (hasta != null && f.isAfter(hasta)) return false;
            return true;
        }).toList();
    }

    // Suma cantidad, operación gravada, IGV y total de una lista de boletas (para el panel de ventas).
    public ResumenVentasDTO resumen(List<Comprobante> lista) {
        double subtotal = redondear(lista.stream().mapToDouble(Comprobante::getSubtotal).sum());
        double igv = redondear(lista.stream().mapToDouble(Comprobante::getIgv).sum());
        double total = redondear(lista.stream().mapToDouble(Comprobante::getTotal).sum());
        return new ResumenVentasDTO(lista.size(), subtotal, igv, total);
    }

    // Redondea a 2 decimales (soles y céntimos).
    private double redondear(double valor) {
        return Math.round(valor * 100) / 100.0;
    }
}
