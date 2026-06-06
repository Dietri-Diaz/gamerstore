package com.gamerstore.app.dto;

import com.gamerstore.app.model.Cliente;

import java.time.LocalDateTime;

public record ClienteDTO(Long id, String dni, String nombres, String apellidos,
                         String telefono, String email, String direccion,
                         String nombreCompleto, LocalDateTime fechaRegistro) {

    public static ClienteDTO from(Cliente c) {
        return new ClienteDTO(
                c.getId(), c.getDni(), c.getNombres(), c.getApellidos(),
                c.getTelefono(), c.getEmail(), c.getDireccion(),
                c.getNombreCompleto(), c.getFechaRegistro()
        );
    }
}
