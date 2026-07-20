package com.gamerstore.app.repository;

import com.gamerstore.app.model.Comprobante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** Acceso a datos de comprobantes (boletas de venta). */
public interface ComprobanteRepository extends JpaRepository<Comprobante, Long> {
    // Lista todas las boletas, de la más reciente a la más antigua (registro de ventas).
    List<Comprobante> findAllByOrderByFechaEmisionDesc();

    // Una boleta por pedido (a lo mucho una: la emisión es idempotente).
    Optional<Comprobante> findByPedidoId(Long pedidoId);

    // Último correlativo usado en una serie, para calcular el siguiente número.
    @Query("SELECT COALESCE(MAX(c.numero), 0) FROM Comprobante c WHERE c.serie = :serie")
    int ultimoNumero(@Param("serie") String serie);
}
