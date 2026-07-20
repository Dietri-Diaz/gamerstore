package com.gamerstore.app.service;

import com.gamerstore.app.dto.PedidoItemRequest;
import com.gamerstore.app.model.Cliente;
import com.gamerstore.app.model.Pedido;
import com.gamerstore.app.model.PedidoItem;
import com.gamerstore.app.model.Producto;
import com.gamerstore.app.repository.ClienteRepository;
import com.gamerstore.app.repository.PedidoRepository;
import com.gamerstore.app.repository.ProductoRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Reglas de negocio de pedidos: alta con cálculo de total e ítems, reportes y estadísticas de ventas. */
@Service
public class PedidoService {

    private final PedidoRepository pedidoRepo;
    private final ClienteRepository clienteRepo;
    private final ProductoRepository productoRepo;

    // Inyecta los repositorios de pedido, cliente y producto.
    public PedidoService(PedidoRepository pedidoRepo,
                         ClienteRepository clienteRepo,
                         ProductoRepository productoRepo) {
        this.pedidoRepo = pedidoRepo;
        this.clienteRepo = clienteRepo;
        this.productoRepo = productoRepo;
    }

    // Lista todos los pedidos, del más reciente al más antiguo.
    public List<Pedido> todos() {
        return pedidoRepo.findAllByOrderByFechaDesc();
    }

    // Busca un pedido por id.
    public Optional<Pedido> porId(Long id) {
        return pedidoRepo.findById(id);
    }

    // Cuenta el total de pedidos.
    public long total() {
        return pedidoRepo.count();
    }

    // Suma el total vendido (campo total) de todos los pedidos.
    public double totalVentas() {
        return pedidoRepo.sumTotal();
    }

    // Trae los productos más vendidos por cantidad, limitado a "limite" resultados.
    public List<Object[]> topProductos(int limite) {
        return pedidoRepo.topProductos(PageRequest.of(0, limite));
    }

    /** Filtra pedidos por rango de fecha (inclusive) y estado, para el reporte. */
    public List<Pedido> reporte(LocalDate desde, LocalDate hasta, String estado) {
        return todos().stream().filter(p -> {
            LocalDate f = p.getFecha().toLocalDate();
            if (desde != null && f.isBefore(desde)) return false;
            if (hasta != null && f.isAfter(hasta)) return false;
            if (estado != null && !estado.isBlank() && !estado.equalsIgnoreCase(p.getEstado())) return false;
            return true;
        }).toList();
    }

    /**
     * Registra un pedido: valida los datos de entrega, arma sus lineas, valida y descuenta
     * stock, calcula el total y lo guarda (cascade).
     *
     * tipoEntrega/direccionEnvio/referenciaEnvio pueden venir en null (venta de mostrador
     * cargada desde el panel): en ese caso el pedido queda como RECOJO_TIENDA.
     */
    @Transactional
    public Pedido crear(Long clienteId, String metodoPago, List<PedidoItemRequest> items,
                        String tipoEntrega, String direccionEnvio, String referenciaEnvio) {
        // Solo TARJETA o YAPE: EFECTIVO/PLIN/TRANSFERENCIA eran del sistema anterior
        // (venta cotizada por WhatsApp, sin pago ni boleta) y ya no aplican.
        if (metodoPago != null && !metodoPago.isBlank()
                && !metodoPago.equalsIgnoreCase("TARJETA") && !metodoPago.equalsIgnoreCase("YAPE")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Método de pago no válido. Solo se acepta TARJETA o YAPE.");
        }

        Cliente cliente = clienteRepo.findById(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));

        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setMetodoPago(metodoPago);
        pedido.setEstado("PENDIENTE");
        aplicarEntrega(pedido, tipoEntrega, direccionEnvio, referenciaEnvio);

        double total = 0;
        for (PedidoItemRequest it : items) {
            Producto prod = productoRepo.findById(it.productoId())
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));

            // Valida stock real (misma regla para el POS del admin y el checkout publico).
            if (prod.getStock() < it.cantidad()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Stock insuficiente para " + prod.getNombre() + " (quedan " + prod.getStock() + ")");
            }

            PedidoItem item = new PedidoItem(pedido, prod, it.cantidad(), prod.getPrecio());
            pedido.getItems().add(item);
            total += item.getSubtotal();

            // Descuenta el stock vendido.
            prod.setStock(prod.getStock() - it.cantidad());
            productoRepo.save(prod);
        }
        pedido.setTotal(total);
        return pedidoRepo.save(pedido);
    }

    /**
     * Valida y copia al pedido los datos de entrega.
     *
     * Esta validacion vive en el SERVIDOR a proposito: el front oculta el campo de direccion
     * cuando el comprador elige recojo en tienda, pero cualquiera puede mandar el JSON a mano,
     * asi que la regla "delivery exige direccion" se comprueba igual aca.
     */
    private void aplicarEntrega(Pedido pedido, String tipoEntrega, String direccion, String referencia) {
        // Sin dato -> recojo en tienda (es la opcion que no necesita direccion).
        String tipo = (tipoEntrega == null || tipoEntrega.isBlank())
                ? "RECOJO_TIENDA"
                : tipoEntrega.trim().toUpperCase();

        if (!tipo.equals("RECOJO_TIENDA") && !tipo.equals("DELIVERY")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de entrega inválido");
        }

        if (tipo.equals("DELIVERY")) {
            // Pedimos un minimo de 10 caracteres: una direccion util para repartir no cabe
            // en menos (calle + numero + distrito). Evita el clasico "asd" para pasar el campo.
            String dir = direccion == null ? "" : direccion.trim();
            if (dir.length() < 10) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La dirección de envío es obligatoria para delivery");
            }
            pedido.setDireccionEnvio(dir);
            pedido.setReferenciaEnvio((referencia == null || referencia.isBlank()) ? null : referencia.trim());
        } else {
            // Recojo en tienda: la direccion y la referencia no aplican, se guardan en null
            // aunque el front las haya mandado (asi no queda un dato de envio que confunda).
            pedido.setDireccionEnvio(null);
            pedido.setReferenciaEnvio(null);
        }

        pedido.setTipoEntrega(tipo);
    }

    /** Edita un pedido: cambia su estado y, opcionalmente, el metodo de pago. */
    @Transactional
    public Pedido actualizar(Long id, String estado, String metodoPago) {
        Pedido p = pedidoRepo.findById(id).orElseThrow();
        if (estado != null && !estado.isBlank()) p.setEstado(estado);
        if (metodoPago != null) p.setMetodoPago(metodoPago);
        return pedidoRepo.save(p);
    }

    // Elimina un pedido por id.
    @Transactional
    public void eliminar(Long id) {
        pedidoRepo.deleteById(id);
    }
}
