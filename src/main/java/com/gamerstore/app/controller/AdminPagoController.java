package com.gamerstore.app.controller;

import com.gamerstore.app.dto.PagoDTO;
import com.gamerstore.app.dto.PagoTarjetaRequest;
import com.gamerstore.app.dto.PagoYapeRequest;
import com.gamerstore.app.dto.YapeConfigDTO;
import com.gamerstore.app.mapper.PagoMapper;
import com.gamerstore.app.service.PagoComprobanteService;
import com.gamerstore.app.service.PagoService;
import com.gamerstore.app.service.StripeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Pasarela de pagos (panel admin): cobro con tarjeta o Yape y comprobante en PDF. */
@RestController
@RequestMapping("/api/admin/pagos")
public class AdminPagoController {

    private final PagoService pagoService;
    private final PagoMapper pagoMapper;
    private final PagoComprobanteService comprobanteService;
    private final StripeService stripeService;

    @Value("${app.yape.numero}")
    private String yapeNumero;

    @Value("${app.yape.titular}")
    private String yapeTitular;

    @Value("${app.yape.qr}")
    private String yapeQr;

    // Yape (la app) tiene tope de monto: la pasarela del ERP lo usa para deshabilitar esa pestaña.
    @Value("${app.yape.monto-maximo:500}")
    private double yapeMontoMaximo;

    @Value("${app.stripe.enabled:false}")
    private boolean stripeEnabled;

    @Value("${app.stripe.public-key:}")
    private String stripePublicKey;

    public AdminPagoController(PagoService pagoService, PagoMapper pagoMapper,
                               PagoComprobanteService comprobanteService, StripeService stripeService) {
        this.pagoService = pagoService;
        this.pagoMapper = pagoMapper;
        this.comprobanteService = comprobanteService;
        this.stripeService = stripeService;
    }

    // GET /api/admin/pagos: lista todos los pagos registrados.
    @GetMapping
    public List<PagoDTO> listar() {
        return pagoService.listar().stream().map(pagoMapper::toDTO).toList();
    }

    // POST /api/admin/pagos/tarjeta: si Stripe está activo y llegó el token, cobra de verdad;
    // si no, cae a la pasarela simulada (valida Luhn, vencimiento y CVV).
    @PostMapping("/tarjeta")
    public PagoDTO pagarConTarjeta(@Valid @RequestBody PagoTarjetaRequest r) {
        boolean conStripe = stripeService.estaActivo() && r.paymentMethodId() != null && !r.paymentMethodId().isBlank();
        return pagoMapper.toDTO(conStripe
                ? pagoService.pagarConStripe(r.pedidoId(), r.paymentMethodId())
                : pagoService.pagarConTarjeta(r.pedidoId(), r.numero(), r.titular(), r.vencimiento(), r.cvv()));
    }

    // POST /api/admin/pagos/yape: cobra un pedido con Yape (registra el N° de operación y el voucher).
    @PostMapping("/yape")
    public PagoDTO pagarConYape(@Valid @RequestBody PagoYapeRequest r) {
        return pagoMapper.toDTO(pagoService.pagarConYape(r.pedidoId(), r.numeroOperacion(), r.voucher()));
    }

    // GET /api/admin/pagos/config: expone la cuenta Yape (número, titular y QR) y la clave PÚBLICA
    // de Stripe (nunca la secreta), leídas de application.properties.
    @GetMapping("/config")
    public YapeConfigDTO config() {
        return new YapeConfigDTO(yapeNumero, yapeTitular, yapeQr, yapeMontoMaximo, stripeEnabled, stripePublicKey);
    }

    // GET /api/admin/pagos/{id}/comprobante.pdf: arma el comprobante del pago con PagoComprobanteService
    // y lo devuelve como descarga adjunta.
    @GetMapping("/{id}/comprobante.pdf")
    public ResponseEntity<byte[]> comprobante(@PathVariable Long id) {
        var pago = pagoService.porId(id);
        byte[] pdf = comprobanteService.generar(pago);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=comprobante-" + pago.getCodigo() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
