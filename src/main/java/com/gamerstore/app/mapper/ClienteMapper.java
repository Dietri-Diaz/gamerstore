package com.gamerstore.app.mapper;

import com.gamerstore.app.dto.ClienteDTO;
import com.gamerstore.app.model.Cliente;
import org.springframework.stereotype.Component;

/** Mapper: convierte la entidad Cliente a su DTO. */
@Component
public class ClienteMapper {

    // Convierte un Cliente (entidad) en ClienteDTO
    public ClienteDTO toDTO(Cliente c) {
        return new ClienteDTO(
                c.getId(),
                c.getDni(),
                c.getNombres(),
                c.getApellidos(),
                c.getTelefono(),
                c.getEmail(),
                c.getDireccion(),
                c.getNombreCompleto(),
                c.getFechaRegistro()
        );
    }
}
