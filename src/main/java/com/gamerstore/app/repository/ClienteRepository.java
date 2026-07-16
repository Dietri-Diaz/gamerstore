package com.gamerstore.app.repository;

import com.gamerstore.app.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Acceso a datos de clientes. */
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    // findByDni: busca un cliente por su DNI.
    Optional<Cliente> findByDni(String dni);
    // existsByDni: ¿ya hay un cliente con ese DNI?
    boolean existsByDni(String dni);
    // existsByDniAndIdNot: ¿existe otro cliente (id distinto) con ese mismo DNI?
    boolean existsByDniAndIdNot(String dni, Long id);
    // findAllByOrderByApellidosAscNombresAsc: lista todos los clientes ordenados por apellido y luego por nombre.
    List<Cliente> findAllByOrderByApellidosAscNombresAsc();
    // existsByEmailIgnoreCase: ¿ya hay un cliente con ese email? (ignora mayúsculas/minúsculas)
    boolean existsByEmailIgnoreCase(String email);
    // existsByEmailIgnoreCaseAndIdNot: ¿existe otro cliente (id distinto) con ese mismo email?
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
}
