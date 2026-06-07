# Plantilla de Documentación Técnica – APF3
Curso: Marcos de Desarrollo Web (100000SI57)
Avance de Proyecto Final 3 (APF3)

---

## 1. PORTADA

**Universidad Tecnológica del Perú (UTP)**

- **Curso:** Marcos de Desarrollo Web (100000SI57)
- **Avance:** Proyecto Final 3 (APF3)
- **Proyecto:** GamerStore — Tienda online gaming con panel administrativo (ERP)
- **Docente:** _________________________
- **Ciclo / Sección:** _________________________
- **Integrantes:**
  - _________________________
  - _________________________
  - _________________________
  - _________________________
- **Fecha:** Junio de 2026

---

## 2. INDICE GENERAL

1. Portada
2. Índice General
3. Resumen Ejecutivo
4. Introducción
5. Objetivos (General y Específicos)
6. Análisis del Problema (RF y RNF)
7. Arquitectura de la Solución
8. Modelo C4
9. Diseño de Base de Datos
10. Implementación del Sistema
11. Implementación de Validaciones
12. Desarrollo de Funcionalidades CRUD
13. Pruebas Realizadas
14. Conclusiones
15. Recomendaciones
16. Bibliografía (IEEE)
17. Anexos

---

## 3. RESUMEN EJECUTIVO

**Problema.** Una tienda de productos gamer gestiona su catálogo, sus categorías y sus
clientes de forma manual (hojas de cálculo y cuadernos), lo que genera errores,
información duplicada y pérdida de tiempo al consultar o actualizar el inventario.

**Objetivo.** Desarrollar GamerStore, un sistema web con una tienda pública y un panel
administrativo (ERP) que persiste su información en una base de datos relacional y permite
gestionar productos, categorías y clientes mediante operaciones CRUD validadas.

**Tecnologías.** Backend con Java 17 y Spring Boot 3.5.6 (Spring Web, Spring Data JPA,
Hibernate), base de datos MariaDB/MySQL, validación con Bean Validation y contraseñas con
BCrypt. Frontend con React 18 + Vite. Maven como gestor de dependencias.

**Resultados obtenidos.** Se logró una API REST funcional conectada a la base de datos,
con CRUD completo de productos, categorías, clientes y pedidos (registrar, listar, editar,
eliminar y buscar), validación de datos en los formularios, una interfaz React responsiva
con dashboard de indicadores (incluye total de pedidos y ventas) y un conjunto de pruebas de
integración que verifican el funcionamiento.

---

## 4. INTRODUCCIÓN

**Contexto.** El comercio de equipos gamer en Perú crece de forma sostenida y las tiendas
necesitan herramientas web para mostrar su catálogo y administrar su negocio sin depender
de procesos manuales.

**Justificación.** Un sistema web con persistencia en base de datos reduce errores,
centraliza la información y permite escalar el negocio. El uso de Spring Boot con Spring
Data JPA acelera el desarrollo y aplica buenas prácticas de la industria (arquitectura por
capas, ORM, validación), que son justamente los temas del curso.

**Alcance.** En este avance (APF3) el sistema cubre: catálogo público con búsqueda y
filtros, detalle de producto, contacto, y un panel administrativo con dashboard y la
gestión (CRUD) de productos, categorías, clientes y pedidos, todo conectado a la base de
datos mediante Spring Data JPA, con validación de los datos ingresados.

**Limitaciones.** El registro de pedidos calcula el total pero aún no descuenta el stock del
producto automáticamente (se hará junto al módulo POS en el avance final). La seguridad con
Spring Security (autenticación/autorización) también corresponde al avance final (Semana 18);
por ahora el login valida credenciales con BCrypt como preparación.

---

## 5. OBJETIVOS (GENERAL Y ESPECÍFICOS)

**Objetivo General.**
Desarrollar un sistema web para una tienda gamer que gestione su catálogo y sus clientes
con persistencia en base de datos relacional, aplicando arquitectura por capas y operaciones
CRUD validadas.

**Objetivos Específicos.**
1. Diseñar e implementar el modelo de base de datos con sus entidades, relaciones y
   restricciones de integridad.
2. Integrar la persistencia con Spring Data JPA e Hibernate sobre MySQL/MariaDB.
3. Implementar el CRUD (registrar, listar, editar, eliminar y buscar) de productos,
   categorías y clientes.
