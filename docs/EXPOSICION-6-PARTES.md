# GamerStore — Guía de exposición (6 partes)

Esta guía divide el proyecto en **6 responsabilidades**. Cada integrante domina UNA parte y puede responder:
**¿qué hace? · ¿cómo fluye (paso a paso)? · ¿en qué carpetas/archivos está? · ¿cómo lo demuestro en vivo?** + preguntas típicas del profesor.

## Cómo está armado (contexto que todos deben saber en 30 seg)

- **Dos servidores en desarrollo:**
  - **Backend (Spring Boot, Java):** API REST en `http://localhost:8080`, rutas bajo `/api/**`. Carpeta `src/main/java/com/gamerstore/app`.
  - **Frontend (React + Vite):** SPA en `http://localhost:5173`. Carpeta `frontend/src`. Llama a la API con `fetch` (Vite hace *proxy* de `/api` y `/images` al `:8080`).
- **Base de datos:** MySQL/MariaDB (XAMPP), BD `tienda_pc`. Hibernate crea las tablas desde las clases `@Entity`.
- **Arquitectura por capas (backend):**

```
  React (pantalla)  ──fetch──►  Controller (/api/...)  ──►  Service (reglas)  ──►  Repository  ──►  Base de datos
        DTO  ◄──────────────────────  Mapper (Entidad → DTO)  ◄────────────────────────────────────┘
```

- **Login:** `admin123` / `gamerstore123`.

### Reparto rápido

| Parte | Título | Responsable | En una frase |
|---|---|---|---|
| 1 | Arquitectura, base de datos y datos | | El “esqueleto”: capas, entidades, tablas y la carga de datos reales |
| 2 | Seguridad: login JWT + refresh + sesión | | Login real con token, protege el panel, refresco automático y contador |
| 3 | Tienda pública (catálogo + WhatsApp) | | La web del cliente: ver productos, filtrar y cotizar por WhatsApp |
| 4 | Admin: Productos, Categorías y subida de imágenes | | ABM del catálogo con imágenes guardadas en el proyecto |
| 5 | Clientes (RENIEC), Pedidos y Reporte PDF | | Clientes con datos reales por DNI, registrar ventas y bajar PDF |
| 6 | Validaciones, Usuarios y Dashboard | | Que no se dupliquen datos (toasts), gestión de usuarios y estadísticas |

> **Truco para el Q&A:** todos siguen el MISMO flujo (pantalla → endpoint → service → repositorio → BD). Si te preguntan algo de otra parte, explica el flujo genérico y di en qué archivo vive.

---

## PARTE 1 — Arquitectura, base de datos y datos reales

**¿Qué hace?**
Es el cimiento del proyecto: define **cómo se organiza el código en capas**, las **entidades** (que se convierten en tablas) y el **sembrador de datos** que llena la BD al arrancar con productos de tecnología reales, pedidos históricos y clientes.

**Flujo (cómo funciona):**
1. Al iniciar el backend, **Hibernate** lee las clases `@Entity` de `model/` y **crea/actualiza las tablas** en la BD `tienda_pc` (config en `application.properties`, `ddl-auto=update`).
2. Justo después arranca **`DataSeeder`** (implementa `CommandLineRunner` → se ejecuta 1 vez al inicio). Es **idempotente**: solo siembra si la tabla está vacía (`count() == 0`).
3. Inserta **10 categorías**, **28 productos** (con ruta de imagen local y precio en S/), **6 clientes** y **~40 pedidos** con fechas repartidas en 6 meses.

**Carpetas y archivos:**
- `pom.xml` — dependencias (Spring Web, Data JPA, Security, MySQL, OpenPDF…).
- `src/main/resources/application.properties` — conexión a la BD, puerto, config.
- `src/main/java/com/gamerstore/app/model/` — entidades: `Producto`, `Categoria`, `Cliente`, `Pedido`, `PedidoItem`, `Usuario`, `Rol`, `RefreshToken`.
- `src/main/java/com/gamerstore/app/config/DataSeeder.java` — carga de datos.
- `src/main/java/com/gamerstore/app/GamerStoreApplication.java` — punto de arranque (`main`).

