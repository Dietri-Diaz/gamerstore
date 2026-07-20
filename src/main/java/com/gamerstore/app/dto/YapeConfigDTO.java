package com.gamerstore.app.dto;

// Configuración pública de la cuenta Yape a la que se debe transferir (número, titular, QR y tope de
// monto) + estado de Stripe y su clave PÚBLICA (nunca la secreta)
public record YapeConfigDTO(String numero, String titular, String qr, double montoMaximo,
                            boolean stripeEnabled, String stripePublicKey) {}
