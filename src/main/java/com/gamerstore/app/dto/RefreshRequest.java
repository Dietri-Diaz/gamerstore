package com.gamerstore.app.dto;

import jakarta.validation.constraints.NotBlank;

// Datos para pedir un nuevo access token usando el refresh token
public record RefreshRequest(@NotBlank(message = "El refresh token es obligatorio") String refreshToken) {}
