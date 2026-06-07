package com.gamerstore.app.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PedidoDTO(Long id, String codigo, Long clienteId, String clienteNombre,
                        LocalDateTime fecha, String estado, double total, String metodoPago,
                        int cantidadTotal, List<PedidoItemDTO> items) {}
