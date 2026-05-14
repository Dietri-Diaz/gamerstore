# service/ — Lógica de negocio

Capa intermedia entre `controller/` y `repository/`. Aquí van las **reglas, validaciones y transacciones**. El controlador llama al servicio, el servicio decide qué hacer y usa el repositorio.

**Por qué separar Controller de Service:**
- El controlador solo coordina HTTP (recibe parámetros, retorna vista).
- El servicio contiene la lógica reutilizable y testeable.
- Si mañana cambias la capa web por una API REST, los servicios se reutilizan sin tocar.

**Archivos:**
- `ProductoService.java` — CRUD de productos + filtros + ajuste de stock.
- `CategoriaService.java` — CRUD de categorías.
- `ClienteService.java` — CRUD de clientes, valida DNI único.
- `UsuarioService.java` — autenticación con BCrypt (`passwordEncoder.matches`).
- `PedidoService.java` — consultas para dashboard (Avance 3 agrega `crearDesdePOS`).

**Patrón:**
```java
@Service
public class MiServicio {
    private final MiRepository repo;

    @Transactional
    public Entidad crear(...) {
        Entidad e = new Entidad();
        e.setCampo(...);
        return repo.save(e);
    }
}
```

`@Transactional` asegura que si algo falla a mitad, se hace **rollback** automático.
