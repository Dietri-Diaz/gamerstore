package com.gamerstore.app.dto;

public record ProductoRequest(String nombre, String descripcion, Double precio,
                              Integer stock, String imagen, Long categoriaId) {}
