# model/ — Capa M del MVC

Aquí están las **entidades JPA** con `@Entity`. Cada clase mapea a una tabla en MariaDB. Hibernate las traduce a SQL automáticamente.

**Archivos:**
- `Producto.java` → tabla `producto` (FK a `categoria`)
- `Categoria.java` → tabla `categoria`
- `Cliente.java` → tabla `cliente` (DNI único)
- `Usuario.java` → tabla `usuario` (empleado del ERP, password BCrypt)
- `Pedido.java` → tabla `pedido` (FK a `cliente`) — se usa en Avance 3
- `PedidoItem.java` → tabla `pedido_item` (FK a `pedido` y `producto`) — Avance 3
- `Rol.java` → enum de roles (ADMIN)

**Patrón:**
```java
@Entity
@Table(name = "nombre_tabla")
public class MiEntidad {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String campo;
}
```
