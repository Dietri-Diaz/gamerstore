package com.gamerstore.app.controller;

import com.gamerstore.app.dto.CategoriaDTO;
import com.gamerstore.app.mapper.CategoriaMapper;
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
    private final CategoriaMapper categoriaMapper;

    public CategoriaController(CategoriaService categoriaService, CategoriaMapper categoriaMapper) {
        this.categoriaService = categoriaService;
        this.categoriaMapper = categoriaMapper;
    }

    @GetMapping
    public List<CategoriaDTO> listar() {
        return categoriaService.listar().stream().map(categoriaMapper::toDTO).toList();
    }
}
