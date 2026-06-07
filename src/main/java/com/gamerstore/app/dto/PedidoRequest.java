package com.gamerstore.app.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Datos para registrar un pedido (cliente + método de pago + líneas). */
public record PedidoRequest(
        @NotNull(message = "El cliente es obligatorio")
        Long clienteId,

        String metodoPago,

        @NotEmpty(message = "El pedido debe tener al menos un producto")
        @Valid
        List<PedidoItemRequest> items) {}
