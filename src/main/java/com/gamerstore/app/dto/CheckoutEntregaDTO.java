package com.gamerstore.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cómo quiere recibir su compra el comprador: recojo en tienda o delivery a una dirección.
 *
 * La dirección NO lleva @NotBlank aquí porque solo es obligatoria cuando el tipo es DELIVERY,
 * y una anotación de campo no puede mirar el valor de otro campo. Esa regla cruzada se valida
 * en PedidoService.crear(...), o sea en el servidor: nunca confiamos en que el front haya
 * ocultado/mostrado bien el formulario.
 */
public record CheckoutEntregaDTO(
        @NotBlank(message = "Elige cómo quieres recibir tu pedido")
        @Pattern(regexp = "RECOJO_TIENDA|DELIVERY", message = "Tipo de entrega inválido")
        String tipo,

        @Size(max = 200, message = "La dirección es demasiado larga")
        String direccion,

        @Size(max = 150, message = "La referencia es demasiado larga")
        String referencia) {}
