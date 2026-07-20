package com.gamerstore.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Motivo por el que el admin anula una venta.
 *
 * Es obligatorio y con un mínimo de caracteres a propósito: la anulación devuelve dinero real
 * y repone stock, así que tiene que quedar registrado POR QUÉ se hizo (sale impreso en la
 * boleta anulada). Un "x" no explica nada a quien revise el registro de ventas después.
 */
public record AnularPedidoRequest(
        @NotBlank(message = "El motivo de la anulación es obligatorio")
        @Size(min = 5, max = 200, message = "El motivo debe tener entre 5 y 200 caracteres")
        String motivo) {}