**Qué mostrar en el código:**
- Una entidad, p. ej. `model/Producto.java`: enseña `@Entity`, `@Id @GeneratedValue`, `@Column(unique=true)` en `nombre`, y `@ManyToOne` a `Categoria` (relación).
- `DataSeeder.java`: el `if (productoRepo.count() == 0)` y la lista de productos.

**Demo en vivo:** abre phpMyAdmin (XAMPP) → BD `tienda_pc` → muestra las tablas (`producto`, `pedido`, etc.) con datos.

**Preguntas típicas + respuesta:**
- *¿Quién crea las tablas?* → Hibernate, a partir de las anotaciones `@Entity`/`@Column`; no escribimos el `CREATE TABLE` a mano.
- *¿Qué es una relación `@ManyToOne`?* → Muchos productos pertenecen a una categoría; en la tabla se guarda `categoria_id` (llave foránea).
- *¿Por qué el seeder no duplica datos si reinicio?* → Porque solo siembra cuando la tabla está vacía (`count()==0`).

---

## PARTE 2 — Seguridad: login con JWT + refresh tokens + sesión

**¿Qué hace?**
Autenticación **real**: el login entrega un **token JWT** que se exige para entrar al panel admin. El token de acceso dura poco (5 min) y se **renueva solo** con un *refresh token*; hay un **contador de sesión** en pantalla.

**Flujo (cómo funciona):**
1. En `Login.jsx` el admin envía usuario/clave → `POST /api/auth/login`.
2. `AuthController` valida con Spring Security; si es correcto, **`JwtService`** firma un **access token** (HS256) y **`RefreshTokenService`** crea un **refresh token** guardado en la BD.
3. El front guarda ambos en `localStorage` (`client.js`) y en **cada petición** manda la cabecera `Authorization: Bearer <token>`.
4. En el backend, **`JwtAuthenticationFilter`** intercepta cada request, valida el token y marca al usuario como autenticado. **`SecurityConfig`** dice qué rutas son públicas y cuáles exigen rol `ADMIN` (`/api/admin/**`).
5. Cuando el access token vence, el backend responde **401**; `client.js` llama a `POST /api/auth/refresh` (que **rota** el refresh en la BD), guarda el nuevo token y **reintenta** solo, sin sacar al usuario.
6. `SessionTimer.jsx` lee la fecha de expiración dentro del token y muestra la cuenta atrás.

**Carpetas y archivos:**
- Backend `src/main/java/com/gamerstore/app/config/security/`: `JwtService`, `JwtAuthenticationFilter`, `SecurityConfig`, `CustomUserDetailsService`, `RefreshTokenService`.
- `controller/AuthController.java` — endpoints `login`, `refresh`, `logout`, `me`.
- `model/RefreshToken.java` + `repository/RefreshTokenRepository.java`.
- Frontend: `frontend/src/api/client.js` (Bearer + refresh silencioso), `auth/AuthContext.jsx`, `auth/ProtectedRoute.jsx`, `components/admin/SessionTimer.jsx`, `pages/admin/Login.jsx`.

**Qué mostrar en el código:**
- `SecurityConfig.java`: la lista `authorizeHttpRequests(...)` con `permitAll()` vs `hasRole("ADMIN")`.
- `client.js`: el bloque donde, ante un 401, llama a `refresh` y reintenta.

**Demo en vivo:** en el navegador, DevTools → Application → Local Storage: muestra `gs_token`/`gs_refresh`. Entra al panel; espera al minuto final y verás el contador en ámbar y cómo NO te saca.

**Preguntas típicas + respuesta:**
- *¿Qué es un JWT?* → Un token firmado que lleva el usuario y rol; el servidor verifica la firma, no necesita guardar sesión (stateless).
- *¿Por qué access corto + refresh largo?* → Si roban el access, expira en 5 min; el refresh se puede **revocar** en la BD (logout real).
- *¿Dónde se protege el panel?* → En `SecurityConfig`: `/api/admin/**` exige `ROLE_ADMIN`. Sin token válido, responde 401.