4. Aplicar validación de datos con Bean Validation y validaciones de negocio.
5. Construir una interfaz web moderna y responsiva en React que consuma la API REST.
6. Verificar el funcionamiento mediante pruebas de integración.

---

## 6. ANÁLISIS DEL PROBLEMA (RF Y RNF)

**Requerimientos Funcionales (RF).**
- RF01: El sistema permite registrar, listar, editar y eliminar productos.
- RF02: El sistema permite registrar, listar, editar y eliminar categorías.
- RF03: El sistema permite registrar, listar, editar y eliminar clientes.
- RF04: El sistema permite buscar productos por nombre y filtrarlos por categoría.
- RF05: El sistema permite buscar clientes por nombre, DNI o correo.
- RF06: El sistema permite registrar pedidos (con cliente, productos y método de pago) y
  cambiar su estado (PENDIENTE, PAGADO, ENVIADO, ENTREGADO, CANCELADO).
- RF07: El sistema muestra un dashboard con indicadores (productos, clientes, pedidos,
  ventas y stock bajo).
- RF08: El administrador inicia sesión con usuario y contraseña.
- RF09: La tienda pública muestra el catálogo y el detalle de cada producto.

**Requerimientos No Funcionales (RNF).**
- RNF01: La interfaz debe ser responsiva (mobile-first) y de uso intuitivo.
- RNF02: Las contraseñas se almacenan cifradas con BCrypt.
- RNF03: Los datos ingresados deben validarse antes de guardarse.
- RNF04: La API responde en formato JSON con códigos HTTP adecuados.
- RNF05: El sistema usa una arquitectura por capas mantenible y escalable.

**Reglas de Negocio.**
- RN01: El DNI del cliente es único y debe tener 8 dígitos.
- RN02: El nombre de la categoría es único.
- RN03: El precio del producto debe ser mayor a 0 y el stock no puede ser negativo.
- RN04: No se puede eliminar una categoría que tenga productos asociados.
- RN05: Todo producto pertenece a una categoría.

---

## 7. ARQUITECTURA DE LA SOLUCIÓN

### 7.1 Arquitectura General
Aplicación cliente–servidor: un frontend **React (SPA)** consume una **API REST** construida
con **Spring Boot**, que persiste los datos en **MySQL/MariaDB** mediante **Spring Data JPA /
Hibernate**.

```
[ Navegador / React (SPA) ]  --HTTP/JSON-->  [ API REST Spring Boot ]  --JPA/Hibernate-->  [ MySQL ]
```

### 7.2 Arquitectura N-Capas
El backend está organizado en capas con responsabilidades separadas:

- **Capa de presentación / API:** `controller/` (`@RestController`) — recibe las peticiones HTTP y devuelve JSON.
- **Capa de negocio:** `service/` (`@Service`) — contiene la lógica y las reglas de negocio.
- **Capa de acceso a datos:** `repository/` (Spring Data JPA) — consultas a la base de datos.
- **Capa de dominio:** `model/` (`@Entity`) — entidades mapeadas a las tablas.
- **Soporte:** `dto/` (objetos de transferencia), `config/` (configuración) y un manejador
  global de excepciones.

### 7.3 Patrón MVC
- **Modelo:** entidades JPA (`Producto`, `Categoria`, `Cliente`, `Usuario`, `Pedido`, `PedidoItem`).
- **Vista:** la SPA de React (páginas y componentes) — la presentación vive en el cliente.
- **Controlador:** los `@RestController` que exponen los endpoints y orquestan servicios.

---

## 8. MODELO C4

### 8.1 Contexto
```
            +-------------------+
   Cliente  |                   |  Administrador
  (público) |    GamerStore     |   (ERP)
     ------> |   (Sistema Web)   | <------
            |                   |
            +---------+---------+
                      |
                      v
              [ Base de datos MySQL ]
```
- **Cliente (público):** navega el catálogo y consulta productos.
- **Administrador:** gestiona productos, categorías y clientes desde el panel.

### 8.2 Contenedores
```
+------------------+        HTTP/JSON        +-----------------------+      JDBC      +-----------+
|  SPA React (Vite)| ----------------------> |  API REST Spring Boot | -------------> |   MySQL   |
|  navegador       | <---------------------- |  (Tomcat embebido)    | <------------- | MariaDB   |
+------------------+                         +-----------------------+                +-----------+
```
- **SPA React:** interfaz de usuario (puerto 5173 en dev / servida por el backend en prod).
- **API REST Spring Boot:** lógica y persistencia (puerto 8080).
- **MySQL/MariaDB:** almacenamiento (base `tienda_pc`).

