# GamerStore — Completar proyecto (JWT + validación + data + reporte) — Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Dejar GamerStore 100% funcional para la presentación final: Spring Security con JWT + refresh tokens (access 5 min, contador discreto), validación de campos únicos con toast, módulo de Usuarios, subida de imágenes al proyecto, Top productos en Dashboard, reporte PDF de pedidos, y BD limpia con data real de tecnología y pedidos históricos.

**Architecture:** Backend Spring Boot 3.5.6 (capas controller/service/repository/model/dto/mapper) gana un paquete `config/security` con filtro JWT stateless + refresh tokens persistidos y rotados. Frontend React/Vite refuerza `client.js` con Authorization + refresh silencioso. Imágenes en `uploads/productos/` del proyecto (nunca en BD), servidas en `/images/productos/**`.

**Tech Stack:** Spring Security, jjwt 0.12.6, OpenPDF 1.3.35, Apache no; React 18, Vite 5, fetch. MariaDB (XAMPP). Java 17 (corre en JDK 21).

**Referencia:** spec en `docs/superpowers/specs/2026-07-11-gamerstore-final-completion-design.md`.

**Rutas base (relativas a `gamerstore-main/`):**
- Backend java: `src/main/java/com/gamerstore/app/`
- Frontend: `frontend/src/`

**Nota de commits:** el usuario commitea sólo cuando lo pide y sin co-autor. Los pasos "Commit" son opcionales/agrupables; ejecutarlos sólo si el usuario lo pide.

---

## Fase 0 — Dependencias y configuración

### Task 0.1: Añadir dependencias (Security, JWT, OpenPDF, security-test)

**Files:**
- Modify: `pom.xml` (bloque `<dependencies>`)

- [ ] **Step 1: Añadir dependencias** dentro de `<dependencies>` (antes del cierre `</dependencies>`), junto a las existentes:

```xml
        <!-- Seguridad real: Spring Security + JWT (avance final) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.12.6</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>0.12.6</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>0.12.6</version>
            <scope>runtime</scope>
        </dependency>
        <!-- Reporte PDF de pedidos -->
        <dependency>
            <groupId>com.github.librepdf</groupId>
            <artifactId>openpdf</artifactId>
            <version>1.3.35</version>
        </dependency>
        <!-- Utilidades de test para seguridad (@WithMockUser) -->
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
```

- [ ] **Step 2: Verificar que resuelve dependencias y compila**

Run: `cd "c:/Users/dietr/Desktop/Pruebas/UNIVERSIDAD/gamerstore-main" && ./mvnw -q -DskipTests compile`
Expected: BUILD SUCCESS (descarga jars la primera vez).

> Nota: al añadir `spring-boot-starter-security`, TODA la app queda protegida por defecto (login form + 401). Hasta implementar `SecurityConfig` (Task 1.6) la app pedirá auth en todo; es esperado. No arrancar/verificar funcionalidad hasta cerrar la Fase 1.

### Task 0.2: Propiedades (JWT, multipart, uploads) y quitar warning de dialecto

**Files:**
- Modify: `src/main/resources/application.properties`

- [ ] **Step 1: Reemplazar el bloque Hibernate** para quitar el dialecto explícito (elimina el warning MariaDB 5.5.5). Sustituir estas 3 líneas actuales:

```
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MariaDBDialect
```

por:

```
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
```

- [ ] **Step 2: Añadir al final del archivo** los bloques de JWT, uploads y multipart:

```
# ===== Seguridad JWT =====
app.jwt.secret=GamerStore2026ClaveSecretaHS256MuyLargaParaFirmarTokensSegura!!
app.jwt.access-expiration-ms=300000
app.jwt.refresh-expiration-ms=604800000

# ===== Subida de imágenes (se guardan en el proyecto, no en la BD) =====
app.uploads.dir=uploads/productos
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=6MB

# ===== apiperu.dev (RENIEC/DNI - datos reales de clientes) =====
# NOTA: token sensible. Para repos públicos, moverlo a variable de entorno
# (p. ej. app.apidevperu.token=${APIDEVPERU_TOKEN}).
app.apidevperu.enabled=true
app.apidevperu.base-url=https://apiperu.dev/api
app.apidevperu.token=860ec14d7924d36cd6cb33e98d4de1c187b0b16a75f30c73499504d485d446d7
```

- [ ] **Step 3: Añadir el mismo bloque JWT + apiperu (deshabilitado) al perfil de test** en `src/test/resources/application-test.properties` (secreto para el contexto y `enabled=false` para no llamar a la red en los tests):

```
app.jwt.secret=GamerStore2026ClaveSecretaHS256MuyLargaParaFirmarTokensSegura!!
app.jwt.access-expiration-ms=300000
app.jwt.refresh-expiration-ms=604800000
app.uploads.dir=uploads/productos
app.apidevperu.enabled=false
app.apidevperu.base-url=https://apiperu.dev/api
app.apidevperu.token=
```

---

## Fase 1 — Seguridad: Spring Security + JWT + Refresh tokens

Paquete nuevo: `src/main/java/com/gamerstore/app/config/security/`.

### Task 1.1: Entidad RefreshToken + repositorio

**Files:**
- Create: `src/main/java/com/gamerstore/app/model/RefreshToken.java`
- Create: `src/main/java/com/gamerstore/app/repository/RefreshTokenRepository.java`

- [ ] **Step 1: Crear la entidad**

```java
package com.gamerstore.app.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "refresh_token")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String token;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "expira_en", nullable = false)
    private Instant expiraEn;

    @Column(nullable = false)
    private boolean revocado = false;

    public RefreshToken() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public Instant getExpiraEn() { return expiraEn; }
    public void setExpiraEn(Instant expiraEn) { this.expiraEn = expiraEn; }
    public boolean isRevocado() { return revocado; }
    public void setRevocado(boolean revocado) { this.revocado = revocado; }

    public boolean estaVigente() {
        return !revocado && expiraEn.isAfter(Instant.now());
    }
}
```

- [ ] **Step 2: Crear el repositorio**

```java
package com.gamerstore.app.repository;

import com.gamerstore.app.model.RefreshToken;
import com.gamerstore.app.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken r SET r.revocado = true WHERE r.usuario = :usuario AND r.revocado = false")
    void revocarTodosDe(Usuario usuario);
}
```

- [ ] **Step 3: Compilar**

Run: `./mvnw -q -DskipTests compile`
Expected: BUILD SUCCESS.

### Task 1.2: JwtService

**Files:**
- Create: `src/main/java/com/gamerstore/app/config/security/JwtService.java`

- [ ] **Step 1: Crear el servicio de tokens (jjwt 0.12.x)**

```java
package com.gamerstore.app.config.security;

import com.gamerstore.app.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/** Genera y valida access tokens JWT (HS256). El refresh se maneja aparte, con estado en BD. */
@Service
public class JwtService {

    private final SecretKey key;
    private final long accessExpMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.access-expiration-ms}") long accessExpMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpMs = accessExpMs;
    }

    public String generarAccess(Usuario u) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(u.getUsername())
                .claim("rol", u.getRol().name())
                .claim("nombre", u.getNombre())
                .claim("id", u.getId())
                .issuedAt(new Date(now))
                .expiration(new Date(now + accessExpMs))
                .signWith(key)
                .compact();
    }

    public String extraerUsername(String token) {
        return parse(token).getSubject();
    }

    public boolean esValido(String token, String username) {
        try {
            Claims c = parse(token);
            return c.getSubject().equals(username) && c.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
```

- [ ] **Step 2: Compilar** — `./mvnw -q -DskipTests compile` → BUILD SUCCESS.

### Task 1.3: CustomUserDetailsService

**Files:**
- Create: `src/main/java/com/gamerstore/app/config/security/CustomUserDetailsService.java`

- [ ] **Step 1: Crear** — carga por username y, si no, por email; authority `ROLE_<rol>`.

```java
package com.gamerstore.app.config.security;

import com.gamerstore.app.model.Usuario;
import com.gamerstore.app.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository repo;

    public CustomUserDetailsService(UsuarioRepository repo) {
        this.repo = repo;
    }

    @Override
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
        Usuario u = repo.findByUsername(loginId)
                .or(() -> repo.findByEmail(loginId))
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        return User.withUsername(u.getUsername())
                .password(u.getPassword())
                .authorities("ROLE_" + u.getRol().name())
                .build();
    }
}
```

- [ ] **Step 2: Compilar** — `./mvnw -q -DskipTests compile` → BUILD SUCCESS.

### Task 1.4: RefreshTokenService

**Files:**
- Create: `src/main/java/com/gamerstore/app/config/security/RefreshTokenService.java`

- [ ] **Step 1: Crear** — emite, valida+rota y revoca refresh tokens.

```java
package com.gamerstore.app.config.security;

import com.gamerstore.app.model.RefreshToken;
import com.gamerstore.app.model.Usuario;
import com.gamerstore.app.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repo;
    private final long refreshExpMs;

    public RefreshTokenService(RefreshTokenRepository repo,
                               @Value("${app.jwt.refresh-expiration-ms}") long refreshExpMs) {
        this.repo = repo;
        this.refreshExpMs = refreshExpMs;
    }

    @Transactional
    public RefreshToken crear(Usuario usuario) {
        RefreshToken rt = new RefreshToken();
        rt.setToken(UUID.randomUUID().toString().replace("-", ""));
        rt.setUsuario(usuario);
        rt.setExpiraEn(Instant.now().plusMillis(refreshExpMs));
        rt.setRevocado(false);
        return repo.save(rt);
    }

    /** Valida el refresh recibido, lo revoca (rotación) y emite uno nuevo para el mismo usuario. */
    @Transactional
    public RefreshToken rotar(String token) {
        RefreshToken actual = repo.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión inválida"));
        if (!actual.estaVigente()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión expirada");
        }
        actual.setRevocado(true);
        repo.save(actual);
        return crear(actual.getUsuario());
    }

    @Transactional
    public void revocar(String token) {
        repo.findByToken(token).ifPresent(rt -> {
            rt.setRevocado(true);
            repo.save(rt);
        });
    }
}
```

- [ ] **Step 2: Compilar** — `./mvnw -q -DskipTests compile` → BUILD SUCCESS.

### Task 1.5: JwtAuthenticationFilter

**Files:**
- Create: `src/main/java/com/gamerstore/app/config/security/JwtAuthenticationFilter.java`

- [ ] **Step 1: Crear el filtro** — lee `Authorization: Bearer`, valida y setea el contexto.

```java
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

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, CustomUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

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
        chain.doFilter(request, response);
    }
}
```

- [ ] **Step 2: Compilar** — `./mvnw -q -DskipTests compile` → BUILD SUCCESS.

### Task 1.6: SecurityConfig (filter chain + CORS + handlers 401/403 + AuthenticationManager)

**Files:**
- Create: `src/main/java/com/gamerstore/app/config/security/SecurityConfig.java`

- [ ] **Step 1: Crear la configuración de seguridad**

```java
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

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login", "/api/auth/refresh").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/productos/**", "/api/categorias/**", "/api/config/**").permitAll()
                .requestMatchers("/images/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
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
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

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

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }
}
```

> El `PasswordEncoder` (BCrypt) ya existe como bean en `config/PasswordConfig.java`. Con `UserDetailsService` + `PasswordEncoder` en el contexto, Spring arma automáticamente el `DaoAuthenticationProvider`.
> `WebConfig.addCorsMappings` (CORS de MVC) queda redundante pero inofensivo; la seguridad usa el `CorsConfigurationSource` de arriba. Se puede dejar como está.

- [ ] **Step 2: Compilar** — `./mvnw -q -DskipTests compile` → BUILD SUCCESS.

### Task 1.7: DTOs de auth + AuthController (login/refresh/logout/me)

**Files:**
- Modify: `src/main/java/com/gamerstore/app/dto/LoginResponse.java`
- Create: `src/main/java/com/gamerstore/app/dto/TokenResponse.java`
- Create: `src/main/java/com/gamerstore/app/dto/RefreshRequest.java`
- Create: `src/main/java/com/gamerstore/app/dto/AuthUser.java`
- Modify: `src/main/java/com/gamerstore/app/service/UsuarioService.java` (añadir `buscar`)
- Modify: `src/main/java/com/gamerstore/app/controller/AuthController.java`
- Modify: `src/main/java/com/gamerstore/app/controller/GlobalExceptionHandler.java` (handler 401 auth)

- [ ] **Step 1: Reescribir `LoginResponse.java`**

```java
package com.gamerstore.app.dto;

public record LoginResponse(String accessToken, String refreshToken,
                            String username, String nombre, String rol) {}
```

- [ ] **Step 2: Crear `TokenResponse.java`**

```java
package com.gamerstore.app.dto;

public record TokenResponse(String accessToken, String refreshToken) {}
```

