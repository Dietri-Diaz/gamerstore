package com.gamerstore.app.controller;

import com.gamerstore.app.dto.ReniecPersona;
import com.gamerstore.app.service.ReniecService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Consulta pública de RENIEC (para autocompletar el DNI en el checkout de la tienda). */
@RestController
@RequestMapping("/api/reniec")
public class ReniecController {

    private final ReniecService reniecService;

    public ReniecController(ReniecService reniecService) {
        this.reniecService = reniecService;
    }

    // GET /api/reniec/{dni}: datos reales de RENIEC para autocompletar el formulario de compra.
    @GetMapping("/{dni}")
    public ReniecPersona porDni(@PathVariable String dni) {
        return reniecService.consultarDni(dni)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No se encontró el DNI o el servicio no está disponible"));
    }
}
