# Documentación técnica — Frontend

**Proyecto:** GamerStore · **Stack:** React 18.3 + Vite 5.4 + React Router 6.26 + Recharts 2.12 + Stripe (`@stripe/react-stripe-js`)
**Código:** `frontend/src` · **Backend:** Spring Boot en `http://localhost:8080`

Leyenda usada en todo el documento:

- ✏️ = **parte modificable**: puedes cambiarlo sin romper nada más.
- ⚠️ = **parte delicada**: si lo tocas, revisa qué más depende de ello.

---

## 1. Cómo está organizado

### 1.1 Estructura de carpetas

Todo el frontend vive dentro de `frontend/`. El código fuente está en `frontend/src`, repartido en carpetas por **responsabilidad** (no por página):

| Carpeta | Qué contiene | Cuándo tocarla |
|---|---|---|
| `src/api/` | `client.js` (fetch con JWT) y `endpoints.js` (todas las URLs del backend) | Cuando agregas/cambias un endpoint del backend |
| `src/auth/` | `AuthContext.jsx` (sesión) y `ProtectedRoute.jsx` (guardia de rutas) | Cuando cambias cómo se inicia/cierra sesión |
| `src/carrito/` | `CarritoContext.jsx` — el carrito de compras completo | Cuando cambias reglas del carrito (stock, persistencia) |
| `src/config/` | `ConfigContext.jsx` — config pública de la tienda (WhatsApp, Yape, Stripe) | Cuando agregas un dato de configuración global |
| `src/hooks/` | `useTableControls`, `useAutoClear`, `useDuplicado` | Cuando cambias comportamiento compartido (tiempos, paginación) |
| `src/utils/` | `format.js` — `money()`, `waUrl()`, `sku()` | Cuando cambias cómo se formatea un precio o un SKU |
| `src/components/ui/` | 9 componentes genéricos: Modal, Toast, Confirm, tablas, skeletons | Cuando quieres que un patrón visual se vea igual en toda la app |
| `src/components/public/` | Navbar, Footer, layout y tarjetas de la **tienda** | Cuando cambias la cabecera/pie/tarjeta de producto |
| `src/components/admin/` | Sidebar, Topbar, layout, SessionTimer y PasarelaPago del **panel** | Cuando cambias el menú, la barra superior o el cobro |
| `src/pages/public/` | 6 páginas de la tienda (Home, Catálogo, Detalle, Contacto, Carrito, Checkout) | Cuando cambias el contenido de una pantalla pública |
| `src/pages/admin/` | 9 páginas del panel (Login, Dashboard y 7 pantallas de gestión) | Cuando cambias una pantalla del panel |
| `src/index.css` | **Todo** el CSS de la aplicación (751 líneas, un solo archivo) | Cuando cambias colores, espaciados o agregas una clase |

Además, fuera de `src/`:

| Archivo | Qué hace |
|---|---|
| `frontend/index.html` | HTML base: carga la fuente **Inter** y los iconos **Bootstrap Icons** desde CDN, y monta el `<div id="root">` |
| `frontend/vite.config.js` | Puerto de desarrollo, proxy hacia el backend y carpeta de salida del build |
| `frontend/package.json` | Dependencias y los 3 scripts: `dev`, `build`, `preview` |

### 1.2 Las dos zonas de la aplicación

La app es **una sola SPA** (Single Page Application) con dos zonas muy distintas, definidas en `src/App.jsx`:

| Zona | Rutas | Layout | ¿Necesita login? |
|---|---|---|---|
| **Tienda pública** | `/`, `/productos`, `/productos/:id`, `/contacto`, `/carrito`, `/checkout` | `PublicLayout` (Navbar + Footer) | No |
| **Panel admin** | `/admin`, `/admin/productos`, `/admin/categorias`, `/admin/clientes`, `/admin/pedidos`, `/admin/pagos`, `/admin/ventas`, `/admin/usuarios` | `AdminLayout` (Sidebar + Topbar) | **Sí** (`ProtectedRoute`) |
| **Login** | `/admin/login` | ninguno (pantalla propia) | No |

Cualquier ruta que no exista redirige al inicio:

```jsx
<Route path="*" element={<Navigate to="/" replace />} />
```

⚠️ El anidado de rutas importa: `ProtectedRoute` envuelve a `AdminLayout`, que a su vez envuelve las páginas. Si sacas una página de ahí, deja de estar protegida.

### 1.3 Cómo arranca la app (`main.jsx`)

`main.jsx` monta `<App />` dentro de una **cadena de providers**. El orden no es casual: cada provider expone un contexto que los de adentro pueden consumir.

```jsx
<BrowserRouter>          {/* habilita las rutas */}
  <ConfigProvider>       {/* config de la tienda */}
    <AuthProvider>       {/* sesión del admin */}
      <ToastProvider>    {/* notificaciones */}
        <ConfirmProvider>{/* diálogos de confirmación */}
          <CarritoProvider><App /></CarritoProvider>
```

⚠️ Si mueves un provider hacia adentro de otro que lo necesita, obtendrás errores del tipo "no se puede leer de null" al llamar a `useAuth()` o `useToast()`.

### 1.4 Cómo Vite habla con el backend (`vite.config.js`)

En desarrollo corren **dos servidores a la vez**: Vite en el `:5173` y Spring Boot en el `:8080`. Para que el navegador no choque con CORS, Vite hace de **proxy**: recibe las peticiones a `/api` y `/images` y se las reenvía al backend.

```js
server: {
  port: 5173,
  proxy: {
    '/api': 'http://localhost:8080',
    '/images': 'http://localhost:8080',
  },
},
build: {
  outDir: '../src/main/resources/static',   // la SPA se compila DENTRO del proyecto Spring
  emptyOutDir: true,
}
```

Por eso en `client.js` la base es simplemente `const BASE = '/api'` (una ruta relativa): en desarrollo la resuelve el proxy y en producción la resuelve el propio Spring Boot, porque el build deja los archivos en `src/main/resources/static` y todo se sirve desde el mismo JAR.

- ✏️ Cambiar el puerto del front → `server.port`.
- ✏️ Cambiar el puerto del backend → las dos URLs del bloque `proxy`.
- ⚠️ No cambies `outDir` sin avisar al backend: Spring espera la SPA exactamente en esa carpeta.

---

## 2. La capa de API (lo más importante de entender)

Toda comunicación con el backend pasa por **dos archivos**. Ninguna página hace `fetch` por su cuenta (salvo la subida de imágenes, que se explica más abajo). Esto es lo que hace que el proyecto sea mantenible: si cambia la autenticación, se cambia en **un** lugar.

### 2.1 `api/client.js` — el fetch centralizado

Este archivo resuelve cuatro problemas de una vez: guardar la sesión, mandar el token, renovarlo cuando vence y normalizar los errores.

**a) La sesión vive en `localStorage`** bajo tres claves:

| Constante | Clave real | Qué guarda |
|---|---|---|
| `USER_KEY` | `gs_user` | El usuario (JSON: `username`, `nombre`, `rol`) |
| `TOKEN_KEY` | `gs_token` | El **access token** (JWT de vida corta) |
| `REFRESH_KEY` | `gs_refresh` | El **refresh token** (vida larga) |

Funciones exportadas: `getUser()`, `getToken()`, `getRefreshToken()`, `saveSession({accessToken, refreshToken, user})`, `saveUser(user)` y `clearSession()`.

**b) Agregar el token:** `rawRequest()` arma el `fetch` y añade la cabecera solo si hay token:

```js
const headers = {}
if (body !== undefined) headers['Content-Type'] = 'application/json'
if (token) headers['Authorization'] = 'Bearer ' + token
```

**c) El refresco silencioso (lo más interesante del archivo).** El access token dura poco a propósito. Cuando vence, el backend responde **401**. En vez de expulsar al usuario, `request()` hace esto:

1. Lanza la petición con el token actual.
2. Si vuelve **401** (y no era la propia petición de login/refresh, y sí hay refresh token):
3. Llama a `refreshAccessToken()` → `POST /api/auth/refresh` con el refresh token.
4. Si el backend devuelve tokens nuevos, los guarda y **reintenta la misma petición**. El usuario no se entera de nada.
5. Si el refresh también falla → `clearSession()` + redirección a `/admin/login`.

