package com.gamerstore.app.dto;

// Una línea del pedido, vista desde el seguimiento público. Es más chica que PedidoItemDTO
// a propósito: al comprador solo le mostramos qué compró, no los ids internos del sistema.
public record SeguimientoItemDTO(String producto, int cantidad, double precioUnitario, double subtotal) {}
