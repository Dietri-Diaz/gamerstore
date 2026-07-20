package com.gamerstore.app.dto;

/**
 * Un paso de la línea de tiempo del seguimiento.
 *
 * - completado: el pedido YA pasó por este paso (o está en él).
 * - actual: es exactamente el estado en el que está ahora (el punto resaltado del recorrido).
 */
public record SeguimientoPasoDTO(String estado, String titulo, String descripcion,
                                 boolean completado, boolean actual) {}