```js
if (res.status === 401 && !noRefresh && getRefreshToken()) {
  const nuevo = await refreshAccessToken()
  if (nuevo) res = await rawRequest(path, { method, body, token: nuevo })
  else { clearSession(); redirigirLogin(); throw new Error('Sesión expirada') }
}
```

⚠️ **Single-flight**: si cinco peticiones reciben 401 al mismo tiempo, no se piden cinco tokens nuevos. La variable `refreshPromise` guarda la promesa en curso y todas comparten la misma; se libera en el `.finally()`. Si tocas esto, ten cuidado de no quitar ese mecanismo.

**d) Manejo de errores.** Cuando la respuesta no es `ok`, se lanza un `Error` **enriquecido** con dos campos extra:

```js
const err = new Error(msg)
err.status = res.status   // código HTTP
err.data = data           // cuerpo JSON del backend
throw err
```

Esto permite que una pantalla reaccione a un caso concreto. El ejemplo real está en `Login.jsx`, que detecta el bloqueo por intentos fallidos:

```jsx
if (err.status === 429 && err.data?.segundosRestantes) setBloqueo(err.data.segundosRestantes)
```

Además, si la respuesta es `204 No Content` (típico de un DELETE) devuelve `null`, y si no es JSON devuelve texto.

**e) `downloadBlob(path, filename)`** — descargas de PDF autenticadas. No se puede usar un `<a href>` normal porque el PDF requiere el header `Authorization`. Entonces: hace el fetch con token (con su propio reintento tras refresh), convierte la respuesta a `blob`, crea una URL temporal, simula un click en un `<a download>` y limpia todo con `URL.revokeObjectURL`.

Se usa en: boleta pública del checkout, reporte de pedidos, comprobantes de pago y boletas del panel.

**f) El objeto `api`** que consume `endpoints.js`:

```js
export const api = {
  get, post, put, patch, del   // todos pasan por request()
}
```

### 2.2 `api/endpoints.js` — el catálogo de URLs

Agrupa **todas** las rutas del backend en cuatro objetos. Ninguna página escribe una URL a mano.

Incluye un helper `qs(params)` que convierte un objeto en query string ignorando valores vacíos/`null`/`undefined` — por eso los filtros opcionales (fechas, estado) se pueden dejar en blanco sin ensuciar la URL.

#### AuthAPI

| Función | Endpoint | Para qué |
|---|---|---|
| `login(username, password)` | `POST /auth/login` | Iniciar sesión, devuelve tokens + usuario |
| `me()` | `GET /auth/me` | Validar la sesión guardada al recargar la página |
| `logout()` | `POST /auth/logout` | Invalidar el refresh token en el servidor |

#### PublicAPI (sin login)

| Función | Endpoint | Para qué |
|---|---|---|
| `config()` | `GET /config` | Datos de la tienda: WhatsApp, Yape, clave pública de Stripe |
| `productos(categoria, q)` | `GET /productos?categoria=&q=` | Listado del catálogo con filtros |
| `producto(id)` | `GET /productos/:id` | Detalle + productos relacionados |
| `categorias()` | `GET /categorias` | Filtro lateral del catálogo |
| `reniec(dni)` | `GET /reniec/:dni` | Autocompletar nombres desde RENIEC en el checkout |
| `verificarCliente(data)` | `POST /checkout/cliente` | Identificar a un cliente ya registrado (DNI + email) |
| `checkout(data)` | `POST /checkout` | **Cerrar la compra** (cliente + items + pago) |
| `boletaUrl(pedidoCodigo, dni)` | `/checkout/boleta/:codigo?dni=` | Ruta de la boleta pública (se pasa a `downloadBlob`) |

#### AdminAPI (requiere JWT)

| Grupo | Funciones | Endpoints |
|---|---|---|
| Dashboard | `dashboard()` | `GET /admin/dashboard` |
| Productos | `productos()`, `crearProducto()`, `actualizarProducto(id)`, `eliminarProducto(id)` | `GET/POST/PUT/DELETE /admin/productos` |
| Categorías | `categorias()`, `crearCategoria()`, `actualizarCategoria(id)`, `eliminarCategoria(id)` | `.../admin/categorias` |
| Clientes | `clientes()`, `crearCliente()`, `actualizarCliente(id)`, `eliminarCliente(id)`, `buscarDni(dni)` | `.../admin/clientes`, `GET /admin/clientes/reniec/:dni` |
| Pedidos | `pedidos()`, `crearPedido()`, `actualizarPedido(id)`, `eliminarPedido(id)`, `reportePedidosUrl(params)` | `.../admin/pedidos`, `/admin/pedidos/reporte.pdf` |
| Comprobantes | `comprobantes(params)`, `resumenVentas(params)`, `boletaUrl(id)`, `boletaPedidoUrl(pedidoId)` | `/admin/comprobantes`, `/admin/comprobantes/resumen`, `.../:id/pdf`, `.../pedido/:id/pdf` |
| Usuarios | `usuarios()`, `crearUsuario()`, `actualizarUsuario(id)`, `eliminarUsuario(id)` | `.../admin/usuarios` |
| Uploads | `subirImagen(formData)` | `POST /api/admin/uploads` |
| Duplicados | `existeProducto`, `existeCategoria`, `existeCliente`, `existeUsuario` | `GET .../existe?...` |

⚠️ `subirImagen()` es la **única excepción** que no usa `api.post`: como el cuerpo es un `FormData` (multipart), no puede llevar `Content-Type: application/json`, así que hace su propio `fetch` y arma el header `Authorization` leyendo directamente `localStorage.getItem('gs_token')`. Efecto secundario: **no tiene refresco automático**; si el token venció justo en ese momento, la subida falla y hay que reintentar.

#### PagosAPI

| Función | Endpoint | Para qué |
|---|---|---|
| `listar()` | `GET /admin/pagos` | Historial de cobros |
| `config()` | `GET /admin/pagos/config` | Datos de la cuenta Yape + estado de Stripe |
| `pagarTarjeta(data)` | `POST /admin/pagos/tarjeta` | Cobrar un pedido con tarjeta |
| `pagarYape(data)` | `POST /admin/pagos/yape` | Cobrar con N° de operación de Yape |
| `comprobanteUrl(id)` | `/admin/pagos/:id/comprobante.pdf` | Ruta del comprobante (para `downloadBlob`) |

### ✏️ Cómo agregar un endpoint nuevo

1. Créalo en el backend (Spring).
2. **Regístralo aquí**, en el grupo que corresponda:
   ```js
   // dentro de AdminAPI
   proveedores: () => api.get('/admin/proveedores'),
   crearProveedor: (data) => api.post('/admin/proveedores', data),
   ```
3. Impórtalo y úsalo en la página: `AdminAPI.proveedores().then(setProveedores)`.

No toques `client.js` para esto: el token, el refresh y los errores ya vienen resueltos.

---

## 3. Estado global (los Context)

React Context evita el *prop drilling*: en vez de pasar el usuario o el carrito de componente en componente, se lee directamente donde haga falta con un hook.

### 3.1 `auth/AuthContext.jsx` — la sesión

**Qué expone:** `{ user, login, logout, isAuth }` (`isAuth` es simplemente `!!user`).

**Cómo funciona:**

- El estado arranca leyendo `localStorage` (`useState(() => getUser())`), así al recargar la página la sesión no se pierde.
- Un `useEffect` al montar **revalida** la sesión contra `AuthAPI.me()`. Si el access token venció pero el refresh sigue vivo, `client.js` lo renueva solo por debajo; si ambos fallaron, limpia la sesión.
- `login(username, password)` llama a la API, guarda `accessToken` + `refreshToken` + usuario y actualiza el estado.
- `logout()` avisa al backend para invalidar el refresh token, pero **siempre** limpia el navegador aunque esa llamada falle.

**Cómo se usa:** `const { user, logout } = useAuth()`. Lo consumen `Navbar`, `Sidebar`, `Topbar` y `Login`.

- ✏️ Para mostrar más datos del usuario en la interfaz, amplía el objeto que se arma en `login()`:
  `const u = { username: r.username, nombre: r.nombre, rol: r.rol }`.
- ⚠️ `ProtectedRoute` **no** usa `useAuth()`: llama a `getUser()` de `localStorage` directamente. Es más rápido (evita un parpadeo al recargar) pero significa que borrar la sesión solo del estado de React no bloquea el acceso.

### 3.2 `carrito/CarritoContext.jsx` — el carrito

**Qué expone:** `{ items, agregar, quitar, cambiarCantidad, limpiar, total, cantidadTotal }`.