---

## PARTE 3 — Tienda pública (catálogo + WhatsApp)

**¿Qué hace?**
Es la web que ve el **cliente**: página de inicio, **catálogo** con búsqueda y filtro por categoría, **detalle** de producto y **contacto**. La compra se cotiza por **WhatsApp** (link `wa.me`).

**Flujo (cómo funciona):**
1. `Catalogo.jsx` pide `GET /api/productos?categoria=&q=` (endpoint **público**, sin token).
2. `ProductoController` → `ProductoService.filtrar(...)` → `ProductoRepository` (consultas por nombre/categoría) → BD.
3. El backend devuelve una lista de **`ProductoDTO`** (armado por `ProductoMapper`); React pinta las tarjetas (`ProductCard.jsx`).
4. En el detalle, el botón **“Cotizar por WhatsApp”** arma el link con `waUrl()` de `utils/format.js` y el número que viene de `GET /api/config`.

**Carpetas y archivos:**
- Frontend: `frontend/src/pages/public/` (`Home`, `Catalogo`, `ProductoDetalle`, `Contacto`), `components/public/` (`Navbar`, `Footer`, `ProductCard`), `utils/format.js`.
- Backend: `controller/ProductoController.java`, `CategoriaController.java`, `ConfigController.java`; `service/ProductoService.java`.

**Qué mostrar en el código:**
- `Catalogo.jsx`: el `useEffect` que recarga productos cuando cambian los filtros.
- `ProductoService.filtrar(...)`: el `if` que elige la consulta según haya categoría y/o texto.

**Demo en vivo:** abre `http://localhost:5173/productos`, filtra por “Procesadores”, busca “RTX”, entra a un producto y toca “Cotizar por WhatsApp”.

**Preguntas típicas + respuesta:**
- *¿Por qué el catálogo no pide token?* → Es información pública; en `SecurityConfig` los `GET /api/productos/**` son `permitAll()`.
- *¿Qué es un DTO y por qué no devuelves la entidad?* → El DTO expone solo los campos necesarios para la pantalla; evita mandar datos internos y desacopla la BD de la API.
- *¿Dónde se hace el filtro?* → En `ProductoService`, usando métodos del repositorio (Spring Data crea la consulta por el nombre del método).

---

## PARTE 4 — Admin: Productos, Categorías y subida de imágenes

**¿Qué hace?**
El **CRUD** del catálogo dentro del panel: crear/editar/eliminar **productos** y **categorías**, y **subir la imagen** del producto (se guarda **en el proyecto**, no en la BD).

**Flujo (cómo funciona):**
1. En `AdminProductos.jsx` se abre el formulario. Al elegir un archivo → `POST /api/admin/uploads` (multipart).
2. `UploadController` valida que sea imagen, la guarda con un nombre único en la carpeta `uploads/productos/` del proyecto y devuelve la **ruta** `/images/productos/xxx.jpg`.
3. Al guardar el producto → `POST/PUT /api/admin/productos`; `ProductoService` valida que el **nombre no esté repetido** y guarda (solo se guarda la **ruta** de la imagen, nunca el archivo).
4. Las imágenes se sirven en `/images/**` gracias a un *resource handler* en `WebConfig` (y Vite las *proxya* en desarrollo).

**Carpetas y archivos:**
- Frontend: `frontend/src/pages/admin/AdminProductos.jsx`, `AdminCategorias.jsx`.
- Backend: `controller/AdminProductoController.java`, `AdminCategoriaController.java`, `UploadController.java`; `service/ProductoService.java`, `CategoriaService.java`; `config/WebConfig.java`.
- Imágenes físicas: carpeta `uploads/productos/` (raíz del proyecto).

**Qué mostrar en el código:**
- `UploadController.java`: cómo arma el nombre con `UUID`, valida el tipo y usa `Files.copy(...)`.
- `ProductoService.crear(...)`: el chequeo `existsByNombreIgnoreCase` que lanza el 409 de duplicado.

