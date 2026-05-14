package com.gamerstore.app.controller;

import com.gamerstore.app.model.Categoria;
import com.gamerstore.app.model.Producto;
import com.gamerstore.app.model.Usuario;
import com.gamerstore.app.service.ProductoService;
import com.gamerstore.app.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;

@Controller
public class HomeController {

    private final UsuarioService usuarioService;
    private final ProductoService productoService;

    @Value("${app.whatsapp.numero:51986969024}")
    private String whatsappNumero;

    @Value("${app.tienda.nombre:GamerStore}")
    private String tiendaNombre;

    public HomeController(UsuarioService usuarioService, ProductoService productoService) {
        this.usuarioService = usuarioService;
        this.productoService = productoService;
    }

    @ModelAttribute
    public void addGlobal(Model model, HttpSession session) {
        model.addAttribute("nombreCompleto", session.getAttribute("nombreCompleto"));
        model.addAttribute("rolSesion", session.getAttribute("rol"));
        model.addAttribute("whatsappNumero", whatsappNumero);
        model.addAttribute("tiendaNombre", tiendaNombre);
    }

    @GetMapping("/")
    public String index(Model model) {
        List<Producto> productos = productoService.todos();
        model.addAttribute("destacados", productos.subList(0, Math.min(8, productos.size())));
        return "index";
    }

    @GetMapping("/productos")
    public String productos(@RequestParam(required = false) String categoria,
                            @RequestParam(required = false) String q,
                            Model model) {
        model.addAttribute("productos", productoService.filtrar(categoria, q));
        model.addAttribute("categorias", productoService.categorias().stream().map(Categoria::getNombre).toList());
        model.addAttribute("categoriaActiva", categoria);
        model.addAttribute("busqueda", q);
        return "productos";
    }

    @GetMapping("/productos/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        Optional<Producto> p = productoService.porId(id);
        if (p.isEmpty()) return "redirect:/productos";
        Producto prod = p.get();
        model.addAttribute("producto", prod);
        List<Producto> relacionados = productoService.porCategoria(prod.getCategoria()).stream()
                .filter(x -> !x.getId().equals(id)).limit(4).toList();
        model.addAttribute("relacionados", relacionados);
        return "productos/producto_detalle";
    }

    @GetMapping("/contacto")
    public String contacto() {
        return "contacto";
    }

    // ===== AUTH =====

    @GetMapping("/auth/login")
    public String login() {
        return "auth/login";
    }

    @PostMapping("/auth/login-custom")
    public String loginPost(@RequestParam String username,
                            @RequestParam String password,
                            HttpSession session,
                            Model model) {
        Optional<Usuario> u = usuarioService.autenticar(username, password);
        if (u.isEmpty()) {
            model.addAttribute("loginError", "Usuario o contraseña incorrectos");
            return "auth/login";
        }
        Usuario user = u.get();
        session.setAttribute("userId", user.getId());
        session.setAttribute("username", user.getUsername());
        session.setAttribute("nombreCompleto", user.getNombre());
        session.setAttribute("rol", user.getRol().name());
        return "redirect:/admin";
    }

    @PostMapping("/auth/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}
