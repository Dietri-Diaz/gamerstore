package com.gamerstore.app.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// Filtro que se ejecuta UNA vez por request (OncePerRequestFilter), antes de que
// Spring Security decida si autoriza el acceso. Se registra en SecurityConfig
// justo antes del filtro estandar de usuario/contrasena, y es el que realmente
// autentica al usuario a partir del JWT en cada llamada (la API es stateless,
// no hay sesion, asi que esto se repite en cada request).
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, CustomUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    // Flujo de validacion del token en cada request:
    // 1) Busca el header "Authorization: Bearer <token>"; si no viene, sigue sin autenticar.
    // 2) Extrae el username del token (sin validar firma todavia) y carga el usuario real.
    // 3) Confirma que el token sea valido para ESE usuario (firma correcta y no expirado).
    // 4) Si todo calza, arma un Authentication y lo deja en el SecurityContext para que
    //    el resto del pipeline (y los controllers) sepan quien esta autenticado.
    // 5) Cualquier error (token invalido/expirado) se ignora y la request sigue sin
    //    autenticar; SecurityConfig es quien luego responde 401/403 segun la ruta.
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                String username = jwtService.extraerUsername(token);
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    if (jwtService.esValido(token, userDetails.getUsername())) {
                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            } catch (Exception ignored) {
                // Token inválido/expirado: se sigue sin autenticar → 401 en el entrypoint.
            }
        }
        // Continua la cadena de filtros (autenticado o no); la decision de permitir
        // o rechazar la ruta la toman las reglas de SecurityConfig.
        chain.doFilter(request, response);
    }
}
