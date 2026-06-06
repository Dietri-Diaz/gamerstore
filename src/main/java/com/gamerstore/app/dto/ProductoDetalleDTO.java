package com.gamerstore.app.dto;

import java.util.List;

public record ProductoDetalleDTO(ProductoDTO producto, List<ProductoDTO> relacionados) {}
