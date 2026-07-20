package com.gamerstore.app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Datos con los que un comprador que YA es cliente se identifica en el checkout.
 * Pedimos DNI + correo (los dos deben coincidir) para no exponer los datos de un
 * cliente a cualquiera que solo conozca su DNI.
 */
public record VerificarClienteRequest(
        @NotBlank(message = "El DNI es obligatorio")
        @Pattern(regexp = "\\d{8}", message = "El DNI debe tener 8 dígitos")
        String dni,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Correo inválido")
        String email) {}