### 8.3 Componentes (dentro de la API REST)
```
Controller  ->  Service  ->  Repository  ->  Entity  ->  MySQL
   (REST)       (negocio)     (Spring Data)   (JPA)
      \                                       
       \--> DTO (entrada/salida JSON)   GlobalExceptionHandler (errores)
```
Componentes principales: `ProductoController`, `CategoriaController`, `AdminProductoController`,
`AdminCategoriaController`, `AdminClienteController`, `AdminPedidoController`,
`AdminDashboardController`, `AuthController`;
`ProductoService`, `CategoriaService`, `ClienteService`, `PedidoService`, `UsuarioService`;
`ProductoRepository`, `CategoriaRepository`, `ClienteRepository`, `PedidoRepository`,
`UsuarioRepository`; y los mappers `ProductoMapper`, `CategoriaMapper`, `ClienteMapper`,
`PedidoMapper`.

### 8.4 Código
Ejemplo del flujo "crear producto" a nivel de código:
```
AdminProductoController.crear(@Valid ProductoRequest)
    -> ProductoService.crear(...)
        -> ProductoRepository.save(producto)   // Spring Data JPA
            -> INSERT INTO producto ...         // Hibernate / MySQL
    <- ProductoDTO.from(producto)               // mapeo entidad -> DTO
```

---

## 9. DISEÑO DE BASE DE DATOS

### 9.1 Modelo ER
Entidades y relaciones (base `tienda_pc`):
```
categoria 1 ----- N producto
cliente   1 ----- N pedido
pedido    1 ----- N pedido_item
producto  1 ----- N pedido_item
usuario   (independiente: administrador del ERP)
```

### 9.2 Diccionario de Datos

**usuario**
| Campo | Tipo | Restricción |
|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT |
| username | VARCHAR(50) | NOT NULL, UNIQUE |
| email | VARCHAR(120) | NOT NULL, UNIQUE |
| nombre | VARCHAR(100) | NOT NULL |
| password | VARCHAR(100) | NOT NULL (hash BCrypt) |
| telefono | VARCHAR(15) | NULL |
| fecha_registro | DATETIME | NOT NULL |
| rol | VARCHAR(20) | NOT NULL (default 'ADMIN') |

**cliente**
| Campo | Tipo | Restricción |
|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT |
| dni | VARCHAR(8) | NOT NULL, UNIQUE |
| nombres | VARCHAR(100) | NOT NULL |
| apellidos | VARCHAR(100) | NOT NULL |
| telefono | VARCHAR(15) | NULL |
| email | VARCHAR(120) | NULL |
| direccion | VARCHAR(200) | NULL |
| fecha_registro | DATETIME | NOT NULL |

**categoria**
| Campo | Tipo | Restricción |
|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT |
| nombre | VARCHAR(100) | NOT NULL, UNIQUE |

**producto**
| Campo | Tipo | Restricción |
|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT |
| nombre | VARCHAR(200) | NOT NULL |
| descripcion | VARCHAR(500) | NULL |
| precio | DOUBLE | NOT NULL |
| imagen | VARCHAR(500) | NULL |
| stock | INT | NOT NULL (default 0) |
| categoria_id | BIGINT | NOT NULL, FK → categoria(id) |

**pedido**
| Campo | Tipo | Restricción |
|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT |
| cliente_id | BIGINT | NOT NULL, FK → cliente(id) |
| fecha | DATETIME | NOT NULL |
| estado | VARCHAR(30) | NOT NULL (default 'PENDIENTE') |
| total | DOUBLE | NOT NULL |
| metodo_pago | VARCHAR(30) | NULL |

**pedido_item**
| Campo | Tipo | Restricción |
|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT |
| pedido_id | BIGINT | NOT NULL, FK → pedido(id) ON DELETE CASCADE |
| producto_id | BIGINT | NOT NULL, FK → producto(id) |
| cantidad | INT | NOT NULL |
| precio_unitario | DOUBLE | NOT NULL |

### 9.3 Restricciones
- **PRIMARY KEY** en el `id` de cada tabla (autoincremental).
- **FOREIGN KEY** con integridad referencial: `producto→categoria`, `pedido→cliente`,
  `pedido_item→pedido` (ON DELETE CASCADE) y `pedido_item→producto`.
