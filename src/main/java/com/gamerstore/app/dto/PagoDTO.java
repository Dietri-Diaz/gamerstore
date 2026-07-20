package com.gamerstore.app.dto;

import java.time.LocalDateTime;

// Datos de un pago para mostrar en el front.
// referenciaDevolucion/fechaDevolucion: solo vienen con dato si el pago fue DEVUELTO al
// anularse la venta (id del refund de Stripe, o "MANUAL" si se devolvió a mano).
public record PagoDTO(Long id, String codigo, Long pedidoId, String pedidoCodigo, String clienteNombre,
                       String metodo, double monto, String estado, String referencia, String tarjetaUlt4,
                       String titular, String voucher, LocalDateTime fecha,
                       String referenciaDevolucion, LocalDateTime fechaDevolucion) {}
