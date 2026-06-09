package com.gamerstore.app.service;

import com.gamerstore.app.model.Categoria;
import com.gamerstore.app.repository.CategoriaRepository;
import com.gamerstore.app.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CategoriaService {

    private final CategoriaRepository repo;
    private final ProductoRepository productoRepo;

    public CategoriaService(CategoriaRepository repo, ProductoRepository productoRepo) {
        this.repo = repo;
        this.productoRepo = productoRepo;
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
        if (repo.existsByNombreIgnoreCase(nombre.trim())) {
            throw new IllegalArgumentException("Ya existe una categoría con el nombre \"" + nombre.trim() + "\"");
        }
        Categoria c = new Categoria();
        c.setNombre(nombre.trim());
        return repo.save(c);
    }

    @Transactional
    public void actualizar(Long id, String nombre) {
        Categoria c = repo.findById(id).orElseThrow();
        if (nombre != null && !nombre.isBlank()) {
            if (repo.existsByNombreIgnoreCaseAndIdNot(nombre.trim(), id)) {
                throw new IllegalArgumentException("Ya existe otra categoría con el nombre \"" + nombre.trim() + "\"");
            }
            c.setNombre(nombre.trim());
        }
        repo.save(c);
    }

    @Transactional
    public void eliminar(Long id) {
        if (productoRepo.existsByCategoriaId(id)) {
            throw new IllegalArgumentException("No se puede eliminar: la categoría tiene productos asociados");
        }
        repo.deleteById(id);
    }
}
