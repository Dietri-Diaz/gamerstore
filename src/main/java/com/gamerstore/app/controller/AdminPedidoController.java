package com.gamerstore.app.controller;

import com.gamerstore.app.dto.PedidoDTO;
import com.gamerstore.app.dto.PedidoRequest;
import com.gamerstore.app.dto.PedidoUpdateRequest;
import com.gamerstore.app.mapper.PedidoMapper;
import com.gamerstore.app.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** CRUD de pedidos (panel admin). */
@RestController
@RequestMapping("/api/admin/pedidos")
public class AdminPedidoController {

    private final PedidoService pedidoService;
    private final PedidoMapper pedidoMapper;

    public AdminPedidoController(PedidoService pedidoService, PedidoMapper pedidoMapper) {
        this.pedidoService = pedidoService;
        this.pedidoMapper = pedidoMapper;
    }

    @GetMapping
    public List<PedidoDTO> listar() {
        return pedidoService.todos().stream().map(pedidoMapper::toDTO).toList();
    }

    @GetMapping("/{id}")
    public PedidoDTO detalle(@PathVariable Long id) {
        return pedidoMapper.toDTO(pedidoService.porId(id).orElseThrow());
    }

    @PostMapping
    public PedidoDTO crear(@Valid @RequestBody PedidoRequest r) {
        return pedidoMapper.toDTO(pedidoService.crear(r.clienteId(), r.metodoPago(), r.items()));
    }

    @PutMapping("/{id}")
    public PedidoDTO actualizar(@PathVariable Long id, @Valid @RequestBody PedidoUpdateRequest r) {
        return pedidoMapper.toDTO(pedidoService.actualizar(id, r.estado(), r.metodoPago()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        pedidoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
