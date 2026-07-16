package com.gamerstore.app.controller;

import com.gamerstore.app.dto.PedidoDTO;
import com.gamerstore.app.dto.PedidoRequest;
import com.gamerstore.app.dto.PedidoUpdateRequest;
import com.gamerstore.app.mapper.PedidoMapper;
import com.gamerstore.app.service.PedidoReporteService;
import com.gamerstore.app.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** CRUD de pedidos (panel admin). */
@RestController
@RequestMapping("/api/admin/pedidos")
public class AdminPedidoController {

    private final PedidoService pedidoService;
    private final PedidoMapper pedidoMapper;
    private final PedidoReporteService reporteService;

    public AdminPedidoController(PedidoService pedidoService, PedidoMapper pedidoMapper,
                                 PedidoReporteService reporteService) {
        this.pedidoService = pedidoService;
        this.pedidoMapper = pedidoMapper;
        this.reporteService = reporteService;
    }

    // GET /api/admin/pedidos: lista todos los pedidos.
    @GetMapping
    public List<PedidoDTO> listar() {
        return pedidoService.todos().stream().map(pedidoMapper::toDTO).toList();
    }

    // GET /api/admin/pedidos/{id}: trae el detalle de un pedido puntual.
    @GetMapping("/{id}")
    public PedidoDTO detalle(@PathVariable Long id) {
        return pedidoMapper.toDTO(pedidoService.porId(id).orElseThrow());
    }

    // POST /api/admin/pedidos: valida y crea un pedido nuevo con sus items, delegando en el service
    // el cálculo de totales y el descuento de stock.
    @PostMapping
    public PedidoDTO crear(@Valid @RequestBody PedidoRequest r) {
        return pedidoMapper.toDTO(pedidoService.crear(r.clienteId(), r.metodoPago(), r.items()));
    }

    // PUT /api/admin/pedidos/{id}: valida y actualiza el estado y/o método de pago del pedido.
    @PutMapping("/{id}")
    public PedidoDTO actualizar(@PathVariable Long id, @Valid @RequestBody PedidoUpdateRequest r) {
        return pedidoMapper.toDTO(pedidoService.actualizar(id, r.estado(), r.metodoPago()));
    }

    // DELETE /api/admin/pedidos/{id}: elimina el pedido.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        pedidoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // GET /api/admin/pedidos/reporte.pdf: filtra los pedidos por rango de fechas y estado, arma el
    // reporte con PedidoReporteService y devuelve el PDF como descarga adjunta.
    @GetMapping("/reporte.pdf")
    public ResponseEntity<byte[]> reporte(@RequestParam(required = false) String desde,
                                          @RequestParam(required = false) String hasta,
                                          @RequestParam(required = false) String estado) {
        LocalDate d = (desde != null && !desde.isBlank()) ? LocalDate.parse(desde) : null;
        LocalDate h = (hasta != null && !hasta.isBlank()) ? LocalDate.parse(hasta) : null;
        byte[] pdf = reporteService.generar(pedidoService.reporte(d, h, estado), d, h, estado);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte-pedidos.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
