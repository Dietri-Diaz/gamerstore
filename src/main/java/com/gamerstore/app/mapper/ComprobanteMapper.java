package com.gamerstore.app.mapper;

import com.gamerstore.app.dto.ComprobanteDTO;
import com.gamerstore.app.model.Comprobante;
import org.springframework.stereotype.Component;

/** Mapper: convierte el Comprobante (entidad) a su DTO. */
@Component
public class ComprobanteMapper {

    // Convierte un Comprobante en ComprobanteDTO, resolviendo el código del pedido.
    public ComprobanteDTO toDTO(Comprobante c) {
        String pedidoCodigo = c.getPedido() != null ? c.getPedido().getCodigo() : null;
        return new ComprobanteDTO(
                c.getId(),
                c.getCodigo(),
                c.getTipo(),
                c.getSerie(),
                c.getNumero(),
                c.getPedido() != null ? c.getPedido().getId() : null,
                pedidoCodigo,
                c.getClienteNombre(),
                c.getClienteDni(),
                c.getSubtotal(),
                c.getIgv(),
                c.getTotal(),
                c.getMoneda(),
                c.getMetodoPago(),
                c.getReferenciaPago(),
                c.getEstado(),
                c.getFechaEmision()
        );
    }
}
