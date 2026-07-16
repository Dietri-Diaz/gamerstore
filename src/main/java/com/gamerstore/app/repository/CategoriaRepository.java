package com.gamerstore.app.repository;

import com.gamerstore.app.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/** Acceso a datos de categorías. */
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    // findByNombre: busca una categoría por su nombre exacto.
    Optional<Categoria> findByNombre(String nombre);
    // existsByNombreIgnoreCaseAndIdNot: ¿existe otra categoría (id distinto) con ese mismo nombre?
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
    // existsByNombreIgnoreCase: ¿ya existe una categoría con ese nombre? (ignora mayúsculas/minúsculas)
    boolean existsByNombreIgnoreCase(String nombre);
}
