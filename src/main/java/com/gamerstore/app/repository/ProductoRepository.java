package com.gamerstore.app.repository;

import com.gamerstore.app.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/** Acceso a datos de productos; los métodos usan "query derivation" de Spring Data (el nombre arma el SQL). */
public interface ProductoRepository extends JpaRepository<Producto, Long> {
    // findByCategoriaNombreIgnoreCase: productos cuya categoría tiene ese nombre (sin distinguir mayúsculas).
    List<Producto> findByCategoriaNombreIgnoreCase(String nombre);
    // findByNombreContainingIgnoreCase: productos cuyo nombre contiene el texto q (sin distinguir mayúsculas).
    List<Producto> findByNombreContainingIgnoreCase(String q);
    // findByCategoriaNombreIgnoreCaseAndNombreContainingIgnoreCase: combina el filtro de categoría y el de texto en el nombre.
    List<Producto> findByCategoriaNombreIgnoreCaseAndNombreContainingIgnoreCase(String categoria, String q);
    // findByStockLessThanEqualOrderByStockAsc: productos con stock <= al dado, ordenados de menor a mayor stock.
    List<Producto> findByStockLessThanEqualOrderByStockAsc(int stock);
    // existsByCategoriaId: ¿hay productos que pertenezcan a esa categoría? (usado para bloquear el borrado de categorías con productos)
    boolean existsByCategoriaId(Long categoriaId);
    // existsByNombreIgnoreCase: ¿ya existe un producto con ese nombre? (ignora mayúsculas/minúsculas)
    boolean existsByNombreIgnoreCase(String nombre);
    // existsByNombreIgnoreCaseAndIdNot: ¿existe otro producto (id distinto) con ese mismo nombre?
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}
