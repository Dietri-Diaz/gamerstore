package com.gamerstore.app.dto;

// Datos mínimos del usuario autenticado (para el contexto de sesión)
public record AuthUser(String username, String nombre, String rol) {}
