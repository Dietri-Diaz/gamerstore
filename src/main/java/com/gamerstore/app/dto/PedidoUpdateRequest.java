package com.gamerstore.app.dto;

import jakarta.validation.constraints.NotBlank;

/** Datos para editar un pedido: su estado y, opcionalmente, el método de pago. */
public record PedidoUpdateRequest(
        @NotBlank(message = "El estado es obligatorio")
        String estado,

        String metodoPago) {}
