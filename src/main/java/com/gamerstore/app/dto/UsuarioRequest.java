package com.gamerstore.app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Datos para crear/editar un usuario del sistema
public record UsuarioRequest(
        @NotBlank(message = "El usuario es obligatorio")
        @Size(min = 3, max = 50, message = "El usuario debe tener entre 3 y 50 caracteres")
        String username,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "Email inválido")
        @Size(max = 120)
        String email,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100)
        String nombre,

        // En creación es obligatoria; en edición puede venir vacía (no se cambia).
        String password,

        @Size(max = 15)
        String telefono,

        @NotBlank(message = "El rol es obligatorio")
        String rol
) {}
