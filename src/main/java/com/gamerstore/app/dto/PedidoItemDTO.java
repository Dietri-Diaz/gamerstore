package com.gamerstore.app.dto;

// Datos de una línea de pedido para mostrar en el front
public record PedidoItemDTO(Long id, Long productoId, String productoNombre,
                            int cantidad, double precioUnitario, double subtotal) {}
