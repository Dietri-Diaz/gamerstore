package com.gamerstore.app.service;

import com.gamerstore.app.model.Categoria;
import com.gamerstore.app.repository.CategoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CategoriaService {

    private final CategoriaRepository repo;

    public CategoriaService(CategoriaRepository repo) {
        this.repo = repo;
    }

    public List<Categoria> listar() {
        return repo.findAll();
    }

    public Optional<Categoria> porId(Long id) {
        return repo.findById(id);
    }

    public long total() {
        return repo.count();
    }

    @Transactional
    public Categoria crear(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        Categoria c = new Categoria();
        c.setNombre(nombre.trim());
        return repo.save(c);
    }

    @Transactional
    public void actualizar(Long id, String nombre) {
        Categoria c = repo.findById(id).orElseThrow();
        if (nombre != null && !nombre.isBlank()) c.setNombre(nombre.trim());
        repo.save(c);
    }

    @Transactional
    public void eliminar(Long id) {
        repo.deleteById(id);
    }
}