- **UNIQUE**: `usuario.username`, `usuario.email`, `cliente.dni`, `categoria.nombre`.
- **NOT NULL** en los campos obligatorios; charset `utf8mb4`.

---

## 10. IMPLEMENTACIÓN DEL SISTEMA

### 10.1 Entidades
Clases `@Entity` mapeadas a las tablas, con relaciones JPA. Ejemplo (`Producto`):
```java
@Entity
@Table(name = "producto")
public class Producto {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String nombre;
    @Column(length = 500) private String descripcion;
    @Column(nullable = false) private double precio;
    private int stock;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;
    // getters/setters...
}
```
Relaciones: `Producto → Categoria` (`@ManyToOne`); `Pedido → Cliente` (`@ManyToOne`);
`Pedido → PedidoItem` (`@OneToMany`, `cascade = ALL`, `orphanRemoval = true`);
`PedidoItem → Producto` (`@ManyToOne`).

### 10.2 Repositorios
Interfaces que extienden `JpaRepository` (Spring Data genera el CRUD automáticamente):
```java
public interface ProductoRepository extends JpaRepository<Producto, Long> {
    List<Producto> findByCategoriaNombreIgnoreCase(String nombre);
    List<Producto> findByNombreContainingIgnoreCase(String q);
    List<Producto> findByStockLessThanEqualOrderByStockAsc(int stock);
}
```

### 10.3 DTOs
Objetos de transferencia (Java `record`) para no exponer las entidades y dar una respuesta
JSON limpia: `ProductoDTO`, `ProductoRequest`, `CategoriaDTO`, `CategoriaRequest`,
`ClienteDTO`, `ClienteRequest`, `PedidoDTO`, `PedidoItemDTO`, `PedidoRequest`,
`PedidoItemRequest`, `PedidoUpdateRequest`, `LoginRequest`, `LoginResponse`, `DashboardDTO`.

### 10.4 Mappers
La conversión **entidad → DTO** se realiza en clases **Mapper** dedicadas (`@Component`) en el
paquete `mapper/`: `ProductoMapper`, `CategoriaMapper`, `ClienteMapper` y `PedidoMapper`.
Centralizan el mapeo y se inyectan en los controladores, manteniéndolos limpios:
```java
@Component
public class ProductoMapper {
    public ProductoDTO toDTO(Producto p) {
        return new ProductoDTO(p.getId(), p.getNombre(), p.getDescripcion(),
            p.getPrecio(), p.getImagen(), p.getStock(),
            p.getCategoria() != null ? p.getCategoria().getId() : null,
            p.getCategoria() != null ? p.getCategoria().getNombre() : null);
    }
}
```

### 10.5 Servicios
Clases `@Service` con la lógica de negocio y las transacciones. Ejemplo (`ProductoService`):
```java
@Transactional
public Producto crear(String nombre, String descripcion, double precio, int stock,
                      String imagen, Long categoriaId) {
    Producto p = new Producto();
    p.setNombre(nombre);
    p.setPrecio(precio);
    p.setStock(Math.max(0, stock));
    if (categoriaId != null) categoriaRepo.findById(categoriaId).ifPresent(p::setCategoria);
    return productoRepo.save(p);
}
```

### 10.6 Controladores
`@RestController` que exponen los endpoints REST. Ejemplo (`AdminProductoController`):
```java
@RestController
@RequestMapping("/api/admin/productos")
public class AdminProductoController {
    @GetMapping public List<ProductoDTO> listar() { ... }
    @PostMapping public ProductoDTO crear(@Valid @RequestBody ProductoRequest r) { ... }
    @PutMapping("/{id}") public ProductoDTO actualizar(@PathVariable Long id,
                                       @Valid @RequestBody ProductoRequest r) { ... }
    @DeleteMapping("/{id}") public ResponseEntity<Void> eliminar(@PathVariable Long id) { ... }
}
```

