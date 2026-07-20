package com.gamerstore.app.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Datos para pagar un pedido con tarjeta. Vencimiento en formato "MM/AA".
 * numero/titular/vencimiento/cvv son de la pasarela SIMULADA (Luhn); paymentMethodId es el
 * token que entrega Stripe cuando la pasarela real está activa. Son opcionales porque cada
 * flujo usa un subconjunto distinto: la validación real la hace PagoService según el caso.
 */
public record PagoTarjetaRequest(
        @NotNull(message = "El pedido es obligatorio")
        Long pedidoId,

        String numero,
        String titular,
        String vencimiento,
        String cvv,
        String paymentMethodId) {}
