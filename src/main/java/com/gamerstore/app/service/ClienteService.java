package com.gamerstore.app.service;

import com.gamerstore.app.model.Cliente;
import com.gamerstore.app.repository.ClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ClienteService {

    private final ClienteRepository repo;

    public ClienteService(ClienteRepository repo) {
        this.repo = repo;
    }

    public List<Cliente> listar() {
        return repo.findAllByOrderByApellidosAscNombresAsc();
    }

    public Optional<Cliente> porId(Long id) {
        return repo.findById(id);
    }

    public long total() {
        return repo.count();
    }

    @Transactional
    public Cliente crear(String dni, String nombres, String apellidos,
                         String telefono, String email, String direccion) {
        if (dni == null || dni.isBlank()) {
            throw new IllegalArgumentException("El DNI es obligatorio");
        }
        if (repo.existsByDni(dni)) {
            throw new IllegalArgumentException("Ya existe un cliente con ese DNI");
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

    @Transactional
    public void actualizar(Long id, String dni, String nombres, String apellidos,
                           String telefono, String email, String direccion) {
        Cliente c = repo.findById(id).orElseThrow();
        if (dni != null && !dni.isBlank() && !dni.equals(c.getDni())) {
            if (repo.existsByDniAndIdNot(dni.trim(), id)) {
                throw new IllegalArgumentException("Ya existe otro cliente con el DNI " + dni.trim());
            }
            c.setDni(dni.trim());
        }
        if (nombres != null && !nombres.isBlank()) c.setNombres(nombres);
        if (apellidos != null && !apellidos.isBlank()) c.setApellidos(apellidos);
        c.setTelefono(telefono);
        c.setEmail(email);
        c.setDireccion(direccion);
        repo.save(c);
    }

    @Transactional
    public void eliminar(Long id) {
        repo.deleteById(id);
    }
}
