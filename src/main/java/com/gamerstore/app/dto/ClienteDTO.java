package com.gamerstore.app.dto;

import java.time.LocalDateTime;

public record ClienteDTO(Long id, String dni, String nombres, String apellidos,
                         String telefono, String email, String direccion,
                         String nombreCompleto, LocalDateTime fechaRegistro) {}
