package com.gamerstore.app.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

/** Pedido completo que envía la tienda pública: comprador + carrito + pago. */
public record CheckoutRequest(
        @NotNull @Valid CheckoutClienteDTO cliente,
        @NotEmpty(message = "El carrito está vacío") @Valid List<PedidoItemRequest> items,
        @NotNull @Valid CheckoutPagoDTO pago) {}