### 10.7 Manejo de Excepciones
Un `@RestControllerAdvice` global convierte las excepciones en respuestas JSON uniformes:
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class) // errores de @Valid -> 400
    @ExceptionHandler(IllegalArgumentException.class)        // reglas de negocio -> 400
    @ExceptionHandler(NoSuchElementException.class)          // no encontrado -> 404
    @ExceptionHandler(DataIntegrityViolationException.class) // FK/único -> 409
    @ExceptionHandler(ResponseStatusException.class)         // login inválido -> 401
}
```
Respuesta de error: `{ "error": "mensaje", "errores": { campo: mensaje } }`.

---

## 11. IMPLEMENTACIÓN DE VALIDACIONES

### Bean Validation
Anotaciones estándar en los DTOs de los formularios, validadas con `@Valid` en los
controladores:
```java
public record ClienteRequest(
    @NotBlank(message = "El DNI es obligatorio")
    @Pattern(regexp = "\\d{8}", message = "El DNI debe tener 8 dígitos") String dni,
    @NotBlank(message = "Los nombres son obligatorios") String nombres,
    @NotBlank(message = "Los apellidos son obligatorios") String apellidos,
    @Email(message = "El email no es válido") String email,
    ...) {}
```
Anotaciones usadas: `@NotBlank`, `@Size`, `@NotNull`, `@Min`, `@DecimalMin`, `@Pattern`, `@Email`.

### Validaciones Personalizadas
Reglas de negocio validadas en la capa de servicio, con mensajes amigables:
```java
// ClienteService
if (repo.existsByDni(dni))
    throw new IllegalArgumentException("Ya existe un cliente con ese DNI");
```
- DNI de cliente único (RN01) y nombre de categoría único (RN02).
- No eliminar categorías/clientes con registros asociados (se devuelve `409` con mensaje).
- Restricciones también a nivel de base de datos (UNIQUE, NOT NULL, FK).

---

## 12. DESARROLLO DE FUNCIONALIDADES CRUD

CRUD completo para **Productos, Categorías, Clientes y Pedidos** (4 de las 5 tablas
principales = 80%).

| Operación | Método/Endpoint | Descripción |
|---|---|---|
| **Registrar** | `POST /api/admin/{productos\|categorias\|clientes\|pedidos}` | Crea un registro (con validación). |
| **Listar** | `GET /api/admin/{...}` | Devuelve todos los registros. |
| **Editar** | `PUT /api/admin/{...}/{id}` | Actualiza un registro existente. |
| **Eliminar** | `DELETE /api/admin/{...}/{id}` | Elimina un registro. |
| **Buscar** | Buscador en vivo + filtros en la tabla del panel (y `GET /api/productos?q=&categoria=`) | Filtra por texto, categoría o estado. |

En el panel, cada tabla incluye **buscador en vivo, ordenamiento por columna y paginación**;
las altas/ediciones se hacen en un modal y las eliminaciones piden confirmación.

**Pedidos:** al registrar un pedido se eligen el cliente, el método de pago y una o más
líneas (producto + cantidad); el sistema calcula el total automáticamente. La edición de un
pedido cambia su **estado** (PENDIENTE, PAGADO, ENVIADO, ENTREGADO, CANCELADO).

---

## 13. PRUEBAS REALIZADAS

Pruebas de integración automatizadas (`ApiIntegrationTest`) sobre una base **H2 en memoria**,
ejecutables con `./mvnw test`.

| # | Caso de prueba | Resultado esperado | Resultado obtenido |
|---|---|---|---|
| 1 | GET de endpoints públicos (productos, categorías, config) | HTTP 200 | ✅ 200 |
| 2 | Login con credenciales correctas (admin123) | HTTP 200 + rol ADMIN | ✅ 200 |
| 3 | Login con contraseña incorrecta | HTTP 401 | ✅ 401 |
| 4 | Login con campos vacíos | HTTP 400 (validación) | ✅ 400 |
| 5 | Crear producto inválido (nombre vacío, precio 0, stock −1) | HTTP 400 con errores por campo | ✅ 400 |
| 6 | Crear cliente con DNI inválido ("abc") | HTTP 400 (validación) | ✅ 400 |
| 7 | Crear pedido válido (cliente + 2 productos) | HTTP 200, estado PENDIENTE, total calculado | ✅ 200 |
| 8 | Crear pedido sin productos | HTTP 400 (validación) | ✅ 400 |

**Resultado global:** 8 pruebas ejecutadas, 0 fallos, 0 errores.

---

## 14. CONCLUSIONES

1. La integración con **Spring Data JPA e Hibernate** permitió implementar la persistencia y
   el CRUD con muy poco código, mapeando las clases Java directamente a las tablas MySQL.
2. La separación en **capas (Controller, Service, Repository, Entity, DTO)** hizo el proyecto
   ordenado, fácil de entender y de mantener, cumpliendo el patrón MVC.
3. El uso de **Bean Validation** y validaciones de negocio mejoró la calidad e integridad de
   los datos, evitando registros incorrectos desde el formulario hasta la base de datos.

---

## 15. RECOMENDACIONES

1. Completar el módulo **POS**: descontar el stock al registrar un pedido y generar el
   comprobante de venta (boleta/ticket).
2. Incorporar **Spring Security** (autenticación y autorización, p. ej. con JWT) para proteger
   las rutas del panel administrativo, según corresponde al avance final.
3. Evaluar **MapStruct** para automatizar los Mappers y agregar reportes/exportación de los
   datos del dashboard.

---

## 16. BIBLIOGRAFÍA (IEEE)

[1] Spring, "Spring Boot Reference Documentation," VMware, 2024. [En línea]. Disponible: https://docs.spring.io/spring-boot/index.html

[2] Spring, "Spring Data JPA - Reference Documentation," VMware, 2024. [En línea]. Disponible: https://docs.spring.io/spring-data/jpa/reference/

[3] Red Hat, "Hibernate ORM User Guide," 2024. [En línea]. Disponible: https://hibernate.org/orm/documentation/

[4] Oracle, "Jakarta Bean Validation Specification," 2024. [En línea]. Disponible: https://beanvalidation.org/

[5] Meta Open Source, "React Documentation," 2024. [En línea]. Disponible: https://react.dev/

[6] MariaDB Foundation, "MariaDB Server Documentation," 2024. [En línea]. Disponible: https://mariadb.com/kb/en/documentation/

---

## 17. ANEXOS

### Anexo A — Scripts SQL
El esquema se genera automáticamente con Hibernate (`ddl-auto=update`) y también está el
script `tienda_pc.sql` para crearlo manualmente. Resumen de tablas:
```sql
CREATE TABLE categoria (
  id BIGINT NOT NULL AUTO_INCREMENT,
  nombre VARCHAR(100) NOT NULL,
  PRIMARY KEY (id), UNIQUE KEY uk_categoria_nombre (nombre)
) ENGINE=InnoDB;

