# GamerStore ERP — Guía de exposición (6 partes)

> Esta guía cubre **solo el panel de administración (ERP)** — que es lo que se presenta. La tienda pública no se incluye.

Divide el ERP en **6 responsabilidades**. Cada integrante domina UNA parte y puede responder:
**¿qué hace? · ¿cómo fluye (paso a paso)? · ¿en qué carpetas/archivos está? · ¿cómo lo demuestro en vivo?** + preguntas típicas del profesor.

## Cómo está armado (lo que todos deben saber en 30 seg)

- El sistema es el **panel de administración (ERP)**. Dos servidores en desarrollo:
  - **Backend (Spring Boot):** API REST en `http://localhost:8080`, rutas `/api/**`.
  - **Frontend (React + Vite):** panel en `http://localhost:5173/admin`.
- **Base de datos:** MySQL (XAMPP), BD `tienda_pc`. Hibernate crea las tablas desde las clases `@Entity`.
- **Capas del backend:**

```
Pantalla admin (React) ─▶ Controller (/api) ─▶ Service (reglas) ─▶ Repository ─▶ Base de datos
       DTO ◀──────────── Mapper (Entidad→DTO) ◀────────────────────────────────────┘
```
- **Login:** `admin123` / `gamerstore123`.

### Reparto

| Parte | Módulo del ERP | Responsable | En una frase |
|---|---|---|---|
| 1 | Arquitectura y modelo de datos | | Las capas del sistema y las entidades que se vuelven tablas |
| 2 | Datos reales + imágenes | | El seeder que llena la BD y las imágenes guardadas en el proyecto |
| 3 | Seguridad: login JWT + sesión | | Login con token, protección del panel, refresco y contador |
| 4 | Productos y Categorías | | ABM del catálogo con subida de imágenes |
| 5 | Clientes (RENIEC), Pedidos, PDF | | DNI real, registrar ventas y descargar el reporte |
| 6 | Dashboard, Usuarios, validaciones | | Estadísticas, gestión de usuarios y anti-duplicados |

> **Truco para el Q&A:** todos los módulos siguen el MISMO flujo (**pantalla → Controller → Service → Repository → base de datos** y de vuelta). Si te preguntan algo de otra parte, explica ese flujo y di en qué archivo vive.

---

## PARTE 1 — Arquitectura y modelo de datos

**¿Qué hace?** Define la **estructura por capas** del backend, la **conexión a la base de datos** y las **entidades** (clases Java que Hibernate convierte en tablas) con sus **repositorios**.

**Flujo:**
1. `application.properties` define la conexión a MySQL (`tienda_pc`) y que Hibernate administre el esquema.
2. Al arrancar, Hibernate lee las clases `@Entity` de `model/` y **crea/actualiza las tablas** (no escribimos `CREATE TABLE`).
3. Los **repositorios** (Spring Data JPA en `repository/`) generan las consultas por el nombre del método (ej. `existsByDni`).
4. Todo usa el esqueleto **Controller → Service → Repository → BD**, y de regreso el **Mapper** convierte la entidad a un **DTO** para enviarlo como JSON.

**Carpetas y archivos:** `pom.xml` · `src/main/resources/application.properties` · `…/model/` (entidades) · `…/repository/` · `…/GamerStoreApplication.java`.

**Qué mostrar:** `model/Producto.java` (`@Entity`, `@Id`, `@Column(unique=true)`, `@ManyToOne`) · un repositorio con métodos derivados.

**Demo:** phpMyAdmin → BD `tienda_pc` → mostrar las tablas creadas desde las entidades.

**Preguntas típicas:**
- *¿Quién crea las tablas?* → Hibernate, desde las anotaciones `@Entity/@Column`.
- *¿Qué es un repositorio de Spring Data?* → Una interfaz con métodos (`findByDni`) cuya consulta genera Spring por el nombre.
- *¿Por qué capas?* → Cada una con una responsabilidad; más ordenado y mantenible.
- *¿@ManyToOne?* → Muchos productos → una categoría; en la tabla va `categoria_id` (FK).

---

## PARTE 2 — Datos reales (seeder) + imágenes en el proyecto

**¿Qué hace?** Llena la BD al arrancar con **datos reales** (10 categorías, 28 productos en S/, ~40 pedidos históricos, clientes con nombres reales de RENIEC) y guarda las **imágenes en el proyecto** (no en la BD).

**Flujo:**
1. `DataSeeder` implementa `CommandLineRunner` → corre 1 vez al arrancar.
2. Es **idempotente**: cada bloque siembra solo si la tabla está vacía (`count()==0`).
3. Inserta categorías, productos (con la **ruta** de su imagen y precio en S/), clientes y ~40 pedidos con **fechas repartidas en 6 meses**.
4. Las imágenes viven en `uploads/productos/` y se sirven en `/images/**` gracias a `WebConfig` (Vite las reenvía al backend en dev).

**Carpetas y archivos:** `…/config/DataSeeder.java` · `…/config/WebConfig.java` · `uploads/productos/` · `application.properties` (multipart/uploads).