**Demo en vivo:** entra a Productos → Nuevo producto → sube una imagen (aparece la vista previa) → guarda; muestra el archivo nuevo dentro de `uploads/productos/`.

**Preguntas típicas + respuesta:**
- *¿Por qué la imagen no va en la BD?* → Guardar binarios en la BD la hace pesada y lenta; guardamos el **archivo en el proyecto** y en la BD solo la **ruta**.
- *¿Cómo evitas que suban un archivo raro?* → `UploadController` valida que el `content-type` empiece por `image/` y limita el tamaño (5 MB); el nombre lo genera el servidor (UUID), no el usuario.
- *¿Qué diferencia hay entre categoría y producto?* → Un producto pertenece a una categoría; por eso al borrar una categoría con productos, el service lo bloquea.

---

## PARTE 5 — Clientes (RENIEC), Pedidos y Reporte PDF

**¿Qué hace?**
Gestiona **clientes** (con datos reales traídos por **DNI desde RENIEC**), registra **pedidos/ventas** (estilo POS) y genera un **reporte PDF** de pedidos con filtros.

**Flujo (cómo funciona):**
1. **Cliente:** en `AdminClientes.jsx` escribes el DNI y pulsas **“Buscar”** → `GET /api/admin/clientes/reniec/{dni}` → `ReniecService` llama a **apiperu.dev** con el token y devuelve nombres/apellidos reales → se autocompletan. Al guardar, `ClienteService` valida DNI/email únicos.
2. **Pedido:** eliges cliente y agregas productos con cantidad → `POST /api/admin/pedidos`; `PedidoService` arma las líneas (`PedidoItem`), **calcula el total** y guarda todo junto (cascade).
3. **Reporte:** botón **“Descargar PDF”** → `GET /api/admin/pedidos/reporte.pdf?desde=&hasta=&estado=`; `PedidoReporteService` construye el PDF con **OpenPDF** (encabezado, tabla y totales) y el navegador lo descarga (`downloadBlob` en `client.js`).

**Carpetas y archivos:**
- Frontend: `frontend/src/pages/admin/AdminClientes.jsx`, `AdminPedidos.jsx`.
- Backend: `controller/AdminClienteController.java` (endpoint `/reniec/{dni}`), `AdminPedidoController.java` (endpoint `/reporte.pdf`); `service/ClienteService.java`, `PedidoService.java`, `ReniecService.java`, `PedidoReporteService.java`; `model/Pedido.java`, `PedidoItem.java`.
- Config del token: `application.properties` (`app.apidevperu.*`).

**Qué mostrar en el código:**
- `ReniecService.consultarDni(...)`: el llamado con `RestClient`, los **timeouts** y el `try/catch` best-effort (si la API falla, no rompe nada).
- `PedidoService.crear(...)`: el bucle que suma `subtotal` para el total.
- `PedidoReporteService.generar(...)`: cómo se arma la tabla del PDF.

**Demo en vivo:** Clientes → Nuevo → escribe `70123456` → “Buscar” (se llenan los nombres reales). Pedidos → registra una venta → “Descargar PDF”.

**Preguntas típicas + respuesta:**
- *¿De dónde salen los nombres reales?* → De la API **apiperu.dev** (RENIEC) consultando por DNI; se llama desde `ReniecService`.
- *¿Qué pasa si esa API se cae?* → Está en *best-effort* con timeouts: si falla, el flujo continúa (en el seeder usa nombres de respaldo; en el form muestra un aviso).
- *¿Cómo se calcula el total del pedido?* → En `PedidoService`: por cada ítem `precio × cantidad` y se suman.
- *¿Cómo generas el PDF?* → Con la librería **OpenPDF** en `PedidoReporteService`; el endpoint devuelve el archivo con `Content-Disposition: attachment`.

---

## PARTE 6 — Validaciones (únicos + toasts), Usuarios y Dashboard

**¿Qué hace?**
Tres cosas que dan “terminado” al sistema: **evitar datos duplicados** avisando con un **toast**, el **módulo de Usuarios** del sistema, y el **Dashboard** con indicadores y “más vendidos”.