**Persistencia:** se guarda en `localStorage` bajo `gs_carrito`. Un `useEffect` reescribe la clave cada vez que cambian los `items`, dentro de un `try/catch` (en modo incógnito `localStorage` puede fallar).

**El tope de stock** es la regla central del carrito, y está en una sola función:

```js
const limitar = (cantidad, stock) => Math.max(1, Math.min(cantidad, stock))
```

Se aplica en los tres puntos donde puede cambiar la cantidad:

| Acción | Comportamiento |
|---|---|
| `agregar(producto, cantidad)` | Si `stock <= 0` no hace nada. Si el producto ya estaba, **suma** las cantidades pero nunca pasa del stock |
| `cambiarCantidad(id, cantidad)` | Si la cantidad baja a `0` o menos, **elimina** el producto; si no, la limita al stock |
| `quitar(id)` / `limpiar()` | Quitan un producto o vacían el carrito |

`total` y `cantidadTotal` se calculan con `reduce` en cada render — no se guardan en estado, así nunca quedan desincronizados.

**Cómo se usa:** `const { items, total, agregar } = useCarrito()`. Lo consumen `Navbar` (el badge), `ProductCard`, `ProductoDetalle`, `Carrito` y `Checkout`.

- ⚠️ El `stock` se guarda **congelado** dentro del item cuando se agrega. Si en el panel alguien cambia el stock, el carrito del visitante seguirá con el valor viejo hasta que recargue el producto. La validación definitiva la hace el backend en `POST /checkout`.
- ✏️ Cambiar la clave de persistencia → `const STORAGE_KEY = 'gs_carrito'`.

### 3.3 `config/ConfigContext.jsx` — la configuración de la tienda

**Qué expone:** el objeto de configuración completo que devuelve `GET /api/config`.

Define unos **valores de respaldo** para que la app funcione aunque el backend no responda:

```js
const DEFAULTS = {
  tiendaNombre: 'GamerStore', whatsappNumero: '51986969024',
  yapeMontoMaximo: 500, stripeEnabled: false, stripePublicKey: '',
}
```

Al montar pide la config real y la reemplaza; si falla, se queda con los defaults (`.catch()` vacío a propósito).

**Campos que consume el frontend:**

| Campo | Dónde se usa |
|---|---|
| `whatsappNumero` | `Footer.jsx`, `Contacto.jsx` (vía `waUrl()`) |
| `yapeMontoMaximo` | `Checkout.jsx` — bloquea Yape si el total lo supera |
| `yapeNumero`, `yapeTitular`, `yapeQr` | `Checkout.jsx` — bloque de pago con Yape |
| `stripeEnabled`, `stripePublicKey` | `Checkout.jsx` — decide entre Stripe real o formulario simulado |

**Cómo se usa:** `const { whatsappNumero, yapeMontoMaximo } = useConfig()`.

- ⚠️ `yapeNumero`, `yapeTitular` y `yapeQr` **no** están en `DEFAULTS`: llegan solo del backend. Si la API no responde, el checkout muestra el QR de respaldo ("Pide el QR al vendedor"), lo cual es intencional.
- ✏️ Para agregar un dato global (por ejemplo un horario), agrégalo al endpoint `/api/config` del backend y a `DEFAULTS` aquí; queda disponible en toda la app sin más cambios.

---

## 4. Hooks propios

Tres hooks pequeños que eliminan repetición. Los tres viven en `src/hooks/`.

### 4.1 `useTableControls(rows, opciones)` — búsqueda, orden y paginación

⚠️ **Concepto clave para la exposición:** este hook trabaja **enteramente en el navegador**. Recibe el arreglo completo que ya devolvió el backend y filtra, ordena y pagina en memoria. **No hace ninguna petición HTTP.** Por eso la búsqueda es instantánea, y por eso también funcionaría mal con decenas de miles de registros (habría que mover la lógica al backend).

**Opciones:** `{ searchKeys = [], pageSize = 8, initialSort = null }`.

**Devuelve:** `{ paged, query, onSearch, sort, toggleSort, page, setPage, totalPages, total }`.

Trabaja en tres pasos encadenados con `useMemo` (para no recalcular en cada render):

1. **Filtrar** — busca el texto (minúsculas, sin espacios extra) en las columnas indicadas en `searchKeys`.
2. **Ordenar** — si son números los compara numéricamente; si no, usa `localeCompare(..., 'es', { numeric: true })`, que ordena bien las tildes y los números dentro de textos.
3. **Paginar** — corta el arreglo con `slice()`.

Tanto `toggleSort()` como `onSearch()` hacen `setPage(1)`, porque al cambiar el orden o el filtro la página actual deja de tener sentido.

```jsx
const t = useTableControls(productos || [], {
  searchKeys: ['nombre', 'categoriaNombre'],
  pageSize: 8,
  initialSort: { key: 'nombre', dir: 'asc' },
})
```

- ✏️ Filas por página → el `pageSize` que pasa cada página (todas usan **8**), o el default del hook.
- ✏️ Qué columnas busca el buscador → `searchKeys`.

### 4.2 `useAutoClear(valor, limpiar, ms = 5000)` — mensajes que se borran solos

Evita que un mensaje de error quede clavado en pantalla después de leerlo. Si `valor` tiene contenido, programa un `setTimeout` que llama a `limpiar('')`; el `return` del efecto cancela el temporizador si el valor cambia antes.

```js
useAutoClear(formError, setFormError)   // se borra a los 5 s
```

Lo usan `AdminProductos`, `AdminCategorias`, `AdminClientes`, `AdminPedidos`, `AdminUsuarios`, `Login` y `Checkout`.

- ✏️ **Cambiar los 5 segundos:** el default está en la firma (`ms = 5000`). Para cambiarlo en todas partes edita ese número; para una sola pantalla pásalo como tercer argumento: `useAutoClear(formError, setFormError, 8000)`.

### 4.3 `useDuplicado(valor, verificar, opciones)` — validación en vivo con debounce

Avisa **mientras el usuario escribe** si un nombre/DNI/email ya existe, sin necesidad de enviar el formulario.

**Opciones:** `{ delay = 500, minLargo = 3, activo = true }`. **Devuelve:** `{ duplicado, mensaje, verificando }`.

Cómo funciona el *debounce*: cada tecla reinicia un temporizador de 500 ms; solo cuando el usuario **deja de escribir** se llama al backend. Sin esto se dispararía una petición por letra. Además usa un flag `cancelado` en la limpieza del efecto para descartar respuestas de peticiones viejas que llegan tarde (condición de carrera).

```jsx
const dupNombre = useDuplicado(
  form.nombre,
  (v) => AdminAPI.existeProducto(v, editing?.id),
  { activo: showModal }
)
```

El resultado se usa para dos cosas: pintar el input de rojo (`input-error` + `<small class="campo-error">`) y **deshabilitar el botón Guardar** (`disabled={saving || dupNombre.duplicado}`).

- ✏️ **Cambiar el debounce (500 ms)** → default `delay` en la firma del hook, o `{ delay: 800 }` en una llamada puntual.
- ✏️ **Cambiar el largo mínimo** → `minLargo`. Ejemplos reales del código: DNI usa `minLargo: 8`, email usa `5`, usuario usa `3`.
- ⚠️ `activo: showModal` es importante: sin eso el hook seguiría consultando al backend con el modal cerrado.

---

## 5. Componentes reutilizables (`components/ui`)

Nueve componentes sin dependencias externas. La regla del proyecto: si un patrón visual aparece en dos pantallas, se convierte en componente.

| Componente | Para qué sirve | Props principales | Ejemplo de uso |
|---|---|---|---|
| `Toast.jsx` | Avisos flotantes arriba a la derecha, se cierran solos | (provider) — se usa vía `useToast()` | `toast.success('Guardado')` |
| `Confirm.jsx` | Diálogo "¿seguro?" que reemplaza a `window.confirm` | (provider) — se usa vía `useConfirm()` | `const ok = await confirm({ message: '¿Eliminar?', danger: true })` |
| `Modal.jsx` | Ventana modal genérica | `title`, `icon`, `onClose`, `footer`, `size` (`sm`/`lg`) | `<Modal title="Nuevo" onClose={cerrar}>…</Modal>` |
| `Alert.jsx` | Mensaje fijo dentro de la página o modal | `type` (`success`/`error`/`info`) | `<Alert type="error">{formError}</Alert>` |
| `Pagination.jsx` | Controles ‹ 1 2 3 › + contador de registros | `page`, `totalPages`, `total`, `onPage` | `<Pagination page={t.page} totalPages={t.totalPages} total={t.total} onPage={t.setPage} />` |
| `TableToolbar.jsx` | Barra superior de tabla: buscador + contador | `query`, `onSearch`, `total`, `right` | `<TableToolbar query={t.query} onSearch={t.onSearch} total={t.total} />` |
| `TableSkeleton.jsx` | Esqueleto de carga con forma de tabla | `rows` (por defecto 6) | `<TableSkeleton rows={4} />` |
| `Skeleton.jsx` | Rectángulo gris con efecto *shimmer* | `className`, `style` | `<Skeleton className="sk-card" />` |
| `Spinner.jsx` | Círculo giratorio (animado por CSS) | ninguna | `<Spinner />` |

