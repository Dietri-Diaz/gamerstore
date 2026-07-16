package com.gamerstore.app.service;

import com.gamerstore.app.model.Rol;
import com.gamerstore.app.model.Usuario;
import com.gamerstore.app.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

/** Reglas de negocio de usuarios: autenticación, alta/edición con username y email únicos, y protección del último admin. */
@Service
public class UsuarioService {

    private final UsuarioRepository repo;
    private final PasswordEncoder passwordEncoder;

    // Inyecta el repositorio de usuarios y el encoder de contraseñas.
    public UsuarioService(UsuarioRepository repo, PasswordEncoder passwordEncoder) {
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
    }

    /** Busca el usuario por username o, si no existe, por email, y valida la contraseña contra el hash guardado. */
    public Optional<Usuario> autenticar(String loginId, String password) {
        Optional<Usuario> u = repo.findByUsername(loginId);
        if (u.isEmpty()) u = repo.findByEmail(loginId);
        return u.filter(x -> passwordEncoder.matches(password, x.getPassword()));
    }

    // Busca un usuario por id.
    public Optional<Usuario> porId(Long id) {
        return repo.findById(id);
    }

    // Busca un usuario por su username.
    public Optional<Usuario> porUsername(String username) {
        return repo.findByUsername(username);
    }

    /** Busca por username y, si no, por email. Para armar el token tras el login. */
    public Optional<Usuario> buscar(String loginId) {
        return repo.findByUsername(loginId).or(() -> repo.findByEmail(loginId));
    }

    // Lista los usuarios ordenados por username.
    public List<Usuario> listar() {
        return repo.findAllByOrderByUsernameAsc();
    }

    // Cuenta el total de usuarios.
    public long total() {
        return repo.count();
    }

    // Convierte el texto de rol al enum Rol; si no es un valor válido, responde 400.
    private Rol parseRol(String rol) {
        try {
            return Rol.valueOf(rol.trim().toUpperCase());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rol inválido");
        }
    }

    /** Crea un usuario validando contraseña obligatoria y que username/email no estén ya registrados; guarda la contraseña encriptada. */
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

    /** Actualiza los datos enviados; si cambia username o email, valida que no choquen con los de otro usuario. */
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

    /** Elimina un usuario, salvo que sea el último administrador del sistema. */
    @Transactional
    public void eliminar(Long id) {
        Usuario u = repo.findById(id).orElseThrow();
        if (u.getRol() == Rol.ADMIN && repo.countByRol(Rol.ADMIN) <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No puedes eliminar el último administrador");
        }
        repo.deleteById(id);
    }
}