**Qué mostrar:** `DataSeeder.java` (el `if (count()==0)` + lista de productos) · `WebConfig.java` (resource handler `/images/productos/**`).

**Demo:** carpeta `uploads/productos/`; abrir `/images/productos/rtx-4070.jpg`; en Pedidos, señalar fechas de distintos meses.

**Preguntas típicas:**
- *¿Por qué las imágenes no van en la BD?* → Pesa y ralentiza; guardamos el archivo en el proyecto y en la BD solo la ruta.
- *¿Por qué no se duplican los datos al reiniciar?* → El seeder solo siembra si la tabla está vacía.
- *¿Cómo hay pedidos de meses pasados?* → El seeder les pone una fecha en el pasado (para que el dashboard tenga historia).
- *¿Los nombres de clientes son reales?* → Sí, por DNI desde RENIEC (apiperu.dev) — detalle en Parte 5.

---

## PARTE 3 — Seguridad: login JWT + refresh + sesión

**¿Qué hace?** Autenticación **real**: el login entrega un **token JWT** exigido en todo el panel. El access dura 5 min y se **renueva solo** con un *refresh token*; hay un **contador de sesión**.

**Flujo:**
1. `Login.jsx` envía usuario/clave → `POST /api/auth/login`.
2. `AuthController` valida con Spring Security; `JwtService` firma el **access** y `RefreshTokenService` crea el **refresh** en la BD.
3. El front guarda ambos en `localStorage` y manda `Authorization: Bearer <token>` en cada petición.
4. `JwtAuthenticationFilter` valida el token; `SecurityConfig` exige rol ADMIN en `/api/admin/**`.
5. Si el access vence → **401**; el front llama a `/api/auth/refresh` (que **rota** el refresh) y **reintenta solo**.
6. `SessionTimer.jsx` muestra la cuenta atrás leyendo el `exp` del token.

**Carpetas y archivos:** `…/config/security/` (JwtService, JwtAuthenticationFilter, SecurityConfig, CustomUserDetailsService, RefreshTokenService) · `…/controller/AuthController.java` · `model/RefreshToken.java` + repo · `frontend/src/api/client.js` · `auth/` · `components/admin/SessionTimer.jsx` · `pages/admin/Login.jsx`.

**Qué mostrar:** `SecurityConfig` (`permitAll()` vs `hasRole("ADMIN")`) · `client.js` (401 → refresh → reintento).

**Demo:** entrar a `/admin` sin login (redirige); iniciar sesión; DevTools → Local Storage (`gs_token`/`gs_refresh`); el contador en ámbar el último minuto sin sacar al usuario.

**Preguntas típicas:**
- *¿Qué es un JWT?* → Token firmado con usuario+rol; el servidor verifica la firma (stateless).
- *¿Por qué access corto + refresh largo?* → Access robado expira en 5 min; el refresh se puede revocar (logout real).
- *¿Dónde se protege el panel?* → `SecurityConfig`: `/api/admin/**` exige ROLE_ADMIN.
- *¿La contraseña en texto plano?* → No, BCrypt.

---

## PARTE 4 — Productos y Categorías (catálogo del admin)

**¿Qué hace?** El **CRUD** de **productos** y **categorías**, con **subida de imagen** al proyecto y **validación de nombres únicos**.

**Flujo:**
1. Al abrir, `AdminProductos.jsx` pide la lista → `GET /api/admin/productos` → Controller → Service → Repository → tabla `producto` → tabla en pantalla.
2. Al subir imagen → `POST /api/admin/uploads`; `UploadController` la guarda en `uploads/productos/` y devuelve la **ruta**.
3. Al guardar → `POST/PUT /api/admin/productos`; `ProductoService` valida **nombre no repetido** y guarda (solo la ruta de la imagen).
4. Categorías es igual + una regla: no se borra una categoría que **tiene productos**.

**Carpetas y archivos:** `pages/admin/AdminProductos.jsx`, `AdminCategorias.jsx` · `…/controller/` (AdminProductoController, AdminCategoriaController, UploadController) · `…/service/` (ProductoService, CategoriaService).

**Qué mostrar:** `ProductoService.crear(...)` (`existsByNombreIgnoreCase` → 409) · `UploadController.subir(...)` (UUID + `Files.copy`) · `CategoriaService.eliminar(...)` (bloqueo si tiene productos).

**Demo:** Nuevo producto → subir imagen (preview) → guardar; crear otro con el mismo nombre (toast rojo); borrar una categoría con productos (se bloquea).

**Preguntas típicas:**
- *¿Cómo evitas nombres repetidos?* → `existsByNombreIgnoreCase` en ProductoService → 409 → toast.
- *¿Cómo evitas un archivo raro?* → content-type `image/` + límite 5 MB; nombre por UUID.
- *¿Borrar categoría con productos?* → Se bloquea (`existsByCategoriaId`).
- *¿Buscar/paginar llama al servidor?* → No, lo hace `useTableControls` en el navegador.

---

## PARTE 5 — Clientes (RENIEC), Pedidos y Reporte PDF

**¿Qué hace?** Gestiona **clientes** (datos reales por **DNI desde RENIEC**), registra **pedidos/ventas** y genera un **reporte PDF** con filtros.

