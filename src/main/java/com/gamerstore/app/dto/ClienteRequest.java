package com.gamerstore.app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Datos para crear/editar un cliente
public record ClienteRequest(
        @NotBlank(message = "El DNI es obligatorio")
        @Pattern(regexp = "\\d{8}", message = "El DNI debe tener 8 dígitos")
        String dni,

        @NotBlank(message = "Los nombres son obligatorios")
        @Size(max = 100, message = "Los nombres son muy largos")
        String nombres,

        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(max = 100, message = "Los apellidos son muy largos")
        String apellidos,

        @Pattern(regexp = "\\d{0,15}", message = "El teléfono solo admite dígitos (máx. 15)")
        String telefono,

        @Email(message = "El email no es válido")
        @Size(max = 120, message = "El email es muy largo")
        String email,

        @Size(max = 200, message = "La dirección es muy larga")
        String direccion) {}
