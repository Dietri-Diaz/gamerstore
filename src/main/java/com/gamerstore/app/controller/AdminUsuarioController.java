package com.gamerstore.app.controller;

import com.gamerstore.app.dto.UsuarioDTO;
import com.gamerstore.app.dto.UsuarioRequest;
import com.gamerstore.app.mapper.UsuarioMapper;
import com.gamerstore.app.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** CRUD de usuarios del panel admin (cuentas que hacen login y su rol). */
@RestController
@RequestMapping("/api/admin/usuarios")
public class AdminUsuarioController {

    private final UsuarioService usuarioService;
    private final UsuarioMapper usuarioMapper;

    public AdminUsuarioController(UsuarioService usuarioService, UsuarioMapper usuarioMapper) {
        this.usuarioService = usuarioService;
        this.usuarioMapper = usuarioMapper;
    }

    // GET /api/admin/usuarios: lista todos los usuarios del panel.
    @GetMapping
    public List<UsuarioDTO> listar() {
        return usuarioService.listar().stream().map(usuarioMapper::toDTO).toList();
    }

    // POST /api/admin/usuarios: valida y crea un usuario nuevo (incluye su contraseña y rol).
    @PostMapping
    public UsuarioDTO crear(@Valid @RequestBody UsuarioRequest r) {
        return usuarioMapper.toDTO(usuarioService.crear(
                r.username(), r.email(), r.nombre(), r.password(), r.telefono(), r.rol()));
    }

    // PUT /api/admin/usuarios/{id}: valida y actualiza los datos del usuario indicado.
    @PutMapping("/{id}")
    public ResponseEntity<Void> actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioRequest r) {
        usuarioService.actualizar(id, r.username(), r.email(), r.nombre(), r.password(), r.telefono(), r.rol());
        return ResponseEntity.noContent().build();
    }

    // DELETE /api/admin/usuarios/{id}: elimina el usuario.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        usuarioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