### 5.1 El patrón provider: `Toast` y `Confirm`

Estos dos no se usan como etiquetas sueltas. Se montan **una sola vez** en `main.jsx` y se consumen desde cualquier profundidad con un hook. Es el mismo patrón que usan librerías como `react-hot-toast`, pero escrito a mano.

**Toast** guarda una lista de avisos en estado. `mostrar()` agrega uno con un id incremental y programa su borrado:

```js
const id = ++contador
setToasts((lista) => [...lista, { id, mensaje, tipo }])
setTimeout(() => quitar(id), 3500)
```

Expone tres métodos: `toast.success(m)`, `toast.error(m)`, `toast.info(m)`. El provider renderiza `{children}` **y** el contenedor `.toaster`, por eso los avisos aparecen sobre cualquier pantalla.

- ✏️ **Duración del toast:** los `3500` ms de `Toast.jsx` (línea del `setTimeout`).
- ✏️ Los iconos están en el objeto `iconos` (`bi-check-circle-fill`, etc.).

**Confirm** es más ingenioso: convierte un diálogo visual en una **promesa**. `confirm()` devuelve una `Promise` y guarda su `resolver` en estado; cuando el usuario pulsa un botón, `cerrar(true/false)` resuelve esa promesa. Por eso se puede escribir código secuencial y legible:

```jsx
const ok = await confirm({ title: 'Eliminar producto', message: '¿Seguro?', confirmText: 'Eliminar', danger: true })
if (!ok) return
```

Internamente reutiliza `Modal` con `size="sm"`. Con `danger: true` el icono pasa a advertencia y el botón a `btn-danger`.

### 5.2 Detalles útiles del resto

- **`Modal`** se cierra de tres formas: la X, click en el fondo (el `.modal-overlay`) y la tecla **Escape**. Además bloquea el scroll del body mientras está abierto (`document.body.style.overflow = 'hidden'`) y lo restaura al desmontarse. El click dentro del modal usa `stopPropagation()` para no cerrarse solo.
- **`Pagination`** devuelve `null` si `total === 0` (no se ve nada si no hay datos) y **pinta todos los números de página**. ⚠️ Con muchas páginas la barra se alarga; si el proyecto crece, aquí habría que agregar puntos suspensivos.
- **`TableToolbar`** acepta una prop `right` para reemplazar el contador por lo que quieras (botones, filtros).

---

## 6. La tienda pública

### 6.1 Componentes de `components/public`

| Componente | Qué hace |
|---|---|
| `PublicLayout.jsx` | Navbar + `<Outlet />` (la página actual) + Footer |
| `Navbar.jsx` | Logo, buscador, enlaces, badge del carrito y acceso admin |
| `Footer.jsx` | 4 columnas: marca/redes, navegación, categorías y contacto |
| `ProductCard.jsx` | Tarjeta de producto del catálogo |
| `ProductGridSkeleton.jsx` | Grilla de tarjetas fantasma mientras carga (`count`, `cols`) |

**`Navbar`** tiene tres detalles a explicar:

- El buscador **navega**, no filtra: `navigate('/productos?q=' + encodeURIComponent(q))`. El estado de búsqueda vive en la URL.
- El badge solo aparece si hay algo: `{cantidadTotal > 0 && <span className="cart-badge">{cantidadTotal}</span>}`.
- Si hay sesión iniciada muestra "Panel" + "Salir"; si no, un botón "Admin". ✏️ El nombre y el icono de la marca están en `.nav-brand` (`<i className="bi bi-controller" /> GamerStore`).

**`ProductCard`** resuelve un conflicto típico: **toda la tarjeta es un `<Link>`**, pero dentro hay un botón "Agregar". Si no se frena la propagación, al agregar también navegarías al detalle:

```jsx
const handleAgregar = (e) => {
  e.preventDefault()
  e.stopPropagation()
  agregar(p, 1)
  toast.success('Agregado al carrito')
}
```

### 6.2 Las páginas

| Página | Ruta | Qué muestra | Endpoints |
|---|---|---|---|
| `Home.jsx` | `/` | Hero, 3 razones para comprar, **8 destacados**, CTA final | `PublicAPI.productos()` (corta con `.slice(0, 8)`) |
| `Catalogo.jsx` | `/productos` | Filtro lateral (búsqueda + categorías) y grilla | `PublicAPI.categorias()`, `PublicAPI.productos(categoria, q)` |
| `ProductoDetalle.jsx` | `/productos/:id` | Imagen, precio, stock, cantidad, especificaciones y relacionados | `PublicAPI.producto(id)` |
| `Contacto.jsx` | `/contacto` | WhatsApp, datos de la tienda y "¿cómo comprar?" | ninguno (usa `useConfig()`) |
| `Carrito.jsx` | `/carrito` | Items, cantidades y total | ninguno (todo desde `CarritoContext`) |
| `Checkout.jsx` | `/checkout` | Wizard de 4 pasos + pago | `verificarCliente`, `reniec`, `checkout`, `boletaUrl` |

**Patrón de carga compartido:** el estado arranca en `null` (no en `[]`) para poder distinguir "cargando" de "vacío":

```jsx
const [productos, setProductos] = useState(null)
// …
{productos === null ? <ProductGridSkeleton /> : productos.length === 0 ? <div className="empty">…</div> : <grilla/>}
```

**`Catalogo`** guarda los filtros en la **URL** con `useSearchParams()`, no en estado local. Ventaja: el enlace se puede compartir y el botón "atrás" del navegador funciona. Un `useEffect` recarga los productos cada vez que cambia `categoria` o `q`.

**`ProductoDetalle`** carga producto + relacionados en una sola llamada (`const { producto: p, relacionados } = data`), hace `window.scrollTo(0, 0)` al cambiar de producto y limita la cantidad al stock directamente en el input:

```jsx
onChange={(e) => setCantidad(Math.max(1, Math.min(Number(e.target.value) || 1, p.stock)))}
```

Tiene dos acciones: "Agregar al carrito" (se queda) y "Comprar ahora" (agrega y navega a `/checkout`).

**`Contacto`** es la página más fácil de editar: los datos y los pasos son dos arreglos al inicio del archivo (`info` y `pasos`). ✏️ Editas el arreglo y la página cambia sola.

### 6.3 `Carrito.jsx` — control de stock en la interfaz

La página consume el contexto (`items, cambiarCantidad, quitar, total`) y no tiene lógica propia de negocio. Muestra un estado vacío con enlace al catálogo si `items.length === 0`.

Lo interesante es cómo se refleja el tope de stock en los botones:

```jsx
<button onClick={() => cambiarCantidad(i.id, i.cantidad - 1)}>−</button>
<span>{i.cantidad}</span>
<button disabled={i.cantidad >= i.stock} onClick={() => cambiarCantidad(i.id, i.cantidad + 1)}>+</button>
```

- El botón **+** se deshabilita al llegar al stock: el usuario ve por qué no puede seguir.
- El botón **−** nunca se deshabilita: bajar de 1 llama a `cambiarCantidad(id, 0)` y el contexto interpreta eso como "quitar el producto".

Es un buen ejemplo de **doble red de seguridad**: la interfaz previene (botón gris), el contexto corrige (`limitar()`) y el backend valida de verdad al hacer checkout.

### 6.4 `Checkout.jsx` — el wizard de 4 pasos

Es la pantalla más grande del proyecto (~990 líneas) y la que más conviene entender para exponer.

**Los pasos** se controlan con un solo número, `const [step, setStep] = useState(1)`, y sus nombres están en una constante:

