package com.gamerstore.app.dto;

import java.time.LocalDateTime;
import java.util.List;

// Datos de un pedido (con sus líneas) para mostrar en el front.
// tipoEntrega/direccionEnvio/referenciaEnvio: cómo recibe el comprador su compra.
// motivoAnulacion: solo viene con dato si la venta fue anulada (estado ANULADO).
public record PedidoDTO(Long id, String codigo, Long clienteId, String clienteNombre,
                        LocalDateTime fecha, String estado, double total, String metodoPago,
                        int cantidadTotal, List<PedidoItemDTO> items,
                        String tipoEntrega, String direccionEnvio, String referenciaEnvio,
                        String motivoAnulacion) {}
