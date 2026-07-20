package com.gamerstore.app.controller;

import com.gamerstore.app.dto.ConfigDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Expone configuración pública del front (nombre de la tienda y número de WhatsApp), leída de application.properties. */
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    @Value("${app.whatsapp.numero:51986969024}")
    private String whatsappNumero;

    @Value("${app.tienda.nombre:GamerStore}")
    private String tiendaNombre;

    // Datos de Yape: los necesita el checkout de la tienda pública para mostrar el QR y el número.
    @Value("${app.yape.numero:}")
    private String yapeNumero;

    @Value("${app.yape.titular:}")
    private String yapeTitular;

    @Value("${app.yape.qr:}")
    private String yapeQr;

    // Yape (la app) tiene tope de monto: el checkout lo usa para bloquear esa opción si el total lo supera.
    @Value("${app.yape.monto-maximo:500}")
    private double yapeMontoMaximo;

    // Stripe (pasarela real): el checkout necesita saber si está activo y su clave PÚBLICA para tokenizar
    // la tarjeta en el navegador. La clave secreta NUNCA se expone aquí.
    @Value("${app.stripe.enabled:false}")
    private boolean stripeEnabled;

    @Value("${app.stripe.public-key:}")
    private String stripePublicKey;

    // GET /api/config: nombre de la tienda, WhatsApp, datos de Yape y clave pública de Stripe
    // (todo público, viene de application.properties).
    @GetMapping
    public ConfigDTO config() {
        return new ConfigDTO(tiendaNombre, whatsappNumero, yapeNumero, yapeTitular, yapeQr, yapeMontoMaximo,
                stripeEnabled, stripePublicKey);
    }
}
