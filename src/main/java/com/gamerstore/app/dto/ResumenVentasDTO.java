package com.gamerstore.app.dto;

// Totales del registro de ventas (filtrado por fecha), para el panel admin
public record ResumenVentasDTO(long cantidad, double subtotal, double igv, double total) {}
