package com.gamerstore.app.controller;

import com.gamerstore.app.dto.ClienteDTO;
import com.gamerstore.app.dto.ClienteRequest;
import com.gamerstore.app.mapper.ClienteMapper;
import com.gamerstore.app.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** CRUD de clientes (panel admin). */
@RestController
@RequestMapping("/api/admin/clientes")
public class AdminClienteController {

    private final ClienteService clienteService;
    private final ClienteMapper clienteMapper;

    public AdminClienteController(ClienteService clienteService, ClienteMapper clienteMapper) {
        this.clienteService = clienteService;
        this.clienteMapper = clienteMapper;
    }

    @GetMapping
    public List<ClienteDTO> listar() {
        return clienteService.listar().stream().map(clienteMapper::toDTO).toList();
    }

    @PostMapping
    public ClienteDTO crear(@Valid @RequestBody ClienteRequest r) {
        return clienteMapper.toDTO(clienteService.crear(
                r.dni(), r.nombres(), r.apellidos(), r.telefono(), r.email(), r.direccion()));
    }

    @PutMapping("/{id}")
    public ClienteDTO actualizar(@PathVariable Long id, @Valid @RequestBody ClienteRequest r) {
        clienteService.actualizar(id, r.nombres(), r.apellidos(), r.telefono(), r.email(), r.direccion());
        return clienteMapper.toDTO(clienteService.porId(id).orElseThrow());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        clienteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