- [ ] **Step 3: Crear `RefreshRequest.java`**

```java
package com.gamerstore.app.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(@NotBlank(message = "El refresh token es obligatorio") String refreshToken) {}
```

- [ ] **Step 4: Crear `AuthUser.java`** (para `/auth/me`)

```java
package com.gamerstore.app.dto;

public record AuthUser(String username, String nombre, String rol) {}
```

- [ ] **Step 5: Añadir `buscar` a `UsuarioService.java`** (tras el método `autenticar`):

```java
    /** Busca por username y, si no, por email. Para armar el token tras el login. */
    public Optional<Usuario> buscar(String loginId) {
        return repo.findByUsername(loginId).or(() -> repo.findByEmail(loginId));
    }
```

- [ ] **Step 6: Reescribir `AuthController.java`**

```java
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

    public AuthController(AuthenticationManager authManager, UsuarioService usuarioService,
                          JwtService jwtService, RefreshTokenService refreshTokenService) {
        this.authManager = authManager;
        this.usuarioService = usuarioService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        authManager.authenticate(new UsernamePasswordAuthenticationToken(req.username(), req.password()));
        Usuario u = usuarioService.buscar(req.username())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos"));
        String access = jwtService.generarAccess(u);
        RefreshToken rt = refreshTokenService.crear(u);
        return new LoginResponse(access, rt.getToken(), u.getUsername(), u.getNombre(), u.getRol().name());
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest req) {
        RefreshToken rotado = refreshTokenService.rotar(req.refreshToken());
        String access = jwtService.generarAccess(rotado.getUsuario());
        return new TokenResponse(access, rotado.getToken());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest req) {
        refreshTokenService.revocar(req.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public AuthUser me(Authentication auth) {
        Usuario u = usuarioService.buscar(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión inválida"));
        return new AuthUser(u.getUsername(), u.getNombre(), u.getRol().name());
    }
}
```

- [ ] **Step 7: Añadir handler de credenciales inválidas en `GlobalExceptionHandler.java`** — añadir el import y el método:

Import (junto a los demás):
```java
import org.springframework.security.core.AuthenticationException;
```

Método (dentro de la clase):
```java
    /** Credenciales inválidas en el login (Spring Security). */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> auth(AuthenticationException e) {
        return body(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos");
    }
```

- [ ] **Step 8: Compilar** — `./mvnw -q -DskipTests compile` → BUILD SUCCESS.

### Task 1.8: Actualizar el test de integración para la seguridad

**Files:**
- Modify: `src/test/java/com/gamerstore/app/ApiIntegrationTest.java`

- [ ] **Step 1: Añadir imports** (junto a los existentes):

```java
import org.springframework.security.test.context.support.WithMockUser;
import static org.hamcrest.Matchers.greaterThan;
```

- [ ] **Step 2: Anotar los 4 tests que llaman a `/api/admin/**`** con `@WithMockUser(roles = "ADMIN")` (justo encima de `@Test`): `crearProductoInvalidoFallaValidacion`, `crearClienteConDniInvalidoFallaValidacion`, `crearPedidoValido`, `crearPedidoSinItemsFallaValidacion`. Ejemplo:

```java
    @Test
    @WithMockUser(roles = "ADMIN")
    void crearProductoInvalidoFallaValidacion() throws Exception {
```

- [ ] **Step 3: Hacer robusta la aserción de total** en `crearPedidoValido` (el precio del producto 1 cambia con la nueva data): reemplazar la línea `.andExpect(jsonPath("$.total").value(4998.0));` por:

```java
                .andExpect(jsonPath("$.total").value(greaterThan(0.0)));
```

- [ ] **Step 4: Ejecutar los tests**

Run: `./mvnw -q test`
Expected: BUILD SUCCESS, todos los tests en verde (public endpoints, login válido/ inválido/ vacío, validaciones admin, pedido válido/ sin items).

- [ ] **Step 5 (opcional): Commit** — `git add -A && git commit -m "feat(security): Spring Security + JWT con refresh tokens"`

---

## Fase 2 — Validación de campos únicos (409 + mensaje) 

Todas las comprobaciones nuevas lanzan `ResponseStatusException(HttpStatus.CONFLICT, "<mensaje>")`; el `GlobalExceptionHandler` ya lo convierte en `{ "error": "<mensaje>" }`. El frontend mostrará ese mensaje como toast (Fase 9).

### Task 2.1: Producto — nombre único

**Files:**
- Modify: `src/main/java/com/gamerstore/app/model/Producto.java` (constraint)
- Modify: `src/main/java/com/gamerstore/app/repository/ProductoRepository.java`
- Modify: `src/main/java/com/gamerstore/app/service/ProductoService.java`

- [ ] **Step 1: Añadir `unique = true` al nombre** en `Producto.java` — cambiar:

```java
    @Column(nullable = false)
    private String nombre;
```
por:
```java
    @Column(nullable = false, unique = true)
    private String nombre;
```

- [ ] **Step 2: Añadir métodos al `ProductoRepository.java`** (dentro de la interfaz):

```java
    boolean existsByNombreIgnoreCase(String nombre);
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
```

- [ ] **Step 3: Chequear duplicado en `ProductoService.java`** — añadir el import y las validaciones.

Import (junto a los demás):
```java
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
```

En `crear(...)`, tras la línea `Producto p = new Producto();` NO; en su lugar, al inicio del método (primera línea del cuerpo) añadir:
```java
        if (nombre != null && productoRepo.existsByNombreIgnoreCase(nombre.trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un producto con ese nombre");
        }
```

En `actualizar(...)`, tras `Producto p = productoRepo.findById(id).orElseThrow();` añadir:
```java
        if (nombre != null && !nombre.isBlank() && productoRepo.existsByNombreIgnoreCaseAndIdNot(nombre.trim(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un producto con ese nombre");
        }
```

- [ ] **Step 4: Compilar** — `./mvnw -q -DskipTests compile` → BUILD SUCCESS.

### Task 2.2: Cliente — email único + mensaje DNI

**Files:**
- Modify: `src/main/java/com/gamerstore/app/repository/ClienteRepository.java`
- Modify: `src/main/java/com/gamerstore/app/service/ClienteService.java`

- [ ] **Step 1: Añadir métodos al `ClienteRepository.java`**:

```java
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
```

- [ ] **Step 2: En `ClienteService.java`** añadir import y validaciones.

Import:
```java
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
```

En `crear(...)`, cambiar el bloque actual del DNI:
```java
        if (repo.existsByDni(dni)) {
            throw new IllegalArgumentException("Ya existe un cliente con ese DNI");
        }
```
por (mensaje pedido + chequeo de email):
```java
        if (repo.existsByDni(dni.trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El DNI ya está registrado");
        }
        if (email != null && !email.isBlank() && repo.existsByEmailIgnoreCase(email.trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ese email ya está registrado");
        }
```

En `actualizar(...)`, dentro del bloque del DNI cambiar:
```java
            if (repo.existsByDniAndIdNot(dni.trim(), id)) {
                throw new IllegalArgumentException("Ya existe otro cliente con el DNI " + dni.trim());
            }
```
por:
```java
            if (repo.existsByDniAndIdNot(dni.trim(), id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El DNI ya está registrado");
            }
```
y justo antes de `repo.save(c);` (al final del método) añadir el chequeo de email:
```java
        if (email != null && !email.isBlank() && repo.existsByEmailIgnoreCaseAndIdNot(email.trim(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ese email ya está registrado");
        }
```

- [ ] **Step 3: Compilar** — `./mvnw -q -DskipTests compile` → BUILD SUCCESS.

### Task 2.3: Categoría — unificar mensaje a "La categoría ya existe"

**Files:**
- Modify: `src/main/java/com/gamerstore/app/service/CategoriaService.java`

- [ ] **Step 1:** En `crear(...)` cambiar el mensaje del duplicado a `"La categoría ya existe"` y en `actualizar(...)` a `"La categoría ya existe"` (dejar como `IllegalArgumentException` está bien; el frontend muestra el mensaje). Reemplazar ambos textos largos por el corto pedido.

- [ ] **Step 2: Compilar** — `./mvnw -q -DskipTests compile` → BUILD SUCCESS.

---

## Fase 3 — Módulo de Usuarios (backend)

### Task 3.1: Repositorio, DTOs, mapper, servicio y controlador de Usuarios

**Files:**
- Modify: `src/main/java/com/gamerstore/app/repository/UsuarioRepository.java`
- Create: `src/main/java/com/gamerstore/app/dto/UsuarioDTO.java`
- Create: `src/main/java/com/gamerstore/app/dto/UsuarioRequest.java`
- Create: `src/main/java/com/gamerstore/app/mapper/UsuarioMapper.java`
- Modify: `src/main/java/com/gamerstore/app/service/UsuarioService.java`
- Create: `src/main/java/com/gamerstore/app/controller/AdminUsuarioController.java`

- [ ] **Step 1: Añadir métodos a `UsuarioRepository.java`**:

```java
    boolean existsByUsernameAndIdNot(String username, Long id);
    boolean existsByEmailAndIdNot(String email, Long id);
    long countByRol(Rol rol);
    java.util.List<Usuario> findAllByOrderByUsernameAsc();
```

- [ ] **Step 2: Crear `UsuarioDTO.java`** (sin password):

```java
package com.gamerstore.app.dto;

import java.time.LocalDateTime;

public record UsuarioDTO(Long id, String username, String email, String nombre,
                         String telefono, String rol, LocalDateTime fechaRegistro) {}
```

- [ ] **Step 3: Crear `UsuarioRequest.java`**:

```java
package com.gamerstore.app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UsuarioRequest(
        @NotBlank(message = "El usuario es obligatorio")
        @Size(min = 3, max = 50, message = "El usuario debe tener entre 3 y 50 caracteres")
        String username,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "Email inválido")
        @Size(max = 120)
        String email,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100)
        String nombre,

        // En creación es obligatoria; en edición puede venir vacía (no se cambia).
        String password,

        @Size(max = 15)
        String telefono,

        @NotBlank(message = "El rol es obligatorio")
        String rol
) {}
```

- [ ] **Step 4: Crear `UsuarioMapper.java`**:

```java
package com.gamerstore.app.mapper;

import com.gamerstore.app.dto.UsuarioDTO;
import com.gamerstore.app.model.Usuario;
import org.springframework.stereotype.Component;

@Component
public class UsuarioMapper {
    public UsuarioDTO toDTO(Usuario u) {
        return new UsuarioDTO(u.getId(), u.getUsername(), u.getEmail(), u.getNombre(),
                u.getTelefono(), u.getRol().name(), u.getFechaRegistro());
    }
}
```

- [ ] **Step 5: Añadir CRUD a `UsuarioService.java`** — imports y métodos.

Imports:
```java
import com.gamerstore.app.model.Rol;
import com.gamerstore.app.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
```

Métodos (dentro de la clase):
```java
    public List<Usuario> listar() {
        return repo.findAllByOrderByUsernameAsc();
    }

    public long total() {
        return repo.count();
    }

    private Rol parseRol(String rol) {
        try {
            return Rol.valueOf(rol.trim().toUpperCase());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rol inválido");
        }
    }

    @Transactional
    public Usuario crear(String username, String email, String nombre, String password,
                         String telefono, String rol) {
        if (password == null || password.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña es obligatoria");
        }
        if (repo.existsByUsername(username.trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ese usuario ya existe");
        }
        if (repo.existsByEmail(email.trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ese email ya está registrado");
        }
        Usuario u = new Usuario();
        u.setUsername(username.trim());
        u.setEmail(email.trim());
        u.setNombre(nombre);
        u.setTelefono(telefono);
        u.setPassword(passwordEncoder.encode(password));
        u.setRol(parseRol(rol));
        return repo.save(u);
    }

    @Transactional
    public void actualizar(Long id, String username, String email, String nombre, String password,
                           String telefono, String rol) {
        Usuario u = repo.findById(id).orElseThrow();
        if (username != null && !username.isBlank() && !username.trim().equals(u.getUsername())) {
            if (repo.existsByUsernameAndIdNot(username.trim(), id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ese usuario ya existe");
            }
            u.setUsername(username.trim());
        }
        if (email != null && !email.isBlank() && !email.trim().equalsIgnoreCase(u.getEmail())) {
            if (repo.existsByEmailAndIdNot(email.trim(), id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ese email ya está registrado");
            }
            u.setEmail(email.trim());
        }
        if (nombre != null && !nombre.isBlank()) u.setNombre(nombre);
        u.setTelefono(telefono);
        if (rol != null && !rol.isBlank()) u.setRol(parseRol(rol));
        if (password != null && !password.isBlank()) u.setPassword(passwordEncoder.encode(password));
        repo.save(u);
    }

    @Transactional
    public void eliminar(Long id) {
        Usuario u = repo.findById(id).orElseThrow();
        if (u.getRol() == Rol.ADMIN && repo.countByRol(Rol.ADMIN) <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No puedes eliminar el último administrador");
        }
        repo.deleteById(id);
    }
```

