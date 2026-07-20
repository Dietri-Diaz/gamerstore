package com.gamerstore.app.dto;

// Configuración general de la tienda (nombre, WhatsApp) + datos de Yape y la clave PÚBLICA de
// Stripe (nunca la secreta) para el pago en la tienda pública
public record ConfigDTO(String tiendaNombre, String whatsappNumero,
                        String yapeNumero, String yapeTitular, String yapeQr, double yapeMontoMaximo,
                        boolean stripeEnabled, String stripePublicKey) {}
