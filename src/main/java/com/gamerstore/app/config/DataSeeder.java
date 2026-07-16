package com.gamerstore.app.config;

import com.gamerstore.app.model.*;
import com.gamerstore.app.repository.*;
import com.gamerstore.app.dto.ReniecPersona;
import com.gamerstore.app.service.ReniecService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

/** Siembra data real de tecnología: categorías, productos (imágenes locales),
 *  clientes y pedidos históricos (últimos ~6 meses). Idempotente por tabla. */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final CategoriaRepository categoriaRepo;
    private final ProductoRepository productoRepo;
    private final UsuarioRepository usuarioRepo;
    private final ClienteRepository clienteRepo;
    private final PedidoRepository pedidoRepo;
    private final PasswordEncoder passwordEncoder;
    private final ReniecService reniecService;

    // Inyeccion por constructor de todos los repositorios y servicios que necesita el seeder.
    public DataSeeder(CategoriaRepository categoriaRepo, ProductoRepository productoRepo,
                      UsuarioRepository usuarioRepo, ClienteRepository clienteRepo,
                      PedidoRepository pedidoRepo, PasswordEncoder passwordEncoder,
                      ReniecService reniecService) {
        this.categoriaRepo = categoriaRepo;
        this.productoRepo = productoRepo;
        this.usuarioRepo = usuarioRepo;
        this.clienteRepo = clienteRepo;
        this.pedidoRepo = pedidoRepo;
        this.passwordEncoder = passwordEncoder;
        this.reniecService = reniecService;
    }

    // Helper para armar la ruta publica de la imagen de un producto a partir de su slug.
    private static String img(String slug) { return "/images/productos/" + slug + ".jpg"; }

    // CommandLineRunner: Spring Boot ejecuta este metodo automaticamente al arrancar.
    // Cada bloque (categorias, productos, clientes, pedidos, admin) chequea si la tabla
    // ya tiene datos (count() == 0) para no duplicar informacion en reinicios sucesivos.
    @Override
    public void run(String... args) {
        Map<String, Categoria> cats = new HashMap<>();

        // ===== CATEGORIAS =====
        if (categoriaRepo.count() == 0) {
            String[] nombres = {"Tarjetas Graficas", "Procesadores", "Placas Madre", "Memorias RAM",
                    "Almacenamiento", "Monitores", "Perifericos", "Audio", "Sillas Gamer", "Consolas"};
            for (String n : nombres) cats.put(n, categoriaRepo.save(new Categoria(n)));
            log.info("Seed: {} categorias creadas", nombres.length);
        } else {
            categoriaRepo.findAll().forEach(c -> cats.put(c.getNombre(), c));
        }

        // ===== PRODUCTOS =====
        if (productoRepo.count() == 0) {
            productoRepo.save(new Producto("NVIDIA GeForce RTX 4060", "Tarjeta grafica 8GB GDDR6 DLSS 3 1080p/1440p", 1399.00, img("rtx-4060"), 14, cats.get("Tarjetas Graficas")));
            productoRepo.save(new Producto("NVIDIA GeForce RTX 4070", "Tarjeta grafica 12GB GDDR6X ideal 1440p", 2599.00, img("rtx-4070"), 9, cats.get("Tarjetas Graficas")));
            productoRepo.save(new Producto("NVIDIA GeForce RTX 4080 Super", "Tarjeta grafica 16GB GDDR6X 4K high-end", 4999.00, img("rtx-4080"), 5, cats.get("Tarjetas Graficas")));
            productoRepo.save(new Producto("AMD Radeon RX 7800 XT", "Tarjeta grafica 16GB GDDR6 1440p rasterizado", 2299.00, img("rx-7800xt"), 7, cats.get("Tarjetas Graficas")));

            productoRepo.save(new Producto("AMD Ryzen 5 5600", "Procesador 6 nucleos 12 hilos AM4 3.5GHz", 549.00, img("ryzen-5-5600"), 20, cats.get("Procesadores")));
            productoRepo.save(new Producto("AMD Ryzen 7 7800X3D", "Procesador gaming 8 nucleos 3D V-Cache AM5", 1899.00, img("ryzen-7-7800x3d"), 11, cats.get("Procesadores")));
            productoRepo.save(new Producto("Intel Core i5-13600K", "Procesador 14 nucleos LGA1700 hasta 5.1GHz", 1199.00, img("intel-i5-13600k"), 13, cats.get("Procesadores")));
            productoRepo.save(new Producto("Intel Core i7-13700K", "Procesador 16 nucleos LGA1700 hasta 5.4GHz", 1699.00, img("intel-i7-13700k"), 8, cats.get("Procesadores")));

            productoRepo.save(new Producto("ASUS TUF Gaming B550-PLUS", "Placa madre AM4 DDR4 ATX PCIe 4.0", 699.00, img("mb-b550"), 15, cats.get("Placas Madre")));
            productoRepo.save(new Producto("MSI MAG B650 Tomahawk", "Placa madre AM5 DDR5 ATX WiFi", 949.00, img("mb-b650"), 10, cats.get("Placas Madre")));
            productoRepo.save(new Producto("Gigabyte Z790 AORUS Elite", "Placa madre LGA1700 DDR5 ATX", 1149.00, img("mb-z790"), 6, cats.get("Placas Madre")));

            productoRepo.save(new Producto("Corsair Vengeance 16GB DDR4", "Memoria RAM 2x8GB 3200MHz CL16", 249.00, img("ram-vengeance-16"), 30, cats.get("Memorias RAM")));
            productoRepo.save(new Producto("Corsair Vengeance 32GB DDR5", "Memoria RAM 2x16GB 6000MHz RGB", 599.00, img("ram-vengeance-32"), 18, cats.get("Memorias RAM")));

            productoRepo.save(new Producto("Samsung 980 NVMe 1TB", "SSD M.2 PCIe 3.0 hasta 3500MB/s", 329.00, img("ssd-980-1tb"), 25, cats.get("Almacenamiento")));
            productoRepo.save(new Producto("WD Black SN850X 2TB", "SSD M.2 PCIe 4.0 gaming hasta 7300MB/s", 799.00, img("ssd-sn850x-2tb"), 12, cats.get("Almacenamiento")));

            productoRepo.save(new Producto("Corsair RM750 80+ Gold", "Fuente de poder 750W modular certificada", 549.00, img("psu-rm750"), 16, cats.get("Placas Madre")));

            productoRepo.save(new Producto("Samsung Odyssey G7 27\"", "Monitor curvo QHD 240Hz 1ms", 1899.00, img("monitor-odyssey-g7"), 8, cats.get("Monitores")));
            productoRepo.save(new Producto("LG UltraGear 34\" UWQHD", "Monitor ultrawide 160Hz Nano IPS", 2299.00, img("monitor-lg-ultragear"), 6, cats.get("Monitores")));

            productoRepo.save(new Producto("Razer BlackWidow V4 Pro", "Teclado mecanico RGB switches verdes", 899.00, img("teclado-blackwidow"), 22, cats.get("Perifericos")));
            productoRepo.save(new Producto("Logitech G Pro X Superlight", "Mouse inalambrico 63g sensor HERO 25K", 499.00, img("mouse-gpro-superlight"), 28, cats.get("Perifericos")));
            productoRepo.save(new Producto("Logitech Brio 4K", "Webcam 4K UHD para streaming", 599.00, img("webcam-brio"), 14, cats.get("Perifericos")));

            productoRepo.save(new Producto("HyperX Cloud III", "Auriculares gaming 7.1 con microfono", 349.00, img("headset-cloud-iii"), 26, cats.get("Audio")));

            productoRepo.save(new Producto("Secretlab Titan Evo", "Silla gaming ergonomica cuero NEO talla R", 2199.00, img("silla-titan-evo"), 7, cats.get("Sillas Gamer")));
            productoRepo.save(new Producto("Cougar Armor One", "Silla gamer reclinable con cojines", 899.00, img("silla-cougar"), 12, cats.get("Sillas Gamer")));

            productoRepo.save(new Producto("Cooler Master ML240L AIO", "Refrigeracion liquida 240mm ARGB", 449.00, img("cooler-aio"), 15, cats.get("Placas Madre")));

            productoRepo.save(new Producto("PlayStation 5 Slim", "Consola Sony PS5 Slim 1TB edicion digital", 2499.00, img("ps5-slim"), 10, cats.get("Consolas")));
            productoRepo.save(new Producto("Xbox Series X", "Consola Microsoft 1TB 4K 120Hz", 2699.00, img("xbox-series-x"), 8, cats.get("Consolas")));
            productoRepo.save(new Producto("Nintendo Switch OLED", "Consola hibrida pantalla OLED 7\"", 1499.00, img("switch-oled"), 16, cats.get("Consolas")));
            log.info("Seed: productos de tecnologia creados");
        }

        // ===== CLIENTES (nombres reales de RENIEC via apiperu.dev, con respaldo) =====
        if (clienteRepo.count() == 0) {
            clienteRepo.save(clienteReal("70123456", "Carlos", "Quispe Vargas", "987654321", "carlos.quispe@gmail.com", "Av. Arequipa 1234, Lima"));
            clienteRepo.save(clienteReal("72345678", "Maria", "Rojas Gomez", "912345678", "maria.rojas@hotmail.com", "Jr. Cusco 567, San Isidro"));
            clienteRepo.save(clienteReal("75987654", "Diego", "Fernandez Torres", "956123789", "diego.fdz@outlook.com", "Calle Las Begonias 89, Miraflores"));
            clienteRepo.save(clienteReal("76543210", "Lucia", "Mendoza Salas", "999888777", "lucia.mendoza@gmail.com", "Av. Brasil 2345, Jesus Maria"));
            clienteRepo.save(clienteReal("78901234", "Andres", "Castillo Ruiz", "987111222", "andres.castillo@gmail.com", "Av. La Marina 456, San Miguel"));
            clienteRepo.save(clienteReal("71222333", "Valeria", "Torres Nunez", "955444333", "valeria.torres@gmail.com", "Av. Javier Prado 789, San Borja"));
            log.info("Seed: clientes creados");
        }

        // ===== PEDIDOS HISTORICOS (ultimos ~6 meses) =====
        if (pedidoRepo.count() == 0) {
            List<Producto> productos = productoRepo.findAll();
            List<Cliente> clientes = clienteRepo.findAll();
            if (!productos.isEmpty() && !clientes.isEmpty()) {
                Random rnd = new Random(20260711L); // semilla fija => reproducible
                String[] metodos = {"EFECTIVO", "TARJETA", "YAPE", "PLIN", "TRANSFERENCIA"};
                String[] estados = {"PENDIENTE", "PAGADO", "ENVIADO", "ENTREGADO", "ENTREGADO", "CANCELADO"};
                int nPedidos = 40;
                // Genera 40 pedidos con cliente, metodo de pago, estado y fecha aleatorios
                // (pero reproducibles por la semilla fija de arriba).
                for (int i = 0; i < nPedidos; i++) {
                    Pedido pedido = new Pedido();
                    pedido.setCliente(clientes.get(rnd.nextInt(clientes.size())));
                    pedido.setMetodoPago(metodos[rnd.nextInt(metodos.length)]);
                    pedido.setEstado(estados[rnd.nextInt(estados.length)]);
                    // fecha repartida en los ultimos 180 dias
                    pedido.setFecha(LocalDateTime.now()
                            .minusDays(rnd.nextInt(180))
                            .minusHours(rnd.nextInt(24))
                            .minusMinutes(rnd.nextInt(60)));

                    int nItems = 1 + rnd.nextInt(4); // 1..4 lineas
                    double total = 0;
                    List<Integer> usados = new ArrayList<>();
                    // Arma las lineas del pedido eligiendo productos al azar y sumando el total.
                    for (int j = 0; j < nItems; j++) {
                        int idx = rnd.nextInt(productos.size());
                        if (usados.contains(idx)) continue; // evita repetir producto en el mismo pedido
                        usados.add(idx);
                        Producto prod = productos.get(idx);
                        int cantidad = 1 + rnd.nextInt(3);
                        PedidoItem item = new PedidoItem(pedido, prod, cantidad, prod.getPrecio());
                        pedido.getItems().add(item);
                        total += item.getSubtotal();
                    }
                    pedido.setTotal(total);
                    pedidoRepo.save(pedido);
                }
                log.info("Seed: {} pedidos historicos creados", nPedidos);
            }
        }

        // ===== ADMIN POR DEFECTO =====
        if (!usuarioRepo.existsByRol(Rol.ADMIN)) {
            Usuario admin = new Usuario();
            admin.setUsername("admin123");
            admin.setPassword(passwordEncoder.encode("gamerstore123"));
            admin.setEmail("admin123@gamerstore.com");
            admin.setNombre("Administrador");
            admin.setTelefono("986969024");
            admin.setRol(Rol.ADMIN);
            usuarioRepo.save(admin);
            log.info("Admin por defecto: admin123 / gamerstore123");
        }
    }

    /** Crea un cliente usando el nombre real de RENIEC (si la API responde); si no, el de respaldo. */
    private Cliente clienteReal(String dni, String nombresFallback, String apellidosFallback,
                                String telefono, String email, String direccion) {
        String nombres = nombresFallback;
        String apellidos = apellidosFallback;
        Optional<ReniecPersona> persona = reniecService.consultarDni(dni);
        if (persona.isPresent()) {
            nombres = persona.get().nombres();
            apellidos = persona.get().apellidos();
            log.info("RENIEC {} -> {} {}", dni, nombres, apellidos);
        }
        Cliente c = new Cliente();
        c.setDni(dni);
        c.setNombres(nombres);
        c.setApellidos(apellidos);
        c.setTelefono(telefono);
        c.setEmail(email);
        c.setDireccion(direccion);
        return c;
    }
}