**Flujo:**
1. **Cliente:** escribes el DNI y pulsas **“Buscar”** → `GET /api/admin/clientes/reniec/{dni}` → `ReniecService` llama a **apiperu.dev** con el token y autocompleta. Al guardar, `ClienteService` valida DNI/email únicos.
2. **Pedido:** eliges cliente y agregas productos → `POST /api/admin/pedidos`; `PedidoService` arma las líneas, **calcula el total** y guarda (cascade).
3. **Reporte:** **“Descargar PDF”** → `GET /api/admin/pedidos/reporte.pdf`; `PedidoReporteService` arma el PDF con **OpenPDF** y el navegador lo descarga.

**Carpetas y archivos:** `pages/admin/AdminClientes.jsx`, `AdminPedidos.jsx` · `…/controller/` (AdminClienteController `/reniec/{dni}`, AdminPedidoController `/reporte.pdf`) · `…/service/` (ClienteService, PedidoService, ReniecService, PedidoReporteService) · `application.properties` (`app.apidevperu.*`).

**Qué mostrar:** `ReniecService.consultarDni(...)` (RestClient + timeouts + best-effort) · `PedidoService.crear(...)` (suma de subtotales) · `PedidoReporteService.generar(...)` (tabla del PDF).

**Demo:** Clientes → Nuevo → `70123456` → “Buscar” (nombres reales). Pedidos → registrar venta → “Descargar PDF”.

**Preguntas típicas:**
- *¿De dónde salen los nombres reales?* → apiperu.dev (RENIEC) por DNI, desde ReniecService.
- *¿Y si la API se cae?* → Best-effort con timeouts; el flujo continúa (respaldo en el seeder, aviso en el form).
- *¿Cómo se calcula el total?* → `precio × cantidad` por ítem, sumados.
- *¿Cómo generas el PDF?* → OpenPDF en PedidoReporteService; el endpoint lo devuelve como descarga.

---

## PARTE 6 — Dashboard, Usuarios y validaciones

**¿Qué hace?** El **Dashboard** con indicadores y “más vendidos”, la gestión de **Usuarios** del sistema, y el **manejo de errores** que avisa con **toast** los duplicados.

**Flujo:**
1. **Dashboard:** `Dashboard.jsx` pide `GET /api/admin/dashboard`; el Controller junta KPIs (totales, ventas, stock bajo) y el **top de productos**, y se dibuja con **Recharts**.
2. **Usuarios:** `AdminUsuarios.jsx` hace CRUD contra `/api/admin/usuarios`; `UsuarioService` valida username/email únicos, **hashea con BCrypt** y **bloquea borrar al último admin**.
3. **Validaciones + toast:** al crear un duplicado, el Service lanza un error → `GlobalExceptionHandler` lo convierte en `{ "error": "..." }` → el front muestra `toast.error(mensaje)`.

**Carpetas y archivos:** `…/controller/` (AdminDashboardController, AdminUsuarioController, GlobalExceptionHandler) · `…/service/UsuarioService.java` (+ las validaciones `existsBy...` de las otras entidades) · `pages/admin/Dashboard.jsx`, `AdminUsuarios.jsx` · `components/ui/Toast.jsx`.

**Qué mostrar:** `GlobalExceptionHandler.java` (cada `@ExceptionHandler` y su status) · `UsuarioService.eliminar(...)` (bloqueo del último ADMIN) · `Dashboard.jsx` (`data.topProductos`).

**Demo:** crear una categoría/usuario que ya existe → toast rojo. Mostrar el Dashboard con gráficos y “Más vendidos”.

**Preguntas típicas:**
- *¿Dónde evitas duplicados?* → En los Service con `existsBy...` → 409 → toast.
- *¿Contraseña en texto plano?* → No, BCrypt.
- *¿De dónde salen los “más vendidos”?* → `PedidoRepository.topProductos` (agrupa líneas por producto y suma).
- *¿Por qué no dejar borrar el último admin?* → Para no perder el acceso (`countByRol(ADMIN)`).

---

## Apéndice — Cómo levantar el ERP (para la demo)

1. **XAMPP:** iniciar **MySQL** (si no existe `tienda_pc`, se crea sola).
2. **Backend:** en la raíz `mvnw.cmd spring-boot:run` → `:8080` + siembra datos.
3. **Frontend:** en `frontend/` `npm run dev` → `http://localhost:5173/admin`.
4. **Login:** `admin123` / `gamerstore123`.

**Estructura del backend (capas):**
```
src/main/java/com/gamerstore/app/
├─ model/        (entidades = tablas)
├─ repository/   (acceso a datos, Spring Data JPA)
├─ service/      (reglas de negocio, validaciones)
├─ controller/   (endpoints /api/...)
├─ dto/  mapper/ (objetos de transporte + conversión)
└─ config/       (seeder, seguridad JWT, web)
```

> **Recuerda:** el flujo es siempre **pantalla → Controller → Service → Repository → base de datos**. Si dominas eso, puedes explicar cualquier módulo del ERP.
