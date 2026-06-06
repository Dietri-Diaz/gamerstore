package com.gamerstore.app.controller;

import com.gamerstore.app.dto.CategoriaDTO;
import com.gamerstore.app.dto.CategoriaRequest;
import com.gamerstore.app.service.CategoriaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** CRUD de categorias (requiere rol ADMIN). */
@RestController
@RequestMapping("/api/admin/categorias")
public class AdminCategoriaController {

    private final CategoriaService categoriaService;

    public AdminCategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping
    public List<CategoriaDTO> listar() {
        return categoriaService.listar().stream().map(CategoriaDTO::from).toList();
    }

    @PostMapping
    public CategoriaDTO crear(@RequestBody CategoriaRequest r) {
        return CategoriaDTO.from(categoriaService.crear(r.nombre()));
    }

    @PutMapping("/{id}")
    public CategoriaDTO actualizar(@PathVariable Long id, @RequestBody CategoriaRequest r) {
        categoriaService.actualizar(id, r.nombre());
        return CategoriaDTO.from(categoriaService.porId(id).orElseThrow());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        categoriaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
