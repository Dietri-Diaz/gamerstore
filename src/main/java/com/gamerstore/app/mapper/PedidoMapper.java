package com.gamerstore.app.mapper;

import com.gamerstore.app.dto.PedidoDTO;
import com.gamerstore.app.dto.PedidoItemDTO;
import com.gamerstore.app.model.Pedido;
import com.gamerstore.app.model.PedidoItem;
import org.springframework.stereotype.Component;

import java.util.List;

/** Mapper: convierte el Pedido (y sus líneas) a sus DTOs. */
@Component
public class PedidoMapper {

    public PedidoDTO toDTO(Pedido p) {
        List<PedidoItemDTO> items = p.getItems().stream().map(this::toItemDTO).toList();
        return new PedidoDTO(
                p.getId(),
                p.getCodigo(),
                p.getCliente() != null ? p.getCliente().getId() : null,
                p.getCliente() != null ? p.getCliente().getNombreCompleto() : null,
                p.getFecha(),
                p.getEstado(),
                p.getTotal(),
                p.getMetodoPago(),
                p.getCantidadTotal(),
                items
        );
    }

    public PedidoItemDTO toItemDTO(PedidoItem i) {
        return new PedidoItemDTO(
                i.getId(),
                i.getProducto() != null ? i.getProducto().getId() : null,
                i.getProducto() != null ? i.getProducto().getNombre() : null,
                i.getCantidad(),
                i.getPrecioUnitario(),
                i.getSubtotal()
        );
    }
}
