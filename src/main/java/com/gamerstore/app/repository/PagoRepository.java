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

    // findByPedidoIdAndEstado: los pagos de un pedido en cierto estado. Se usa al anular la venta
    // para ubicar el cobro APROBADO que hay que devolver. Devuelve lista porque un pedido puede
    // tener varios intentos de pago registrados (p. ej. uno RECHAZADO y después uno APROBADO).
    List<Pago> findByPedidoIdAndEstado(Long pedidoId, String estado);
}