```js
const PASOS_LABEL = ['Identificación', 'Datos', 'Pago', 'Confirmar']
```

El *stepper* de arriba se genera recorriendo ese arreglo: un paso es `done` si `n < step`, `active` si `n === step`. El **resumen del pedido** (columna derecha) se ve en los 4 pasos.

| Paso | Qué pide | Estado clave | Cómo se avanza |
|---|---|---|---|
| **1 · Identificación** | Elegir "invitado" o "ya soy cliente" | `modo`, `identDni`, `identEmail`, `identificado` | Invitado: botón directo. Cliente: `PublicAPI.verificarCliente()` y si acierta salta al paso 2 con los datos cargados |
| **2 · Datos** | DNI, nombres, apellidos, teléfono, email, dirección | `cliente` | `datosValidos` exige DNI de 8 dígitos + nombres + apellidos. Botón "Buscar" consulta RENIEC (`PublicAPI.reniec`) y autocompleta |
| **3 · Pago** | Yape o tarjeta | `metodo`, `numeroOperacion`, `tarjeta`, `paymentMethodId` | Con Stripe: `continuarConStripe()`. Sin Stripe: valida `pagoValido` |
| **4 · Confirmar** | Revisar todo y pagar | `procesando`, `pasoAnim`, `confirmacion` | `pagar()` → `POST /api/checkout` |

Si la identificación como cliente falla, hay una **salida de emergencia**: el error trae un botón "Continuar como invitado" (`continuarComoInvitado()`) para no bloquear la venta.

#### Integración de Stripe (tokenización en el navegador)

Este es el punto técnicamente más fino. La idea: **el número de tarjeta nunca llega a nuestro servidor**.

1. La promesa de Stripe se crea **una sola vez** con `useMemo`, y solo si el backend activó la pasarela:
   ```js
   const stripePromise = useMemo(
     () => (stripeEnabled && stripePublicKey ? loadStripe(stripePublicKey) : null),
     [stripeEnabled, stripePublicKey]
   )
   ```
2. El formulario vive dentro de `<Elements stripe={stripePromise}>`, porque los hooks `useStripe()` y `useElements()` **exigen** ese contexto. Por eso existe el subcomponente `FormularioTarjetaStripe`.
3. `CardElement` es un iframe de Stripe: los datos de la tarjeta están fuera de nuestro DOM.
4. ⚠️ **La tokenización ocurre al salir del paso 3, no al pagar.** Motivo: en el paso 4 el formulario ya no se renderiza y el `CardElement` desaparecería. `continuarConStripe()` llama a `stripe.createPaymentMethod()`, guarda el `paymentMethodId` y un resumen (`{ marca, ult4 }`) para mostrarlo en la revisión.
5. Al pagar, el cuerpo cambia según el modo:
   ```js
   pago: stripeEnabled && paymentMethodId
     ? { metodo: 'TARJETA', paymentMethodId }              // solo el token
     : { metodo: 'TARJETA', numero, titular, vencimiento, cvv, ... }  // modo simulado
   ```
6. ⚠️ Si el pago se rechaza, el `paymentMethodId` **se descarta** (`setPaymentMethodId(null)`) y se vuelve al paso 3: un PaymentMethod ya usado no se puede reutilizar.

**Modo simulado (Stripe desactivado):** se usa un formulario propio con validación real en el navegador — algoritmo de **Luhn** (`luhn()`), formateo en grupos de 4 (`formatearTarjeta()`), `MM/AA` automático (`formatearVencimiento()`) y detección de marca por BIN (`detectarMarca()`: Visa empieza en 4, Mastercard 51-55 o 2221-2720, Amex 34/37, Discover 6011/65).

#### El bloque de Yape

- Muestra el **QR** (`yapeQr`) con respaldo si la imagen falla (`onError={() => setQrError(true)}`).
- Muestra número y titular, con botón **Copiar** (`navigator.clipboard.writeText`) que da feedback 1.5 s.
- Pide el **N° de operación** (solo dígitos, máx. 20; válido con ≥ 6).
- ⚠️ **Tope de S/ 500**: `const yapeBloqueado = total > yapeMontoMaximo`. Si se supera, la opción se deshabilita y un `useEffect` cambia el método a tarjeta automáticamente, para que nunca quede seleccionado un método imposible.
- Lleva un aviso ámbar: *"Pago de prueba: este QR es real (cuenta de {yapeTitular}). Si yapeas por error, el monto será devuelto."*

#### La animación de procesado

No es puramente decorativa: corre **en paralelo** a la llamada real y `Promise.all` espera a que ambas terminen.

```js
const [resp] = await Promise.all([PublicAPI.checkout(body), animar()])
```

`animar()` avanza `pasoAnim` cada 600 ms por tres etapas. La del medio cambia de texto según el método:

```js
const pasosAnimacion = ['Validando datos',
  metodo === 'yape' ? 'Verificando la operación' : 'Autorizando con el banco',
  'Confirmando tu pedido']
```

Esto garantiza un mínimo de ~1.8 s de espera: si el backend responde en 100 ms, el salto instantáneo se sentiría falso.

#### La confirmación

Si `confirmacion` tiene valor, la página entera se reemplaza por la pantalla de éxito (un `return` temprano, antes incluso de comprobar si el carrito está vacío — que lo está, porque `limpiar()` se ejecutó a propósito). Muestra código de pedido, código de pago, método, referencia, total, cliente y boleta.

Si el backend emitió comprobante, aparece **Descargar boleta**, que usa `downloadBlob` con la URL pública verificada por DNI:

```js
await downloadBlob(PublicAPI.boletaUrl(confirmacion.pedidoCodigo, cliente.dni),
                   'boleta-' + confirmacion.comprobanteCodigo + '.pdf')
```

#### ✏️ Qué se puede modificar en el Checkout

| Quiero… | Dónde |
|---|---|
| Cambiar los nombres de los pasos | `const PASOS_LABEL = [...]` (arriba del archivo) |
| Agregar o quitar un paso | El arreglo `PASOS_LABEL` + un bloque `{step === N && (…)}` + los `setStep()` de los botones ⚠️ |
| Cambiar los textos de la animación | `const pasosAnimacion = [...]` |
| Cambiar la velocidad de la animación | Los tres `esperar(600)` dentro de `animar()` |
| Cambiar el aviso de demo | Los bloques `.aviso-demo` (uno para Stripe, otro para el modo simulado, otro para Yape) |
| Cambiar el tope de Yape | Backend (`yapeMontoMaximo` de `/api/config`); el fallback local está en `DEFAULTS` de `ConfigContext` |
| Cambiar la validación de datos | `datosValidos` y `pagoValido` |

---

## 7. El panel admin

### 7.1 El patrón común de las páginas CRUD

**Cinco páginas comparten exactamente la misma estructura**: `AdminProductos`, `AdminCategorias`, `AdminClientes`, `AdminUsuarios` y (con más piezas) `AdminPedidos`. Si entiendes una, entiendes todas.

**1. Estado:**

```jsx
const [productos, setProductos] = useState(null)   // null = cargando, [] = vacío
const [showModal, setShowModal] = useState(false)  // ¿modal abierto?
const [editing, setEditing] = useState(null)       // null = crear, objeto = editar
const [form, setForm] = useState(EMPTY)            // campos del formulario
const [saving, setSaving] = useState(false)        // bloquea el botón Guardar
const [formError, setFormError] = useState('')     // error dentro del modal
```

**2. `cargar()`** — una línea que trae los datos y los deja en estado, con `.catch()` a arreglo vacío para que un fallo no deje la pantalla en "cargando" para siempre:

```js
const cargar = () => AdminAPI.productos().then(setProductos).catch(() => setProductos([]))
useEffect(() => { cargar() }, [])
```

**3. El modal, en dos modos.** Un solo `<Modal>` sirve para crear y editar; la diferencia es `editing`:

- `abrirCrear()` → `setEditing(null)` + `setForm(EMPTY)`.
- `abrirEditar(p)` → `setEditing(p)` + `setForm({...datos de p})`.

Y el título se decide con un ternario: `title={editing ? 'Editar producto' : 'Nuevo producto'}`.

**4. `guardar(e)`** — decide POST o PUT según `editing`, muestra un toast, cierra el modal y **recarga la tabla**:

```js
if (editing) { await AdminAPI.actualizarProducto(editing.id, payload); toast.success('Producto actualizado') }
else         { await AdminAPI.crearProducto(payload);                 toast.success('Producto creado correctamente') }
setShowModal(false)
cargar()
```

