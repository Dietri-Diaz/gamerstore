package com.gamerstore.app.controller;

import com.gamerstore.app.config.security.JwtService;
import com.gamerstore.app.config.security.RefreshTokenService;
import com.gamerstore.app.dto.AuthUser;
import com.gamerstore.app.dto.LoginRequest;
import com.gamerstore.app.dto.LoginResponse;
import com.gamerstore.app.dto.RefreshRequest;
import com.gamerstore.app.dto.TokenResponse;
import com.gamerstore.app.model.RefreshToken;
import com.gamerstore.app.model.Usuario;
import com.gamerstore.app.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Login del panel admin con JWT (access de 5 min) + refresh tokens persistidos y rotados. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authManager;
    private final UsuarioService usuarioService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    // Inyecta el AuthenticationManager de Spring Security y los servicios de usuarios y de tokens (JWT + refresh).
    public AuthController(AuthenticationManager authManager, UsuarioService usuarioService,
                          JwtService jwtService, RefreshTokenService refreshTokenService) {
        this.authManager = authManager;
        this.usuarioService = usuarioService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    // POST /api/auth/login: valida usuario/contraseña con Spring Security y, si son correctos,
    // genera un access token JWT (5 min) y crea un refresh token nuevo persistido en BD.
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        authManager.authenticate(new UsernamePasswordAuthenticationToken(req.username(), req.password()));
        Usuario u = usuarioService.buscar(req.username())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos"));
        String access = jwtService.generarAccess(u);
        RefreshToken rt = refreshTokenService.crear(u);
        return new LoginResponse(access, rt.getToken(), u.getUsername(), u.getNombre(), u.getRol().name());
    }

    // POST /api/auth/refresh: rota el refresh token recibido (invalida el anterior y entrega uno nuevo)
    // y emite un access token fresco, sin pedir credenciales de nuevo.
    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest req) {
        RefreshToken rotado = refreshTokenService.rotar(req.refreshToken());
        String access = jwtService.generarAccess(rotado.getUsuario());
        return new TokenResponse(access, rotado.getToken());
    }

    // POST /api/auth/logout: revoca (invalida) el refresh token recibido para cerrar la sesión.
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest req) {
        refreshTokenService.revocar(req.refreshToken());
        return ResponseEntity.noContent().build();
    }

    // GET /api/auth/me: usa el usuario ya autenticado por el filtro JWT para devolver sus datos básicos.
    @GetMapping("/me")
    public AuthUser me(Authentication auth) {
        Usuario u = usuarioService.buscar(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión inválida"));
        return new AuthUser(u.getUsername(), u.getNombre(), u.getRol().name());
    }
}
