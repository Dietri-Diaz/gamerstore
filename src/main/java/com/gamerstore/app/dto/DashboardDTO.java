package com.gamerstore.app.dto;

import java.util.List;

// Datos agregados para el panel de dashboard/administración
public record DashboardDTO(long totalProductos, long totalCategorias, long totalClientes,
                           long totalPedidos, double totalVentas, List<ProductoDTO> stockBajo,
                           List<TopProductoDTO> topProductos) {}
