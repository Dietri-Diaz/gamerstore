package com.gamerstore.app.controller;

import com.gamerstore.app.dto.LoginRequest;
import com.gamerstore.app.dto.LoginResponse;
import com.gamerstore.app.model.Usuario;
import com.gamerstore.app.security.JwtService;
import com.gamerstore.app.service.UsuarioService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioService usuarioService;
    private final JwtService jwtService;

    public AuthController(UsuarioService usuarioService, JwtService jwtService) {
        this.usuarioService = usuarioService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest req) {
        Usuario u = usuarioService.autenticar(req.username(), req.password())
                .orElseThrow(() -> new BadCredentialsException("Usuario o contraseña incorrectos"));
        String token = jwtService.generar(u);
        return new LoginResponse(token, u.getUsername(), u.getNombre(), u.getRol().name());
    }

    /** Devuelve los datos del admin del token actual (sirve para validar la sesion al recargar). */
    @GetMapping("/me")
    public LoginResponse me(Authentication auth) {
        Usuario u = usuarioService.porUsername(auth.getName())
                .orElseThrow(() -> new BadCredentialsException("Sesión inválida"));
        return new LoginResponse(null, u.getUsername(), u.getNombre(), u.getRol().name());
    }
}
