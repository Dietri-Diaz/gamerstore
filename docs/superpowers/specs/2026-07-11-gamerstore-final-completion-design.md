# GamerStore — Completar proyecto para presentación final

**Fecha:** 2026-07-11
**Proyecto:** `gamerstore-main` (Spring Boot 3.5.6 / Java 17 / MariaDB + React 18 + Vite 5)
**Objetivo:** Dejar el sistema 100% funcional para la presentación final: seguridad real con JWT, validación de campos únicos con feedback tipo toast, datos reales de tecnología, imágenes guardadas en el proyecto, reporte PDF de pedidos, y los módulos que faltaban por completar. Cero errores en cualquier pantalla.

---

## 1. Estado actual (línea base verificada)

- **Seguridad:** No existe Spring Security ni JWT. `POST /api/auth/login` valida credenciales con BCrypt y devuelve `{username, nombre, rol}` **sin token**. `/api/admin/**` está **abierto** a nivel de red; el `ProtectedRoute` del frontend solo revisa `localStorage` (cosmético).
- **Validación de únicos:** Ya existe para `categoria.nombre` (`existsByNombreIgnoreCase[AndIdNot]`) y `cliente.dni` (`existsByDni[AndIdNot]`). El `GlobalExceptionHandler` mapea validación → 400 y `DataIntegrityViolationException` → 409. **Falta** validar `producto.nombre`, `cliente.email`, y `usuario.username`/`usuario.email` en runtime.
- **Módulos:** No hay marcadores "en construcción" en el código; los 6 CRUD (productos, categorías, clientes, pedidos, dashboard, login) están implementados. Piezas incompletas/faltantes reales: seguridad, reporte de pedidos, imágenes locales, query `PedidoRepository.topProductos` que existe pero nadie invoca, y no hay módulo de usuarios.
- **Imágenes:** `producto.imagen` es un `String` con URL externa de Unsplash. No hay subida de archivos ni almacenamiento local.
- **Datos:** `config/DataSeeder.java` (idempotente, `CommandLineRunner`) siembra 7 categorías, 12 productos gaming, 5 clientes y 1 admin (`admin123` / `gamerstore123`).
- **Frontend:** Sistema de `Toast` (`useToast`), `Confirm`, `Modal`, `Alert` ya existen y se reusan. `client.js` NO manda cabecera de autorización. La tienda pública es "cotizar por WhatsApp" (deep link `wa.me`), sin carrito.

## 2. Alcance (decisiones tomadas)

- **Reporte de pedidos:** PDF profesional (OpenPDF).
- **Imágenes:** descargar imágenes libres de alta calidad (Unsplash/Pexels) al proyecto, lo más parecidas a cada producto. `producto.imagen` guarda solo la ruta.
- **Extras incluidos:** subida de imágenes en admin, módulo de Usuarios (admin), Top productos en Dashboard.
- **Datos reales (apiperu.dev / RENIEC):** integración con la API de apiperu.dev usando el token del usuario para traer nombres reales de personas por DNI. Se usa en el seeder (clientes con nombres reales de RENIEC) y en un autocompletar de DNI en el formulario de clientes.
- **Se mantiene** el flujo de tienda por **WhatsApp** (cotizar por WhatsApp). **NO** se implementa carrito/checkout público.
- **Moneda/locale:** Soles peruanos (S/), consistente con el número de WhatsApp +51 y el `money()` actual.

## 3. Diseño por componente

### 3.1 Seguridad — Spring Security + JWT (stateless)

**Enfoque elegido:** Spring Security con filtro JWT stateless + **refresh tokens con estado (persistidos en BD) y rotación**. El access token es de vida corta; el refresh token es opaco, se guarda en BD y se puede revocar (logout / rotación), lo que da un flujo de sesión completo y revocable. Descartado: sesión/cookie con estado (no encaja con SPA en dominio separado) y filtro casero sin el starter (reinventa lo que Security ya da). Descartado también el refresh "stateless" (JWT largo sin persistir), porque no permite revocar.

**Dependencias (`pom.xml`):**
- `org.springframework.boot:spring-boot-starter-security`
- `io.jsonwebtoken:jjwt-api:0.12.6`, `jjwt-impl:0.12.6` (runtime), `jjwt-jackson:0.12.6` (runtime)