> `passwordEncoder` ya es un campo de `UsuarioService`. Mantener los métodos existentes (`autenticar`, `porId`, `porUsername`, `buscar`).

- [ ] **Step 6: Crear `AdminUsuarioController.java`**:

```java
package com.gamerstore.app.controller;

import com.gamerstore.app.dto.UsuarioDTO;
import com.gamerstore.app.dto.UsuarioRequest;
import com.gamerstore.app.mapper.UsuarioMapper;
import com.gamerstore.app.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/usuarios")
public class AdminUsuarioController {

    private final UsuarioService usuarioService;
    private final UsuarioMapper usuarioMapper;

    public AdminUsuarioController(UsuarioService usuarioService, UsuarioMapper usuarioMapper) {
        this.usuarioService = usuarioService;
        this.usuarioMapper = usuarioMapper;
    }

    @GetMapping
    public List<UsuarioDTO> listar() {
        return usuarioService.listar().stream().map(usuarioMapper::toDTO).toList();
    }

    @PostMapping
    public UsuarioDTO crear(@Valid @RequestBody UsuarioRequest r) {
        return usuarioMapper.toDTO(usuarioService.crear(
                r.username(), r.email(), r.nombre(), r.password(), r.telefono(), r.rol()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioRequest r) {
        usuarioService.actualizar(id, r.username(), r.email(), r.nombre(), r.password(), r.telefono(), r.rol());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        usuarioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 7: Compilar** — `./mvnw -q -DskipTests compile` → BUILD SUCCESS.

---

## Fase 3B — Integración RENIEC (apiperu.dev) para datos reales de clientes

Proveedor verificado: `GET https://apiperu.dev/api/dni/{dni}` con `Authorization: Bearer <token>` → `{ success, data: { nombres, apellido_paterno, apellido_materno, nombre_completo } }`.

### Task 3B.1: ReniecPersona DTO + ReniecService + endpoint de consulta

**Files:**
- Create: `src/main/java/com/gamerstore/app/dto/ReniecPersona.java`
- Create: `src/main/java/com/gamerstore/app/service/ReniecService.java`
- Modify: `src/main/java/com/gamerstore/app/controller/AdminClienteController.java`

- [ ] **Step 1: Crear `ReniecPersona.java`**:

```java
package com.gamerstore.app.dto;

public record ReniecPersona(String nombres, String apellidos, String nombreCompleto) {}
```

- [ ] **Step 2: Crear `ReniecService.java`** (RestClient, best-effort):

```java
package com.gamerstore.app.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gamerstore.app.dto.ReniecPersona;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/** Consulta datos reales de personas por DNI en apiperu.dev (RENIEC). Best-effort: nunca rompe el flujo. */
@Service
public class ReniecService {

    private static final Logger log = LoggerFactory.getLogger(ReniecService.class);

    private final boolean enabled;
    private final String token;
    private final RestClient client;

    public ReniecService(@Value("${app.apidevperu.enabled:true}") boolean enabled,
                         @Value("${app.apidevperu.base-url:https://apiperu.dev/api}") String baseUrl,
                         @Value("${app.apidevperu.token:}") String token) {
        this.enabled = enabled;
        this.token = token;
        this.client = RestClient.builder().baseUrl(baseUrl).build();
    }

    public Optional<ReniecPersona> consultarDni(String dni) {
        if (!enabled || token == null || token.isBlank() || dni == null || !dni.matches("\\d{8}")) {
            return Optional.empty();
        }
        try {
            ApiPeruResponse resp = client.get()
                    .uri("/dni/{dni}", dni)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(ApiPeruResponse.class);
            if (resp != null && resp.success() && resp.data() != null) {
                ApiPeruData d = resp.data();
                String ap = d.apellidoPaterno() != null ? d.apellidoPaterno() : "";
                String am = d.apellidoMaterno() != null ? d.apellidoMaterno() : "";
                String apellidos = (ap + " " + am).trim();
                return Optional.of(new ReniecPersona(d.nombres(), apellidos, d.nombreCompleto()));
            }
        } catch (Exception e) {
            log.warn("RENIEC: no se pudo consultar el DNI {} ({})", dni, e.getMessage());
        }
        return Optional.empty();
    }

    // ---- Estructura de la respuesta de apiperu.dev ----
    private record ApiPeruResponse(boolean success, ApiPeruData data) {}
    private record ApiPeruData(
            String nombres,
            @JsonProperty("apellido_paterno") String apellidoPaterno,
            @JsonProperty("apellido_materno") String apellidoMaterno,
            @JsonProperty("nombre_completo") String nombreCompleto) {}
}
```

- [ ] **Step 3: Endpoint en `AdminClienteController.java`** — inyectar `ReniecService` y añadir el mapping.

Imports:
```java
import com.gamerstore.app.dto.ReniecPersona;
import com.gamerstore.app.service.ReniecService;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
```

Añadir el campo y ampliar el constructor:
```java
    private final ReniecService reniecService;

    public AdminClienteController(ClienteService clienteService, ClienteMapper clienteMapper,
                                  ReniecService reniecService) {
        this.clienteService = clienteService;
        this.clienteMapper = clienteMapper;
        this.reniecService = reniecService;
    }
```

Método (dentro de la clase):
```java
    /** Consulta datos reales de RENIEC (apiperu.dev) para autocompletar el formulario. */
    @GetMapping("/reniec/{dni}")
    public ReniecPersona reniec(@PathVariable String dni) {
        return reniecService.consultarDni(dni)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No se encontró el DNI o el servicio no está disponible"));
    }
```

- [ ] **Step 4: Compilar** — `./mvnw -q -DskipTests compile` → BUILD SUCCESS.

- [ ] **Step 5: Probar el endpoint** (tras arrancar el backend, con token admin):

Run: `curl -s http://localhost:8080/api/admin/clientes/reniec/70123456 -H "Authorization: Bearer <ACCESS>"`
Expected: `{"nombres":"FIORELLA","apellidos":"DE LA SOTA CASTRO","nombreCompleto":"DE LA SOTA CASTRO, FIORELLA"}`.

---

## Fase 4 — Subida de imágenes al proyecto

### Task 4.1: Endpoint de subida + servir `/images/**` desde el filesystem

**Files:**
- Create: `src/main/java/com/gamerstore/app/dto/UploadResponse.java`
- Create: `src/main/java/com/gamerstore/app/controller/UploadController.java`
- Modify: `src/main/java/com/gamerstore/app/config/WebConfig.java`
- Modify: `src/main/java/com/gamerstore/app/controller/GlobalExceptionHandler.java` (tamaño máximo)

- [ ] **Step 1: Crear `UploadResponse.java`**:

```java
package com.gamerstore.app.dto;

public record UploadResponse(String url) {}
```

- [ ] **Step 2: Crear `UploadController.java`**:

```java
package com.gamerstore.app.controller;

import com.gamerstore.app.dto.UploadResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/** Guarda la imagen subida en la carpeta del proyecto (uploads/productos) y devuelve su ruta pública. */
@RestController
@RequestMapping("/api/admin/uploads")
public class UploadController {

    private final Path dir;

    public UploadController(@Value("${app.uploads.dir}") String uploadsDir) {
        this.dir = Paths.get(uploadsDir).toAbsolutePath().normalize();
    }

    @PostMapping
    public UploadResponse subir(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío");
        }
        String ct = file.getContentType();
        if (ct == null || !ct.startsWith("image/")) {
            throw new IllegalArgumentException("El archivo debe ser una imagen");
        }
        Files.createDirectories(dir);
        String ext = switch (ct) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
        String name = UUID.randomUUID().toString().replace("-", "") + ext;
        Files.copy(file.getInputStream(), dir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        return new UploadResponse("/images/productos/" + name);
    }
}
```

- [ ] **Step 3: Servir `/images/productos/**` desde el filesystem** — en `WebConfig.java`, añadir el import, el `@Value` y registrar el handler ANTES del handler `/**` existente.

Imports:
```java
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import java.nio.file.Path;
import java.nio.file.Paths;
```
(el `ResourceHandlerRegistry` ya está importado; añadir sólo `Path`/`Paths`.)

Campo:
```java
    @Value("${app.uploads.dir}")
    private String uploadsDir;
```

Al inicio de `addResourceHandlers(...)`, antes del `registry.addResourceHandler("/**")...` existente, añadir:
```java
        Path uploads = Paths.get(uploadsDir).toAbsolutePath().normalize();
        String uploadsLocation = uploads.toUri().toString();
        if (!uploadsLocation.endsWith("/")) uploadsLocation += "/";
        registry.addResourceHandler("/images/productos/**")
                .addResourceLocations(uploadsLocation);
```

- [ ] **Step 4: Manejar imagen demasiado grande en `GlobalExceptionHandler.java`** — añadir import y handler:

Import:
```java
import org.springframework.web.multipart.MaxUploadSizeExceededException;
```
Método:
```java
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> tooLarge(MaxUploadSizeExceededException e) {
        return body(HttpStatus.PAYLOAD_TOO_LARGE, "La imagen supera el tamaño máximo (5MB)");
    }
```

- [ ] **Step 5: Compilar** — `./mvnw -q -DskipTests compile` → BUILD SUCCESS.

---

## Fase 5 — Top productos en Dashboard (backend)

### Task 5.1: TopProductoDTO + DashboardDTO + controlador

**Files:**
- Create: `src/main/java/com/gamerstore/app/dto/TopProductoDTO.java`
- Modify: `src/main/java/com/gamerstore/app/dto/DashboardDTO.java`
- Modify: `src/main/java/com/gamerstore/app/controller/AdminDashboardController.java`

- [ ] **Step 1: Crear `TopProductoDTO.java`**:

```java
package com.gamerstore.app.dto;

public record TopProductoDTO(Long productoId, String productoNombre, String imagen, long cantidad) {}
```

- [ ] **Step 2: Añadir `topProductos` a `DashboardDTO.java`**:

```java
package com.gamerstore.app.dto;

import java.util.List;

public record DashboardDTO(long totalProductos, long totalCategorias, long totalClientes,
                           long totalPedidos, double totalVentas, List<ProductoDTO> stockBajo,
                           List<TopProductoDTO> topProductos) {}
```

- [ ] **Step 3: Wire en `AdminDashboardController.java`** — construir la lista y pasarla al DTO.

Imports:
```java
import com.gamerstore.app.dto.TopProductoDTO;
import java.util.List;
```

Cambiar el cuerpo de `dashboard()`:
```java
    @GetMapping
    public DashboardDTO dashboard() {
        List<TopProductoDTO> top = pedidoService.topProductos(5).stream()
                .map(r -> new TopProductoDTO(
                        ((Number) r[0]).longValue(),
                        (String) r[1],
                        (String) r[2],
                        ((Number) r[3]).longValue()))
                .toList();
        return new DashboardDTO(
                productoService.total(),
                categoriaService.total(),
                clienteService.total(),
                pedidoService.total(),
                pedidoService.totalVentas(),
                productoService.stockBajo(UMBRAL_STOCK_BAJO).stream().map(productoMapper::toDTO).toList(),
                top
        );
    }
```

> `pedidoService.topProductos(5)` devuelve `List<Object[]>` con columnas `[producto.id, producto.nombre, producto.imagen, SUM(cantidad)]` (ver `PedidoRepository.topProductos`).

- [ ] **Step 4: Compilar** — `./mvnw -q -DskipTests compile` → BUILD SUCCESS.

---

## Fase 6 — Reporte PDF de pedidos (backend)

### Task 6.1: Servicio PDF (OpenPDF) + filtro en servicio + endpoint

**Files:**
- Modify: `src/main/java/com/gamerstore/app/service/PedidoService.java` (método `reporte`)
- Create: `src/main/java/com/gamerstore/app/service/PedidoReporteService.java`
- Modify: `src/main/java/com/gamerstore/app/controller/AdminPedidoController.java`

- [ ] **Step 1: Filtro en `PedidoService.java`** — imports y método.

Imports:
```java
import com.gamerstore.app.model.Pedido;
import java.time.LocalDate;
```
(el `Pedido` ya está importado; añadir sólo `LocalDate`.)

Método:
```java
    /** Filtra pedidos por rango de fecha (inclusive) y estado, para el reporte. */
    public List<Pedido> reporte(LocalDate desde, LocalDate hasta, String estado) {
        return todos().stream().filter(p -> {
            LocalDate f = p.getFecha().toLocalDate();
            if (desde != null && f.isBefore(desde)) return false;
            if (hasta != null && f.isAfter(hasta)) return false;
            if (estado != null && !estado.isBlank() && !estado.equalsIgnoreCase(p.getEstado())) return false;
            return true;
        }).toList();
    }
```

