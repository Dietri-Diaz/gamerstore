package com.gamerstore.app.controller;

import com.gamerstore.app.dto.DashboardDTO;
import com.gamerstore.app.mapper.ProductoMapper;
import com.gamerstore.app.service.CategoriaService;
import com.gamerstore.app.service.ClienteService;
import com.gamerstore.app.service.PedidoService;
import com.gamerstore.app.service.ProductoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** KPIs del panel + productos con stock bajo. */
@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private static final int UMBRAL_STOCK_BAJO = 10;

    private final ProductoService productoService;
    private final CategoriaService categoriaService;
    private final ClienteService clienteService;
    private final PedidoService pedidoService;
    private final ProductoMapper productoMapper;

    public AdminDashboardController(ProductoService productoService,
                                   CategoriaService categoriaService,
                                   ClienteService clienteService,
                                   PedidoService pedidoService,
                                   ProductoMapper productoMapper) {
        this.productoService = productoService;
        this.categoriaService = categoriaService;
        this.clienteService = clienteService;
        this.pedidoService = pedidoService;
        this.productoMapper = productoMapper;
    }

    @GetMapping
    public DashboardDTO dashboard() {
        return new DashboardDTO(
                productoService.total(),
                categoriaService.total(),
                clienteService.total(),
                pedidoService.total(),
                pedidoService.totalVentas(),
                productoService.stockBajo(UMBRAL_STOCK_BAJO).stream().map(productoMapper::toDTO).toList()
        );
    }
}
