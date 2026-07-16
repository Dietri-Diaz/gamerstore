package com.gamerstore.app.dto;

// Datos que devuelve el login: tokens + info del usuario
public record LoginResponse(String accessToken, String refreshToken,
                            String username, String nombre, String rol) {}
