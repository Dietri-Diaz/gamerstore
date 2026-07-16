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

    // La clave secreta y el tiempo de expiracion vienen de application.properties;
    // secret se convierte en una SecretKey HMAC valida para firmar/verificar tokens.
    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.access-expiration-ms}") long accessExpMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpMs = accessExpMs;
    }

    // Construye y firma (HMAC-SHA256 con la clave "key") un access token con el usuario
    // como subject y datos utiles (rol, nombre, id) como claims, con fecha de expiracion
    // calculada a partir de accessExpMs.
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

    // Lee el subject (username) del token sin validar nada mas; se usa en el filtro
    // para saber a quien cargar antes de verificar la firma/expiracion.
    public String extraerUsername(String token) {
        return parse(token).getSubject();
    }

    // Verifica que el token pertenezca al username indicado y que no haya expirado.
    // Si la firma es invalida o el token esta mal formado, parse() lanza excepcion
    // y se considera no valido (false).
    public boolean esValido(String token, String username) {
        try {
            Claims c = parse(token);
            return c.getSubject().equals(username) && c.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    // Decodifica el token y valida la firma contra "key"; si alguien lo altero o usa
    // otra clave, esto lanza excepcion (JWT invalido).
    private Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
