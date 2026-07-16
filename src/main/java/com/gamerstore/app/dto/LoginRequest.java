package com.gamerstore.app.dto;

import jakarta.validation.constraints.NotBlank;

// Credenciales que envía el usuario para iniciar sesión
public record LoginRequest(
        @NotBlank(message = "El usuario es obligatorio")
        String username,

        @NotBlank(message = "La contraseña es obligatoria")
        String password) {}