**5. `eliminar(x)`** — siempre pide confirmación primero:

```js
const ok = await confirm({ title: 'Eliminar producto', message: `¿Seguro…?`, confirmText: 'Eliminar', danger: true })
if (!ok) return
await AdminAPI.eliminarProducto(p.id); toast.success('Producto eliminado'); cargar()
```

**6. La tabla** usa `useTableControls` + los tres componentes de UI, y cada página define localmente un pequeño componente `Th` para las cabeceras ordenables (muestra ▲ ▼ ↕ según el orden actual):

```jsx
<TableToolbar query={t.query} onSearch={t.onSearch} total={t.total} />
{/* filas: t.paged.map(...) */}
<Pagination page={t.page} totalPages={t.totalPages} total={t.total} onPage={t.setPage} />
```

**7. Retroalimentación:** `toast` para acciones puntuales, `Alert` + `useAutoClear` para errores dentro del modal, `useDuplicado` para el aviso en vivo.

Una convención que se repite: `<button type="submit" hidden />` dentro del `<form>`. Sirve para que **Enter** envíe el formulario, aunque el botón visible de Guardar esté en el `footer` del modal (fuera del `<form>`).

### 7.2 Página por página

| Página | Ruta | Columnas de la tabla | Endpoints | Lo particular |
|---|---|---|---|---|
| `Login.jsx` | `/admin/login` | — | `AuthAPI.login` (vía `useAuth`) | **Bloqueo con contador** |
| `Dashboard.jsx` | `/admin` | — | `dashboard()`, `productos()` | **KPIs + gráficos Recharts** |
| `AdminProductos.jsx` | `/admin/productos` | Producto (+SKU), Categoría, Precio, Stock, Acciones | CRUD productos + `categorias()` + `subirImagen()` | **Subida de imagen** |
| `AdminCategorias.jsx` | `/admin/categorias` | ID, Nombre, Acciones | CRUD categorías | Modal `size="sm"`, un solo campo |
| `AdminClientes.jsx` | `/admin/clientes` | Cliente (+email), DNI, Contacto, Dirección, Acciones | CRUD clientes + `buscarDni()` | **Autocompletado RENIEC** |
| `AdminPedidos.jsx` | `/admin/pedidos` | Código, Cliente, Fecha, Ítems, Total, Estado, Acciones | CRUD pedidos + `PagosAPI` + comprobantes | **Detalle con pago y boleta + cobrar** |
| `AdminVentas.jsx` | `/admin/ventas` | Boleta, Pedido, Cliente(+DNI), Fecha, Op. gravada, IGV, Total, Pago, Estado, Acciones | `comprobantes()`, `resumenVentas()`, `boletaUrl()` | **IGV 18% y filtros de fecha** |
| `AdminPagos.jsx` | `/admin/pagos` | Código, Pedido, Cliente, Método, Monto, Estado, Referencia, Fecha, Acciones | `PagosAPI.listar()`, `comprobanteUrl()` | **Fuera del menú** |
| `AdminUsuarios.jsx` | `/admin/usuarios` | Usuario, Nombre, Email, Rol, Acciones | CRUD usuarios | Roles y contraseña opcional al editar |

#### `Login.jsx` — el bloqueo con contador

Si el backend responde **429** (demasiados intentos), la respuesta trae `segundosRestantes` y la pantalla arranca una cuenta atrás:

```jsx
if (err.status === 429 && err.data?.segundosRestantes) setBloqueo(err.data.segundosRestantes)
```

Mientras `bloqueo > 0`: los inputs se deshabilitan, el botón dice `Bloqueado (Ns)` y se muestra un `Alert` fijo en lugar del error normal. Un `useEffect` con `setInterval` baja el contador cada segundo, y otro detecta la transición a 0 (usando un `useRef` llamado `prevBloqueo`) para **limpiar el formulario** automáticamente. Además, si ya hay sesión guardada, redirige al panel sin mostrar el formulario.

#### `Dashboard.jsx` — KPIs y gráficos

**Seis tarjetas KPI** (`stat-card`) armadas desde un arreglo `stats`: Productos, Categorías, Clientes, Pedidos, Ventas (formateado con `money()`) y Stock bajo.

**Dos gráficos de Recharts**, ambos dentro de `<ResponsiveContainer>`:

- **BarChart** "Productos por categoría" — ⚠️ los datos se agrupan **en el frontend** con un `reduce` sobre la lista de productos, no vienen del backend.
- **PieChart** (dona) "Estado del stock" — tres cortes calculados en el front: `> 10`, `1-10` y `0`, con los colores de `COLORES_STOCK = ['#10b981', '#f59e0b', '#ef4444']`.

Debajo: lista de "Stock bajo", ranking "Más vendidos" (ese **sí** viene calculado del backend, en `data.topProductos`) y "Acciones rápidas". Mientras carga muestra skeletons con la misma forma que el contenido final.

- ✏️ Colores del gráfico → `COLORES_STOCK` y el `fill="#6366f1"` del `<Bar>`.
- ✏️ Umbrales de stock → el bloque `estadoStock`.

#### `AdminProductos.jsx` — subida de imagen

El único CRUD con archivos. El input real está **oculto dentro de un `<label>`** estilizado como botón, un truco estándar para no usar el feo input de archivos del navegador:

```jsx
<label className="btn btn-outline" style={{ cursor: 'pointer', margin: 0 }}>
  <i className="bi bi-upload" /> {subiendo ? 'Subiendo...' : 'Subir imagen'}
  <input type="file" accept="image/*" hidden onChange={subirImagen} disabled={subiendo} />
</label>
```

`subirImagen()` arma un `FormData`, llama a `AdminAPI.subirImagen(fd)` y guarda la URL devuelta en `form.imagen`, con vista previa inmediata. Se guarda en `uploads/productos`, máx. 5 MB.

Los badges de stock siguen la misma escala del dashboard: `> 10` verde, `1-10` ámbar, `0` rojo "Sin stock".

#### `AdminClientes.jsx` — RENIEC

El botón "Buscar" junto al DNI valida el formato y consulta RENIEC para autocompletar:

```js
if (!/^\d{8}$/.test(form.dni)) { setFormError('Ingresa un DNI de 8 dígitos'); return }
const p = await AdminAPI.buscarDni(form.dni)
setForm((f) => ({ ...f, nombres: p.nombres, apellidos: p.apellidos }))
```

⚠️ Al **editar**, el DNI queda bloqueado (`disabled={!!editing}`, label "(no editable)") y el botón de RENIEC desaparece: el DNI es la identidad del cliente. Por eso `dupDni` solo se activa al crear (`activo: showModal && !editing`), mientras que `dupEmail` está activo siempre.

#### `AdminPedidos.jsx` — la página más completa

Tiene **cuatro modales** y varias funciones. Estados de pedido: `PENDIENTE`, `PAGADO`, `ENVIADO`, `ENTREGADO`, `CANCELADO`; métodos: solo `TARJETA` y `YAPE`. La función `badgeEstado()` decide el color del badge.

| Modal | Qué hace |
|---|---|
| **Nuevo pedido** | Elige cliente y método, y arma los ítems uno a uno con un `draft` (`{productoId, cantidad}`) → `agregarItem()` / `quitarItem(idx)`. El total se calcula en vivo con `reduce` |
| **Editar estado** | Solo cambia `estado` y `metodoPago` |
| **Cobrar** | Monta `<PasarelaPago>`; al aprobarse, `onPagado` cierra y recarga |
| **Ver detalle** | ⭐ Reúne **tres cosas** en una vista |

**El detalle** (`verDetalle`) es la parte más interesante: pide en paralelo el listado de pagos y el de boletas, y cruza los datos para encontrar los de ese pedido:

```js
const [pagos, boletas] = await Promise.all([PagosAPI.listar(), AdminAPI.comprobantes()])
const pago = pagos.filter((x) => x.pedidoId === p.id).sort((a, b) => b.id - a.id)[0] || null
const boleta = boletas.find((c) => c.pedidoId === p.id) || null
```

(El `sort` descendente por id se queda con el **pago más reciente**, por si hubo reintentos.) El modal `size="lg"` muestra: **Ítems** (ya venían dentro del pedido, no se piden aparte), **Pago** (código, método, estado, monto, referencia, últimos 4 dígitos, botón de comprobante) y **Boleta** (código, op. gravada, IGV, total, botón de descarga). Si no hay pago, ofrece un botón "Cobrar ahora" que cierra el detalle y abre la pasarela.

