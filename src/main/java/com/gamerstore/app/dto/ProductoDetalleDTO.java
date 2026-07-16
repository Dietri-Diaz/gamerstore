package com.gamerstore.app.dto;

import java.util.List;

// Detalle de un producto: el producto + productos relacionados/sugeridos
public record ProductoDetalleDTO(ProductoDTO producto, List<ProductoDTO> relacionados) {}
