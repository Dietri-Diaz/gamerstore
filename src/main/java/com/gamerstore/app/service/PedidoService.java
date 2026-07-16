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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /** Registra un pedido: arma sus lineas, calcula el total y lo guarda (cascade). */
    @Transactional
    public Pedido crear(Long clienteId, String metodoPago, List<PedidoItemRequest> items) {
        Cliente cliente = clienteRepo.findById(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));

        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setMetodoPago(metodoPago);
        pedido.setEstado("PENDIENTE");

        double total = 0;
        for (PedidoItemRequest it : items) {
            Producto prod = productoRepo.findById(it.productoId())
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
            PedidoItem item = new PedidoItem(pedido, prod, it.cantidad(), prod.getPrecio());
            pedido.getItems().add(item);
            total += item.getSubtotal();
        }
        pedido.setTotal(total);
        return pedidoRepo.save(pedido);
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
