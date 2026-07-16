package com.gamerstore.app.mapper;

import com.gamerstore.app.dto.ProductoDTO;
import com.gamerstore.app.model.Producto;
import org.springframework.stereotype.Component;

/**
 * Mapper: convierte la entidad Producto a su DTO.
 * Mantener el mapeo en una clase aparte (capa Mapper) deja los controladores
 * y servicios más limpios y centraliza la conversión entidad ↔ DTO.
 */
@Component
public class ProductoMapper {

    // Convierte un Producto (entidad) en ProductoDTO
    public ProductoDTO toDTO(Producto p) {
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
