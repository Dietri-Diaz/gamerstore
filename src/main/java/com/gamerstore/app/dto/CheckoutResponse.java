package com.gamerstore.app.dto;

/** Confirmación que se muestra al comprador al terminar la compra. */
public record CheckoutResponse(String pedidoCodigo, String pagoCodigo, String estado,
                               String metodo, String referencia, double total, String clienteNombre,
                               String comprobanteCodigo) {}
