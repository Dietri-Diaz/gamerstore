package com.gamerstore.app.mapper;

import com.gamerstore.app.dto.PagoDTO;
import com.gamerstore.app.model.Pago;
import org.springframework.stereotype.Component;

/** Mapper: convierte el Pago (entidad) a su DTO. */
@Component
public class PagoMapper {

    // Convierte un Pago en PagoDTO, resolviendo el código del pedido y el nombre del cliente.
    public PagoDTO toDTO(Pago p) {
        String pedidoCodigo = p.getPedido() != null ? p.getPedido().getCodigo() : null;
        String clienteNombre = (p.getPedido() != null && p.getPedido().getCliente() != null)
                ? p.getPedido().getCliente().getNombreCompleto()
                : null;
        return new PagoDTO(
                p.getId(),
                p.getCodigo(),
                p.getPedido() != null ? p.getPedido().getId() : null,
                pedidoCodigo,
                clienteNombre,
                p.getMetodo(),
                p.getMonto(),
                p.getEstado(),
                p.getReferencia(),
                p.getTarjetaUlt4(),
                p.getTitular(),
                p.getVoucher(),
                p.getFecha()
        );
    }
}
