package com.gamerstore.app.model;

import jakarta.persistence.*;

// Entidad Categoria: agrupa productos, mapea a la tabla "categoria"
@Entity
@Table(name = "categoria")
public class Categoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nombre único de la categoría
    @Column(nullable = false, unique = true)
    private String nombre;

    public Categoria() {}
    public Categoria(String nombre) { this.nombre = nombre; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}
