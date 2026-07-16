package com.gamerstore.app.repository;

import com.gamerstore.app.model.Rol;
import com.gamerstore.app.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Acceso a datos de usuarios. */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    // findByUsername: busca un usuario por su username.
    Optional<Usuario> findByUsername(String username);
    // findByEmail: busca un usuario por su email.
    Optional<Usuario> findByEmail(String email);
    // existsByUsername: ¿ya existe ese username?
    boolean existsByUsername(String username);
    // existsByEmail: ¿ya existe ese email?
    boolean existsByEmail(String email);
    // existsByRol: ¿hay al menos un usuario con ese rol?
    boolean existsByRol(Rol rol);
    // existsByUsernameAndIdNot: ¿existe otro usuario (id distinto) con ese mismo username?
    boolean existsByUsernameAndIdNot(String username, Long id);
    // existsByEmailAndIdNot: ¿existe otro usuario (id distinto) con ese mismo email?
    boolean existsByEmailAndIdNot(String email, Long id);
    // countByRol: cuenta cuántos usuarios tienen ese rol (se usa para no dejar el sistema sin administradores).
    long countByRol(Rol rol);
    // findAllByOrderByUsernameAsc: lista todos los usuarios ordenados por username.
    java.util.List<Usuario> findAllByOrderByUsernameAsc();
}
