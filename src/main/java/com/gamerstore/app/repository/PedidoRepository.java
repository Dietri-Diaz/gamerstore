package com.gamerstore.app.repository;

import com.gamerstore.app.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    List<Pedido> findAllByOrderByFechaDesc();

    @Query("SELECT COALESCE(SUM(p.total), 0) FROM Pedido p")
    double sumTotal();

    @Query("SELECT pi.producto.id, pi.producto.nombre, pi.producto.imagen, SUM(pi.cantidad) as qty " +
           "FROM PedidoItem pi GROUP BY pi.producto.id, pi.producto.nombre, pi.producto.imagen " +
           "ORDER BY qty DESC")
    List<Object[]> topProductos(org.springframework.data.domain.Pageable pageable);
}
