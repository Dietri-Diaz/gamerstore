package com.gamerstore.app.controller;

import com.gamerstore.app.dto.CheckoutClienteDTO;
import com.gamerstore.app.dto.CheckoutRequest;
import com.gamerstore.app.dto.CheckoutResponse;
import com.gamerstore.app.dto.VerificarClienteRequest;
import com.gamerstore.app.model.Comprobante;
import com.gamerstore.app.service.BoletaPdfService;
import com.gamerstore.app.service.CheckoutService;
import com.gamerstore.app.service.ComprobanteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Checkout público: compra desde la tienda (sin login). */
@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {

    private final CheckoutService checkoutService;
    private final ComprobanteService comprobanteService;
    private final BoletaPdfService boletaPdfService;

    public CheckoutController(CheckoutService checkoutService, ComprobanteService comprobanteService,
                              BoletaPdfService boletaPdfService) {
        this.checkoutService = checkoutService;
        this.comprobanteService = comprobanteService;
        this.boletaPdfService = boletaPdfService;
    }

    // POST /api/checkout: compra desde la tienda pública (crea cliente si es nuevo, pedido y pago).
    @PostMapping
    public CheckoutResponse comprar(@Valid @RequestBody CheckoutRequest req) {
        return checkoutService.comprar(req);
    }

    // POST /api/checkout/cliente: el comprador que ya es cliente se identifica con DNI + correo
    // y le devolvemos sus datos para prellenar el formulario (404 si no coinciden).
    @PostMapping("/cliente")
    public CheckoutClienteDTO verificarCliente(@Valid @RequestBody VerificarClienteRequest req) {
        return checkoutService.verificarCliente(req.dni(), req.email());
    }

    // GET /api/checkout/boleta/{pedidoCodigo}?dni=XXXXXXXX -> el comprador descarga su boleta.
    // Es pública (no requiere login) pero verificada con el DNI del cliente, para que nadie
    // pueda descargar la boleta de un pedido ajeno solo adivinando el código.
    @GetMapping("/boleta/{pedidoCodigo}")
    public ResponseEntity<byte[]> boleta(@PathVariable String pedidoCodigo, @RequestParam String dni) {
        Comprobante c = comprobanteService.porPedidoCodigo(pedidoCodigo);
        if (c.getClienteDni() == null || !c.getClienteDni().equals(dni)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes descargar esta boleta");
        }
        byte[] pdf = boletaPdfService.generar(c, c.getPedido().getItems());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=boleta-" + c.getCodigo() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