- [ ] **Step 2: Crear `PedidoReporteService.java`** (OpenPDF, paquete `com.lowagie.text`):

```java
package com.gamerstore.app.service;

import com.gamerstore.app.model.Pedido;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Genera el PDF del reporte de pedidos con OpenPDF. */
@Service
public class PedidoReporteService {

    private static final Color ACCENT = new Color(99, 102, 241);
    private static final Color HEAD_BG = new Color(30, 27, 75);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] generar(List<Pedido> pedidos, LocalDate desde, LocalDate hasta, String estado) {
        Document doc = new Document(PageSize.A4, 36, 36, 42, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, ACCENT);
            doc.add(new Paragraph("GamerStore — Reporte de Pedidos", titulo));

            Font sub = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
            StringBuilder filtros = new StringBuilder("Filtros: ");
            filtros.append(desde != null ? "desde " + desde.format(FMT) + " " : "");
            filtros.append(hasta != null ? "hasta " + hasta.format(FMT) + " " : "");
            filtros.append(estado != null && !estado.isBlank() ? "estado " + estado : "");
            if (desde == null && hasta == null && (estado == null || estado.isBlank())) filtros.append("todos");
            doc.add(new Paragraph(filtros.toString().trim(), sub));
            doc.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(new float[]{2f, 3.2f, 2f, 2.2f, 2f, 1.4f, 2.2f});
            table.setWidthPercentage(100);
            String[] cols = {"Código", "Cliente", "Fecha", "Estado", "Método", "Ítems", "Total (S/)"};
            Font th = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
            for (String c : cols) {
                PdfPCell cell = new PdfPCell(new Phrase(c, th));
                cell.setBackgroundColor(HEAD_BG);
                cell.setPadding(6);
                cell.setBorderColor(Color.LIGHT_GRAY);
                table.addCell(cell);
            }

            Font td = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
            double totalVentas = 0;
            for (Pedido p : pedidos) {
                addCell(table, p.getCodigo(), td, false);
                addCell(table, p.getCliente() != null ? p.getCliente().getNombreCompleto() : "—", td, false);
                addCell(table, p.getFecha() != null ? p.getFecha().toLocalDate().format(FMT) : "—", td, false);
                addCell(table, p.getEstado(), td, false);
                addCell(table, p.getMetodoPago() != null ? p.getMetodoPago() : "—", td, false);
                addCell(table, String.valueOf(p.getCantidadTotal()), td, false);
                addCell(table, String.format("%,.2f", p.getTotal()), td, true);
                totalVentas += p.getTotal();
            }
            doc.add(table);
            doc.add(Chunk.NEWLINE);

            Font pie = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, HEAD_BG);
            doc.add(new Paragraph(
                    "Total ventas: S/ " + String.format("%,.2f", totalVentas) + "   |   "
                            + pedidos.size() + " pedidos", pie));

            doc.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new RuntimeException("No se pudo generar el PDF", e);
        }
    }

    private void addCell(PdfPTable table, String text, Font font, boolean right) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        cell.setBorderColor(Color.LIGHT_GRAY);
        if (right) cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(cell);
    }
}
```

- [ ] **Step 3: Endpoint en `AdminPedidoController.java`** — inyectar el servicio y añadir el mapping.

En imports:
```java
import com.gamerstore.app.service.PedidoReporteService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.time.LocalDate;
```

Añadir el campo y ampliar el constructor:
```java
    private final PedidoReporteService reporteService;

    public AdminPedidoController(PedidoService pedidoService, PedidoMapper pedidoMapper,
                                 PedidoReporteService reporteService) {
        this.pedidoService = pedidoService;
        this.pedidoMapper = pedidoMapper;
        this.reporteService = reporteService;
    }
```

Método (dentro de la clase):
```java
    @GetMapping("/reporte.pdf")
    public ResponseEntity<byte[]> reporte(@RequestParam(required = false) String desde,
                                          @RequestParam(required = false) String hasta,
                                          @RequestParam(required = false) String estado) {
        LocalDate d = (desde != null && !desde.isBlank()) ? LocalDate.parse(desde) : null;
        LocalDate h = (hasta != null && !hasta.isBlank()) ? LocalDate.parse(hasta) : null;
        byte[] pdf = reporteService.generar(pedidoService.reporte(d, h, estado), d, h, estado);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte-pedidos.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
```

- [ ] **Step 4: Compilar** — `./mvnw -q -DskipTests compile` → BUILD SUCCESS.

- [ ] **Step 5 (opcional): Commit** — `git add -A && git commit -m "feat: validación de únicos, usuarios, uploads, top productos y reporte PDF"`

---

## Fase 7 — Data real: imágenes en el proyecto + seeder + reset de BD

### Task 7.1: Descargar imágenes de tecnología a `uploads/productos/`

**Files:**
- Create: `uploads/productos/` (carpeta con ~28 .jpg)
- Create (temporal): `scripts/descargar-imagenes.sh` (script de descarga)

Los nombres de archivo (slug) deben coincidir EXACTAMENTE con los que usa el seeder en Task 7.2. Se usan fotos libres de Unsplash (parámetro `?w=800&q=80`), lo más afines posible a cada producto.

- [ ] **Step 1: Crear `scripts/descargar-imagenes.sh`** con el mapeo slug→URL:

```bash
#!/usr/bin/env bash
set -e
DIR="uploads/productos"
mkdir -p "$DIR"

# slug|url  (fotos libres Unsplash; ?w=800&q=80)
items=(
  "rtx-4060|https://images.unsplash.com/photo-1591488320449-011701bb6704?w=800&q=80"
  "rtx-4070|https://images.unsplash.com/photo-1587202372775-e229f172b9d7?w=800&q=80"
  "rtx-4080|https://images.unsplash.com/photo-1555618254-84e5f7d1e0f2?w=800&q=80"
  "rx-7800xt|https://images.unsplash.com/photo-1591238372338-22d30c883f5b?w=800&q=80"
  "ryzen-5-5600|https://images.unsplash.com/photo-1555617981-dac3880eac6e?w=800&q=80"
  "ryzen-7-7800x3d|https://images.unsplash.com/photo-1591799264318-7e6ef8ddb7ea?w=800&q=80"
  "intel-i5-13600k|https://images.unsplash.com/photo-1518770660439-4636190af475?w=800&q=80"
  "intel-i7-13700k|https://images.unsplash.com/photo-1591405351990-4726e331f141?w=800&q=80"
  "mb-b550|https://images.unsplash.com/photo-1518774147153-2a3f1f0f0a3f?w=800&q=80"
  "mb-b650|https://images.unsplash.com/photo-1600348759986-9c1b8f4c0a3a?w=800&q=80"
  "mb-z790|https://images.unsplash.com/photo-1563986768609-322da13575f3?w=800&q=80"
  "ram-vengeance-16|https://images.unsplash.com/photo-1541029071515-84cc54f84dc5?w=800&q=80"
  "ram-vengeance-32|https://images.unsplash.com/photo-1562976540-1502c2145186?w=800&q=80"
  "ssd-980-1tb|https://images.unsplash.com/photo-1597872200969-2b65d56bd16b?w=800&q=80"
  "ssd-sn850x-2tb|https://images.unsplash.com/photo-1531492746076-161ca9bcad58?w=800&q=80"
  "psu-rm750|https://images.unsplash.com/photo-1587134160368-5d9c2f5c9e9a?w=800&q=80"
  "monitor-odyssey-g7|https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=800&q=80"
  "monitor-lg-ultragear|https://images.unsplash.com/photo-1616711906333-23cf8b918a76?w=800&q=80"
  "teclado-blackwidow|https://images.unsplash.com/photo-1618384887929-16ec33fab9ef?w=800&q=80"
  "mouse-gpro-superlight|https://images.unsplash.com/photo-1527814050087-3793815479db?w=800&q=80"
  "headset-cloud-iii|https://images.unsplash.com/photo-1599669454699-248893623440?w=800&q=80"
  "silla-titan-evo|https://images.unsplash.com/photo-1598550476439-6847785fcea6?w=800&q=80"
  "silla-cougar|https://images.unsplash.com/photo-1610395219791-21b0353e43c1?w=800&q=80"
  "ps5-slim|https://images.unsplash.com/photo-1606813907291-d86efa9b94db?w=800&q=80"
  "xbox-series-x|https://images.unsplash.com/photo-1621259182978-fbf93132d53d?w=800&q=80"
  "switch-oled|https://images.unsplash.com/photo-1612036782180-6f0b6cd846fe?w=800&q=80"
  "webcam-brio|https://images.unsplash.com/photo-1596742578443-7682ef5251cd?w=800&q=80"
  "cooler-aio|https://images.unsplash.com/photo-1587202372634-32705e3bf49c?w=800&q=80"
)

for it in "${items[@]}"; do
  slug="${it%%|*}"; url="${it#*|}"
  echo "-> $slug"
  curl -fsSL "$url" -o "$DIR/$slug.jpg" || echo "   (falló $slug, se reintenta manual)"
done
echo "Listo: $(ls -1 "$DIR" | wc -l) imágenes en $DIR"
```

- [ ] **Step 2: Ejecutar el script** desde la raíz del proyecto (Git Bash):

Run: `cd "c:/Users/dietr/Desktop/Pruebas/UNIVERSIDAD/gamerstore-main" && bash scripts/descargar-imagenes.sh`
Expected: ~28 archivos `.jpg` en `uploads/productos/`.

- [ ] **Step 3: Verificar** que cada slug tenga su `.jpg` y pese > 3KB:

Run: `ls -la "c:/Users/dietr/Desktop/Pruebas/UNIVERSIDAD/gamerstore-main/uploads/productos"`
Expected: 28 archivos. Si alguna URL falló (0 bytes o error), reemplazar su URL en el script por otra foto Unsplash del mismo tema y re-descargar sólo ese slug. NINGÚN slug puede quedar sin imagen (el seeder los referencia).

> Nota: si varias URLs de Unsplash fallan, es aceptable reutilizar una foto genérica de "componentes de PC" para ese slug; el requisito es que exista el archivo y sea una imagen válida. Registrar en el reporte final qué slugs usaron imagen genérica.

### Task 7.2: Reescribir DataSeeder (categorías, productos, clientes, pedidos históricos)

**Files:**
- Modify: `src/main/java/com/gamerstore/app/config/DataSeeder.java` (reescritura completa)

- [ ] **Step 1: Reemplazar todo el contenido de `DataSeeder.java`** por:

