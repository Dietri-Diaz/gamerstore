package com.gamerstore.app.service;

import com.gamerstore.app.model.Usuario;
import com.gamerstore.app.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository repo;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository repo, PasswordEncoder passwordEncoder) {
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<Usuario> autenticar(String loginId, String password) {
        Optional<Usuario> u = repo.findByUsername(loginId);
        if (u.isEmpty()) u = repo.findByEmail(loginId);
        return u.filter(x -> passwordEncoder.matches(password, x.getPassword()));
    }

    public Optional<Usuario> porId(Long id) {
        return repo.findById(id);
    }

    public Optional<Usuario> porUsername(String username) {
        return repo.findByUsername(username);
    }
}
