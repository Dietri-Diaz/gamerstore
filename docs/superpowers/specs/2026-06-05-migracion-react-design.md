# Migración del frontend de GamerStore: Thymeleaf → React

**Fecha:** 2026-06-05
**Autor:** Dietri-Diaz (con Claude Code)
**Estado:** Aprobado para implementación

## 1. Objetivo

Reemplazar el frontend de GamerStore (hoy Thymeleaf renderizado en servidor) por una
**SPA en React**. El backend **sigue siendo Spring Boot**, pero deja de renderizar HTML y
pasa a exponer una **API REST (JSON)**. Sin recortes de funcionalidad: se migran todas las
páginas actuales.

## 2. Decisiones (acordadas con el usuario)

| Tema | Decisión |
|------|----------|
| Backend | Spring Boot se queda; se convierte en API REST pura (JSON). |
| Stack front | **Vite + React + React Router** (SPA), lo más simple sobre una API Java. |
| Autenticación | **Spring Security + JWT** (ambos son dependencias Maven, sin instalación aparte). |
| Estructura | Un solo repo; el front vive en `frontend/`. |
| Estilos | Diseño **desde cero**, CSS propio limpio (acento índigo/superficies blancas). Sin Thymeleaf, sin Bootstrap heredado. |
| Alcance | Todas las páginas públicas + panel admin completo. **Pedidos queda fuera** (no tiene UI hoy). |

## 3. Arquitectura

```
React (Vite) SPA  ── fetch JSON / JWT en Authorization ──▶  Spring Boot REST API
frontend/                                                    @RestController + Services + JPA (igual)
```

- La capa de negocio **no cambia**: `model/`, `repository/`, `service/` se conservan.
- Cambia la capa web: controladores Thymeleaf → `@RestController` que devuelven DTOs JSON.
- Auth de sesión (`HttpSession` + `AdminInterceptor`) → **JWT stateless** validado por un filtro
  de Spring Security.

### Dev vs Prod
- **Dev:** dos procesos. Spring en `:8080`, Vite en `:5173`. Vite proxea `/api` → `:8080`
  (sin problemas de CORS, con hot-reload).
- **Prod:** `npm run build` genera la SPA en `src/main/resources/static/`. Un controlador
  *catch-all* sirve `index.html` para rutas de React. Resultado: un solo JAR.

## 4. API REST

| Método | Ruta | Protegido | Reemplaza |
|--------|------|-----------|-----------|
| POST | `/api/auth/login` | No | `/auth/login-custom` |
| GET | `/api/auth/me` | JWT | (datos del admin logueado) |
| GET | `/api/config` | No | nombre tienda + nº WhatsApp |
| GET | `/api/productos?categoria=&q=` | No | catálogo + filtros |
| GET | `/api/productos/{id}` | No | detalle + relacionados |
| GET | `/api/categorias` | No | filtros |
| GET | `/api/admin/dashboard` | JWT | dashboard (KPIs + stock bajo) |
| GET/POST/PUT/DELETE | `/api/admin/productos[/{id}]` | JWT | CRUD productos |
| PATCH | `/api/admin/productos/{id}/stock?delta=` | JWT | ajustar stock |
| GET/POST/PUT/DELETE | `/api/admin/categorias[/{id}]` | JWT | CRUD categorías |
| GET/POST/PUT/DELETE | `/api/admin/clientes[/{id}]` | JWT | CRUD clientes |

Errores: `@RestControllerAdvice` global → `{ "error": "mensaje" }` con 400 (validación),
404 (no encontrado), 401/403 (auth).

### Seguridad
- `JwtService`: firma/valida HS256; claims `sub=username`, `uid`, `nombre`, `rol`.
- `JwtAuthFilter` (OncePerRequestFilter): lee `Authorization: Bearer`, valida y pone el
  `Authentication` (authority `ROLE_ADMIN`) en el contexto.
- `SecurityConfig`: CSRF off, CORS on, sesión STATELESS. `permitAll` en auth/config/GET
  públicos + estáticos; `/api/admin/**` requiere `ROLE_ADMIN`.
