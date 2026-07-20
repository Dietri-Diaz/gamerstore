package com.gamerstore.app.dto;

import java.time.LocalDateTime;

// Datos de una boleta de venta para el registro de ventas y el detalle del comprobante
public record ComprobanteDTO(Long id, String codigo, String tipo, String serie, int numero,
                             Long pedidoId, String pedidoCodigo, String clienteNombre, String clienteDni,
                             double subtotal, double igv, double total, String moneda,
                             String metodoPago, String referenciaPago, String estado, LocalDateTime fechaEmision) {}
