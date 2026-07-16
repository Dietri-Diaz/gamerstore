package com.gamerstore.app.mapper;

import com.gamerstore.app.dto.UsuarioDTO;
import com.gamerstore.app.model.Usuario;
import org.springframework.stereotype.Component;

// Mapper: convierte la entidad Usuario a su DTO.
@Component
public class UsuarioMapper {
    // Convierte un Usuario (entidad) en UsuarioDTO
    public UsuarioDTO toDTO(Usuario u) {
        return new UsuarioDTO(u.getId(), u.getUsername(), u.getEmail(), u.getNombre(),
                u.getTelefono(), u.getRol().name(), u.getFechaRegistro());
    }
}
