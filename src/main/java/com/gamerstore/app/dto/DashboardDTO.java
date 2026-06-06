package com.gamerstore.app.dto;

import java.util.List;

public record DashboardDTO(long totalProductos, long totalCategorias, long totalClientes,
                           List<ProductoDTO> stockBajo) {}
