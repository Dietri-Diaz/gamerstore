package com.gamerstore.app.dto;

import com.gamerstore.app.model.Categoria;

public record CategoriaDTO(Long id, String nombre) {

    public static CategoriaDTO from(Categoria c) {
        return new CategoriaDTO(c.getId(), c.getNombre());
    }
}
