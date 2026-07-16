package com.gamerstore.app.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// Entidad Usuario: cuenta de acceso al sistema (login), mapea a la tabla "usuario"
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Username único para el login
    @Column(unique = true, nullable = false, length = 50)
    private String username;

    // Email único del usuario
    @Column(unique = true, nullable = false, length = 120)
    private String email;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(length = 15)
    private String telefono;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    // Rol del usuario (USUARIO/ADMIN), guardado como texto en la BD
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol = Rol.ADMIN;

    public Usuario() {}

    // Se ejecuta antes de insertar: fija fecha de registro y rol por defecto si faltan
    @PrePersist
    protected void onCreate() {
        if (fechaRegistro == null) fechaRegistro = LocalDateTime.now();
        if (rol == null) rol = Rol.ADMIN;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }
}
