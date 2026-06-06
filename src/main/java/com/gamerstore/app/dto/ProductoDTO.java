package com.gamerstore.app.dto;

import com.gamerstore.app.model.Producto;

public record ProductoDTO(Long id, String nombre, String descripcion, double precio,
                          String imagen, int stock, Long categoriaId, String categoriaNombre) {

    public static ProductoDTO from(Producto p) {
        return new ProductoDTO(
                p.getId(),
                p.getNombre(),
                p.getDescripcion(),
                p.getPrecio(),
                p.getImagen(),
                p.getStock(),
                p.getCategoria() != null ? p.getCategoria().getId() : null,
                p.getCategoria() != null ? p.getCategoria().getNombre() : null
        );
    }
}
