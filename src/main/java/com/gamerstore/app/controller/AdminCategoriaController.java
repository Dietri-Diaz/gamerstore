package com.gamerstore.app.controller;

import com.gamerstore.app.dto.CategoriaDTO;
import com.gamerstore.app.dto.CategoriaRequest;
import com.gamerstore.app.dto.ExisteDTO;
import com.gamerstore.app.mapper.CategoriaMapper;
import com.gamerstore.app.service.CategoriaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** CRUD de categorias (panel admin). */
@RestController
@RequestMapping("/api/admin/categorias")
public class AdminCategoriaController {

    private final CategoriaService categoriaService;
    private final CategoriaMapper categoriaMapper;

    public AdminCategoriaController(CategoriaService categoriaService, CategoriaMapper categoriaMapper) {
        this.categoriaService = categoriaService;
        this.categoriaMapper = categoriaMapper;
    }

    // GET /api/admin/categorias: lista todas las categorías para el panel admin.
    @GetMapping
    public List<CategoriaDTO> listar() {
        return categoriaService.listar().stream().map(categoriaMapper::toDTO).toList();
    }

    // POST /api/admin/categorias: valida y crea una categoría nueva.
    @PostMapping
    public CategoriaDTO crear(@Valid @RequestBody CategoriaRequest r) {
        return categoriaMapper.toDTO(categoriaService.crear(r.nombre()));
    }

    // PUT /api/admin/categorias/{id}: valida y actualiza el nombre de la categoría.
    @PutMapping("/{id}")
    public CategoriaDTO actualizar(@PathVariable Long id, @Valid @RequestBody CategoriaRequest r) {
        categoriaService.actualizar(id, r.nombre());
        return categoriaMapper.toDTO(categoriaService.porId(id).orElseThrow());
    }

    // DELETE /api/admin/categorias/{id}: elimina la categoría.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        categoriaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    /** Verifica en vivo (mientras el usuario escribe) si ya existe una categoría con ese nombre. */
    @GetMapping("/existe")
    public ExisteDTO existe(@RequestParam String nombre, @RequestParam(required = false) Long id) {
        boolean existe = categoriaService.existeNombre(nombre, id);
        return new ExisteDTO(existe, existe ? "La categoría ya existe" : null);
    }
}