```java
package com.gamerstore.app.config;

import com.gamerstore.app.model.*;
import com.gamerstore.app.repository.*;
import com.gamerstore.app.dto.ReniecPersona;
import com.gamerstore.app.service.ReniecService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

/** Siembra data real de tecnología: categorías, productos (imágenes locales),
 *  clientes y pedidos históricos (últimos ~6 meses). Idempotente por tabla. */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final CategoriaRepository categoriaRepo;
    private final ProductoRepository productoRepo;
    private final UsuarioRepository usuarioRepo;
    private final ClienteRepository clienteRepo;
    private final PedidoRepository pedidoRepo;
    private final PasswordEncoder passwordEncoder;
    private final ReniecService reniecService;

    public DataSeeder(CategoriaRepository categoriaRepo, ProductoRepository productoRepo,
                      UsuarioRepository usuarioRepo, ClienteRepository clienteRepo,
                      PedidoRepository pedidoRepo, PasswordEncoder passwordEncoder,
                      ReniecService reniecService) {
        this.categoriaRepo = categoriaRepo;
        this.productoRepo = productoRepo;
        this.usuarioRepo = usuarioRepo;
        this.clienteRepo = clienteRepo;
        this.pedidoRepo = pedidoRepo;
        this.passwordEncoder = passwordEncoder;
        this.reniecService = reniecService;
    }

    private static String img(String slug) { return "/images/productos/" + slug + ".jpg"; }

    @Override
    public void run(String... args) {
        Map<String, Categoria> cats = new HashMap<>();

        // ===== CATEGORIAS =====
        if (categoriaRepo.count() == 0) {
            String[] nombres = {"Tarjetas Graficas", "Procesadores", "Placas Madre", "Memorias RAM",
                    "Almacenamiento", "Monitores", "Perifericos", "Audio", "Sillas Gamer", "Consolas"};
            for (String n : nombres) cats.put(n, categoriaRepo.save(new Categoria(n)));
            log.info("Seed: {} categorias creadas", nombres.length);
        } else {
            categoriaRepo.findAll().forEach(c -> cats.put(c.getNombre(), c));
        }

        // ===== PRODUCTOS =====
        if (productoRepo.count() == 0) {
            productoRepo.save(new Producto("NVIDIA GeForce RTX 4060", "Tarjeta grafica 8GB GDDR6 DLSS 3 1080p/1440p", 1399.00, img("rtx-4060"), 14, cats.get("Tarjetas Graficas")));
            productoRepo.save(new Producto("NVIDIA GeForce RTX 4070", "Tarjeta grafica 12GB GDDR6X ideal 1440p", 2599.00, img("rtx-4070"), 9, cats.get("Tarjetas Graficas")));
            productoRepo.save(new Producto("NVIDIA GeForce RTX 4080 Super", "Tarjeta grafica 16GB GDDR6X 4K high-end", 4999.00, img("rtx-4080"), 5, cats.get("Tarjetas Graficas")));
            productoRepo.save(new Producto("AMD Radeon RX 7800 XT", "Tarjeta grafica 16GB GDDR6 1440p rasterizado", 2299.00, img("rx-7800xt"), 7, cats.get("Tarjetas Graficas")));

            productoRepo.save(new Producto("AMD Ryzen 5 5600", "Procesador 6 nucleos 12 hilos AM4 3.5GHz", 549.00, img("ryzen-5-5600"), 20, cats.get("Procesadores")));
            productoRepo.save(new Producto("AMD Ryzen 7 7800X3D", "Procesador gaming 8 nucleos 3D V-Cache AM5", 1899.00, img("ryzen-7-7800x3d"), 11, cats.get("Procesadores")));
            productoRepo.save(new Producto("Intel Core i5-13600K", "Procesador 14 nucleos LGA1700 hasta 5.1GHz", 1199.00, img("intel-i5-13600k"), 13, cats.get("Procesadores")));
            productoRepo.save(new Producto("Intel Core i7-13700K", "Procesador 16 nucleos LGA1700 hasta 5.4GHz", 1699.00, img("intel-i7-13700k"), 8, cats.get("Procesadores")));

            productoRepo.save(new Producto("ASUS TUF Gaming B550-PLUS", "Placa madre AM4 DDR4 ATX PCIe 4.0", 699.00, img("mb-b550"), 15, cats.get("Placas Madre")));
            productoRepo.save(new Producto("MSI MAG B650 Tomahawk", "Placa madre AM5 DDR5 ATX WiFi", 949.00, img("mb-b650"), 10, cats.get("Placas Madre")));
            productoRepo.save(new Producto("Gigabyte Z790 AORUS Elite", "Placa madre LGA1700 DDR5 ATX", 1149.00, img("mb-z790"), 6, cats.get("Placas Madre")));

            productoRepo.save(new Producto("Corsair Vengeance 16GB DDR4", "Memoria RAM 2x8GB 3200MHz CL16", 249.00, img("ram-vengeance-16"), 30, cats.get("Memorias RAM")));
            productoRepo.save(new Producto("Corsair Vengeance 32GB DDR5", "Memoria RAM 2x16GB 6000MHz RGB", 599.00, img("ram-vengeance-32"), 18, cats.get("Memorias RAM")));

            productoRepo.save(new Producto("Samsung 980 NVMe 1TB", "SSD M.2 PCIe 3.0 hasta 3500MB/s", 329.00, img("ssd-980-1tb"), 25, cats.get("Almacenamiento")));
            productoRepo.save(new Producto("WD Black SN850X 2TB", "SSD M.2 PCIe 4.0 gaming hasta 7300MB/s", 799.00, img("ssd-sn850x-2tb"), 12, cats.get("Almacenamiento")));

            productoRepo.save(new Producto("Corsair RM750 80+ Gold", "Fuente de poder 750W modular certificada", 549.00, img("psu-rm750"), 16, cats.get("Placas Madre")));

            productoRepo.save(new Producto("Samsung Odyssey G7 27\"", "Monitor curvo QHD 240Hz 1ms", 1899.00, img("monitor-odyssey-g7"), 8, cats.get("Monitores")));
            productoRepo.save(new Producto("LG UltraGear 34\" UWQHD", "Monitor ultrawide 160Hz Nano IPS", 2299.00, img("monitor-lg-ultragear"), 6, cats.get("Monitores")));

            productoRepo.save(new Producto("Razer BlackWidow V4 Pro", "Teclado mecanico RGB switches verdes", 899.00, img("teclado-blackwidow"), 22, cats.get("Perifericos")));
            productoRepo.save(new Producto("Logitech G Pro X Superlight", "Mouse inalambrico 63g sensor HERO 25K", 499.00, img("mouse-gpro-superlight"), 28, cats.get("Perifericos")));
            productoRepo.save(new Producto("Logitech Brio 4K", "Webcam 4K UHD para streaming", 599.00, img("webcam-brio"), 14, cats.get("Perifericos")));

            productoRepo.save(new Producto("HyperX Cloud III", "Auriculares gaming 7.1 con microfono", 349.00, img("headset-cloud-iii"), 26, cats.get("Audio")));

            productoRepo.save(new Producto("Secretlab Titan Evo", "Silla gaming ergonomica cuero NEO talla R", 2199.00, img("silla-titan-evo"), 7, cats.get("Sillas Gamer")));
            productoRepo.save(new Producto("Cougar Armor One", "Silla gamer reclinable con cojines", 899.00, img("silla-cougar"), 12, cats.get("Sillas Gamer")));

            productoRepo.save(new Producto("Cooler Master ML240L AIO", "Refrigeracion liquida 240mm ARGB", 449.00, img("cooler-aio"), 15, cats.get("Placas Madre")));

            productoRepo.save(new Producto("PlayStation 5 Slim", "Consola Sony PS5 Slim 1TB edicion digital", 2499.00, img("ps5-slim"), 10, cats.get("Consolas")));
            productoRepo.save(new Producto("Xbox Series X", "Consola Microsoft 1TB 4K 120Hz", 2699.00, img("xbox-series-x"), 8, cats.get("Consolas")));
            productoRepo.save(new Producto("Nintendo Switch OLED", "Consola hibrida pantalla OLED 7\"", 1499.00, img("switch-oled"), 16, cats.get("Consolas")));
            log.info("Seed: productos de tecnologia creados");
        }

        // ===== CLIENTES (nombres reales de RENIEC via apiperu.dev, con respaldo) =====
        if (clienteRepo.count() == 0) {
            clienteRepo.save(clienteReal("70123456", "Carlos", "Quispe Vargas", "987654321", "carlos.quispe@gmail.com", "Av. Arequipa 1234, Lima"));
            clienteRepo.save(clienteReal("72345678", "Maria", "Rojas Gomez", "912345678", "maria.rojas@hotmail.com", "Jr. Cusco 567, San Isidro"));
            clienteRepo.save(clienteReal("75987654", "Diego", "Fernandez Torres", "956123789", "diego.fdz@outlook.com", "Calle Las Begonias 89, Miraflores"));
            clienteRepo.save(clienteReal("76543210", "Lucia", "Mendoza Salas", "999888777", "lucia.mendoza@gmail.com", "Av. Brasil 2345, Jesus Maria"));
            clienteRepo.save(clienteReal("78901234", "Andres", "Castillo Ruiz", "987111222", "andres.castillo@gmail.com", "Av. La Marina 456, San Miguel"));
            clienteRepo.save(clienteReal("71222333", "Valeria", "Torres Nunez", "955444333", "valeria.torres@gmail.com", "Av. Javier Prado 789, San Borja"));
            log.info("Seed: clientes creados");
        }

        // ===== PEDIDOS HISTORICOS (ultimos ~6 meses) =====
        if (pedidoRepo.count() == 0) {
            List<Producto> productos = productoRepo.findAll();
            List<Cliente> clientes = clienteRepo.findAll();
            if (!productos.isEmpty() && !clientes.isEmpty()) {
                Random rnd = new Random(20260711L); // semilla fija => reproducible
                String[] metodos = {"EFECTIVO", "TARJETA", "YAPE", "PLIN", "TRANSFERENCIA"};
                String[] estados = {"PENDIENTE", "PAGADO", "ENVIADO", "ENTREGADO", "ENTREGADO", "CANCELADO"};
                int nPedidos = 40;
                for (int i = 0; i < nPedidos; i++) {
                    Pedido pedido = new Pedido();
                    pedido.setCliente(clientes.get(rnd.nextInt(clientes.size())));
                    pedido.setMetodoPago(metodos[rnd.nextInt(metodos.length)]);
                    pedido.setEstado(estados[rnd.nextInt(estados.length)]);
                    // fecha repartida en los ultimos 180 dias
                    pedido.setFecha(LocalDateTime.now()
                            .minusDays(rnd.nextInt(180))
                            .minusHours(rnd.nextInt(24))
                            .minusMinutes(rnd.nextInt(60)));

                    int nItems = 1 + rnd.nextInt(4); // 1..4 lineas
                    double total = 0;
                    List<Integer> usados = new ArrayList<>();
                    for (int j = 0; j < nItems; j++) {
                        int idx = rnd.nextInt(productos.size());
                        if (usados.contains(idx)) continue; // evita repetir producto en el mismo pedido
                        usados.add(idx);
                        Producto prod = productos.get(idx);
                        int cantidad = 1 + rnd.nextInt(3);
                        PedidoItem item = new PedidoItem(pedido, prod, cantidad, prod.getPrecio());
                        pedido.getItems().add(item);
                        total += item.getSubtotal();
                    }
                    pedido.setTotal(total);
                    pedidoRepo.save(pedido);
                }
                log.info("Seed: {} pedidos historicos creados", nPedidos);
            }
        }

        // ===== ADMIN POR DEFECTO =====
        if (!usuarioRepo.existsByRol(Rol.ADMIN)) {
            Usuario admin = new Usuario();
            admin.setUsername("admin123");
            admin.setPassword(passwordEncoder.encode("gamerstore123"));
            admin.setEmail("admin123@gamerstore.com");
            admin.setNombre("Administrador");
            admin.setTelefono("986969024");
            admin.setRol(Rol.ADMIN);
            usuarioRepo.save(admin);
            log.info("Admin por defecto: admin123 / gamerstore123");
        }
    }

    /** Crea un cliente usando el nombre real de RENIEC (si la API responde); si no, el de respaldo. */
    private Cliente clienteReal(String dni, String nombresFallback, String apellidosFallback,
                                String telefono, String email, String direccion) {
        String nombres = nombresFallback;
        String apellidos = apellidosFallback;
        Optional<ReniecPersona> persona = reniecService.consultarDni(dni);
        if (persona.isPresent()) {
            nombres = persona.get().nombres();
            apellidos = persona.get().apellidos();
            log.info("RENIEC {} -> {} {}", dni, nombres, apellidos);
        }
        Cliente c = new Cliente();
        c.setDni(dni);
        c.setNombres(nombres);
        c.setApellidos(apellidos);
        c.setTelefono(telefono);
        c.setEmail(email);
        c.setDireccion(direccion);
        return c;
    }
}
```

> El `@PrePersist` de `Pedido` ya respeta `fecha` si viene seteada (`if (fecha == null)`), así que las fechas históricas NO se pisan. Igual `Cliente.fechaRegistro`. No hay que tocar los modelos por esto.

- [ ] **Step 2: Compilar** — `./mvnw -q -DskipTests compile` → BUILD SUCCESS.

### Task 7.3: Reset de la base de datos y reseed

**Files:** (ninguno — operación sobre MySQL)

- [ ] **Step 1: Detener el backend** si está corriendo (parar el proceso de `mvnw spring-boot:run`).

- [ ] **Step 2: DROP + CREATE de la BD** (XAMPP MySQL/MariaDB, root sin password):

Run: `"C:/xampp/mysql/bin/mysql.exe" -u root -e "DROP DATABASE IF EXISTS tienda_pc; CREATE DATABASE tienda_pc CHARACTER SET utf8mb4;"`
Expected: sin errores. (Si `mysql.exe` no está en esa ruta, ubicar el binario de XAMPP.)

- [ ] **Step 3: Arrancar el backend** para que Hibernate reconstruya el esquema (con los constraints únicos nuevos) y el seeder cargue la data:

Run (background): `cd "c:/Users/dietr/Desktop/Pruebas/UNIVERSIDAD/gamerstore-main" && ./mvnw -q spring-boot:run`
Esperar a "Started GamerStoreApplication" y a los logs "Seed: ... creados".

- [ ] **Step 4: Verificar la data** por API pública:

Run: `curl -s http://localhost:8080/api/productos | head -c 400`
Expected: JSON con productos de tecnología y `"imagen":"/images/productos/....jpg"`.

Run: `curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/images/productos/rtx-4070.jpg`
Expected: `200` (la imagen se sirve desde el proyecto).

- [ ] **Step 5 (opcional): Commit** — `git add -A && git commit -m "feat(data): catalogo real de tecnologia, imagenes locales y pedidos historicos"`

---

## Fase 8 — Frontend: autenticación, refresh silencioso y contador de sesión

### Task 8.1: Reescribir `client.js` (tokens + Authorization + refresh silencioso + downloadBlob)

**Files:**
- Modify: `frontend/src/api/client.js` (reescritura completa)

