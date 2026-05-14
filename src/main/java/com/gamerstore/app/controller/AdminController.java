package com.gamerstore.app.controller;

import com.gamerstore.app.service.CategoriaService;
import com.gamerstore.app.service.ClienteService;
import com.gamerstore.app.service.ProductoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final ProductoService productoService;
    private final CategoriaService categoriaService;
    private final ClienteService clienteService;

    public AdminController(ProductoService productoService,
                           CategoriaService categoriaService,
                           ClienteService clienteService) {
        this.productoService = productoService;
        this.categoriaService = categoriaService;
        this.clienteService = clienteService;
    }

    private void common(Model model, HttpSession session, String moduloActivo, String titulo) {
        model.addAttribute("moduloActivo", moduloActivo);
        model.addAttribute("tituloModulo", titulo);
        model.addAttribute("adminNombre", session.getAttribute("nombreCompleto"));
    }

    // ==================== DASHBOARD ====================

    @GetMapping({"", "/"})
    public String dashboard(Model model, HttpSession session) {
        common(model, session, "dashboard", "Dashboard");
        model.addAttribute("totalProductos", productoService.total());
        model.addAttribute("totalCategorias", categoriaService.total());
        model.addAttribute("totalClientes", clienteService.total());
        model.addAttribute("stockBajo", productoService.stockBajo(10));
        return "admin/dashboard";
    }

    // ==================== PRODUCTOS ====================

    @GetMapping("/productos")
    public String listarProductos(Model model, HttpSession session) {
        common(model, session, "productos", "Productos");
        model.addAttribute("productos", productoService.todos());
        model.addAttribute("categorias", categoriaService.listar());
        return "admin/productos";
    }

    @PostMapping("/productos/crear")
    public String crearProducto(@RequestParam String nombre,
                                @RequestParam(required = false) String descripcion,
                                @RequestParam double precio,
                                @RequestParam int stock,
                                @RequestParam(required = false) String imagen,
                                @RequestParam(required = false) Long categoriaId,
                                RedirectAttributes ra) {
        productoService.crear(nombre, descripcion, precio, stock, imagen, categoriaId);
        ra.addFlashAttribute("mensaje", "Producto creado correctamente");
        return "redirect:/admin/productos";
    }

    @PostMapping("/productos/{id}/actualizar")
    public String actualizarProducto(@PathVariable Long id,
                                     @RequestParam String nombre,
                                     @RequestParam(required = false) String descripcion,
                                     @RequestParam Double precio,
                                     @RequestParam Integer stock,
                                     @RequestParam(required = false) String imagen,
                                     @RequestParam(required = false) Long categoriaId,
                                     RedirectAttributes ra) {
        productoService.actualizar(id, nombre, descripcion, precio, stock, imagen, categoriaId);
        ra.addFlashAttribute("mensaje", "Producto actualizado");
        return "redirect:/admin/productos";
    }

    @PostMapping("/productos/{id}/stock")
    public String ajustarStock(@PathVariable Long id, @RequestParam int delta) {
        productoService.ajustarStock(id, delta);
        return "redirect:/admin/productos";
    }

    @PostMapping("/productos/{id}/eliminar")
    public String eliminarProducto(@PathVariable Long id, RedirectAttributes ra) {
        try {
            productoService.eliminar(id);
            ra.addFlashAttribute("mensaje", "Producto eliminado");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "No se pudo eliminar: " + e.getMessage());
        }
        return "redirect:/admin/productos";
    }

    // ==================== CATEGORIAS ====================

    @GetMapping("/categorias")
    public String listarCategorias(Model model, HttpSession session) {
        common(model, session, "categorias", "Categorias");
        model.addAttribute("categorias", categoriaService.listar());
        return "admin/categorias";
    }

    @PostMapping("/categorias/crear")
    public String crearCategoria(@RequestParam String nombre, RedirectAttributes ra) {
        try {
            categoriaService.crear(nombre);
            ra.addFlashAttribute("mensaje", "Categoría creada");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/categorias";
    }

    @PostMapping("/categorias/{id}/actualizar")
    public String actualizarCategoria(@PathVariable Long id, @RequestParam String nombre,
                                      RedirectAttributes ra) {
        categoriaService.actualizar(id, nombre);
        ra.addFlashAttribute("mensaje", "Categoría actualizada");
        return "redirect:/admin/categorias";
    }

    @PostMapping("/categorias/{id}/eliminar")
    public String eliminarCategoria(@PathVariable Long id, RedirectAttributes ra) {
        try {
            categoriaService.eliminar(id);
            ra.addFlashAttribute("mensaje", "Categoría eliminada");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "No se puede eliminar (puede tener productos asociados)");
        }
        return "redirect:/admin/categorias";
    }

    // ==================== CLIENTES ====================

    @GetMapping("/clientes")
    public String listarClientes(Model model, HttpSession session) {
        common(model, session, "clientes", "Clientes");
        model.addAttribute("clientes", clienteService.listar());
        return "admin/clientes";
    }

    @PostMapping("/clientes/crear")
    public String crearCliente(@RequestParam String dni,
                               @RequestParam String nombres,
                               @RequestParam String apellidos,
                               @RequestParam(required = false) String telefono,
                               @RequestParam(required = false) String email,
                               @RequestParam(required = false) String direccion,
                               RedirectAttributes ra) {
        try {
            clienteService.crear(dni, nombres, apellidos, telefono, email, direccion);
            ra.addFlashAttribute("mensaje", "Cliente registrado correctamente");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/clientes";
    }

    @PostMapping("/clientes/{id}/actualizar")
    public String actualizarCliente(@PathVariable Long id,
                                    @RequestParam String nombres,
                                    @RequestParam String apellidos,
                                    @RequestParam(required = false) String telefono,
                                    @RequestParam(required = false) String email,
                                    @RequestParam(required = false) String direccion,
                                    RedirectAttributes ra) {
        clienteService.actualizar(id, nombres, apellidos, telefono, email, direccion);
        ra.addFlashAttribute("mensaje", "Cliente actualizado");
        return "redirect:/admin/clientes";
    }

    @PostMapping("/clientes/{id}/eliminar")
    public String eliminarCliente(@PathVariable Long id, RedirectAttributes ra) {
        try {
            clienteService.eliminar(id);
            ra.addFlashAttribute("mensaje", "Cliente eliminado");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "No se puede eliminar (puede tener pedidos asociados)");
        }
        return "redirect:/admin/clientes";
    }
}
