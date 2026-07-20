package com.gamerstore.app.controller;

import com.gamerstore.app.dto.ExisteDTO;
import com.gamerstore.app.dto.ProductoDTO;
import com.gamerstore.app.dto.ProductoRequest;
import com.gamerstore.app.mapper.ProductoMapper;
import com.gamerstore.app.model.Producto;
import com.gamerstore.app.service.ProductoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** CRUD de productos (panel admin). */
@RestController
@RequestMapping("/api/admin/productos")
public class AdminProductoController {

    private final ProductoService productoService;
    private final ProductoMapper productoMapper;

    public AdminProductoController(ProductoService productoService, ProductoMapper productoMapper) {
        this.productoService = productoService;
        this.productoMapper = productoMapper;
    }

    // GET /api/admin/productos: lista todos los productos (incluidos los sin stock) para el panel admin.
    @GetMapping
    public List<ProductoDTO> listar() {
        return productoService.todos().stream().map(productoMapper::toDTO).toList();
    }

    // POST /api/admin/productos: valida el body y delega en el service la creación del producto.
    @PostMapping
    public ProductoDTO crear(@Valid @RequestBody ProductoRequest r) {
        Producto p = productoService.crear(
                r.nombre(),
                r.descripcion(),
                r.precio() != null ? r.precio() : 0,
                r.stock() != null ? r.stock() : 0,
                r.imagen(),
                r.categoriaId());
        return productoMapper.toDTO(p);
    }

    // PUT /api/admin/productos/{id}: valida y actualiza los datos del producto indicado.
    @PutMapping("/{id}")
    public ProductoDTO actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequest r) {
        productoService.actualizar(id, r.nombre(), r.descripcion(), r.precio(),
                r.stock(), r.imagen(), r.categoriaId());
        return productoMapper.toDTO(productoService.porId(id).orElseThrow());
    }

    // PATCH /api/admin/productos/{id}/stock: suma (o resta, si delta es negativo) unidades al stock del producto.
    @PatchMapping("/{id}/stock")
    public ProductoDTO ajustarStock(@PathVariable Long id, @RequestParam int delta) {
        productoService.ajustarStock(id, delta);
        return productoMapper.toDTO(productoService.porId(id).orElseThrow());
    }

    // DELETE /api/admin/productos/{id}: elimina el producto delegando en el service.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        productoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    /** Verifica en vivo (mientras el usuario escribe) si ya existe un producto con ese nombre. */
    @GetMapping("/existe")
    public ExisteDTO existe(@RequestParam String nombre, @RequestParam(required = false) Long id) {
        boolean existe = productoService.existeNombre(nombre, id);
        return new ExisteDTO(existe, existe ? "Ya existe un producto con ese nombre" : null);
    }
}
