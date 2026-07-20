package com.gamerstore.app.dto;

import java.time.LocalDateTime;

// Datos de un pago para mostrar en el front
public record PagoDTO(Long id, String codigo, Long pedidoId, String pedidoCodigo, String clienteNombre,
                       String metodo, double monto, String estado, String referencia, String tarjetaUlt4,
                       String titular, String voucher, LocalDateTime fecha) {}
