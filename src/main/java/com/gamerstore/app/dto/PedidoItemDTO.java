package com.gamerstore.app.dto;

public record PedidoItemDTO(Long id, Long productoId, String productoNombre,
                            int cantidad, double precioUnitario, double subtotal) {}
