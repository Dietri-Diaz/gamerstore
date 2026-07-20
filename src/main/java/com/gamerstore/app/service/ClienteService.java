package com.gamerstore.app.service;

import com.gamerstore.app.model.Cliente;
import com.gamerstore.app.repository.ClienteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

/** Reglas de negocio de clientes: alta/edición con validación de DNI y email únicos. */
@Service
public class ClienteService {

    private final ClienteRepository repo;

    // Inyecta el repositorio de clientes.
    public ClienteService(ClienteRepository repo) {
        this.repo = repo;
    }

    // Lista los clientes ordenados por apellido y, dentro de este, por nombre.
    public List<Cliente> listar() {
        return repo.findAllByOrderByApellidosAscNombresAsc();
    }

    // Busca un cliente por id.
    public Optional<Cliente> porId(Long id) {
        return repo.findById(id);
    }

    // Cuenta el total de clientes.
    public long total() {
        return repo.count();
    }

    /** Crea un cliente validando DNI obligatorio y único, y email único si se envía. */
    @Transactional
    public Cliente crear(String dni, String nombres, String apellidos,
                         String telefono, String email, String direccion) {
        if (dni == null || dni.isBlank()) {
            throw new IllegalArgumentException("El DNI es obligatorio");
        }
        if (repo.existsByDni(dni.trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El DNI ya está registrado");
        }
        if (email != null && !email.isBlank() && repo.existsByEmailIgnoreCase(email.trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ese email ya está registrado");
        }
        Cliente c = new Cliente();
        c.setDni(dni.trim());
        c.setNombres(nombres);
        c.setApellidos(apellidos);
        c.setTelefono(telefono);
        c.setEmail(email);
        c.setDireccion(direccion);
        return repo.save(c);
    }

    /** Actualiza los datos del cliente; si cambia el DNI o el email, valida que no choquen con los de otro cliente. */
    @Transactional
    public void actualizar(Long id, String dni, String nombres, String apellidos,
                           String telefono, String email, String direccion) {
        Cliente c = repo.findById(id).orElseThrow();
        if (dni != null && !dni.isBlank() && !dni.equals(c.getDni())) {
            if (repo.existsByDniAndIdNot(dni.trim(), id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El DNI ya está registrado");
            }
            c.setDni(dni.trim());
        }
        if (nombres != null && !nombres.isBlank()) c.setNombres(nombres);
        if (apellidos != null && !apellidos.isBlank()) c.setApellidos(apellidos);
        c.setTelefono(telefono);
        c.setEmail(email);
        c.setDireccion(direccion);
        if (email != null && !email.isBlank() && repo.existsByEmailIgnoreCaseAndIdNot(email.trim(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ese email ya está registrado");
        }
        repo.save(c);
    }

    // Elimina un cliente por id.
    @Transactional
    public void eliminar(Long id) {
        repo.deleteById(id);
    }

    /** ¿Ya existe un cliente con ese DNI? Si viene id, lo excluye (para validar en vivo al editar). */
    public boolean existeDni(String dni, Long id) {
        if (dni == null || dni.isBlank()) return false;
        return id == null
                ? repo.existsByDni(dni.trim())
                : repo.existsByDniAndIdNot(dni.trim(), id);
    }

    /** ¿Ya existe un cliente con ese email? Si viene id, lo excluye (para validar en vivo al editar). */
    public boolean existeEmail(String email, Long id) {
        if (email == null || email.isBlank()) return false;
        return id == null
                ? repo.existsByEmailIgnoreCase(email.trim())
                : repo.existsByEmailIgnoreCaseAndIdNot(email.trim(), id);
    }
}
