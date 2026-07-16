package com.gamerstore.app.repository;

import com.gamerstore.app.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/** Acceso a datos de pedidos, incluye consultas a medida para reportes y estadísticas de ventas. */
public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    // findAllByOrderByFechaDesc: lista todos los pedidos del más reciente al más antiguo.
    List<Pedido> findAllByOrderByFechaDesc();

    // sumTotal: suma el campo total de todos los pedidos (0 si no hay ninguno).
    @Query("SELECT COALESCE(SUM(p.total), 0) FROM Pedido p")
    double sumTotal();

    // topProductos: agrupa las líneas de pedido por producto, suma la cantidad vendida y ordena de mayor a menor.
    @Query("SELECT pi.producto.id, pi.producto.nombre, pi.producto.imagen, SUM(pi.cantidad) as qty " +
           "FROM PedidoItem pi GROUP BY pi.producto.id, pi.producto.nombre, pi.producto.imagen " +
           "ORDER BY qty DESC")
    List<Object[]> topProductos(org.springframework.data.domain.Pageable pageable);
}