- [ ] **Step 1: Reemplazar todo el contenido de `frontend/src/api/client.js`** por:

```js
// Cliente HTTP sobre fetch con JWT: agrega Authorization, hace refresh silencioso
// del access token (5 min) y reintenta la peticion; guarda sesion en localStorage.

const BASE = '/api'

export const USER_KEY = 'gs_user'
export const TOKEN_KEY = 'gs_token'
export const REFRESH_KEY = 'gs_refresh'

export const getUser = () => {
  const raw = localStorage.getItem(USER_KEY)
  return raw ? JSON.parse(raw) : null
}
export const getToken = () => localStorage.getItem(TOKEN_KEY)
export const getRefreshToken = () => localStorage.getItem(REFRESH_KEY)

export function saveSession({ accessToken, refreshToken, user }) {
  if (accessToken) localStorage.setItem(TOKEN_KEY, accessToken)
  if (refreshToken) localStorage.setItem(REFRESH_KEY, refreshToken)
  if (user) localStorage.setItem(USER_KEY, JSON.stringify(user))
}
export function saveUser(user) {
  if (user) localStorage.setItem(USER_KEY, JSON.stringify(user))
}
function saveTokens(accessToken, refreshToken) {
  if (accessToken) localStorage.setItem(TOKEN_KEY, accessToken)
  if (refreshToken) localStorage.setItem(REFRESH_KEY, refreshToken)
}
export function clearSession() {
  localStorage.removeItem(USER_KEY)
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_KEY)
}

function redirigirLogin() {
  if (!location.pathname.startsWith('/admin/login')) location.assign('/admin/login')
}

// --- refresh en un solo vuelo (evita disparos en paralelo) ---
let refreshPromise = null
function refreshAccessToken() {
  const rt = getRefreshToken()
  if (!rt) return Promise.resolve(null)
  if (!refreshPromise) {
    refreshPromise = fetch(BASE + '/auth/refresh', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: rt }),
    })
      .then(async (res) => {
        if (!res.ok) throw new Error('refresh failed')
        const data = await res.json()
        saveTokens(data.accessToken, data.refreshToken)
        return data.accessToken
      })
      .catch(() => {
        clearSession()
        return null
      })
      .finally(() => {
        refreshPromise = null
      })
  }
  return refreshPromise
}

function rawRequest(path, { method, body, token }) {
  const headers = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (token) headers['Authorization'] = 'Bearer ' + token
  return fetch(BASE + path, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })
}

async function request(path, { method = 'GET', body } = {}) {
  const noRefresh = path.startsWith('/auth/login') || path.startsWith('/auth/refresh')
  let res = await rawRequest(path, { method, body, token: getToken() })

  if (res.status === 401 && !noRefresh && getRefreshToken()) {
    const nuevo = await refreshAccessToken()
    if (nuevo) {
      res = await rawRequest(path, { method, body, token: nuevo })
    } else {
      clearSession()
      redirigirLogin()
      throw new Error('Sesión expirada')
    }
  }

  if (!res.ok) {
    if (res.status === 401 && !noRefresh) {
      clearSession()
      redirigirLogin()
    }
    let msg = 'Ocurrió un error'
    try {
      const data = await res.json()
      if (data && data.error) msg = data.error
    } catch {
      /* sin cuerpo JSON */
    }
    throw new Error(msg)
  }

  if (res.status === 204) return null
  const ct = res.headers.get('content-type') || ''
  return ct.includes('application/json') ? res.json() : res.text()
}

// Descarga un archivo (blob) autenticado, p. ej. el reporte PDF.
export async function downloadBlob(path, filename) {
  let res = await rawRequest(path, { method: 'GET', token: getToken() })
  if (res.status === 401 && getRefreshToken()) {
    const nuevo = await refreshAccessToken()
    if (nuevo) res = await rawRequest(path, { method: 'GET', token: nuevo })
  }
  if (!res.ok) {
    let msg = 'No se pudo generar el archivo'
    try {
      const d = await res.json()
      if (d && d.error) msg = d.error
    } catch {
      /* ignore */
    }
    throw new Error(msg)
  }
  const blob = await res.blob()
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(url)
}

export const api = {
  get: (p) => request(p),
  post: (p, body) => request(p, { method: 'POST', body }),
  put: (p, body) => request(p, { method: 'PUT', body }),
  patch: (p, body) => request(p, { method: 'PATCH', body }),
  del: (p) => request(p, { method: 'DELETE' }),
}
```

### Task 8.2: Endpoints (auth me/logout, usuarios, upload, reporte)

**Files:**
- Modify: `frontend/src/api/endpoints.js`

- [ ] **Step 1: Reemplazar el import y `AuthAPI`, y ampliar `AdminAPI`.** Cambiar la línea 1:

```js
import { api } from './client'
```
por:
```js
import { api, getRefreshToken } from './client'
```

Reemplazar el bloque `AuthAPI`:
```js
export const AuthAPI = {
  login: (username, password) => api.post('/auth/login', { username, password }),
  me: () => api.get('/auth/me'),
  logout: () => api.post('/auth/logout', { refreshToken: getRefreshToken() }),
}
```

Dentro de `AdminAPI`, añadir (por ejemplo tras el bloque `pedidos`):
```js
  reportePedidosUrl: (params) => '/admin/pedidos/reporte.pdf' + qs(params || {}),

  usuarios: () => api.get('/admin/usuarios'),
  crearUsuario: (data) => api.post('/admin/usuarios', data),
  actualizarUsuario: (id, data) => api.put('/admin/usuarios/' + id, data),
  eliminarUsuario: (id) => api.del('/admin/usuarios/' + id),

  subirImagen: (formData) => fetch('/api/admin/uploads', {
    method: 'POST',
    headers: { Authorization: 'Bearer ' + (localStorage.getItem('gs_token') || '') },
    body: formData,
  }).then(async (res) => {
    if (!res.ok) {
      let msg = 'No se pudo subir la imagen'
      try { const d = await res.json(); if (d && d.error) msg = d.error } catch {}
      throw new Error(msg)
    }
    return res.json()
  }),
```

> `subirImagen` usa `fetch` directo (multipart) porque `api.post` fuerza `Content-Type: application/json`. Manda el token manualmente. (No hace refresh silencioso; si el token expiró justo al subir, el usuario reintenta — es aceptable para esta pantalla.)

### Task 8.3: AuthContext (login/logout/me con tokens)

**Files:**
- Modify: `frontend/src/auth/AuthContext.jsx` (reescritura)

- [ ] **Step 1: Reemplazar todo `AuthContext.jsx`** por:

```jsx
import { createContext, useContext, useState, useEffect } from 'react'
import { AuthAPI } from '../api/endpoints'
import { saveSession, saveUser, clearSession, getUser, getToken, getRefreshToken } from '../api/client'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => getUser())

  // Al arrancar, si hay sesion guardada, la validamos contra /auth/me
  // (si el access expiro pero el refresh es valido, client.js lo renueva solo).
  useEffect(() => {
    if (getToken() || getRefreshToken()) {
      AuthAPI.me()
        .then((u) => {
          setUser(u)
          saveUser(u)
        })
        .catch(() => {
          clearSession()
          setUser(null)
        })
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const login = async (username, password) => {
    const r = await AuthAPI.login(username, password)
    const u = { username: r.username, nombre: r.nombre, rol: r.rol }
    saveSession({ accessToken: r.accessToken, refreshToken: r.refreshToken, user: u })
    setUser(u)
    return u
  }

  const logout = async () => {
    try {
      await AuthAPI.logout()
    } catch {
      /* aunque falle, limpiamos localmente */
    }
    clearSession()
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, login, logout, isAuth: !!user }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)
```

> `ProtectedRoute.jsx` sigue igual (usa `getUser()`), y ahora el servidor respalda la protección de verdad. `Login.jsx` no cambia (usa `login()` del contexto). `Sidebar.jsx`: el `onClick` de "Cerrar sesión" llama `logout()` — como ahora es async, cambiar a `onClick={async () => { await logout(); navigate('/admin/login') }}` (ver Task 11.2, se toca el Sidebar igual).

### Task 8.4: Componente SessionTimer (contador discreto) + montaje + estilos

**Files:**
- Create: `frontend/src/components/admin/SessionTimer.jsx`
- Modify: `frontend/src/components/admin/AdminLayout.jsx`
- Modify: `frontend/src/index.css` (append)

- [ ] **Step 1: Crear `SessionTimer.jsx`**:

```jsx
import { useEffect, useState } from 'react'
import { getToken } from '../../api/client'

// Lee el claim exp del access token y cuenta atras hasta que expira.
function expDeToken(token) {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    return payload.exp ? payload.exp * 1000 : null
  } catch {
    return null
  }
}

export default function SessionTimer() {
  const [restante, setRestante] = useState(null)

  useEffect(() => {
    const tick = () => {
      const token = getToken()
      const exp = token ? expDeToken(token) : null
      if (!exp) {
        setRestante(null)
        return
      }
      setRestante(Math.max(0, Math.floor((exp - Date.now()) / 1000)))
    }
    tick()
    const id = setInterval(tick, 1000)
    return () => clearInterval(id)
  }, [])

  if (restante === null) return null
  const m = Math.floor(restante / 60)
  const s = restante % 60
  const bajo = restante <= 60

  return (
    <div className={'session-timer' + (bajo ? ' session-timer-low' : '')} title="Tiempo hasta renovar la sesión">
      <i className="bi bi-clock-history" />
      <span>Sesión {m}:{String(s).padStart(2, '0')}</span>
    </div>
  )
}
```

- [ ] **Step 2: Montar en `AdminLayout.jsx`** — importar y renderizar dentro del `<div className="admin ...">`.

Añadir import:
```jsx
import SessionTimer from './SessionTimer.jsx'
```
Y antes del cierre `</div>` del contenedor `admin` (después de `</div>` de `admin-main`), añadir:
```jsx
      <SessionTimer />
```
Es decir el return queda:
```jsx
    <div className={'admin' + (collapsed ? ' collapsed' : '')}>
      <Sidebar onToggle={toggle} collapsed={collapsed} />
      <div className="admin-main">
        <Topbar />
        <div className="admin-content">
          <Outlet />
        </div>
      </div>
      <SessionTimer />
    </div>
```

- [ ] **Step 3: Estilos** — añadir al final de `frontend/src/index.css`:

```css
/* Contador de sesion discreto (esquina inferior derecha del admin) */
.session-timer {
  position: fixed;
  right: 14px;
  bottom: 12px;
  z-index: 60;
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.72rem;
  color: var(--muted, #6b7280);
  background: var(--surface, #ffffff);
  border: 1px solid var(--border, #e5e7eb);
  border-radius: 999px;
  padding: 0.3rem 0.7rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  opacity: 0.8;
  pointer-events: none;
}
.session-timer i { font-size: 0.8rem; }
.session-timer-low {
  color: var(--warning-text, #b45309);
  border-color: var(--warning, #f59e0b);
  opacity: 1;
}
```

> Los nombres de variables (`--muted`, `--surface`, `--border`, `--warning`, `--warning-text`) llevan fallback por si alguno no existe en el `:root`. Ajustar `--surface` si el tema usa otro nombre para el fondo de tarjetas.

- [ ] **Step 4: Verificación** — se hace junto con la Fase 13 (requiere front + back corriendo). Por ahora, arrancar el front y comprobar que no hay errores de import en la consola de Vite.

---

## Fase 9 — Frontend: toast en errores de duplicado

En las páginas CRUD, el error del backend (409 con mensaje) hoy se muestra sólo en el `<Alert>` inline del modal. Añadimos `toast.error(err.message)` en el `catch` del submit para el toast pedido, manteniendo el Alert.

### Task 9.1: Toast en Productos, Categorías y Clientes

**Files:**
- Modify: `frontend/src/pages/admin/AdminProductos.jsx`
- Modify: `frontend/src/pages/admin/AdminCategorias.jsx`
- Modify: `frontend/src/pages/admin/AdminClientes.jsx`

- [ ] **Step 1: En `AdminProductos.jsx`**, en el `catch` del método `guardar` cambiar:
```js
    } catch (err) {
      setFormError(err.message)
    } finally {
```
por:
```js
    } catch (err) {
      setFormError(err.message)
      toast.error(err.message)
    } finally {
```

- [ ] **Step 2: En `AdminCategorias.jsx`** — aplicar el MISMO cambio en el `catch` del submit de crear/editar (añadir `toast.error(err.message)` junto al `setFormError(err.message)`). `toast` ya está disponible vía `useToast()` en esa página (mismo patrón que Productos).

- [ ] **Step 3: En `AdminClientes.jsx`** — aplicar el MISMO cambio en el `catch` del submit (añadir `toast.error(err.message)` junto al `setFormError(err.message)`).

