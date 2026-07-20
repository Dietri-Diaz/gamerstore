package com.gamerstore.app.dto;

import jakarta.validation.constraints.NotBlank;

/** Datos del pago elegido en el checkout: metodo = "YAPE" o "TARJETA". */
public record CheckoutPagoDTO(
        @NotBlank String metodo,
        String numero, String titular, String vencimiento, String cvv,   // tarjeta (simulada)
        String numeroOperacion, String voucher,                          // yape
        String paymentMethodId) {}                                        // tarjeta real (Stripe)
