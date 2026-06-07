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

import java.util.List;
import java.util.Optional;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepo;
    private final ClienteRepository clienteRepo;
    private final ProductoRepository productoRepo;

    public PedidoService(PedidoRepository pedidoRepo,
                         ClienteRepository clienteRepo,
                         ProductoRepository productoRepo) {
        this.pedidoRepo = pedidoRepo;
        this.clienteRepo = clienteRepo;
        this.productoRepo = productoRepo;
    }

    public List<Pedido> todos() {
        return pedidoRepo.findAllByOrderByFechaDesc();
    }

    public Optional<Pedido> porId(Long id) {
        return pedidoRepo.findById(id);
    }

    public long total() {
        return pedidoRepo.count();
    }

    public double totalVentas() {
        return pedidoRepo.sumTotal();
    }

    public List<Object[]> topProductos(int limite) {
        return pedidoRepo.topProductos(PageRequest.of(0, limite));
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

    @Transactional
    public void eliminar(Long id) {
        pedidoRepo.deleteById(id);
    }
}
