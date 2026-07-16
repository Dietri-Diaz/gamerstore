package com.gamerstore.app.service;

import com.gamerstore.app.model.Categoria;
import com.gamerstore.app.repository.CategoriaRepository;
import com.gamerstore.app.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** Reglas de negocio de categorías: alta/edición con nombre único y borrado solo si no tiene productos asociados. */
@Service
public class CategoriaService {

    private final CategoriaRepository repo;
    private final ProductoRepository productoRepo;

    // Inyecta el repositorio de categorías y el de productos (este último para validar el borrado).
    public CategoriaService(CategoriaRepository repo, ProductoRepository productoRepo) {
        this.repo = repo;
        this.productoRepo = productoRepo;
    }

    // Lista todas las categorías.
    public List<Categoria> listar() {
        return repo.findAll();
    }

    // Busca una categoría por id.
    public Optional<Categoria> porId(Long id) {
        return repo.findById(id);
    }

    // Cuenta el total de categorías.
    public long total() {
        return repo.count();
    }

    /** Crea una categoría validando nombre no vacío y que no exista otra con el mismo nombre (ignora mayúsculas). */
    @Transactional
    public Categoria crear(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (repo.existsByNombreIgnoreCase(nombre.trim())) {
            throw new IllegalArgumentException("La categoría ya existe");
        }
        Categoria c = new Categoria();
        c.setNombre(nombre.trim());
        return repo.save(c);
    }

    /** Actualiza el nombre de la categoría validando que no choque con el de otra categoría ya existente. */
    @Transactional
    public void actualizar(Long id, String nombre) {
        Categoria c = repo.findById(id).orElseThrow();
        if (nombre != null && !nombre.isBlank()) {
            if (repo.existsByNombreIgnoreCaseAndIdNot(nombre.trim(), id)) {
                throw new IllegalArgumentException("La categoría ya existe");
            }
            c.setNombre(nombre.trim());
        }
        repo.save(c);
    }

    /** Elimina una categoría, pero solo si no tiene productos asociados. */
    @Transactional
    public void eliminar(Long id) {
        if (productoRepo.existsByCategoriaId(id)) {
            throw new IllegalArgumentException("No se puede eliminar: la categoría tiene productos asociados");
        }
        repo.deleteById(id);
    }
}
