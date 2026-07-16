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

// Maneja el ciclo de vida del refresh token (tabla en BD, a diferencia del access
// token que es stateless): crearlo al loguear, rotarlo al renovar el access token
// y revocarlo al hacer logout.
@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repo;
    private final long refreshExpMs;

    public RefreshTokenService(RefreshTokenRepository repo,
                               @Value("${app.jwt.refresh-expiration-ms}") long refreshExpMs) {
        this.repo = repo;
        this.refreshExpMs = refreshExpMs;
    }

    // Genera un refresh token opaco (UUID sin guiones, no es un JWT) asociado al
    // usuario, con su fecha de expiracion y sin revocar.
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

    // Invalida un refresh token (por ejemplo al hacer logout) sin borrarlo, solo
    // marcandolo como revocado para que rotar() ya no lo acepte.
    @Transactional
    public void revocar(String token) {
        repo.findByToken(token).ifPresent(rt -> {
            rt.setRevocado(true);
            repo.save(rt);
        });
    }
}
