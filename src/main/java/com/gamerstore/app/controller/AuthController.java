package com.gamerstore.app.controller;

import com.gamerstore.app.dto.LoginRequest;
import com.gamerstore.app.dto.LoginResponse;
import com.gamerstore.app.model.Usuario;
import com.gamerstore.app.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Login del panel admin. Valida usuario/clave (BCrypt) y devuelve los datos del
 * administrador. La proteccion real con Spring Security se agregara en el avance final.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        Usuario u = usuarioService.autenticar(req.username(), req.password())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos"));
        return new LoginResponse(u.getUsername(), u.getNombre(), u.getRol().name());
    }
}
