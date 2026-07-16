package com.gamerstore.app.dto;

import java.time.LocalDateTime;

// Datos de un usuario del sistema para mostrar en el front
public record UsuarioDTO(Long id, String username, String email, String nombre,
                         String telefono, String rol, LocalDateTime fechaRegistro) {}
