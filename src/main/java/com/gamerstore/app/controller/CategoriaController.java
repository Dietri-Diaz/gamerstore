package com.gamerstore.app.controller;

import com.gamerstore.app.dto.CategoriaDTO;
import com.gamerstore.app.service.CategoriaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Listado publico de categorias (para los filtros del catalogo). */
@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping
    public List<CategoriaDTO> listar() {
        return categoriaService.listar().stream().map(CategoriaDTO::from).toList();
    }
}
