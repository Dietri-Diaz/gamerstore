package com.gamerstore.app.mapper;

import com.gamerstore.app.dto.CategoriaDTO;
import com.gamerstore.app.model.Categoria;
import org.springframework.stereotype.Component;

/** Mapper: convierte la entidad Categoria a su DTO. */
@Component
public class CategoriaMapper {

    public CategoriaDTO toDTO(Categoria c) {
        return new CategoriaDTO(c.getId(), c.getNombre());
    }
}