- [ ] **Step 4: Verificación** — al final (Fase 13): crear una categoría/cliente/producto ya existente debe mostrar un toast rojo con el mensaje del backend y NO crear el registro.

---

## Fase 10 — Frontend: subir imagen en el formulario de producto

### Task 10.1: Reemplazar el input URL por selector de archivo con preview

**Files:**
- Modify: `frontend/src/pages/admin/AdminProductos.jsx`

- [ ] **Step 1: Añadir estado de subida** — dentro del componente, junto a los otros `useState`:
```js
  const [subiendo, setSubiendo] = useState(false)
```

- [ ] **Step 2: Añadir el handler de archivo** — antes del `return`, tras la función `guardar`:
```js
  const subirImagen = async (e) => {
    const file = e.target.files?.[0]
    if (!file) return
    setSubiendo(true)
    setFormError('')
    try {
      const fd = new FormData()
      fd.append('file', file)
      const { url } = await AdminAPI.subirImagen(fd)
      setForm((f) => ({ ...f, imagen: url }))
      toast.success('Imagen subida')
    } catch (err) {
      setFormError(err.message)
      toast.error(err.message)
    } finally {
      setSubiendo(false)
    }
  }
```

- [ ] **Step 3: Reemplazar el campo "URL imagen"** dentro del `<form>`. Cambiar:
```jsx
              <div className="field full">
                <label className="label">URL imagen</label>
                <input className="input" type="url" placeholder="https://..." value={form.imagen} onChange={cambiar('imagen')} />
              </div>
```
por:
```jsx
              <div className="field full">
                <label className="label">Imagen del producto</label>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                  {form.imagen ? (
                    <img
                      src={form.imagen}
                      alt="preview"
                      style={{ width: 64, height: 64, objectFit: 'cover', borderRadius: 8, border: '1px solid var(--border)' }}
                    />
                  ) : (
                    <div style={{ width: 64, height: 64, borderRadius: 8, background: 'var(--border)', display: 'grid', placeItems: 'center' }}>
                      <i className="bi bi-image text-muted" />
                    </div>
                  )}
                  <label className="btn btn-outline" style={{ cursor: 'pointer', margin: 0 }}>
                    <i className="bi bi-upload" /> {subiendo ? 'Subiendo...' : 'Subir imagen'}
                    <input type="file" accept="image/*" hidden onChange={subirImagen} disabled={subiendo} />
                  </label>
                </div>
                <small className="text-muted">Se guarda en el proyecto (uploads/productos). Máx 5MB.</small>
              </div>
```

> El preview muestra `form.imagen` = ruta `/images/productos/xxx.jpg`. En dev necesita el proxy `/images` de Vite (Task 12.3 config Vite) para resolver contra :8080. En caso de no querer tocar el guardado del backend: el `imagen` (ruta) se envía igual en el payload de crear/actualizar; el backend lo persiste como string. Sin cambios en `guardar()`.

---

## Fase 11 — Frontend: módulo de Usuarios

### Task 11.1: Página AdminUsuarios

**Files:**
- Create: `frontend/src/pages/admin/AdminUsuarios.jsx`

- [ ] **Step 1: Crear `AdminUsuarios.jsx`** (mismo patrón que AdminProductos: tabla + modal + toast + confirm):

```jsx
import { useEffect, useState } from 'react'
import { AdminAPI } from '../../api/endpoints.js'
import { useTableControls } from '../../hooks/useTableControls.js'
import { useToast } from '../../components/ui/Toast.jsx'
import { useConfirm } from '../../components/ui/Confirm.jsx'
import Modal from '../../components/ui/Modal.jsx'
import Alert from '../../components/ui/Alert.jsx'
import TableToolbar from '../../components/ui/TableToolbar.jsx'
import TableSkeleton from '../../components/ui/TableSkeleton.jsx'
import Pagination from '../../components/ui/Pagination.jsx'

const EMPTY = { username: '', email: '', nombre: '', password: '', telefono: '', rol: 'ADMIN' }
const ROLES = ['ADMIN', 'USUARIO']

export default function AdminUsuarios() {
  const toast = useToast()
  const confirm = useConfirm()

  const [usuarios, setUsuarios] = useState(null)
  const [showModal, setShowModal] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState(EMPTY)
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState('')

  const t = useTableControls(usuarios || [], {
    searchKeys: ['username', 'email', 'nombre'],
    pageSize: 8,
    initialSort: { key: 'username', dir: 'asc' },
  })

  const cargar = () => AdminAPI.usuarios().then(setUsuarios).catch(() => setUsuarios([]))
  useEffect(() => { cargar() }, [])

  const abrirCrear = () => {
    setEditing(null)
    setForm(EMPTY)
    setFormError('')
    setShowModal(true)
  }
  const abrirEditar = (u) => {
    setEditing(u)
    setForm({ username: u.username, email: u.email, nombre: u.nombre, password: '', telefono: u.telefono || '', rol: u.rol })
    setFormError('')
    setShowModal(true)
  }
  const cambiar = (campo) => (e) => setForm((f) => ({ ...f, [campo]: e.target.value }))

  const guardar = async (e) => {
    e.preventDefault()
    setSaving(true)
    setFormError('')
    try {
      if (editing) {
        await AdminAPI.actualizarUsuario(editing.id, form)
        toast.success('Usuario actualizado')
      } else {
        await AdminAPI.crearUsuario(form)
        toast.success('Usuario creado correctamente')
      }
      setShowModal(false)
      cargar()
    } catch (err) {
      setFormError(err.message)
      toast.error(err.message)
    } finally {
      setSaving(false)
    }
  }

  const eliminar = async (u) => {
    const ok = await confirm({
      title: 'Eliminar usuario',
      message: `¿Eliminar al usuario "${u.username}"? Esta acción no se puede deshacer.`,
      confirmText: 'Eliminar',
      danger: true,
    })
    if (!ok) return
    try {
      await AdminAPI.eliminarUsuario(u.id)
      toast.success('Usuario eliminado')
      cargar()
    } catch (err) {
      toast.error(err.message)
    }
  }

  const Th = ({ label, col }) => (
    <th className={'sortable' + (t.sort?.key === col ? ' is-sorted' : '')} onClick={() => t.toggleSort(col)}>
      {label}
      <span className="sort-ind">{t.sort?.key === col ? (t.sort.dir === 'asc' ? '▲' : '▼') : '↕'}</span>
    </th>
  )

  return (
    <>
      <div className="page-head">
        <div>
          <h2>Usuarios del sistema</h2>
          <p>Administra los accesos al panel</p>
        </div>
        <button className="btn btn-primary" onClick={abrirCrear}>
          <i className="bi bi-person-plus" /> Nuevo usuario
        </button>
      </div>

      {usuarios === null ? (
        <TableSkeleton />
      ) : (
        <div className="table-wrap">
          <TableToolbar query={t.query} onSearch={t.onSearch} total={t.total} />
          <div className="table-scroll">
            <table className="table">
              <thead>
                <tr>
                  <Th label="Usuario" col="username" />
                  <Th label="Nombre" col="nombre" />
                  <Th label="Email" col="email" />
                  <Th label="Rol" col="rol" />
                  <th style={{ textAlign: 'right' }}>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {t.paged.length === 0 && (
                  <tr>
                    <td colSpan={5}>
                      <div className="empty"><i className="bi bi-people" /><div>No hay usuarios</div></div>
                    </td>
                  </tr>
                )}
                {t.paged.map((u) => (
                  <tr key={u.id}>
                    <td className="fw-bold">{u.username}</td>
                    <td>{u.nombre}</td>
                    <td>{u.email}</td>
                    <td>
                      <span className={'badge ' + (u.rol === 'ADMIN' ? 'badge-accent' : 'badge-cat')}>{u.rol}</span>
                    </td>
                    <td>
                      <div className="cell-actions">
                        <button className="btn btn-outline btn-icon" onClick={() => abrirEditar(u)} title="Editar">
                          <i className="bi bi-pencil" />
                        </button>
                        <button className="btn btn-danger btn-icon" onClick={() => eliminar(u)} title="Eliminar">
                          <i className="bi bi-trash" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination page={t.page} totalPages={t.totalPages} total={t.total} onPage={t.setPage} />
        </div>
      )}

      {showModal && (
        <Modal
          title={editing ? 'Editar usuario' : 'Nuevo usuario'}
          icon={editing ? 'bi-pencil-square' : 'bi-person-plus'}
          onClose={() => setShowModal(false)}
          footer={
            <>
              <button className="btn btn-ghost" onClick={() => setShowModal(false)}>Cancelar</button>
              <button className="btn btn-primary" onClick={guardar} disabled={saving}>
                <i className="bi bi-check2" /> {saving ? 'Guardando...' : 'Guardar'}
              </button>
            </>
          }
        >
          {formError && <Alert type="error">{formError}</Alert>}
          <form onSubmit={guardar}>
            <div className="form-grid">
              <div className="field">
                <label className="label">Usuario *</label>
                <input className="input" value={form.username} onChange={cambiar('username')} required />
              </div>
              <div className="field">
                <label className="label">Email *</label>
                <input className="input" type="email" value={form.email} onChange={cambiar('email')} required />
              </div>
              <div className="field full">
                <label className="label">Nombre *</label>
                <input className="input" value={form.nombre} onChange={cambiar('nombre')} required />
              </div>
              <div className="field">
                <label className="label">Teléfono</label>
                <input className="input" value={form.telefono} onChange={cambiar('telefono')} />
              </div>
              <div className="field">
                <label className="label">Rol *</label>
                <select className="select" value={form.rol} onChange={cambiar('rol')} required>
                  {ROLES.map((r) => <option key={r} value={r}>{r}</option>)}
                </select>
              </div>
              <div className="field full">
                <label className="label">{editing ? 'Contraseña (dejar vacío para no cambiar)' : 'Contraseña *'}</label>
                <input className="input" type="password" value={form.password} onChange={cambiar('password')} required={!editing} placeholder="••••••••" />
              </div>
            </div>
            <button type="submit" hidden />
          </form>
        </Modal>
      )}
    </>
  )
}
```

### Task 11.2: Ruta + link en Sidebar

**Files:**
- Modify: `frontend/src/App.jsx`
- Modify: `frontend/src/components/admin/Sidebar.jsx`

- [ ] **Step 1: En `App.jsx`** añadir el import y la ruta protegida.

Import (junto a los otros admin):
```jsx
import AdminUsuarios from './pages/admin/AdminUsuarios.jsx'
```
Dentro del bloque `<Route element={<AdminLayout />}>`, tras la ruta de pedidos:
```jsx
          <Route path="/admin/usuarios" element={<AdminUsuarios />} />
```

- [ ] **Step 2: En `Sidebar.jsx`** añadir el link al array `links` (tras "Pedidos"):
```jsx
  { to: '/admin/usuarios', label: 'Usuarios', icon: 'bi-person-badge-fill' },
```

- [ ] **Step 3: Ajustar el logout a async** (por el cambio de Task 8.3). En `Sidebar.jsx` cambiar el `onClick` del botón "Cerrar sesión":
```jsx
          onClick={async () => {
            await logout()
            navigate('/admin/login')
          }}
```

---

## Fase 11B — Frontend: autocompletar DNI (RENIEC) en Clientes

### Task 11B.1: Endpoint + botón "Buscar" que autocompleta desde RENIEC

**Files:**
- Modify: `frontend/src/api/endpoints.js`
- Modify: `frontend/src/pages/admin/AdminClientes.jsx`

- [ ] **Step 1: Añadir el endpoint** en `endpoints.js`, dentro de `AdminAPI` (junto a los de clientes):
```js
  buscarDni: (dni) => api.get('/admin/clientes/reniec/' + dni),
```

- [ ] **Step 2: Estado + handler en `AdminClientes.jsx`** — dentro del componente, junto a los otros `useState`:
```js
  const [buscandoDni, setBuscandoDni] = useState(false)
```
y tras la función `guardar` (antes del `return`):
```js
  const buscarDni = async () => {
    if (!/^\d{8}$/.test(form.dni)) {
      setFormError('Ingresa un DNI de 8 dígitos')
      return
    }
    setBuscandoDni(true)
    setFormError('')
    try {
      const p = await AdminAPI.buscarDni(form.dni)
      setForm((f) => ({ ...f, nombres: p.nombres, apellidos: p.apellidos }))
      toast.success('Datos obtenidos de RENIEC')
    } catch (err) {
      setFormError(err.message)
      toast.error(err.message)
    } finally {
      setBuscandoDni(false)
    }
  }
```

