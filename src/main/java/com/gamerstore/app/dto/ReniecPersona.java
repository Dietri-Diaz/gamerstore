package com.gamerstore.app.dto;

// Datos de una persona obtenidos por consulta a RENIEC (a partir del DNI)
public record ReniecPersona(String nombres, String apellidos, String nombreCompleto) {}