**Propiedades nuevas (`application.properties`):**
```
app.jwt.secret=<clave-HS256-larga-en-base64>
app.jwt.access-expiration-ms=300000       # 5 minutos (access token)
app.jwt.refresh-expiration-ms=604800000   # 7 días (refresh token)
```

**Clases nuevas (`config/security/`):**
- `JwtService` — genera y valida **access tokens** HS256 (vida corta). Subject = `username`; claims `rol`, `nombre`, `id`. Métodos: `generarAccess(Usuario)`, `extraerUsername(token)`, `esValido(token, userDetails)`.
- `RefreshToken` (`@Entity`, tabla `refresh_token`) — `id`, `token` (UUID opaco, único), `usuario_id` (`@ManyToOne`), `expiraEn` (Instant), `revocado` (boolean). `RefreshTokenRepository` (`findByToken`, `deleteByUsuario`, etc.).
- `RefreshTokenService` — `crear(usuario)` emite un refresh token nuevo; `validarYRotar(token)` verifica que exista, no esté expirado ni revocado, revoca el actual y emite uno nuevo (rotación); `revocar(token)` para logout. Limpieza de expirados oportunista.
- `CustomUserDetailsService implements UserDetailsService` — carga `Usuario` por username (fallback email) y arma `UserDetails` con authority `ROLE_<rol>`.
- `JwtAuthenticationFilter extends OncePerRequestFilter` — extrae `Authorization: Bearer`, valida y setea el `SecurityContext`. Ignora la ruta de login.
- `SecurityConfig` (`@EnableWebSecurity`) — `SecurityFilterChain`:
  - `sessionCreationPolicy(STATELESS)`, `csrf.disable()`, CORS con la config existente.
  - Permit: `POST /api/auth/login`, `POST /api/auth/refresh`, `GET /api/productos/**`, `GET /api/categorias/**`, `GET /api/config/**`, `/images/**`, recursos estáticos/SPA.
  - `requestMatchers("/api/admin/**").hasRole("ADMIN")`.
  - `anyRequest().authenticated()`.
  - Registra `JwtAuthenticationFilter` antes de `UsernamePasswordAuthenticationFilter`.
  - `AuthenticationEntryPoint` → 401 JSON `{error:"No autenticado"}`; `AccessDeniedHandler` → 403 JSON.
- `AuthenticationManager` y `PasswordEncoder` (BCrypt, ya existe) expuestos como beans.

**Cambios en `AuthController`:**
- `POST /api/auth/login` — autentica via `AuthenticationManager` + `CustomUserDetailsService` (patrón estándar; `UsuarioService.autenticar` queda como apoyo/legado), emite access + refresh token y devuelve `LoginResponse { accessToken, refreshToken, username, nombre, rol }`.
- `POST /api/auth/refresh` — recibe `{ refreshToken }`, lo valida y rota (`RefreshTokenService.validarYRotar`), y devuelve `{ accessToken, refreshToken }` nuevos. Si el refresh es inválido/expirado/revocado → 401.
- `POST /api/auth/logout` — recibe `{ refreshToken }` y lo revoca (requiere autenticación).
- `GET /api/auth/me` — devuelve `{username, nombre, rol}` del access token (para restaurar sesión al recargar). Requiere autenticación.

**Frontend:**
- `client.js`: guardar/leer `gs_token` (access) y `gs_refresh` en `localStorage`; añadir `Authorization: Bearer <access>` cuando exista.
- **Refresh silencioso:** ante una respuesta **401** por access token expirado, `client.js` llama una sola vez a `POST /api/auth/refresh` con el `gs_refresh`; si tiene éxito, guarda los nuevos tokens y **reintenta** la petición original de forma transparente; si el refresh falla, limpia sesión y redirige a `/admin/login`. Se serializa el refresh para no dispararlo en paralelo (una única promesa de refresh compartida).
- `AuthContext.login()` guarda ambos tokens + `user`; `logout()` llama a `POST /api/auth/logout` y limpia todo; `getToken()`; en el arranque valida con `GET /api/auth/me` (si falla, intenta refresh; si también falla, limpia sesión).
- `ProtectedRoute` se mantiene (ahora respaldado por el servidor).
- **Contador de sesión discreto:** componente `SessionTimer.jsx` montado solo en el área admin (dentro de `AdminLayout`), fijo y sutil en una esquina (abajo-derecha, texto pequeño/tenue, p. ej. `Sesión 4:37`). Decodifica el claim `exp` del access token y cuenta atrás hasta su expiración (5:00). Cuando el refresh silencioso emite un access nuevo, el token cambia y el contador **se reinicia** a ~5:00 automáticamente. En el último minuto se resalta en ámbar. No bloquea nada: es solo informativo.

