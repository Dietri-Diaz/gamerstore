package com.gamerstore.app.controller;

import com.gamerstore.app.dto.ClienteDTO;
import com.gamerstore.app.dto.ClienteRequest;
import com.gamerstore.app.dto.ExisteDTO;
import com.gamerstore.app.dto.ReniecPersona;
import com.gamerstore.app.mapper.ClienteMapper;
import com.gamerstore.app.service.ClienteService;
import com.gamerstore.app.service.ReniecService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** CRUD de clientes (panel admin). */
@RestController
@RequestMapping("/api/admin/clientes")
public class AdminClienteController {

    private final ClienteService clienteService;
    private final ClienteMapper clienteMapper;
    private final ReniecService reniecService;

    public AdminClienteController(ClienteService clienteService, ClienteMapper clienteMapper,
                                  ReniecService reniecService) {
        this.clienteService = clienteService;
        this.clienteMapper = clienteMapper;
        this.reniecService = reniecService;
    }

    // GET /api/admin/clientes: lista todos los clientes.
    @GetMapping
    public List<ClienteDTO> listar() {
        return clienteService.listar().stream().map(clienteMapper::toDTO).toList();
    }

    // POST /api/admin/clientes: valida y crea un cliente nuevo.
    @PostMapping
    public ClienteDTO crear(@Valid @RequestBody ClienteRequest r) {
        return clienteMapper.toDTO(clienteService.crear(
                r.dni(), r.nombres(), r.apellidos(), r.telefono(), r.email(), r.direccion()));
    }

    // PUT /api/admin/clientes/{id}: valida y actualiza los datos del cliente.
    @PutMapping("/{id}")
    public ClienteDTO actualizar(@PathVariable Long id, @Valid @RequestBody ClienteRequest r) {
        clienteService.actualizar(id, r.dni(), r.nombres(), r.apellidos(), r.telefono(), r.email(), r.direccion());
        return clienteMapper.toDTO(clienteService.porId(id).orElseThrow());
    }

    // DELETE /api/admin/clientes/{id}: elimina el cliente.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        clienteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // GET /api/admin/clientes/reniec/{dni}: llama a un servicio externo (RENIEC vía apiperu.dev) para
    // traer nombres/apellidos reales del DNI y así autocompletar el formulario de cliente.
    /** Consulta datos reales de RENIEC (apiperu.dev) para autocompletar el formulario. */
    @GetMapping("/reniec/{dni}")
    public ReniecPersona reniec(@PathVariable String dni) {
        return reniecService.consultarDni(dni)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No se encontró el DNI o el servicio no está disponible"));
    }

    /** Verifica en vivo (mientras el usuario escribe) si el DNI o el email ya están registrados por otro cliente. */
    @GetMapping("/existe")
    public ExisteDTO existe(@RequestParam(required = false) String dni,
                            @RequestParam(required = false) String email,
                            @RequestParam(required = false) Long id) {
        if (dni != null && !dni.isBlank() && clienteService.existeDni(dni, id)) {
            return new ExisteDTO(true, "El DNI ya está registrado");
        }
        if (email != null && !email.isBlank() && clienteService.existeEmail(email, id)) {
            return new ExisteDTO(true, "Ese email ya está registrado");
        }
        return new ExisteDTO(false, null);
    }
}
