package com.gamerstore.app.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

// Configuracion central de Spring Security: define que rutas son publicas, cuales
// requieren estar logueado, cuales requieren rol ADMIN, y conecta el filtro JWT
// (JwtAuthenticationFilter) al pipeline de seguridad. Al ser una API stateless
// (sin sesiones de servidor), toda la autenticacion se resuelve por request via token.
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    // Define las reglas de autorizacion de la API y como se manejan los errores
    // de autenticacion/autorizacion.
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        http
            // CSRF no aplica: es una API stateless consumida con tokens, no con cookies/forms.
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // Sin sesiones HTTP: cada request se autentica desde cero con el JWT (ver filtro).
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Reglas de acceso evaluadas en orden: login/refresh y catalogo (GET) son
            // publicos, /api/admin/** solo para rol ADMIN, el resto de /api/** requiere
            // estar autenticado, y cualquier otra ruta (la SPA de React) queda abierta.
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login", "/api/auth/refresh").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/productos/**", "/api/categorias/**", "/api/config/**").permitAll()
                .requestMatchers("/images/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            // Respuestas JSON (en vez del HTML por defecto de Spring) cuando falta
            // autenticacion (401) o el usuario no tiene el rol requerido (403).
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) -> {
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    res.getWriter().write(mapper.writeValueAsString(Map.of("error", "No autenticado")));
                })
                .accessDeniedHandler((req, res, e) -> {
                    res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    res.getWriter().write(mapper.writeValueAsString(Map.of("error", "No tienes permisos")));
                })
            )
            // El filtro JWT corre antes que el filtro estandar de usuario/contrasena,
            // asi el SecurityContext ya tiene la autenticacion (si el token es valido)
            // cuando se evaluan las reglas de arriba.
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // Configura CORS a nivel global: solo los origenes listados en
    // app.cors.allowed-origins pueden llamar a la API, con los metodos y headers
    // necesarios para el front (incluye exponer Content-Disposition para descargas).
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setExposedHeaders(List.of("Content-Disposition"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    // Expone el AuthenticationManager de Spring (usado en el login para validar
    // usuario/contrasena) como bean inyectable en los controllers de auth.
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }
}
