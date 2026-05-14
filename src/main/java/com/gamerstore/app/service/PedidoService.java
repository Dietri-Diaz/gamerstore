package com.gamerstore.app.service;

import com.gamerstore.app.model.Pedido;
import com.gamerstore.app.repository.PedidoRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepo;

    public PedidoService(PedidoRepository pedidoRepo) {
        this.pedidoRepo = pedidoRepo;
    }

    public List<Pedido> todos() {
        return pedidoRepo.findAllByOrderByFechaDesc();
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
}
