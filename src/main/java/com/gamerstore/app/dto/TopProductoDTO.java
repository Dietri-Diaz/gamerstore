package com.gamerstore.app.dto;

// Datos de un producto dentro del ranking de más vendidos
public record TopProductoDTO(Long productoId, String productoNombre, String imagen, long cantidad) {}