También hay un **reporte PDF** con filtros propios de fecha y estado (`repDesde`, `repHasta`, `repEstado`) que se descarga con `downloadBlob(AdminAPI.reportePedidosUrl({...}), 'reporte-pedidos.pdf')`.

**Botones por fila, condicionales:** ver detalle (siempre); cobrar (solo si no está `PAGADO` ni `CANCELADO`); ver boleta (solo si está `PAGADO`); editar y eliminar (siempre).

#### `AdminVentas.jsx` — registro de ventas con IGV

Lista las **boletas emitidas** (la base del libro de ventas). Arriba, cuatro tarjetas de resumen que vienen de `resumenVentas()`: Boletas emitidas, **Op. gravada** (subtotal), **IGV (18%)** y Total vendido.

⚠️ Los montos y el IGV se calculan **en el backend**; el frontend solo los muestra con `money()`. Los filtros `desde`/`hasta` se aplican con el botón "Filtrar", que vuelve a llamar a `cargar()` con los mismos parámetros para la tabla y el resumen. Si se dejan vacíos, trae todo.

#### `AdminPagos.jsx` — la página fuera del menú

Su ruta existe en `App.jsx` pero **no aparece en el Sidebar**. Lo dice el propio comentario del código:

> *Pagos ya no está en el menú: el pago se ve dentro del detalle del pedido. La ruta queda para auditoría.*

Es una tabla de solo lectura (sin crear/editar/eliminar) con la única acción de descargar el comprobante PDF. Se llega escribiendo `/admin/pagos` a mano. ✏️ Para reponerla en el menú, agrega su entrada al arreglo `links` de `Sidebar.jsx`.

### 7.3 Los componentes de `components/admin`

**`AdminLayout.jsx`** — arma `Sidebar` + (`Topbar` + `<Outlet />`) + `SessionTimer`. Maneja el sidebar colapsable y **recuerda la preferencia** en `localStorage` bajo `gs_sidebar` (`'1'` = colapsado). La clase `.collapsed` en el contenedor es la que dispara el CSS.

**`Sidebar.jsx`** — el menú se genera desde un arreglo, no está escrito a mano en el JSX:

```js
const links = [
  { to: '/admin', label: 'Dashboard', icon: 'bi-grid-1x2-fill', end: true },
  { to: '/admin/productos', label: 'Productos', icon: 'bi-box-seam-fill' },
  // … categorías, clientes, pedidos, ventas, usuarios
]
```

Usa `NavLink`, que aplica la clase `active` sola cuando la ruta coincide. La opción `end: true` del Dashboard evita que `/admin` se marque activo estando en `/admin/productos`. Abajo: "Volver a la tienda" y "Cerrar sesión".

> ✏️ **Cómo agregar un ítem al menú:** añade un objeto a `links` con `to`, `label` e `icon` (nombre de Bootstrap Icons). Recuerda que además necesitas la ruta en `App.jsx` y la página en `pages/admin/`.

**`Topbar.jsx`** — título de la sección + chip del usuario (inicial del nombre, nombre y rol). El título sale de un mapa:

```js
const TITLES = { '/admin': 'Dashboard', '/admin/productos': 'Productos', '/admin/categorias': 'Categorías',
                 '/admin/clientes': 'Clientes', '/admin/pedidos': 'Pedidos' }
const title = TITLES[pathname] || 'Panel'
```

⚠️ **Ventas, Usuarios y Pagos no están en ese mapa**, así que muestran "Panel". Si agregas una página, agrégala también aquí.

**`SessionTimer.jsx`** — reloj flotante que cuenta cuánto le queda al access token. Decodifica el JWT **en el navegador** (sin librerías) leyendo el claim `exp`:

```js
const payload = JSON.parse(atob(token.split('.')[1]))
return payload.exp ? payload.exp * 1000 : null
```

Un `setInterval` de 1 s **relee el token** cada vez (no guarda el tiempo aparte). Ese detalle es lo que hace que el contador se reinicie solo cuando `client.js` refresca el token: como el `exp` del token nuevo es mayor, el número sube. Si no hay token válido, el componente no se renderiza (`return null`). Con ≤ 60 s añade la clase `session-timer-low` (aviso visual).

- ✏️ El umbral de aviso → `const bajo = restante <= 60`.
- ✏️ El texto → `Sesión {m}:{String(s).padStart(2, '0')}`.
- ⚠️ La **duración real** del token la decide el backend, no este componente.

**`PasarelaPago.jsx`** — modal de cobro con dos pestañas (`tab`: `'yape'` | `'tarjeta'`). Al montar pide `PagosAPI.config()` para traer número, titular, QR, tope y estado de Stripe.

| Pestaña | Flujo |
|---|---|
| **Yape** | Muestra QR + número (con botón Copiar), pide el N° de operación (mínimo 6 dígitos) y permite **subir el voucher** reutilizando `AdminAPI.subirImagen()`. Luego `PagosAPI.pagarYape()` |
| **Tarjeta** | Si `cfg.stripeEnabled`: `<Elements>` + `FormularioTarjetaStripe`. Si no: formulario propio con validación Luhn y `PagosAPI.pagarTarjeta()` |

⚠️ **Diferencia importante con el checkout público:** aquí el modal es de una sola vista, así que el botón "Pagar" **tokeniza y cobra en el mismo click** (no hay un paso intermedio donde el `CardElement` desaparezca).

Igual que en el checkout, `yapeBloqueado = pedido.total > (cfg?.montoMaximo || 500)` deshabilita la pestaña de Yape, y un `useEffect` cambia a "tarjeta" cuando llega la config si el monto la supera. Al terminar muestra `pago-ok` (con descarga de comprobante) o `pago-fail` (con botón Reintentar), y llama a `onPagado()` para que el padre recargue.

---

## 8. Estilos (`index.css`)

**Un solo archivo, 751 líneas, sin frameworks CSS.** No hay Tailwind ni Bootstrap (de Bootstrap solo se usan los **iconos**, cargados por CDN en `index.html`). Todo son clases escritas a mano.

### 8.1 Las variables de `:root`

Arriba del archivo hay un bloque de **tokens de diseño**. La regla del proyecto: los colores no se escriben sueltos en los componentes, se referencian con `var(--nombre)`. Por eso cambiar un token cambia toda la app de golpe.

| Grupo | Variables | Para qué |
|---|---|---|
| Fondos | `--bg`, `--bg-2`, `--surface`, `--surface-2`, `--border`, `--border-strong` | Fondo de página, tarjetas y bordes |
| Texto | `--text-strong`, `--text`, `--muted`, `--faint` | Jerarquía tipográfica (escala slate) |
| **Marca** | `--accent`, `--accent-hover`, `--accent-soft`, `--accent-border`, `--accent-grad` | **El color principal (indigo `#4f46e5`)** |
| WhatsApp | `--whatsapp`, `--whatsapp-hover` | Botones verdes de contacto |
| Estados | `--success`, `--danger`, `--warning` (+ variantes `-soft` y `-text`) | Badges y alertas |
| Sombras | `--shadow-xs`, `--shadow-sm`, `--shadow`, `--shadow-lg` | Elevación en capas |
| Forma | `--r` (14px), `--r-sm` (10px), `--r-xs` (8px), `--t` (0.16s) | Redondeo y velocidad de transición |

> ✏️ **Cambiar el color de marca:** edita `--accent`, `--accent-hover`, `--accent-soft` y `--accent-grad` en `:root`. Botones, enlaces, badges, iconos del sidebar y gráficos cambian solos. (Ojo: los colores de los gráficos de Recharts están **hardcodeados** en `Dashboard.jsx`, hay que cambiarlos ahí aparte.)

### 8.2 Las familias de clases

El archivo está dividido en secciones con comentarios `/* ====== NOMBRE ====== */`. Estas son las familias que más vas a usar:

