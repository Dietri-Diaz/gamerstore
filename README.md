# 🎮 GamerStore

**Tienda online de tecnología con panel administrativo (ERP).** Los clientes compran en la tienda pública
(catálogo, carrito, pago con tarjeta o Yape, boleta y seguimiento de su pedido) y el equipo gestiona todo desde
un panel protegido (productos, pedidos, ventas, clientes y usuarios).

Proyecto académico — **Marcos de Desarrollo Web (100000SI57)**, Universidad Tecnológica del Perú (UTP).

---

## ✨ Características

**Tienda pública (sin login)**
- Catálogo con búsqueda y filtro por categoría, ficha de producto y carrito.
- Checkout guiado en pasos: identificación, datos, **entrega (recojo en tienda o delivery)**, pago y confirmación.
- **Pago real con tarjeta** vía Stripe (modo prueba) y **Yape** (QR + número de operación).
- **Boleta electrónica** descargable en PDF (serie + correlativo, IGV 18 %, importe en letras y QR).
- **Seguimiento del pedido** con código + DNI: línea de tiempo del estado de la compra.

**Panel administrativo (con login)**
- **Seguridad con JWT**: token de acceso de 5 min, *refresh token* con rotación, contraseñas con BCrypt y
  bloqueo tras 3 intentos fallidos (30 s).
- CRUD de **productos** (con imágenes guardadas en disco y control de stock), **categorías**, **clientes**
  (autocompletado por DNI vía RENIEC) y **usuarios**.
- **Pedidos**: detalle con su pago y boleta, cambio de estado y cobro desde el ERP.
- **Anular una venta**: devuelve el dinero (reembolso real en Stripe), repone el stock y anula la boleta,
  todo en una sola transacción.
- **Registro de ventas** con totales de IGV (las boletas anuladas no suman).
- **Dashboard** con estadísticas.
- Validación de campos únicos con aviso **en vivo** de duplicados (sin recargar).

---

## 🧱 Stack tecnológico

| Capa | Tecnología |
|------|-----------|
| Backend | Java 17 · Spring Boot 3.5 · Spring Web (REST) · Spring Data JPA / Hibernate |
| Seguridad | Spring Security · JWT (jjwt) · BCrypt |
| Base de datos | MySQL |
| Pagos | Stripe (stripe-java) · Yape (QR) |
| Boleta PDF | Thymeleaf + openhtmltopdf · ZXing (QR) |
| Frontend | React 18 · Vite · React Router · Recharts |

> La API es REST (JSON). El frontend nunca toca la base de datos: siempre pide los datos al backend, que es el
> único que aplica reglas, seguridad y validaciones.

---

## 📁 Estructura del proyecto

```
gamerstore/
├── src/main/java/com/gamerstore/app/
│   ├── controller/   # reciben las peticiones HTTP (una clase por módulo)
│   ├── service/      # reglas de negocio
│   ├── repository/   # acceso a la base de datos (Spring Data JPA)
│   ├── model/        # entidades = tablas
│   ├── dto/          # objetos de entrada/salida de la API
│   ├── mapper/       # traducen Entidad ↔ DTO
│   └── config/       # seguridad (JWT) y datos iniciales (seeder)
├── src/main/resources/
│   ├── application.properties   # configuración (puerto, BD, IGV, Yape…)
│   └── templates/boleta.html    # plantilla de la boleta PDF
└── frontend/src/
    ├── pages/public/  # tienda: Catálogo, Carrito, Checkout, Seguimiento
    ├── pages/admin/   # panel: Productos, Pedidos, Ventas, Usuarios…
    ├── api/           # client.js (conexión + JWT) y endpoints.js
    └── components/    # piezas reutilizables (modal, tabla, toast…)
```

Documentación de estudio completa (flujos, reparto por módulo y ejemplos): **`docs/GamerStore-Guia-Estudio.html`**.

---

## 🚀 Cómo ejecutar

**Requisitos:** Java 17+, MySQL y Node.js (solo si vas a desarrollar el frontend).

### 1) Base de datos
Ten MySQL encendido. La base `tienda_pc` se crea sola al arrancar el backend (o puedes importar el dump si lo tienes).

### 2) Configura las claves secretas
Copia el archivo de ejemplo y complétalo con tus claves (Stripe en modo prueba y token de apiperu.dev):

```bash
cp src/main/resources/application-local.properties.ejemplo src/main/resources/application-local.properties
```

> ⚠️ `application-local.properties` **no se sube a GitHub** (está en `.gitignore`). Sin él, el pago con tarjeta
> y el autocompletado por DNI no funcionarán, pero el resto del sistema sí.

### 3) Backend
```bash
./mvnw spring-boot:run
```
Queda disponible en **http://localhost:8080** (tienda en `/`, panel en `/admin`).

### 4) Frontend (solo en desarrollo)
```bash
cd frontend
npm install
npm run dev
```
En la versión final no hace falta: al construir el frontend (`npm run build`), sus archivos quedan dentro del
backend y todo corre junto en el puerto 8080.

### Usuario de prueba del panel
```
Usuario:    admin123
Contraseña: gamerstore123
```

---

## 👥 Equipo

| Integrante | GitHub | Módulo a cargo |
|-----------|--------|----------------|
| Dietri | [@Dietri-Diaz](https://github.com/Dietri-Diaz) | Productos, Categorías, Pasarela de pago y Dashboard |
| bruno | [@BruVi13](https://github.com/BruVi13) | Seguridad y Autenticación |
| berny | [@BGV14](https://github.com/BGV14) | Clientes, RENIEC y Entrega |
| chuco | [@jorgeo26](https://github.com/jorgeo26) | Boleta electrónica y Registro de ventas |
| Belu | [@beluarrietaberrocal](https://github.com/beluarrietaberrocal) | Pedidos, Anulación y Devolución |
| rodrigo | [@rodrigoojeda381](https://github.com/rodrigoojeda381) | Tienda pública y Seguimiento |

---

## 📝 Nota

Proyecto con fines **académicos**. La boleta que genera el sistema es una **representación de demostración**:
no se transmite a SUNAT y no tiene validez tributaria (una emisión real requiere RUC y certificado digital).
Los pagos con tarjeta usan **Stripe en modo prueba** (no se mueve dinero real).
