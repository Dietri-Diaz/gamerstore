package com.gamerstore.app.controller;

import com.gamerstore.app.dto.ConfigDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

    @Value("${app.whatsapp.numero:51986969024}")
    private String whatsappNumero;

    @Value("${app.tienda.nombre:GamerStore}")
    private String tiendaNombre;

    @GetMapping
    public ConfigDTO config() {
        return new ConfigDTO(tiendaNombre, whatsappNumero);
    }
}
