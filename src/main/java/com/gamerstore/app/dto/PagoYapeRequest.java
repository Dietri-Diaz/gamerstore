package com.gamerstore.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Datos para pagar un pedido con Yape. El voucher (ruta de la captura) es opcional. */
public record PagoYapeRequest(
        @NotNull(message = "El pedido es obligatorio")
        Long pedidoId,

        @NotBlank(message = "El número de operación es obligatorio")
        String numeroOperacion,

        String voucher) {}
