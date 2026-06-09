package com.gamerstore.app.repository;

import com.gamerstore.app.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    List<Producto> findByCategoriaNombreIgnoreCase(String nombre);
    List<Producto> findByNombreContainingIgnoreCase(String q);
    List<Producto> findByCategoriaNombreIgnoreCaseAndNombreContainingIgnoreCase(String categoria, String q);
    List<Producto> findByStockLessThanEqualOrderByStockAsc(int stock);
    boolean existsByCategoriaId(Long categoriaId);
}
