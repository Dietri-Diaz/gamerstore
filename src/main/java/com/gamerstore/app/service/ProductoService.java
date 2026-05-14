package com.gamerstore.app.service;

import com.gamerstore.app.model.Categoria;
import com.gamerstore.app.model.Producto;
import com.gamerstore.app.repository.CategoriaRepository;
import com.gamerstore.app.repository.ProductoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductoService {
    private final ProductoRepository productoRepo;
    private final CategoriaRepository categoriaRepo;

    public ProductoService(ProductoRepository productoRepo, CategoriaRepository categoriaRepo) {
        this.productoRepo = productoRepo;
        this.categoriaRepo = categoriaRepo;
    }

    public List<Producto> todos() { return productoRepo.findAll(); }

    public List<Producto> filtrar(String categoria, String q) {
        boolean hayCat = categoria != null && !categoria.isBlank();
        boolean hayQ = q != null && !q.isBlank();
        if (hayCat && hayQ) return productoRepo.findByCategoriaNombreIgnoreCaseAndNombreContainingIgnoreCase(categoria, q);
        if (hayCat) return productoRepo.findByCategoriaNombreIgnoreCase(categoria);
        if (hayQ) return productoRepo.findByNombreContainingIgnoreCase(q);
        return productoRepo.findAll();
    }

    public Optional<Producto> porId(Long id) { return productoRepo.findById(id); }

    public List<Producto> porCategoria(Categoria c) {
        return productoRepo.findByCategoriaNombreIgnoreCase(c.getNombre());
    }

    public List<Categoria> categorias() { return categoriaRepo.findAll(); }

    public List<Producto> stockBajo(int umbral) {
        return productoRepo.findByStockLessThanEqualOrderByStockAsc(umbral);
    }

    public long total() { return productoRepo.count(); }

    @org.springframework.transaction.annotation.Transactional
    public Producto crear(String nombre, String descripcion, double precio, int stock,
                          String imagen, Long categoriaId) {
        Producto p = new Producto();
        p.setNombre(nombre);
        p.setDescripcion(descripcion);
        p.setPrecio(precio);
        p.setStock(Math.max(0, stock));
        p.setImagen(imagen);
        if (categoriaId != null) categoriaRepo.findById(categoriaId).ifPresent(p::setCategoria);
        return productoRepo.save(p);
    }

    @org.springframework.transaction.annotation.Transactional
    public void actualizar(Long id, String nombre, String descripcion, Double precio,
                           Integer stock, String imagen, Long categoriaId) {
        Producto p = productoRepo.findById(id).orElseThrow();
        if (nombre != null && !nombre.isBlank()) p.setNombre(nombre);
        if (descripcion != null) p.setDescripcion(descripcion);
        if (precio != null && precio > 0) p.setPrecio(precio);
        if (stock != null && stock >= 0) p.setStock(stock);
        if (imagen != null && !imagen.isBlank()) p.setImagen(imagen);
        if (categoriaId != null) categoriaRepo.findById(categoriaId).ifPresent(p::setCategoria);
        productoRepo.save(p);
    }

    @org.springframework.transaction.annotation.Transactional
    public void ajustarStock(Long id, int delta) {
        Producto p = productoRepo.findById(id).orElseThrow();
        int nuevo = Math.max(0, p.getStock() + delta);
        p.setStock(nuevo);
        productoRepo.save(p);
    }

    @org.springframework.transaction.annotation.Transactional
    public void eliminar(Long id) {
        productoRepo.deleteById(id);
    }
}
