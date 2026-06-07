package com.gamerstore.app.dto;

import java.util.List;

public record DashboardDTO(long totalProductos, long totalCategorias, long totalClientes,
                           long totalPedidos, double totalVentas, List<ProductoDTO> stockBajo) {}
