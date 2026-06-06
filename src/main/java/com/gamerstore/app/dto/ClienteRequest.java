package com.gamerstore.app.dto;

public record ClienteRequest(String dni, String nombres, String apellidos,
                             String telefono, String email, String direccion) {}
