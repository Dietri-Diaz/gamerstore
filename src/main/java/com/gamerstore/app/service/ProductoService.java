package com.gamerstore.app.service;

import com.gamerstore.app.model.Categoria;
import com.gamerstore.app.model.Producto;
import com.gamerstore.app.repository.CategoriaRepository;
import com.gamerstore.app.repository.ProductoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

/** Reglas de negocio de productos: listado/filtrado, alta y edición con nombre único, ajuste de stock y borrado. */
@Service
public class ProductoService {
    private final ProductoRepository productoRepo;
    private final CategoriaRepository categoriaRepo;

    // Inyecta los repositorios de producto y categoría por constructor.
    public ProductoService(ProductoRepository productoRepo, CategoriaRepository categoriaRepo) {
        this.productoRepo = productoRepo;
        this.categoriaRepo = categoriaRepo;
    }

    // Lista todos los productos.
    public List<Producto> todos() { return productoRepo.findAll(); }

    // Filtra productos por categoría y/o texto de búsqueda; si vienen ambos, combina los dos filtros.
    public List<Producto> filtrar(String categoria, String q) {
        boolean hayCat = categoria != null && !categoria.isBlank();
        boolean hayQ = q != null && !q.isBlank();
        if (hayCat && hayQ) return productoRepo.findByCategoriaNombreIgnoreCaseAndNombreContainingIgnoreCase(categoria, q);
        if (hayCat) return productoRepo.findByCategoriaNombreIgnoreCase(categoria);
        if (hayQ) return productoRepo.findByNombreContainingIgnoreCase(q);
        return productoRepo.findAll();
    }

    // Busca un producto por su id.
    public Optional<Producto> porId(Long id) { return productoRepo.findById(id); }

    // Lista los productos de una categoría dada.
    public List<Producto> porCategoria(Categoria c) {
        return productoRepo.findByCategoriaNombreIgnoreCase(c.getNombre());
    }

    // Lista todas las categorías (atajo usado por el catálogo).
    public List<Categoria> categorias() { return categoriaRepo.findAll(); }

    // Lista productos con stock igual o menor al umbral, ordenados de menor a mayor stock.
    public List<Producto> stockBajo(int umbral) {
        return productoRepo.findByStockLessThanEqualOrderByStockAsc(umbral);
    }

    // Cuenta el total de productos.
    public long total() { return productoRepo.count(); }

    /** Crea un producto validando que el nombre no esté repetido (ignora mayúsculas/minúsculas); el stock nunca baja de 0. */
    @org.springframework.transaction.annotation.Transactional
    public Producto crear(String nombre, String descripcion, double precio, int stock,
                          String imagen, Long categoriaId) {
        if (nombre != null && productoRepo.existsByNombreIgnoreCase(nombre.trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un producto con ese nombre");
        }
        Producto p = new Producto();
        p.setNombre(nombre);
        p.setDescripcion(descripcion);
        p.setPrecio(precio);
        p.setStock(Math.max(0, stock));
        p.setImagen(imagen);
        if (categoriaId != null) categoriaRepo.findById(categoriaId).ifPresent(p::setCategoria);
        return productoRepo.save(p);
    }

    /** Actualiza solo los campos enviados (no nulos/no vacíos) y valida que el nuevo nombre no choque con otro producto. */
    @org.springframework.transaction.annotation.Transactional
    public void actualizar(Long id, String nombre, String descripcion, Double precio,
                           Integer stock, String imagen, Long categoriaId) {
        Producto p = productoRepo.findById(id).orElseThrow();
        if (nombre != null && !nombre.isBlank() && productoRepo.existsByNombreIgnoreCaseAndIdNot(nombre.trim(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un producto con ese nombre");
        }
        if (nombre != null && !nombre.isBlank()) p.setNombre(nombre);
        if (descripcion != null) p.setDescripcion(descripcion);
        if (precio != null && precio > 0) p.setPrecio(precio);
        if (stock != null && stock >= 0) p.setStock(stock);
        if (imagen != null && !imagen.isBlank()) p.setImagen(imagen);
        if (categoriaId != null) categoriaRepo.findById(categoriaId).ifPresent(p::setCategoria);
        productoRepo.save(p);
    }

    // Suma o resta stock (delta) sin dejar que el resultado quede negativo.
    @org.springframework.transaction.annotation.Transactional
    public void ajustarStock(Long id, int delta) {
        Producto p = productoRepo.findById(id).orElseThrow();
        int nuevo = Math.max(0, p.getStock() + delta);
        p.setStock(nuevo);
        productoRepo.save(p);
    }

    // Elimina un producto por id.
    @org.springframework.transaction.annotation.Transactional
    public void eliminar(Long id) {
        productoRepo.deleteById(id);
    }
}
