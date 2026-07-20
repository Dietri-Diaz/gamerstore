package com.gamerstore.app.controller;

import com.gamerstore.app.dto.ComprobanteDTO;
import com.gamerstore.app.dto.ResumenVentasDTO;
import com.gamerstore.app.mapper.ComprobanteMapper;
import com.gamerstore.app.model.Comprobante;
import com.gamerstore.app.service.BoletaPdfService;
import com.gamerstore.app.service.ComprobanteService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Registro de ventas (panel admin): boletas emitidas, resumen y descarga del PDF. */
@RestController
@RequestMapping("/api/admin/comprobantes")
public class AdminComprobanteController {

    private final ComprobanteService comprobanteService;
    private final ComprobanteMapper comprobanteMapper;
    private final BoletaPdfService boletaPdfService;

    public AdminComprobanteController(ComprobanteService comprobanteService, ComprobanteMapper comprobanteMapper,
                                      BoletaPdfService boletaPdfService) {
        this.comprobanteService = comprobanteService;
        this.comprobanteMapper = comprobanteMapper;
        this.boletaPdfService = boletaPdfService;
    }

    // GET /api/admin/comprobantes: registro de ventas, con filtro opcional de fechas (yyyy-MM-dd).
    @GetMapping
    public List<ComprobanteDTO> listar(@RequestParam(required = false) String desde,
                                       @RequestParam(required = false) String hasta) {
        return comprobanteService.filtrar(parseFecha(desde), parseFecha(hasta)).stream()
                .map(comprobanteMapper::toDTO).toList();
    }

    // GET /api/admin/comprobantes/resumen: totales del registro de ventas, con los mismos filtros.
    @GetMapping("/resumen")
    public ResumenVentasDTO resumen(@RequestParam(required = false) String desde,
                                    @RequestParam(required = false) String hasta) {
        return comprobanteService.resumen(comprobanteService.filtrar(parseFecha(desde), parseFecha(hasta)));
    }

    // GET /api/admin/comprobantes/{id}/pdf: descarga la boleta puntual.
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        return pdfDe(comprobanteService.porId(id));
    }

    // GET /api/admin/comprobantes/pedido/{pedidoId}/pdf: descarga la boleta de un pedido puntual.
    @GetMapping("/pedido/{pedidoId}/pdf")
    public ResponseEntity<byte[]> pdfPorPedido(@PathVariable Long pedidoId) {
        return pdfDe(comprobanteService.porPedido(pedidoId));
    }

    // Arma la respuesta de descarga del PDF a partir de un comprobante ya resuelto.
    private ResponseEntity<byte[]> pdfDe(Comprobante c) {
        byte[] pdf = boletaPdfService.generar(c, c.getPedido().getItems());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=boleta-" + c.getCodigo() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // Convierte el parámetro de fecha (yyyy-MM-dd) a LocalDate; null/vacío si no vino.
    private LocalDate parseFecha(String valor) {
        return (valor != null && !valor.isBlank()) ? LocalDate.parse(valor) : null;
    }
}
