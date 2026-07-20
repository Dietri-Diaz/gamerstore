package com.gamerstore.app.repository;

import com.gamerstore.app.model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Acceso a datos de pagos. */
public interface PagoRepository extends JpaRepository<Pago, Long> {
    // findAllByOrderByFechaDesc: lista todos los pagos del más reciente al más antiguo.
    List<Pago> findAllByOrderByFechaDesc();

    // existsByReferenciaAndMetodo: evita registrar dos veces la misma operación de Yape.
    boolean existsByReferenciaAndMetodo(String referencia, String metodo);
}