- `PasswordEncoder` (BCrypt) se mantiene; login sigue usando `UsuarioService.autenticar`.

## 5. DTOs

`LoginRequest`, `LoginResponse(token, username, nombre, rol)`, `ConfigDTO`,
`ProductoDTO`, `ProductoRequest`, `ProductoDetalleDTO(producto, relacionados)`,
`CategoriaDTO`, `CategoriaRequest`, `ClienteDTO`, `ClienteRequest`,
`DashboardDTO(totalProductos, totalCategorias, totalClientes, stockBajo[])`.

## 6. Frontend

```
frontend/
  vite.config.js        # proxy /api → :8080 ; build.outDir → ../src/main/resources/static
  index.html            # fuentes Inter + bootstrap-icons (CDN)
  src/
    main.jsx, App.jsx   # rutas
    index.css           # design system propio (variables, botones, cards, tablas, admin)
    api/                # client (fetch + token) + endpoints
    auth/               # AuthContext, useAuth, ProtectedRoute
    config/             # ConfigContext (whatsapp, nombre)
    components/         # public (Navbar, Footer, ProductCard) / admin (Sidebar, Topbar) / ui (Modal, Alert)
    pages/
      public/  Home, Catalogo, ProductoDetalle, Contacto
      admin/   Login, Dashboard, AdminProductos, AdminCategorias, AdminClientes
```

- Rutas públicas con `PublicLayout` (Navbar+Footer). Rutas admin con `AdminLayout`
  (Sidebar+Topbar) envueltas en `ProtectedRoute`. `/admin/login` sin layout.
- Token + usuario en `localStorage`; el client adjunta el header y ante 401 limpia y
  redirige a login.
- CRUDs con modal propio (sin JS de Bootstrap). Confirmación al eliminar.

## 7. Paridad de funcionalidad (qué se recrea)

- **Pública:** Home (hero, features, destacados, CTA WhatsApp), Catálogo (búsqueda +
  filtro por categoría + grid), Detalle (specs, stock, WhatsApp con SKU, relacionados),
  Contacto (WhatsApp, info tienda, "cómo comprar").
- **Admin:** Login (con demo admin123/gamerstore123), Dashboard (4 KPIs + stock bajo +
  acciones rápidas), Productos (tabla + crear/editar modal + eliminar), Categorías
  (tabla + crear/editar + eliminar), Clientes (tabla + crear/editar + eliminar).

## 8. Limpieza (qué se elimina del backend)

- Dependencia `spring-boot-starter-thymeleaf` y config Thymeleaf en `application.properties`.
- `templates/` (todas las vistas) y `static/css/theme.css`.
- `HomeController`, `AdminController` (Thymeleaf) → reemplazados por REST controllers.
- `AdminInterceptor` y el `WebConfig` que lo registra → reemplazados por Spring Security.

## 9. Verificación

- Backend: `./mvnw -q -DskipTests compile` debe pasar. Tests de endpoints si da tiempo.
- Frontend: `npm install` + `npm run build` deben pasar; la SPA queda en `static/`.
- Manual (cuando haya XAMPP/MySQL): login JWT, catálogo+filtros, detalle/WhatsApp,
  cada CRUD.

## 10. Plan de implementación (orden)

1. Backend: `pom.xml` (quitar Thymeleaf, añadir Security + jjwt).
2. Backend: `JwtService`, `JwtAuthFilter`, `SecurityConfig` (CORS, stateless, rutas).
3. Backend: DTOs.
4. Backend: REST controllers (auth, config, productos públicos, categorías, admin x4,
   dashboard) + `@RestControllerAdvice` + `SpaController` (catch-all).
5. Backend: borrar capa Thymeleaf (controllers viejos, interceptor, templates, theme.css)
   y ajustar `application.properties`.
6. Backend: compilar.
7. Frontend: scaffold Vite + router + design system CSS.
8. Frontend: api client + auth + config contexts.
9. Frontend: páginas públicas + layout.
10. Frontend: páginas admin + layout.
11. Frontend: `npm install` + `build`; verificar integración
