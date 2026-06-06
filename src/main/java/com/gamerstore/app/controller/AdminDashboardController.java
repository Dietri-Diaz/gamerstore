package com.gamerstore.app.controller;

import com.gamerstore.app.dto.DashboardDTO;
import com.gamerstore.app.dto.ProductoDTO;
import com.gamerstore.app.service.CategoriaService;
import com.gamerstore.app.service.ClienteService;
import com.gamerstore.app.service.ProductoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** KPIs del panel + productos con stock bajo (requiere rol ADMIN). */
@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private static final int UMBRAL_STOCK_BAJO = 10;

    private final ProductoService productoService;
    private final CategoriaService categoriaService;
    private final ClienteService clienteService;

    public AdminDashboardController(ProductoService productoService,
                                   CategoriaService categoriaService,
                                   ClienteService clienteService) {
        this.productoService = productoService;
        this.categoriaService = categoriaService;
        this.clienteService = clienteService;
    }

    @GetMapping
    public DashboardDTO dashboard() {
        return new DashboardDTO(
                productoService.total(),
                categoriaService.total(),
                clienteService.total(),
                productoService.stockBajo(UMBRAL_STOCK_BAJO).stream().map(ProductoDTO::from).toList()
        );
    }
}
