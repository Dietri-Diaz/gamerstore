package com.gamerstore.app.config.security;

import com.gamerstore.app.model.Usuario;
import com.gamerstore.app.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// Puente entre Spring Security y la tabla Usuario: le dice al framework como buscar
// un usuario y con que credenciales/roles construir el UserDetails que usa en el
// filtro y en el AuthenticationManager (login).
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository repo;

    public CustomUserDetailsService(UsuarioRepository repo) {
        this.repo = repo;
    }

    // Busca al usuario por username y, si no existe, por email (permite loguear con
    // cualquiera de los dos). Mapea el rol de la app (ADMIN/CLIENTE) al formato
    // "ROLE_X" que espera Spring Security para las reglas de autorizacion.
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