### 3.2 Validación de campos únicos → toast de duplicado

**Backend:**
- Introducir `DuplicateResourceException` (o reutilizar `ResponseStatusException(CONFLICT, msg)`) que el `GlobalExceptionHandler` mapea a **409** `{error: msg}`.
- `Producto`: agregar `@Column(unique = true)` en `nombre`; `ProductoRepository.existsByNombreIgnoreCase` / `...AndIdNot`; `ProductoService.crear/actualizar` lanzan 409 "Ya existe un producto con ese nombre".
- `Cliente`: agregar `existsByEmailIgnoreCase[AndIdNot]`; validar en `crear/actualizar` → "Ese email ya está registrado". DNI ya validado → mensaje "El DNI ya está registrado".
- `Categoria`: ya validado → mensaje "La categoría ya existe".
- `Usuario` (módulo nuevo): `existsByUsername`/`existsByEmail` (+`AndIdNot`) → "Ese usuario ya existe" / "Ese email ya está registrado".

**Mensajes exactos (español):**
- Producto duplicado: `Ya existe un producto con ese nombre`
- Categoría duplicada: `La categoría ya existe`
- DNI duplicado: `El DNI ya está registrado`
- Email de cliente duplicado: `Ese email ya está registrado`
- Usuario duplicado: `Ese usuario ya existe`
- Email de usuario duplicado: `Ese email ya está registrado`

**Frontend:** en cada handler de submit (crear/editar) de Productos, Categorías, Clientes y Usuarios, ante error disparar `toast.error(err.message)` (además del `<Alert>` inline que ya existe). El mensaje del 409 llega tal cual desde el backend.

### 3.3 Base de datos limpia + data real de tecnología

- **Reset de BD:** `DROP DATABASE tienda_pc` (elección definitiva; no TRUNCATE, porque `ddl-auto=update` no agrega de forma fiable el nuevo constraint único de `producto.nombre` sobre una tabla existente). Al reiniciar, Hibernate (`ddl-auto=update`, `createDatabaseIfNotExist=true`) reconstruye el esquema desde cero con los constraints y corre el seeder.
- **Reescribir `DataSeeder`** (idempotente por `count()==0`) con:
  - **~10 categorías:** Tarjetas Gráficas, Procesadores, Placas Madre, Memorias RAM, Almacenamiento, Monitores, Periféricos, Audio, Sillas Gamer, Consolas.
  - **~28 productos** reales con precios creíbles en Soles del mercado peruano y `imagen` = ruta local (`/images/productos/<slug>.jpg`). Ejemplos: RTX 4060/4070/4080, RX 7800 XT; Ryzen 5 5600 / 7 7800X3D, Intel i5-13600K/i7-13700K; B550/B650/Z790; Corsair Vengeance 16/32GB DDR4/DDR5; SSD Samsung 980 1TB / WD SN850X 2TB; PSU Corsair RM750; monitores (Odyssey G7, LG UltraGear); teclados/mouse/headsets; sillas gamer; consolas (PS5, Xbox Series X, Switch OLED).
  - **~6 clientes** con DNI y **nombres reales obtenidos de RENIEC** (apiperu.dev) a partir del DNI en tiempo de seed (best-effort; si la API está deshabilitada u offline, usa nombres de respaldo). Email, teléfono y dirección de contacto se mantienen como datos de la tienda.
  - **1 admin** por defecto (`admin123` / `gamerstore123`) para no perder el acceso; además el módulo de Usuarios permite crear más.
  - **~40 pedidos históricos** con `fecha` repartida en los últimos ~6 meses (usar `Random` con semilla fija para reproducibilidad), 1–4 ítems por pedido, `metodoPago` y `estado` variados, `total` calculado. Fechas de registro de clientes también en el pasado.
- **Ajuste de `@PrePersist`:** en `Pedido` y `Cliente`, setear la fecha **solo si es null** (`if (fecha == null) fecha = now()`), para no pisar las fechas históricas del seeder.

### 3.4 Imágenes en el proyecto (no en la BD)

