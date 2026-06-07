package com.gamerstore.app.dto;

public record ProductoDTO(Long id, String nombre, String descripcion, double precio,
                          String imagen, int stock, Long categoriaId, String categoriaNombre) {}
