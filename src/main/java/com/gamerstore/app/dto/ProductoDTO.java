package com.gamerstore.app.dto;

// Datos de un producto para mostrar en el front
public record ProductoDTO(Long id, String nombre, String descripcion, double precio,
                          String imagen, int stock, Long categoriaId, String categoriaNombre) {}
