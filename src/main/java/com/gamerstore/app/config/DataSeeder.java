package com.gamerstore.app.config;

import com.gamerstore.app.model.Categoria;
import com.gamerstore.app.model.Cliente;
import com.gamerstore.app.model.Producto;
import com.gamerstore.app.model.Rol;
import com.gamerstore.app.model.Usuario;
import com.gamerstore.app.repository.CategoriaRepository;
import com.gamerstore.app.repository.ClienteRepository;
import com.gamerstore.app.repository.ProductoRepository;
import com.gamerstore.app.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final CategoriaRepository categoriaRepo;
    private final ProductoRepository productoRepo;
    private final UsuarioRepository usuarioRepo;
    private final ClienteRepository clienteRepo;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(CategoriaRepository categoriaRepo,
                      ProductoRepository productoRepo,
                      UsuarioRepository usuarioRepo,
                      ClienteRepository clienteRepo,
                      PasswordEncoder passwordEncoder) {
        this.categoriaRepo = categoriaRepo;
        this.productoRepo = productoRepo;
        this.usuarioRepo = usuarioRepo;
        this.clienteRepo = clienteRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        Map<String, Categoria> cats = new HashMap<>();

        // ===== CATEGORIAS =====
        if (categoriaRepo.count() == 0) {
            String[] nombres = {"Consolas", "Perifericos", "Monitores", "Sillas", "Streaming", "Mandos", "VR"};
            for (String n : nombres) cats.put(n, categoriaRepo.save(new Categoria(n)));
            log.info("Seed: {} categorias creadas", nombres.length);
        } else {
            categoriaRepo.findAll().forEach(c -> cats.put(c.getNombre(), c));
        }

        // ===== PRODUCTOS =====
        if (productoRepo.count() == 0) {
            productoRepo.save(new Producto("PlayStation 5 Slim", "Consola Sony PS5 Slim 1TB edicion digital", 2499.00, "https://images.unsplash.com/photo-1606813907291-d86efa9b94db?w=600", 15, cats.get("Consolas")));
            productoRepo.save(new Producto("Xbox Series X", "Consola Microsoft Xbox Series X 1TB 4K 120Hz", 2699.00, "https://images.unsplash.com/photo-1621259182978-fbf93132d53d?w=600", 10, cats.get("Consolas")));
            productoRepo.save(new Producto("Nintendo Switch OLED", "Consola Nintendo Switch modelo OLED blanca", 1499.00, "https://images.unsplash.com/photo-1612036782180-6f0b6cd846fe?w=600", 20, cats.get("Consolas")));
            productoRepo.save(new Producto("Logitech G Pro X Superlight", "Mouse gaming inalambrico 63g sensor HERO 25K", 499.00, "https://images.unsplash.com/photo-1527814050087-3793815479db?w=600", 30, cats.get("Perifericos")));
            productoRepo.save(new Producto("Razer BlackWidow V4 Pro", "Teclado mecanico RGB switches verdes Chroma", 899.00, "https://images.unsplash.com/photo-1618384887929-16ec33fab9ef?w=600", 25, cats.get("Perifericos")));
            productoRepo.save(new Producto("HyperX Cloud III", "Auriculares gaming 7.1 DTS con microfono", 349.00, "https://images.unsplash.com/photo-1599669454699-248893623440?w=600", 40, cats.get("Perifericos")));
            productoRepo.save(new Producto("Samsung Odyssey G7 27\"", "Monitor gaming curvo QHD 240Hz 1ms", 1899.00, "https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=600", 8, cats.get("Monitores")));
            productoRepo.save(new Producto("Secretlab Titan Evo", "Silla gaming ergonomica talla R cuero NEO", 2199.00, "https://images.unsplash.com/photo-1598550476439-6847785fcea6?w=600", 8, cats.get("Sillas")));
            productoRepo.save(new Producto("Elgato Stream Deck MK.2", "Controlador para streaming 15 teclas LCD", 599.00, "https://images.unsplash.com/photo-1591799264318-7e6ef8ddb7ea?w=600", 18, cats.get("Streaming")));
            productoRepo.save(new Producto("DualSense Edge PS5", "Mando PS5 profesional personalizable", 799.00, "https://images.unsplash.com/photo-1592840496694-26d035b52b48?w=600", 22, cats.get("Mandos")));
            productoRepo.save(new Producto("Meta Quest 3", "Visor realidad virtual 128GB wireless", 1799.00, "https://images.unsplash.com/photo-1622979135225-d2ba269cf1ac?w=600", 9, cats.get("VR")));
            productoRepo.save(new Producto("LG UltraGear 34\"", "Monitor ultrawide QHD 160Hz Nano IPS", 2299.00, "https://images.unsplash.com/photo-1616711906333-23cf8b918a76?w=600", 6, cats.get("Monitores")));
            log.info("Seed: 12 productos creados");
        }

        // ===== CLIENTES DEMO =====
        if (clienteRepo.count() == 0) {
            clienteRepo.save(crearCliente("70123456", "Carlos", "Quispe Vargas", "987654321", "carlos.quispe@gmail.com", "Av. Arequipa 1234, Lima"));
            clienteRepo.save(crearCliente("72345678", "Maria", "Rojas Gomez", "912345678", "maria.rojas@hotmail.com", "Jr. Cusco 567, San Isidro"));
            clienteRepo.save(crearCliente("75987654", "Diego", "Fernandez Torres", "956123789", "diego.fdz@outlook.com", "Calle Las Begonias 89, Miraflores"));
            clienteRepo.save(crearCliente("76543210", "Lucia", "Mendoza Salas", "999888777", "lucia.mendoza@gmail.com", "Av. Brasil 2345, Jesus Maria"));
            clienteRepo.save(crearCliente("78901234", "Andres", "Castillo Ruiz", "987111222", null, null));
            log.info("Seed: 5 clientes demo creados");
        }

        // ===== ADMIN POR DEFECTO =====
        if (!usuarioRepo.existsByRol(Rol.ADMIN)) {
            Usuario admin = new Usuario();
            admin.setUsername("admin123");
            admin.setPassword(passwordEncoder.encode("gamerstore123"));
            admin.setEmail("admin123@gamerstore.com");
            admin.setNombre("Administrador");
            admin.setRol(Rol.ADMIN);
            usuarioRepo.save(admin);
            log.info("==================================================");
            log.info("Admin por defecto creado (password BCrypt):");
            log.info("  usuario: admin123");
            log.info("  email:   admin123@gamerstore.com");
            log.info("  pass:    gamerstore123");
            log.info("==================================================");
        }
    }

    private Cliente crearCliente(String dni, String nombres, String apellidos,
                                 String telefono, String email, String direccion) {
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
