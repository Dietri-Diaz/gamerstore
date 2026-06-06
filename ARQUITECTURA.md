# GamerStore

Tienda online gaming con panel administrativo. Proyecto del curso Marcos de Desarrollo Web (UTP).

Arquitectura: **backend Spring Boot como API REST** + **frontend React (SPA)**.

## Tecnologías

**Backend**
- Java 17 (compila y corre en Java 21)
- Spring Boot 3.5.6 — Spring Web (REST), Spring Data JPA + Hibernate
- Spring Security + **JWT** (autenticación stateless) — jjwt 0.12.6
- MariaDB/MySQL (XAMPP), BCrypt para passwords, Maven

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
- `POST /api/auth/login` — devuelve `{ token, username, nombre, rol }`

Admin (requieren header `Authorization: Bearer <token>` y rol ADMIN):

- `GET  /api/auth/me`
- `GET  /api/admin/dashboard`
- `GET/POST/PUT/DELETE /api/admin/productos[/{id}]` · `PATCH /api/admin/productos/{id}/stock?delta=`
- `GET/POST/PUT/DELETE /api/admin/categorias[/{id}]`
- `GET/POST/PUT/DELETE /api/admin/clientes[/{id}]`

## Autenticación (JWT)

1. El front envía usuario/clave a `POST /api/auth/login`.
2. `UsuarioService` valida con BCrypt y `JwtService` firma un token (HS256).
3. El front guarda el token y lo manda en cada llamada a `/api/admin/**`.
4. `JwtAuthFilter` valida el token en cada request y `SecurityConfig` exige rol ADMIN.

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
