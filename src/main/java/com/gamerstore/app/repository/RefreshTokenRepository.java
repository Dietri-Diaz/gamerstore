package com.gamerstore.app.repository;

import com.gamerstore.app.model.RefreshToken;
import com.gamerstore.app.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** Acceso a datos de refresh tokens (sesión persistente / renovación de JWT). */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    // findByToken: busca un refresh token por su valor.
    Optional<RefreshToken> findByToken(String token);

    // revocarTodosDe: marca como revocados todos los refresh tokens activos de ese usuario (logout / invalidar sesiones).
    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken r SET r.revocado = true WHERE r.usuario = :usuario AND r.revocado = false")
    void revocarTodosDe(Usuario usuario);
}