- **Ubicación física:** `src/main/resources/static/images/productos/`. `producto.imagen` guarda solo la ruta relativa `/images/productos/<archivo>` (nunca el binario).
- **Descarga de seed:** un script/paso de setup descarga ~28 imágenes libres (Unsplash/Pexels) a esa carpeta con nombres por slug; el seeder referencia esas rutas.
- **Subida en admin:** `POST /api/admin/uploads` (multipart, `MultipartFile file`) valida tipo (`image/*`) y tamaño (≤ 5 MB), guarda como `<uuid>.<ext>` en la carpeta del proyecto y devuelve `{ url: "/images/productos/<uuid>.<ext>" }`.
- **Config:** habilitar multipart (`spring.servlet.multipart.max-file-size=5MB`, `max-request-size=5MB`); `WebConfig` añade `addResourceHandler("/images/**")` apuntando a la carpeta física (location `file:` a la ruta del proyecto) para servir subidas en runtime sin reconstruir; `/images/**` es público en `SecurityConfig`.
- **Vite:** añadir `/images` al `proxy` de `vite.config.js` (dev) para que `<img src="/images/...">` cargado desde :5173 resuelva contra :8080.
- **Frontend:** el form de producto reemplaza el `<input type="url">` por un selector de archivo que sube a `/api/admin/uploads`, muestra preview y guarda la ruta devuelta en `form.imagen`. Las 4 vistas que renderizan `<img src={p.imagen}>` no cambian de estructura.

### 3.5 Módulo de Usuarios (admin)

- **Backend:** `AdminUsuarioController` `/api/admin/usuarios`:
  - `GET /` lista (sin exponer el hash de password).
  - `POST /` crea (`@Valid UsuarioRequest`: username, email, nombre, password, rol, telefono) — hashea con BCrypt, valida duplicados.
  - `PUT /{id}` actualiza (password opcional; si viene vacío, no se cambia).
  - `DELETE /{id}` elimina; **bloquea** eliminar el último `ADMIN`.
  - DTOs: `UsuarioDTO` (sin password), `UsuarioRequest`. `UsuarioService` gana `listar/crear/actualizar/eliminar`.
- **Frontend:** nueva ruta protegida `/admin/usuarios`, link en `Sidebar`, página `AdminUsuarios.jsx` (tabla + modal con select de rol ADMIN/USUARIO), mismo patrón toast/confirm. Toast en duplicado de username/email.

### 3.6 Top productos en Dashboard

- **Backend:** `DashboardDTO` gana `List<TopProductoDTO> topProductos` (`productoNombre`, `cantidad`). `AdminDashboardController` invoca `PedidoService.topProductos(limit=5)` (que ya existe en el repo).
- **Frontend:** `Dashboard.jsx` muestra una card/lista "Más vendidos" con nombre + unidades.

### 3.7 Reporte PDF de pedidos

- **Backend:** `GET /api/admin/pedidos/reporte.pdf?desde=&hasta=&estado=` — genera PDF con **OpenPDF** (`com.github.librepdf:openpdf`): encabezado GamerStore, resumen de filtros aplicados, tabla (Código, Cliente, Fecha, Estado, Método, Ítems, Total) y pie con total de ventas y conteo. Devuelve `application/pdf` con `Content-Disposition: attachment`. Filtros opcionales por rango de fecha y estado; sin filtros = todos.
- **Frontend:** botón "Descargar PDF" en `AdminPedidos.jsx`. Como requiere cabecera `Authorization`, la descarga se hace con `fetch` (token) → `blob` → enlace temporal de descarga. Helper `downloadBlob(path, filename)` en `client.js`.

### 3.8 Limpieza "cero errores"

- Quitar `spring.jpa.properties.hibernate.dialect` para eliminar el warning de MariaDB (Hibernate lo autodetecta).
- Actualizar `ApiIntegrationTest`: como `/api/admin/**` ahora exige `ROLE_ADMIN`, el test hace login para obtener el token y lo envía en las llamadas admin (o se configura un usuario/token de prueba). Los endpoints públicos siguen sin auth.
- Wire de `topProductos` (elimina el código muerto).
- Verificación end-to-end en el navegador antes de cerrar.

### 3.9 Datos reales de clientes vía RENIEC (apiperu.dev)

