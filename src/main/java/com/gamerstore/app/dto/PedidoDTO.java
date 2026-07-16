package com.gamerstore.app.dto;

import java.time.LocalDateTime;
import java.util.List;

// Datos de un pedido (con sus líneas) para mostrar en el front
public record PedidoDTO(Long id, String codigo, Long clienteId, String clienteNombre,
                        LocalDateTime fecha, String estado, double total, String metodoPago,
                        int cantidadTotal, List<PedidoItemDTO> items) {}