**Flujo (cómo funciona):**
1. **Únicos + toast:** al crear algo repetido (categoría, DNI, email, nombre de producto, usuario), el `Service` lanza un error → **`GlobalExceptionHandler`** lo convierte en un JSON `{ "error": "..." }` con código 409/400 → el front muestra `toast.error(mensaje)` (sistema `Toast.jsx`).
2. **Usuarios:** `AdminUsuarios.jsx` hace CRUD contra `/api/admin/usuarios`; `UsuarioService` valida username/email únicos, **hashea la contraseña con BCrypt** y **bloquea borrar al último admin**.
3. **Dashboard:** `Dashboard.jsx` pide `GET /api/admin/dashboard`; `AdminDashboardController` junta los KPIs (totales, ventas, stock bajo) y el **top de productos** (consulta `topProductos`) y se dibujan con **Recharts**.

**Carpetas y archivos:**
- Backend: `controller/GlobalExceptionHandler.java`, `AdminUsuarioController.java`, `AdminDashboardController.java`; `service/UsuarioService.java` (y las validaciones `existsBy...` en `ProductoService`, `ClienteService`, `CategoriaService`).
- Frontend: `frontend/src/pages/admin/AdminUsuarios.jsx`, `Dashboard.jsx`, `components/ui/Toast.jsx`.

**Qué mostrar en el código:**
- `GlobalExceptionHandler.java`: cada `@ExceptionHandler` y a qué status HTTP mapea.
- `UsuarioService.eliminar(...)`: el `if` que impide borrar el último `ADMIN`.
- `Dashboard.jsx`: cómo consume `data.topProductos` para el panel “Más vendidos”.

**Demo en vivo:** intenta crear una categoría que ya existe → sale toast rojo “La categoría ya existe”. Muestra el Dashboard con los gráficos y “Más vendidos”.

**Preguntas típicas + respuesta:**
- *¿Dónde y cómo evitas duplicados?* → En los `Service` con métodos `existsBy...` del repositorio; si existe, lanzo un error que el `GlobalExceptionHandler` transforma en 409 con mensaje.
- *¿La contraseña se guarda en texto plano?* → No, se **hashea con BCrypt** antes de guardar (`passwordEncoder.encode(...)`).
- *¿De dónde salen los “más vendidos”?* → De una consulta que agrupa las líneas de pedido por producto y suma cantidades (`PedidoRepository.topProductos`).

---

## Apéndice — Cómo levantar el proyecto (para el que hace la demo)

1. **XAMPP:** iniciar **MySQL** (y que exista la BD `tienda_pc`; si no, se crea sola al arrancar).
2. **Backend:** en la raíz `gamerstore-main/` ejecutar `./mvnw spring-boot:run` (o `mvnw.cmd spring-boot:run`). Levanta en `:8080` y siembra los datos.
3. **Frontend:** en `frontend/` ejecutar `npm run dev`. Abre `http://localhost:5173`.
4. **Login del panel:** usuario `admin123`, contraseña `gamerstore123`.

**Estructura resumida:**
```
gamerstore-main/
├─ src/main/java/com/gamerstore/app/
│  ├─ model/        (entidades = tablas)
│  ├─ repository/   (acceso a datos, Spring Data JPA)
│  ├─ service/      (reglas de negocio, validaciones)
│  ├─ controller/   (endpoints /api/...)
│  ├─ dto/  mapper/ (objetos de transporte + conversión)
│  └─ config/       (seeder, seguridad JWT, web)
├─ src/main/resources/application.properties
├─ uploads/productos/     (imágenes guardadas en el proyecto)
└─ frontend/src/
   ├─ pages/public/   pages/admin/   (pantallas)
   ├─ components/      (piezas reutilizables: navbar, tabla, toast…)
   ├─ api/             (client.js = fetch + token; endpoints.js)
   ├─ auth/            (contexto de sesión + ruta protegida)
   └─ utils/  hooks/   config/
```