| Familia | Clases principales | Dónde aparece |
|---|---|---|
| **Botones** | `btn` + `btn-primary` / `btn-outline` / `btn-ghost` / `btn-danger` / `btn-success` / `btn-whatsapp`, y tamaños `btn-sm` / `btn-lg` / `btn-block` / `btn-icon` | Toda la app |
| **Formularios** | `field`, `label`, `input`, `select`, `textarea`, `input-group`, `input-error`, `campo-error`, `form-grid` (+ `full` para ocupar dos columnas) | Modales y checkout |
| **Badges** | `badge` + `badge-ok` / `badge-warn` / `badge-danger` / `badge-accent` / `badge-cat` / `badge-yape` / `badge-tarjeta` | Estados en tablas |
| **Tablas** | `table-wrap`, `table-scroll`, `table`, `table-toolbar`, `table-search`, `table-count`, `sortable`, `is-sorted`, `sort-ind`, `cell-actions`, `thumb`, `pagination`, `page-btn` | Panel admin |
| **Paneles** | `panel`, `panel-head`, `panel-grid`, `page-head`, `grid-2` | Panel admin |
| **Stat cards** | `stat-grid`, `stat-card`, `stat-icon`, `stat-label`, `stat-value`, `chart-box`, `chart-legend`, `legend-dot` | Dashboard y Ventas |
| **Modal** | `modal-overlay`, `modal`, `modal-sm`, `modal-lg`, `modal-header`, `modal-body`, `modal-footer`, `modal-close` | Todos los modales |
| **Toast** | `toaster`, `toast`, `toast-success` / `toast-error` / `toast-info`, `toast-icon`, `toast-msg`, `toast-close` | Notificaciones |
| **Tienda** | `hero`, `section`, `section-title`, `eyebrow`, `feature-grid`, `product-grid` (+ `grid-3` / `grid-4`), `product-card`, `catalog-layout`, `filter-card`, `filter-link`, `detail-grid`, `spec-card`, `footer-grid` | Páginas públicas |
| **Carrito** | `carrito-list`, `carrito-item`, `carrito-item-qty`, `carrito-resumen`, `carrito-total`, `cart-badge` | Navbar y carrito |
| **Checkout** | `stepper`, `step`, `step-circle`, `step-line`, `wizard-nav`, `opcion-card` (+ `sel`, `opcion-card-off`), `checkout-grid`, `checkout-resumen`, `revision-bloque`, `procesando-pasos`, `compra-ok` | Checkout |
| **Pasarela** | `pasarela-tabs`, `pasarela-tab`, `pasarela-yape`, `pasarela-qr`, `pasarela-cuenta`, `pasarela-guia`, `stripe-card` (+ `focus`, `err`), `marca-badge`, `aviso-demo`, `pago-ok`, `pago-fail` | Checkout y PasarelaPago |
| **Carga** | `skeleton`, `sk-card`, `sk-row`, `sk-line`, `spinner`, `spinner-sm`, `empty` | Estados de carga y vacío |
| **Layout admin** | `admin`, `collapsed`, `sidebar`, `sidebar-link`, `sidebar-brand`, `admin-main`, `admin-content`, `topbar`, `userchip`, `session-timer` | Panel |
| **Login** | `login-wrap`, `login-card`, `login-side`, `login-form` | Login |

Al final del archivo hay una sección **RESPONSIVE** con los `@media` que colapsan el sidebar, convierten el menú en hamburguesa (`.nav-toggle` / `.nav-links.open`) y pasan las grillas a una columna.

> ✏️ **Dónde agregar clases nuevas:** al final de la sección temática que corresponda (por ejemplo, un botón nuevo va junto a los demás `btn-*`). Usa siempre `var(--…)` en vez de códigos de color literales, y respeta la convención de nombres en español que ya existe (`pasarela-`, `carrito-`, `detalle-`).
>
> ⚠️ Si cambias un nombre de clase existente, búscalo antes en `src/`: al no haber CSS Modules, nada avisa si rompes un estilo.

---

## 9. Guía rápida: "quiero cambiar X, ¿dónde toco?"

| # | Quiero… | Archivo y punto exacto |
|---|---|---|
| 1 | Cambiar el **color principal** de la marca | `index.css` → `:root` → `--accent`, `--accent-hover`, `--accent-soft`, `--accent-grad` |
| 2 | Cambiar el **logo o el nombre** de la tienda | `components/public/Navbar.jsx` (`.nav-brand`) y `components/admin/Sidebar.jsx` (`.sidebar-brand`) |
| 3 | Agregar una **página al panel** | 3 pasos: crear el archivo en `pages/admin/` → agregar `<Route>` en `App.jsx` → agregar el objeto al arreglo `links` de `Sidebar.jsx` (y opcionalmente a `TITLES` de `Topbar.jsx`) |
| 4 | Agregar un **endpoint** | `api/endpoints.js`, en el grupo que corresponda (`AuthAPI` / `PublicAPI` / `AdminAPI` / `PagosAPI`) |
| 5 | Cambiar el **tiempo del toast** (3.5 s) | `components/ui/Toast.jsx` → `setTimeout(() => quitar(id), 3500)` |
| 6 | Cambiar cuánto tarda en **borrarse un error** (5 s) | `hooks/useAutoClear.js` → parámetro `ms = 5000`, o el 3.er argumento en la llamada |
| 7 | Cambiar el **debounce del duplicado** (500 ms) | `hooks/useDuplicado.js` → `delay = 500`; el mínimo de caracteres, `minLargo = 3` |
| 8 | Cambiar las **filas por página** (8) | El `pageSize` que pasa cada página admin a `useTableControls`, o el default del hook |
| 9 | Cambiar por **qué columnas busca** una tabla | El `searchKeys` de esa página (ej. `['nombre', 'categoriaNombre']` en `AdminProductos.jsx`) |
| 10 | Cambiar los **textos de la tienda** | La página: `pages/public/Home.jsx` (hero y features), `Contacto.jsx` (arreglos `info` y `pasos`), `components/public/Footer.jsx` |
| 11 | Cambiar los **pasos del checkout** | `pages/public/Checkout.jsx` → `PASOS_LABEL` + los bloques `{step === N && …}` ⚠️ |
| 12 | Cambiar los **avisos de demo** del pago | `Checkout.jsx` y `components/admin/PasarelaPago.jsx` → bloques `.aviso-demo` |
| 13 | Cambiar el **contador de sesión** | `components/admin/SessionTimer.jsx` → umbral `restante <= 60` y el texto. ⚠️ La duración real la fija el backend |
| 14 | Cambiar el **puerto o el proxy** | `frontend/vite.config.js` → `server.port` y el bloque `proxy` |
| 15 | Cambiar los **estados de un pedido** | `pages/admin/AdminPedidos.jsx` → `const ESTADOS = [...]` (⚠️ deben coincidir con el backend) y `badgeEstado()` para los colores |
| 16 | Cambiar los **colores de los gráficos** | `pages/admin/Dashboard.jsx` → `COLORES_STOCK` y el `fill` del `<Bar>` |
| 17 | Cambiar los **umbrales de stock** (verde/ámbar/rojo) | `Dashboard.jsx` (`estadoStock`) y `AdminProductos.jsx` (los ternarios `p.stock > 10 ? … : p.stock > 0 ? …`) |
| 18 | Cambiar el **tope de Yape** (S/ 500) | Backend (`/api/config`); el respaldo local está en `config/ConfigContext.jsx` → `DEFAULTS.yapeMontoMaximo` |
| 19 | Cambiar cuántos **destacados** muestra el inicio | `pages/public/Home.jsx` → `list.slice(0, 8)` |
| 20 | Cambiar el **formato del precio o el SKU** | `utils/format.js` → `money()` y `sku()` |
| 21 | Cambiar los **roles de usuario** | `pages/admin/AdminUsuarios.jsx` → `const ROLES = ['ADMIN', 'USUARIO']` (⚠️ debe coincidir con el backend) |
| 22 | Volver a poner **Pagos en el menú** | `components/admin/Sidebar.jsx` → agregar `{ to: '/admin/pagos', label: 'Pagos', icon: 'bi-credit-card' }` a `links` |
| 23 | Cambiar el **título de la pestaña** o el favicon | `frontend/index.html` → `<title>` y `<link rel="icon">` |
| 24 | Cambiar la **tipografía** | `frontend/index.html` (el `<link>` de Google Fonts) + `index.css` → `body { font-family: … }` |
| 25 | Cambiar **dónde se compila** el build | `vite.config.js` → `build.outDir` ⚠️ (Spring espera la SPA en `src/main/resources/static`) |

---

### Cómo ejecutarlo

```bash
cd frontend
npm install
npm run dev       # http://localhost:5173 (necesita el backend en :8080)
npm run build     # compila hacia ../src/main/resources/static
npm run preview   # sirve el build compilado
```
