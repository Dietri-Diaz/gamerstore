package com.gamerstore.app.service;

import com.stripe.Stripe;
import com.stripe.exception.CardException;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.param.PaymentIntentCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Cobra pedidos de verdad contra Stripe (modo prueba): el navegador tokeniza la tarjeta
 * (paymentMethodId) y aquí se crea/confirma el PaymentIntent con la clave secreta.
 */
@Service
public class StripeService {

    private static final Logger log = LoggerFactory.getLogger(StripeService.class);

    // Resultado del cobro real contra Stripe.
    // (se agrega "titular" respecto al nombre reportado por la tarjeta: PagoService lo necesita
    // para armar el Pago y no hay otra forma de obtenerlo fuera de este servicio)
    public record ResultadoStripe(boolean aprobado, String referencia, String ult4, String marca,
                                  String mensaje, String titular) {}

    private final boolean enabled;
    private final String secretKey;
    private final String currency;

    public StripeService(@Value("${app.stripe.enabled:false}") boolean enabled,
                         @Value("${app.stripe.secret-key:}") String secretKey,
                         @Value("${app.stripe.currency:pen}") String currency) {
        this.enabled = enabled;
        this.secretKey = secretKey;
        this.currency = currency;
    }

    // Stripe está listo para cobrar solo si está habilitado y hay clave secreta configurada.
    public boolean estaActivo() {
        return enabled && secretKey != null && !secretKey.isBlank();
    }

    /** Cobra el monto con el paymentMethodId que tokenizó el navegador. */
    public ResultadoStripe cobrar(String paymentMethodId, double monto, String descripcion) {
        Stripe.apiKey = secretKey;
        try {
            // Consulta la tarjeta para saber marca, últimos 4 y titular (para el comprobante).
            PaymentMethod pm = PaymentMethod.retrieve(paymentMethodId);
            String ult4 = pm.getCard() != null ? pm.getCard().getLast4() : null;
            String marca = pm.getCard() != null && pm.getCard().getBrand() != null
                    ? pm.getCard().getBrand().toUpperCase() : null;
            String titular = pm.getBillingDetails() != null ? pm.getBillingDetails().getName() : null;

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(Math.round(monto * 100))   // Stripe usa centimos
                    .setCurrency(currency)
                    .setPaymentMethod(paymentMethodId)
                    .setConfirm(true)
                    .setDescription(descripcion)
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                                    .build())
                    .build();
            PaymentIntent pi = PaymentIntent.create(params);

            if ("succeeded".equals(pi.getStatus())) {
                return new ResultadoStripe(true, pi.getId(), ult4, marca, "Pago aprobado", titular);
            }
            return new ResultadoStripe(false, pi.getId(), ult4, marca,
                    "El pago no pudo completarse (estado: " + pi.getStatus() + ")", titular);
        } catch (CardException e) {
            // Tarjeta rechazada por el banco: no es un error del sistema, es un rechazo normal.
            log.warn("Stripe: tarjeta rechazada ({})", e.getMessage());
            return new ResultadoStripe(false, e.getCharge() != null ? e.getCharge() : "-", null, null, e.getMessage(), null);
        } catch (StripeException e) {
            log.error("Stripe: no se pudo contactar la pasarela de pagos", e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No se pudo contactar la pasarela de pagos: " + e.getMessage());
        }
    }
}
