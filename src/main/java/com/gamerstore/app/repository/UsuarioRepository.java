package com.gamerstore.app.repository;

import com.gamerstore.app.model.Rol;
import com.gamerstore.app.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * Migración: pasa a ADMIN cualquier fila que haya quedado con el rol viejo (USUARIO) o sin rol.
     * Es un UPDATE masivo de JPQL, o sea que se traduce a un solo SQL y NO carga las entidades en
     * memoria. Eso es justo lo que necesitamos: como el enum Rol ya no tiene la constante USUARIO,
     * si Hibernate intentara "hidratar" una fila con rol='USUARIO' reventaría al convertir el texto
     * al enum. Al no leerlas, solo las corrige.
     * Devuelve cuántas filas migró (0 si no había ninguna, por eso es idempotente).
     */
    @Modifying
    @Transactional
    @Query("UPDATE Usuario u SET u.rol = :admin WHERE u.rol IS NULL OR u.rol <> :admin")
    int migrarRolesAAdmin(@org.springframework.data.repository.query.Param("admin") Rol admin);
}
