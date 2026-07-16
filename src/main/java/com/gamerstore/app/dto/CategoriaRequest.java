package com.gamerstore.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Datos para crear/editar una categoría
public record CategoriaRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 60, message = "El nombre no puede superar 60 caracteres")
        String nombre) {}
