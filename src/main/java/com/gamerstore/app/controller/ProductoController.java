package com.gamerstore.app.controller;

import com.gamerstore.app.dto.ProductoDTO;
import com.gamerstore.app.dto.ProductoDetalleDTO;
import com.gamerstore.app.mapper.ProductoMapper;
import com.gamerstore.app.model.Producto;
import com.gamerstore.app.service.ProductoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Endpoints publicos del catalogo (no requieren login). */
@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService productoService;
    private final ProductoMapper productoMapper;

    public ProductoController(ProductoService productoService, ProductoMapper productoMapper) {
        this.productoService = productoService;
        this.productoMapper = productoMapper;
    }

    // GET /api/productos: lista productos del catálogo, filtrando opcionalmente por categoría y/o texto de búsqueda.
    @GetMapping
    public List<ProductoDTO> listar(@RequestParam(required = false) String categoria,
                                    @RequestParam(required = false) String q) {
        return productoService.filtrar(categoria, q).stream().map(productoMapper::toDTO).toList();
    }

    // GET /api/productos/{id}: trae el detalle de un producto y hasta 4 productos relacionados de su misma categoría.
    @GetMapping("/{id}")
    public ProductoDetalleDTO detalle(@PathVariable Long id) {
        Producto p = productoService.porId(id).orElseThrow();
        List<ProductoDTO> relacionados = p.getCategoria() == null ? List.of() :
                productoService.porCategoria(p.getCategoria()).stream()
                        .filter(x -> !x.getId().equals(id))
                        .limit(4)
                        .map(productoMapper::toDTO)
                        .toList();
        return new ProductoDetalleDTO(productoMapper.toDTO(p), relacionados);
    }
}