- **Proveedor confirmado:** `apiperu.dev`. Endpoint `GET https://apiperu.dev/api/dni/{dni}` con header `Authorization: Bearer <token>`. Respuesta: `{ success: true, data: { nombres, apellido_paterno, apellido_materno, nombre_completo, ... } }`. El token del usuario ya fue verificado y funciona.
- **Config:** token, base-url y un flag `enabled` en `application.properties` (`enabled=false` en el perfil de test para no llamar a la red en los tests). El token es sensible: se coloca en properties para el uso local, con nota de moverlo a variable de entorno antes de subir a un repo público.
- **Backend:** `ReniecService.consultarDni(dni)` (usa `RestClient`) devuelve `Optional<ReniecPersona{nombres, apellidos, nombreCompleto}>`; ante deshabilitado/token vacío/error/DNI inválido devuelve vacío (best-effort, nunca rompe). Endpoint admin `GET /api/admin/clientes/reniec/{dni}` que devuelve la persona o 404.
- **Seeder:** cada cliente sembrado consulta su DNI en RENIEC y usa el nombre real; si la consulta falla, usa el nombre de respaldo. Así la BD local queda con nombres reales, pero los tests (enabled=false) usan respaldo sin red.
- **Frontend:** en el formulario de clientes, botón "Buscar" junto al DNI que llama al endpoint y autocompleta nombres/apellidos con los datos de RENIEC (toast de éxito/error).

## 4. Contrato de datos (DTOs nuevos/cambiados)

- `LoginResponse`: `{ accessToken, refreshToken, username, nombre, rol }` (antes sin tokens).
- `RefreshRequest`: `{ refreshToken }`; `TokenResponse`: `{ accessToken, refreshToken }` (respuesta de `/auth/refresh`).
- `UsuarioDTO`: `{ id, username, email, nombre, telefono, rol, fechaRegistro }` (sin password).
- `UsuarioRequest`: `{ username, email, nombre, password, telefono, rol }` con validación Jakarta.
- `TopProductoDTO`: `{ productoNombre, cantidad }`; `DashboardDTO` gana `topProductos`.
- `UploadResponse`: `{ url }`.

## 5. Criterios de aceptación (verificación end-to-end)

1. **Sin token, `/api/admin/**` responde 401**; con token de admin, responde 200. La página de admin en el navegador exige login real.
2. Login devuelve access + refresh token; recargar la página mantiene la sesión (via `/api/auth/me`); logout revoca el refresh token en BD y limpia el cliente.
2b. Con el access token expirado (5 min), la primera llamada da 401 y el cliente hace **refresh silencioso** (rota el refresh en BD) y reintenta con éxito, sin sacar al usuario. Si el refresh está revocado/expirado, sí redirige a login.
2c. En el área admin se ve un **contador discreto** en la esquina (abajo-derecha) que cuenta atrás desde 5:00 hasta la expiración del access token y se reinicia solo al refrescarse; se resalta en ámbar en el último minuto.
3. Crear producto/categoría/cliente/usuario con un valor único ya existente muestra un **toast** con el mensaje en español correspondiente y **no** crea el registro.
4. La BD arranca limpia con ~10 categorías, ~28 productos con precios en S/ e imágenes locales, ~6 clientes **con nombres reales de RENIEC** y ~40 pedidos con fechas repartidas en ~6 meses.
4b. En el formulario de clientes, escribir un DNI y pulsar "Buscar" autocompleta nombres/apellidos reales desde RENIEC (apiperu.dev).
5. Las imágenes se sirven desde `/images/productos/...` (archivos en el proyecto); ninguna imagen se guarda en la BD.
6. Subir una imagen al crear/editar un producto la guarda en la carpeta del proyecto y la muestra.
7. El Dashboard muestra "Más vendidos" calculado desde los pedidos.
8. "Descargar PDF" en Pedidos baja un PDF válido con la tabla y los totales; respeta filtros de fecha/estado.
9. La tienda pública (Home, Catálogo, Detalle, Contacto) funciona con "cotizar por WhatsApp" y muestra las imágenes locales.
10. Backend levanta **sin warnings de dialecto** y `mvn test` pasa. Ninguna pantalla muestra error.

## 6. Fuera de alcance

- Carrito y checkout público (la tienda queda como cotización por WhatsApp).
- Pasarela de pago real (se verá más adelante; por ahora se deja como está).
- Refactors no relacionados con los objetivos anteriores.
