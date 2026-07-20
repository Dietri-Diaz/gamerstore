package com.gamerstore.app.dto;

import jakarta.validation.constraints.*;

/** Datos del comprador que llegan desde la tienda pública. */
public record CheckoutClienteDTO(
        @NotBlank(message = "El DNI es obligatorio") @Pattern(regexp = "\\d{8}", message = "El DNI debe tener 8 dígitos") String dni,
        @NotBlank(message = "Los nombres son obligatorios") @Size(max = 100) String nombres,
        @NotBlank(message = "Los apellidos son obligatorios") @Size(max = 100) String apellidos,
        @Pattern(regexp = "\\d{0,15}", message = "Teléfono inválido") String telefono,
        @Email(message = "Email inválido") @Size(max = 120) String email,
        @Size(max = 200) String direccion) {}