- [ ] **Step 3: Añadir el botón "Buscar"** junto al campo DNI. Reemplazar el bloque del campo DNI:
```jsx
              <div className="field">
                <label className="label">DNI *{editing && ' (no editable)'}</label>
                <input
                  className="input"
                  value={form.dni}
                  onChange={cambiar('dni')}
                  pattern="[0-9]{8}"
                  maxLength={8}
                  placeholder="8 dígitos"
                  required
                  disabled={!!editing}
                />
              </div>
```
por (input + botón que consulta RENIEC; sólo activo al crear, porque el DNI no se edita):
```jsx
              <div className="field">
                <label className="label">DNI *{editing && ' (no editable)'}</label>
                <div style={{ display: 'flex', gap: '0.4rem' }}>
                  <input
                    className="input"
                    value={form.dni}
                    onChange={cambiar('dni')}
                    pattern="[0-9]{8}"
                    maxLength={8}
                    placeholder="8 dígitos"
                    required
                    disabled={!!editing}
                  />
                  {!editing && (
                    <button
                      type="button"
                      className="btn btn-outline"
                      onClick={buscarDni}
                      disabled={buscandoDni}
                      title="Autocompletar con RENIEC"
                    >
                      <i className="bi bi-search" /> {buscandoDni ? '...' : 'Buscar'}
                    </button>
                  )}
                </div>
              </div>
```

- [ ] **Step 4: Verificación** (Fase 13): en "Nuevo cliente", escribir `70123456` y pulsar "Buscar" → nombres/apellidos se autocompletan con datos reales de RENIEC y sale toast "Datos obtenidos de RENIEC".

---

## Fase 12 — Frontend: Top productos en Dashboard + botón Descargar PDF

### Task 12.1: Panel "Más vendidos" en el Dashboard

**Files:**
- Modify: `frontend/src/pages/admin/Dashboard.jsx`

- [ ] **Step 1: Asegurar default de `topProductos`** — en el `.catch` del `AdminAPI.dashboard()` añadir `topProductos: []` al objeto por defecto:
```js
      .catch(() => setData({ totalProductos: 0, totalCategorias: 0, totalClientes: 0, stockBajo: [], topProductos: [] }))
```

- [ ] **Step 2: Añadir el panel** dentro del `<div className="panel-grid">`, como tercer panel (o reemplazando "Acciones rápidas" por una fila nueva). Insertar tras el panel de "Stock bajo" un nuevo panel:
```jsx
        <div className="panel">
          <div className="panel-head">
            <h3><i className="bi bi-trophy-fill" style={{ color: 'var(--accent)' }} /> Más vendidos</h3>
          </div>
          {(!data.topProductos || data.topProductos.length === 0) ? (
            <div className="empty"><i className="bi bi-bar-chart" /><p>Aún no hay ventas registradas</p></div>
          ) : (
            <div className="stock-list">
              {data.topProductos.map((p, i) => (
                <div key={p.productoId} className="stock-card">
                  <img src={p.imagen} alt={p.productoNombre} />
                  <div>
                    <div className="fw-bold" style={{ fontSize: '0.9rem' }}>
                      <span style={{ color: 'var(--accent)' }}>#{i + 1}</span> {p.productoNombre}
                    </div>
                    <div className="text-muted" style={{ fontSize: '0.82rem' }}>
                      Vendidos: <strong style={{ color: 'var(--accent)' }}>{p.cantidad}</strong> unidades
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
```

> El grid `panel-grid` es de 2 columnas; al añadir un tercer panel se reacomoda. Si se ve apretado, mover "Acciones rápidas" debajo. Verificar visualmente en Fase 13.

### Task 12.2: Botón "Descargar PDF" en Pedidos (con filtros)

**Files:**
- Modify: `frontend/src/pages/admin/AdminPedidos.jsx`

- [ ] **Step 1: Imports** — añadir `downloadBlob` y usar `AdminAPI.reportePedidosUrl`. En la línea de import de endpoints ya está `AdminAPI`; añadir import del cliente:
```jsx
import { downloadBlob } from '../../api/client.js'
```

- [ ] **Step 2: Estado de filtros del reporte + handler** — dentro del componente añadir:
```js
  const [repDesde, setRepDesde] = useState('')
  const [repHasta, setRepHasta] = useState('')
  const [repEstado, setRepEstado] = useState('')
  const [descargando, setDescargando] = useState(false)

  const descargarPDF = async () => {
    setDescargando(true)
    try {
      const url = AdminAPI.reportePedidosUrl({ desde: repDesde, hasta: repHasta, estado: repEstado })
      await downloadBlob(url, 'reporte-pedidos.pdf')
    } catch (err) {
      toast.error(err.message)
    } finally {
      setDescargando(false)
    }
  }
```

- [ ] **Step 3: UI del reporte** — en la `page-head`, junto al botón "Nuevo pedido", añadir el botón de PDF y (opcional) filtros. Cambiar el bloque del header:
```jsx
      <div className="page-head">
        <div>
          <h2>Pedidos</h2>
          <p>Registra y gestiona las ventas de la tienda</p>
        </div>
        <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'flex-end', flexWrap: 'wrap' }}>
          <div className="field" style={{ margin: 0 }}>
            <label className="label" style={{ fontSize: '0.72rem' }}>Desde</label>
            <input className="input" type="date" value={repDesde} onChange={(e) => setRepDesde(e.target.value)} />
          </div>
          <div className="field" style={{ margin: 0 }}>
            <label className="label" style={{ fontSize: '0.72rem' }}>Hasta</label>
            <input className="input" type="date" value={repHasta} onChange={(e) => setRepHasta(e.target.value)} />
          </div>
          <div className="field" style={{ margin: 0 }}>
            <label className="label" style={{ fontSize: '0.72rem' }}>Estado</label>
            <select className="select" value={repEstado} onChange={(e) => setRepEstado(e.target.value)}>
              <option value="">Todos</option>
              {ESTADOS.map((s) => <option key={s} value={s}>{s}</option>)}
            </select>
          </div>
          <button className="btn btn-outline" onClick={descargarPDF} disabled={descargando}>
            <i className="bi bi-file-earmark-pdf" /> {descargando ? 'Generando...' : 'Descargar PDF'}
          </button>
          <button className="btn btn-primary" onClick={abrirCrear}>
            <i className="bi bi-bag-plus" /> Nuevo pedido
          </button>
        </div>
      </div>
```

### Task 12.3: Proxy de `/images` en Vite (dev)

**Files:**
- Modify: `frontend/vite.config.js`

- [ ] **Step 1: Añadir `/images` al proxy.** Cambiar el bloque `proxy`:
```js
    proxy: {
      '/api': 'http://localhost:8080',
    },
```
por:
```js
    proxy: {
      '/api': 'http://localhost:8080',
      '/images': 'http://localhost:8080',
    },
```

- [ ] **Step 2 (opcional): Commit** — `git add -A && git commit -m "feat(front): sesion JWT + refresh + usuarios + top productos + reporte PDF + imagenes"`

---

## Fase 13 — Verificación end-to-end (criterios de aceptación)

### Task 13.1: Tests backend + arranque + recorrido en navegador

**Files:** (ninguno — verificación)

- [ ] **Step 1: Tests backend en verde**

Run: `cd "c:/Users/dietr/Desktop/Pruebas/UNIVERSIDAD/gamerstore-main" && ./mvnw -q test`
Expected: BUILD SUCCESS. Si falla `crearPedidoValido` por el total, confirmar que se aplicó el matcher `greaterThan(0.0)` (Task 1.8). Si algún admin test da 401, confirmar `@WithMockUser(roles = "ADMIN")`.

- [ ] **Step 2: Arrancar backend y frontend** (dos procesos en background):

Backend: `./mvnw -q spring-boot:run` → esperar "Started GamerStoreApplication".
Frontend: `cd frontend && npm run dev` → esperar "VITE ready" en :5173.

- [ ] **Step 3: Verificar protección real (sin token → 401)**

Run: `curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/admin/productos`
Expected: `401`.

Run (login):
```
curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d "{\"username\":\"admin123\",\"password\":\"gamerstore123\"}"
```
Expected: JSON con `accessToken`, `refreshToken`, `rol:"ADMIN"`. Copiar el `accessToken`.

Run (con token):
```
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/admin/productos -H "Authorization: Bearer <ACCESS>"
```
Expected: `200`.

- [ ] **Step 4: Recorrido en navegador** — abrir http://localhost:5173 y comprobar cada criterio del spec:
  1. Ir a `/admin/productos` sin login → redirige a `/admin/login`.
  2. Login `admin123 / gamerstore123` → entra al panel. Aparece el **contador "Sesión 5:00"** abajo-derecha, cuenta atrás; al pasar 5 min (o forzando una llamada tras expirar) se renueva solo sin sacar al usuario (queda en ~5:00). En el último minuto se ve ámbar.
  3. Recargar la página estando logueado → sigue la sesión (via `/auth/me`).
  4. Productos: se ven ~28 con **imágenes locales** (`/images/productos/...`) y precios en **S/**. Crear un producto con un nombre existente → **toast rojo** "Ya existe un producto con ese nombre". Crear/editar producto **subiendo una imagen** (archivo) → preview y se guarda; el archivo aparece en `uploads/productos/`.
  5. Categorías: crear una repetida → toast "La categoría ya existe".
  6. Clientes: los ~6 sembrados muestran **nombres reales de RENIEC**. Crear con DNI repetido → toast "El DNI ya está registrado"; con email repetido → "Ese email ya está registrado". En "Nuevo cliente", escribir un DNI (p. ej. `70123456`) y pulsar **"Buscar"** autocompleta nombres/apellidos desde RENIEC (apiperu.dev).
  7. Usuarios (nuevo módulo en el sidebar): crear/editar/eliminar; usuario o email duplicado → toast; intentar borrar el último ADMIN → toast "No puedes eliminar el último administrador".
  8. Dashboard: KPIs correctos y panel **"Más vendidos"** con datos de los pedidos.
  9. Pedidos: ~40 con fechas repartidas en ~6 meses. Botón **"Descargar PDF"** baja un PDF válido con tabla y total; probar con filtro de fecha/estado.
  10. Tienda pública (Home/Catálogo/Detalle/Contacto) muestra las imágenes locales y el botón "Cotizar por WhatsApp".
  11. Cerrar sesión → el refresh queda revocado; volver a entrar funciona.
  12. Revisar la consola del navegador y los logs del backend: **sin errores** ni warning de dialecto.

- [ ] **Step 5: Reporte final** — anotar cualquier slug de imagen que haya quedado con foto genérica (Task 7.1) y confirmar que los 12 criterios pasan. Si algo falla, volver a la tarea correspondiente.

- [ ] **Step 6 (opcional): Commit final** — `git add -A && git commit -m "chore: verificacion end-to-end de la version final"`

---

## Self-review (cobertura del spec)

- **§3.1 Seguridad JWT + refresh (5 min + contador):** Fase 1 (JWT/filter/config/refresh) + Fase 8 (client refresh silencioso, AuthContext, SessionTimer). ✓
- **§3.2 Validación de únicos → toast:** Fase 2 (producto nombre, cliente email, DNI, categoría) + Fase 3 (usuario username/email) + Fase 9 (toast). ✓
- **§3.3 BD limpia + data real + pedidos históricos:** Fase 7 (seeder + reset). ✓
- **§3.4 Imágenes en el proyecto + subida admin:** Fase 4 (upload + resource handler) + Fase 7.1 (descarga) + Fase 10 (form) + Fase 12.3 (proxy Vite). ✓
- **§3.5 Módulo Usuarios:** Fase 3 (backend) + Fase 11 (frontend). ✓
- **§3.6 Top productos Dashboard:** Fase 5 (backend) + Fase 12.1 (frontend). ✓
- **§3.7 Reporte PDF:** Fase 6 (backend) + Fase 12.2 (frontend). ✓
- **§3.8 Cero errores:** Fase 0.2 (dialecto) + Fase 1.8 (tests) + Fase 13 (verificación). ✓
- **§3.9 Datos reales RENIEC (apiperu.dev):** Fase 0.2 (props token, `enabled=false` en test) + Fase 3B (ReniecService + endpoint) + Fase 7.2 (seeder con nombres reales) + Fase 11B (autocompletar DNI). ✓
- **Fuera de alcance:** carrito/checkout y pasarela — no se implementan. ✓

**Consistencia de tipos verificada:** `LoginResponse{accessToken,refreshToken,username,nombre,rol}` (back) ↔ `AuthContext.login` lee `r.accessToken/r.refreshToken/r.username/...`. `TokenResponse{accessToken,refreshToken}` ↔ `refreshAccessToken` lee `data.accessToken/data.refreshToken`. `AuthUser{username,nombre,rol}` ↔ `/auth/me`. `TopProductoDTO{productoId,productoNombre,imagen,cantidad}` ↔ Dashboard usa `p.productoId/p.productoNombre/p.imagen/p.cantidad`. `UploadResponse{url}` ↔ `subirImagen` lee `{ url }`. Endpoint reporte `/admin/pedidos/reporte.pdf` ↔ `reportePedidosUrl`. ✓

