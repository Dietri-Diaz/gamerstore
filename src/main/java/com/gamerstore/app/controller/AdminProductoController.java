package com.gamerstore.app.controller;

import com.gamerstore.app.dto.ProductoDTO;
import com.gamerstore.app.dto.ProductoRequest;
import com.gamerstore.app.model.Producto;
import com.gamerstore.app.service.ProductoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** CRUD de productos (requiere rol ADMIN). */
@RestController
@RequestMapping("/api/admin/productos")
public class AdminProductoController {

    private final ProductoService productoService;

    public AdminProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    public List<ProductoDTO> listar() {
        return productoService.todos().stream().map(ProductoDTO::from).toList();
    }

    @PostMapping
    public ProductoDTO crear(@Valid @RequestBody ProductoRequest r) {
        Producto p = productoService.crear(
                r.nombre(),
                r.descripcion(),
                r.precio() != null ? r.precio() : 0,
                r.stock() != null ? r.stock() : 0,
                r.imagen(),
                r.categoriaId());
        return ProductoDTO.from(p);
    }

    @PutMapping("/{id}")
    public ProductoDTO actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequest r) {
        productoService.actualizar(id, r.nombre(), r.descripcion(), r.precio(),
                r.stock(), r.imagen(), r.categoriaId());
        return ProductoDTO.from(productoService.porId(id).orElseThrow());
    }

    @PatchMapping("/{id}/stock")
    public ProductoDTO ajustarStock(@PathVariable Long id, @RequestParam int delta) {
        productoService.ajustarStock(id, delta);
        return ProductoDTO.from(productoService.porId(id).orElseThrow());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        productoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
