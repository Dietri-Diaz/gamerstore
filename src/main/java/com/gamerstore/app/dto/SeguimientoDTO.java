package com.gamerstore.app.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Todo lo que ve el comprador al rastrear su pedido con su código + DNI, sin loguearse.
 *
 * No incluye ids internos ni datos del pago (referencia de tarjeta, voucher, etc.): es una
 * respuesta pública, así que solo lleva lo que el propio comprador ya sabe de su compra.
 */
public record SeguimientoDTO(String codigo, LocalDateTime fecha, String estado, double total,
                             String metodoPago, String clienteNombre,
                             String tipoEntrega, String direccionEnvio, String referenciaEnvio,
                             String comprobanteCodigo, String motivoAnulacion,
                             List<SeguimientoItemDTO> items, List<SeguimientoPasoDTO> historial) {}
