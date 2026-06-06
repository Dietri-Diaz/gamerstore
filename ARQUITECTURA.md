# GamerStore

Tienda online gaming con panel administrativo. Proyecto del curso Marcos de Desarrollo Web (UTP).

Arquitectura: **backend Spring Boot como API REST** + **frontend React (SPA)**.

## Tecnologías

**Backend**
- Java 17 (compila y corre en Java 21)
- Spring Boot 3.5.6 — Spring Web (REST), Spring Data JPA + Hibernate
- **Spring Validator** (Bean Validation) para validar los formularios
- MariaDB/MySQL (XAMPP), BCrypt para passwords, Maven
- (La seguridad con **Spring Security** se agrega en el avance final, Semana 18)

**Frontend**
- React 18 + React Router 6
- Vite 5 (dev server + build)
- CSS propio (sin framework), Bootstrap Icons + fuente Inter por CDN

## Estructura del proyecto

```
gamerstore/
├── pom.xml
├── tienda_pc.sql
├── frontend/                       FRONTEND React (Vite)
│   ├── package.json
│   ├── vite.config.js              proxy /api → :8080 ; build → ../resources/static
│   └── src/
│       ├── api/                    cliente fetch + endpoints
│       ├── auth/                   AuthContext + ProtectedRoute (JWT)
│       ├── config/                 ConfigContext (whatsapp, nombre tienda)
│       ├── components/             public/ (Navbar, Footer, ProductCard) · admin/ (Sidebar, Topbar) · ui/ (Modal, Alert, Spinner)
│       └── pages/                  public/ (Home, Catalogo, ProductoDetalle, Contacto) · admin/ (Login, Dashboard, AdminProductos, AdminCategorias, AdminClientes)
└── src/main/
    ├── java/com/gamerstore/app/
    │   ├── controller/             @RestController (API REST JSON)
    │   ├── dto/                    objetos de transferencia (records)
    │   ├── security/               JwtService + JwtAuthFilter
    │   ├── model/                  entidades JPA
    │   ├── repository/             Spring Data JPA
    │   ├── service/                lógica de negocio
    │   └── config/                 SecurityConfig (JWT + CORS) · WebConfig (SPA fallback) · DataSeeder
    └── resources/
        ├── application.properties  BD + JWT + CORS
        └── static/                 SPA compilada (la genera `npm run build`)
```

## Cómo correr el proyecto

Requisito: iniciar **XAMPP (MySQL)**. La base `tienda_pc` se crea sola al primer arranque
y se llena con datos de ejemplo.

### Opción A — Desarrollo (2 servidores, con hot-reload)

1. **Backend** (en la carpeta raíz):
   ```
   ./mvnw spring-boot:run
   ```
   Levanta la API en `http://localhost:8080`.

2. **Frontend** (en `frontend/`, primera vez `npm install`):
   ```
   npm run dev
   ```
   Abre la tienda en `http://localhost:5173`. Vite redirige automáticamente las
   llamadas `/api` al backend.

### Opción B — Producción (un solo JAR)

```
cd frontend && npm run build        # genera la SPA dentro de src/main/resources/static
cd .. && ./mvnw clean package       # empaqueta API + SPA en un único JAR
java -jar target/gamerstore-0.0.1-SNAPSHOT.jar
```
Todo queda servido desde `http://localhost:8080`.

## Acceso al panel admin

- URL: `http://localhost:5173/admin/login` (dev) o `/admin/login` (prod)
- Usuario: `admin123`
- Contraseña: `gamerstore123`

## API REST

Públicos (sin token):

- `GET  /api/config` — nombre de tienda + número de WhatsApp
- `GET  /api/productos?categoria=&q=` — catálogo con filtros
- `GET  /api/productos/{id}` — detalle + relacionados
- `GET  /api/categorias` — categorías
- `POST /api/auth/login` — valida y devuelve `{ username, nombre, rol }`

Admin (el panel; la protección con Spring Security llega en el avance final):

- `GET  /api/admin/dashboard`
- `GET/POST/PUT/DELETE /api/admin/productos[/{id}]` · `PATCH /api/admin/productos/{id}/stock?delta=`
- `GET/POST/PUT/DELETE /api/admin/categorias[/{id}]`
- `GET/POST/PUT/DELETE /api/admin/clientes[/{id}]`

## Autenticación

1. El front envía usuario/clave a `POST /api/auth/login`.
2. `UsuarioService` valida la contraseña con **BCrypt** y devuelve los datos del admin.
3. El front guarda esos datos y muestra el panel.

La protección real de las rutas (Spring Security) se implementará en el **avance final
(Semana 18)**, según la rúbrica del curso.

## Validación de datos (Spring Validator)

Los formularios validan en el backend con **Bean Validation** (`@Valid` en los
controladores + anotaciones en los DTOs de `dto/`):

- `@NotBlank`, `@Size` — campos de texto obligatorios y su longitud.
- `@NotNull`, `@Min`, `@DecimalMin` — precio y stock del producto.
- `@Pattern` — DNI de 8 dígitos y teléfono numérico del cliente.
- `@Email` — correo del cliente.

Si algún dato es inválido, la API responde `400` con un JSON
`{ "error": "...", "errores": { campo: mensaje } }` y el formulario muestra el mensaje.

## Pruebas

```
./mvnw test
```
`ApiIntegrationTest` corre sobre una base **H2 en memoria** (no necesita XAMPP) y verifica
el arranque del contexto, los endpoints públicos, la protección de `/api/admin/**` y el
flujo login → JWT → acceso.

## Base de datos

Tablas: `usuario`, `cliente`, `categoria`, `producto`, `pedido`, `pedido_item`
(pedidos/POS quedan para el siguiente avance). Configuración en `application.properties`.

Valores por defecto (XAMPP):
```
spring.datasource.url=jdbc:mysql://localhost:3306/tienda_pc?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=
```