CREATE TABLE producto (
  id BIGINT NOT NULL AUTO_INCREMENT,
  nombre VARCHAR(200) NOT NULL,
  descripcion VARCHAR(500), precio DOUBLE NOT NULL,
  imagen VARCHAR(500), stock INT NOT NULL DEFAULT 0,
  categoria_id BIGINT NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_producto_categoria FOREIGN KEY (categoria_id) REFERENCES categoria(id)
) ENGINE=InnoDB;

CREATE TABLE cliente (
  id BIGINT NOT NULL AUTO_INCREMENT,
  dni VARCHAR(8) NOT NULL,
  nombres VARCHAR(100) NOT NULL, apellidos VARCHAR(100) NOT NULL,
  telefono VARCHAR(15), email VARCHAR(120), direccion VARCHAR(200),
  fecha_registro DATETIME NOT NULL,
  PRIMARY KEY (id), UNIQUE KEY uk_cliente_dni (dni)
) ENGINE=InnoDB;
```
(El script completo, con `usuario`, `pedido`, `pedido_item` y los datos de ejemplo, está en
`tienda_pc.sql` en la raíz del proyecto.)

### Anexo B — Manual de Usuario
1. **Tienda (público):** ingresa a la página de inicio, explora el catálogo, usa el buscador
   o filtra por categoría, abre un producto para ver su detalle y cotiza por WhatsApp.
2. **Panel (administrador):** ingresa a `/admin/login` con `admin123` / `gamerstore123`.
   En el dashboard verás los indicadores; en cada módulo (Productos, Categorías, Clientes)
   puedes crear con el botón "Nuevo", editar con el lápiz, eliminar con el tacho y buscar
   con la barra superior de la tabla.

### Anexo C — Manual de Instalación
**Requisitos:** Java 17+, Node.js 18+, XAMPP (MySQL/MariaDB) y Maven (incluido como wrapper).
1. Iniciar **MySQL** en XAMPP (la base `tienda_pc` se crea sola al primer arranque).
2. **Backend:** en la carpeta del proyecto ejecutar `./mvnw spring-boot:run` (API en `:8080`).
3. **Frontend (desarrollo):** en `frontend/` ejecutar `npm install` y luego `npm run dev`
   (tienda en `:5173`).
4. **Producción (un solo JAR):** `cd frontend && npm run build`, luego `./mvnw clean package`
   y `java -jar target/gamerstore-0.0.1-SNAPSHOT.jar` (todo en `:8080`).
