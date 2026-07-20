# Documentación técnica — Backend

**Proyecto:** GamerStore — tienda gaming + panel administrativo (ERP)
**Stack:** Spring Boot 3.5.6 · Java 17 · Spring Data JPA (Hibernate) · Spring Security + JWT (jjwt 0.12.6) · MariaDB/MySQL · Thymeleaf + openhtmltopdf · OpenPDF 1.3.35 · ZXing 3.5.3 · Stripe Java 29.2.0
**Paquete raíz:** `com.gamerstore.app` (`src/main/java/com/gamerstore/app`)
**Punto de entrada:** `GamerStoreApplication.java` — `@SpringBootApplication`, arranca el contexto y ejecuta los `CommandLineRunner` (el `DataSeeder`).

> Leyenda usada en todo el documento:
> ✏️ = **modificable sin miedo** (es un valor de configuración o una decisión de negocio).
> ⚠️ = **delicado** (tocarlo puede romper seguridad, integridad de datos o el flujo de compra).

---

## 1. Cómo está organizado (las capas)

El backend sigue el patrón clásico en capas. Una petición HTTP entra siempre por el mismo camino:

```
Navegador (React)
      │  JSON  (HTTP)
      ▼
┌──────────────┐   recibe la request, valida el body con @Valid,
│ CONTROLLER   │   NO decide reglas de negocio
└──────┬───────┘
       │ pasa datos primitivos / DTOs
       ▼
┌──────────────┐   aquí viven las REGLAS: stock, únicos, IGV,
│  SERVICE     │   tope de Yape, correlativos, @Transactional
└──────┬───────┘
       │ llama métodos de consulta/guardado
       ▼
┌──────────────┐   interfaces de Spring Data: el SQL se genera solo
│ REPOSITORY   │   a partir del NOMBRE del método
└──────┬───────┘
       │ Hibernate traduce a SQL
       ▼
┌──────────────┐   clases @Entity que mapean 1 a 1 con las tablas
│   MODEL      │
└──────────────┘

  ENTIDAD ──(Mapper)──► DTO ──► se devuelve como JSON
```

| Capa | Carpeta | Responsabilidad | ¿Qué NO debe hacer? |
|---|---|---|---|
| **Controller** | `controller/` | Definir rutas (`@GetMapping`, `@PostMapping`…), validar el body con `@Valid`, convertir entidad→DTO con el mapper y devolver el JSON o el PDF. | No debe calcular precios, ni validar stock, ni tocar repositorios directamente. |
| **Service** | `service/` | Toda la lógica de negocio: validaciones de únicos, descuento de stock, cálculo de IGV, correlativo de boleta, rechazo de pagos. Marca las transacciones con `@Transactional`. | No debe saber nada de HTTP más allá de lanzar `ResponseStatusException` con el código adecuado. No arma JSON ni HTML. |
| **Repository** | `repository/` | Consultas a la BD. Son **interfaces** que extienden `JpaRepository`; Spring genera la implementación. | No debe contener reglas de negocio ni transformaciones de datos. |
| **Model (entidad)** | `model/` | Representar las tablas con `@Entity`, sus columnas y relaciones. Getters derivados sencillos (`getCodigo()`, `getSubtotal()`). | No debe llamar a repositorios ni servicios. No se devuelve nunca directamente al front. |
| **DTO** | `dto/` | Contrato de datos con el front: qué entra (`*Request`) y qué sale (`*DTO`, `*Response`). Son `record` inmutables con anotaciones de validación. | No debe tener lógica. |
| **Mapper** | `mapper/` | Convertir entidad → DTO en un solo lugar. | No debe consultar la BD. |
| **Config** | `config/`, `config/security/` | Beans de infraestructura: CORS, recursos estáticos, BCrypt, seguridad, JWT, semilla de datos. | No debe contener reglas de negocio de la tienda. |
| **Util** | `util/` | Helpers puros sin estado (`NumeroALetras`). | No debe depender de Spring. |

**Regla práctica para el equipo:** si te preguntan *"¿dónde pongo esta validación?"* → en el **Service**. Si te preguntan *"¿dónde cambio una URL?"* → en el **Controller**. Si te preguntan *"¿dónde cambio un valor?"* → en **application.properties**.

---

## 2. Configuración (lo que más vas a tocar)

Archivo: `src/main/resources/application.properties`

### 2.1 Base de datos y arranque

| Propiedad | Para qué sirve | Valor actual | ¿Se puede cambiar? |
|---|---|---|---|
| `spring.application.name` | Nombre lógico de la app en logs. | `gamerstore` | ✏️ Sí, cosmético. |
| `spring.profiles.active` | Perfil activo. Carga además `application-local.properties`. | `local` | ⚠️ Si lo quitas, dejan de cargarse las claves privadas de Stripe/apiperu. |
| `server.port` | Puerto del backend. | `8080` | ✏️ Sí (si lo cambias, actualiza también la URL de la API en el front). |
| `spring.datasource.url` | Conexión JDBC. Incluye `createDatabaseIfNotExist=true`, así que XAMPP crea la BD solo. | `jdbc:mysql://127.0.0.1:3306/tienda_pc?...` | ✏️ Sí: cambia el host/puerto/nombre de BD según tu entorno. |
| `spring.datasource.username` | Usuario de MariaDB. | `root` | ✏️ Sí. |
| `spring.datasource.password` | Contraseña de MariaDB. | *(vacío)* | ✏️ Sí. En XAMPP por defecto va vacío. |
| `spring.jpa.hibernate.ddl-auto` | Hibernate crea/actualiza las tablas al arrancar leyendo las `@Entity`. | `update` | ⚠️ **No pongas `create` ni `create-drop`**: borrarían todos los datos en cada arranque. |
| `spring.jpa.show-sql` | Imprime el SQL generado en consola. | `false` | ✏️ Ponlo en `true` para depurar y ver las consultas reales. |

### 2.2 Tienda y CORS

| Propiedad | Para qué sirve | Valor actual | ¿Se puede cambiar? |
|---|---|---|---|
| `app.tienda.nombre` | Nombre que muestra el front (lo entrega `GET /api/config`). | `GamerStore` | ✏️ Sí. |
| `app.whatsapp.numero` | Número de contacto que usa el front. | `51986969024` | ✏️ Sí. |
| `app.cors.allowed-origins` | Orígenes autorizados a llamar la API. Lo leen `WebConfig` y `SecurityConfig` (hacen `.split(",")`, así que admite varios separados por coma). | `http://localhost:5173` | ✏️ Sí. ⚠️ En producción pon el dominio real, **nunca `*`**. |

### 2.3 Seguridad JWT

| Propiedad | Para qué sirve | Valor actual | ¿Se puede cambiar? |
|---|---|---|---|
| `app.jwt.secret` | Clave HMAC-SHA256 con la que se firman los access tokens (`JwtService`). | Cadena literal en el archivo | ⚠️ Sí, pero **debe tener al menos 32 caracteres** o `Keys.hmacShaKeyFor` falla. Cambiarla invalida todos los tokens ya emitidos. En producción debería ir en variable de entorno. |
| `app.jwt.access-expiration-ms` | Duración del access token. | `300000` (5 minutos) | ✏️ Sí, es el caso de cambio más habitual. |
| `app.jwt.refresh-expiration-ms` | Duración del refresh token guardado en BD. | `604800000` (7 días) | ✏️ Sí. |

### 2.4 Subida de imágenes

| Propiedad | Para qué sirve | Valor actual | ¿Se puede cambiar? |
|---|---|---|---|
| `app.uploads.dir` | Carpeta física donde `UploadController` guarda las imágenes; `WebConfig` la publica en `/images/productos/**`. | `uploads/productos` | ✏️ Sí (ruta relativa al directorio de ejecución o absoluta). |
| `spring.servlet.multipart.max-file-size` | Peso máximo por archivo. | `5MB` | ✏️ Sí. Si lo cambias, actualiza también el mensaje de `GlobalExceptionHandler.tooLarge()` que dice "5MB". |
| `spring.servlet.multipart.max-request-size` | Peso máximo de la request completa. | `6MB` | ✏️ Sí; mantenlo por encima del anterior. |

> Nota: el QR de Yape **no** usa `app.uploads.dir`. `WebConfig` publica una segunda carpeta con ruta fija `uploads/qr` bajo la URL `/images/qr/**`.

### 2.5 apiperu.dev (RENIEC)

| Propiedad | Para qué sirve | Valor actual | ¿Se puede cambiar? |
|---|---|---|---|
| `app.apidevperu.enabled` | Interruptor del autocompletado por DNI. | `true` | ✏️ Sí. En `false`, `ReniecService` ni siquiera llama a la API. |
| `app.apidevperu.base-url` | URL base del proveedor. | `https://apiperu.dev/api` | ✏️ Sí (si cambias de proveedor tendrás que ajustar también el mapeo del JSON). |
| `app.apidevperu.token` | Token Bearer. Se lee de la variable de entorno `APIDEVPERU_TOKEN` o del archivo local. | `${APIDEVPERU_TOKEN:}` | ⚠️ Secreto. Va en `application-local.properties`, nunca en git. |

### 2.6 Yape

| Propiedad | Para qué sirve | Valor actual | ¿Se puede cambiar? |
|---|---|---|---|
| `app.yape.numero` | Número al que el comprador yapea. Se expone en `GET /api/config` y `GET /api/admin/pagos/config`. | `912073109` | ✏️ Sí. |
| `app.yape.titular` | Nombre del titular mostrado junto al QR. | `Dietri Josue Diaz Asto` | ✏️ Sí. |
| `app.yape.qr` | Ruta pública de la imagen del QR. | `/images/qr/yape-qr.jpg` | ✏️ Sí; el archivo físico va en `uploads/qr/`. |
| `app.yape.monto-maximo` | **Tope de Yape**: si el total del pedido lo supera, `PagoService.pagarConYape()` responde 409 y el front deshabilita esa opción. | `500` | ✏️ Sí, es una regla de negocio pura. |

### 2.7 Bloqueo de login

| Propiedad | Para qué sirve | Valor actual | ¿Se puede cambiar? |
|---|---|---|---|
| `app.login.max-intentos` | Intentos fallidos antes de bloquear (`LoginAttemptService`). | `3` | ✏️ Sí. |
| `app.login.bloqueo-segundos` | Duración del bloqueo temporal. | `30` | ✏️ Sí. |

### 2.8 Stripe (pasarela real, modo prueba)

| Propiedad | Para qué sirve | Valor actual | ¿Se puede cambiar? |
|---|---|---|---|
| `app.stripe.enabled` | Activa la pasarela real. Si está en `false` (o falta la clave secreta), el sistema **cae automáticamente a la pasarela simulada con Luhn**. | `true` | ✏️ Sí. |
| `app.stripe.public-key` | Clave pública; el navegador la usa para tokenizar la tarjeta. Se expone en `/api/config`. | `${STRIPE_PUBLIC_KEY:}` | ✏️ Sí (no es secreta, pero va en el archivo local). |
| `app.stripe.secret-key` | Clave secreta que firma el cobro en `StripeService`. | `${STRIPE_SECRET_KEY:}` | ⚠️ **Secreto absoluto**. Nunca se expone en ningún endpoint. |
| `app.stripe.currency` | Moneda del `PaymentIntent`. | `pen` | ✏️ Sí, pero debe ser una moneda soportada por tu cuenta Stripe. |

### 2.9 Comprobante y empresa emisora

| Propiedad | Para qué sirve | Valor actual | ¿Se puede cambiar? |
|---|---|---|---|
| `app.empresa.razon-social` | Nombre del emisor en la cabecera de la boleta. | `GamerStore S.A.C.` | ✏️ Sí. |
| `app.empresa.ruc` | RUC del emisor. Aparece dos veces en la boleta **y es el primer campo del QR**. | `20601234567` | ✏️ Sí (datos demo). |
| `app.empresa.direccion` | Dirección fiscal impresa. | `Av. Javier Prado Este 4200, ...` | ✏️ Sí. |
| `app.comprobante.serie` | Serie de la boleta. Define el prefijo del código (`B001-00000001`) y **el correlativo se lleva por serie**. | `B001` | ✏️ Sí. ⚠️ Al cambiarla el correlativo arranca de nuevo en 1 para la serie nueva. |
| `app.comprobante.igv` | Tasa de IGV usada para desglosar el total. | `0.18` | ✏️ Sí. ⚠️ Si la cambias, actualiza también el literal "I.G.V. (18%)" en `templates/boleta.html`, que está escrito a mano. |

### 2.10 El archivo `application-local.properties`

- La **plantilla** versionada es `application-local.properties.ejemplo`. El archivo real (`application-local.properties`) está en `.gitignore` y **nunca se sube** — GitHub bloquea los push que contienen secretos.
- Contiene solo tres claves: `app.apidevperu.token`, `app.stripe.public-key`, `app.stripe.secret-key`.
- **Cómo funciona el pisado de valores:** como `spring.profiles.active=local`, Spring carga primero `application.properties` y **encima** `application-local.properties`. Las propiedades repetidas en el archivo local **ganan**. Por eso `application.properties` puede declarar `app.stripe.secret-key=${STRIPE_SECRET_KEY:}` (vacío por defecto) y el archivo local lo rellena.
- **Degradación elegante:** el sistema funciona igual sin claves. Sin token de apiperu, `ReniecService.consultarDni()` devuelve `Optional.empty()` y el DNI simplemente no se autocompleta. Sin clave de Stripe, `StripeService.estaActivo()` devuelve `false` y `CheckoutService` usa la pasarela simulada.

**Para configurar tu entorno:** copia `application-local.properties.ejemplo` → `application-local.properties` y rellena tus claves.

---

## 3. Seguridad (paquete `config/security`)

### 3.1 El flujo completo, paso a paso

**A. Login** (`POST /api/auth/login`)

1. `AuthController.login()` pregunta primero a `LoginAttemptService.segundosBloqueo(username)`. Si es > 0 lanza `LoginBloqueadoException` → **429**.
2. Llama a `authManager.authenticate(...)`. Spring Security usa `CustomUserDetailsService` para cargar el usuario (por username **o** por email) y compara la contraseña contra el hash BCrypt.
3. Si falla: `registrarFallo()`. Si ese fallo alcanza el máximo, se bloquea; si no, responde **401** indicando cuántos intentos quedan.
4. Si acierta: `limpiar()` borra el contador, `JwtService.generarAccess(u)` emite el access token (5 min) y `RefreshTokenService.crear(u)` guarda un refresh token nuevo en BD.

**B. Cada request posterior**

```
Request con  "Authorization: Bearer eyJ..."
      ▼
JwtAuthenticationFilter (corre 1 vez por request)
   1. lee el header
   2. jwtService.extraerUsername(token)
   3. userDetailsService.loadUserByUsername(...)
   4. jwtService.esValido(token, username)  → firma + expiración
   5. deja el Authentication en el SecurityContext
      ▼
SecurityConfig decide: ¿ruta pública / autenticada / ROLE_ADMIN?
      ▼
Controller
```

Si el token es inválido, el filtro **no lanza error**: simplemente no autentica, y `SecurityConfig` responde 401 o 403 según la ruta.

**C. Refresh** (`POST /api/auth/refresh`) — `RefreshTokenService.rotar()` valida que el token exista y esté vigente, lo marca `revocado = true` y crea uno nuevo (**rotación**: un refresh token solo sirve una vez). Devuelve access + refresh nuevos.

**D. Logout** (`POST /api/auth/logout`) — `revocar()` marca el token como revocado sin borrarlo.

### 3.2 Archivo por archivo

#### `SecurityConfig.java`
Configuración central. Es **el archivo que más se toca** cuando agregas endpoints.

- `csrf().disable()` — correcto aquí: la API es stateless y usa tokens, no cookies de sesión.
- `SessionCreationPolicy.STATELESS` — sin sesión de servidor.
- `authorizeHttpRequests` — ⚠️ **las reglas se evalúan en orden**, la primera que coincide manda:

```java
.requestMatchers("/api/auth/login", "/api/auth/refresh").permitAll()
.requestMatchers(HttpMethod.GET, "/api/productos/**", "/api/categorias/**", "/api/config/**").permitAll()
.requestMatchers("/images/**").permitAll()
.requestMatchers(HttpMethod.POST, "/api/checkout", "/api/checkout/**").permitAll()
.requestMatchers(HttpMethod.GET, "/api/checkout/boleta/**").permitAll()
.requestMatchers(HttpMethod.GET, "/api/reniec/**").permitAll()
.requestMatchers("/api/admin/**").hasRole("ADMIN")
.requestMatchers("/api/**").authenticated()
.anyRequest().permitAll()          // ← la SPA de React
```

- `exceptionHandling` — devuelve JSON en vez del HTML de Spring: `{"error":"No autenticado"}` (401) y `{"error":"No tienes permisos"}` (403). ✏️ Los mensajes son editables aquí mismo.
- `addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)` — ⚠️ el orden importa: el filtro JWT debe correr **antes** para que el `SecurityContext` ya esté poblado cuando se evalúan las reglas.
- `corsConfigurationSource()` — lee `app.cors.allowed-origins`. Expone `Content-Disposition` para que el navegador pueda leer el nombre de los PDF descargados.

**✏️ Qué modificar aquí:** agregar rutas públicas, cambiar qué rutas exigen ADMIN, cambiar los mensajes de 401/403.

#### `JwtService.java`
Genera y valida los access tokens (HS256).

| Método | Qué hace |
|---|---|
| `generarAccess(Usuario u)` | Firma un JWT con `subject = username` y claims `rol`, `nombre`, `id`. |
| `extraerUsername(String token)` | Devuelve el subject. |
| `esValido(String token, String username)` | Comprueba que el subject coincide y que no expiró. Devuelve `false` ante cualquier excepción. |
| `parse(String token)` *(privado)* | Verifica la firma; lanza excepción si el token fue alterado. |

**✏️ Modificable:** añadir claims al token (ej. el email) en `generarAccess`. **⚠️ Delicado:** la clave y el algoritmo.

#### `JwtAuthenticationFilter.java`
Extiende `OncePerRequestFilter`. Solo actúa si hay header `Authorization: Bearer `. Nótese la condición `SecurityContextHolder.getContext().getAuthentication() == null`: no pisa una autenticación ya establecida. El `catch (Exception ignored)` es intencional (token inválido = seguir sin autenticar).

#### `CustomUserDetailsService.java`
Puente entre Spring Security y la tabla `usuario`.

```java
Usuario u = repo.findByUsername(loginId)
        .or(() -> repo.findByEmail(loginId))
        .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
return User.withUsername(u.getUsername())
        .password(u.getPassword())
        .authorities("ROLE_" + u.getRol().name())
        .build();
```

⚠️ El prefijo `ROLE_` es obligatorio: `hasRole("ADMIN")` internamente busca la autoridad `ROLE_ADMIN`. Permite loguearse con **username o email**.

#### `RefreshTokenService.java`
| Método | Qué hace |
|---|---|
| `crear(Usuario)` | Genera un token **opaco** (UUID sin guiones, no es un JWT) con su fecha de expiración. |
| `rotar(String token)` | Valida vigencia, revoca el actual y emite uno nuevo. 401 "Sesión inválida" / "Sesión expirada". |
| `revocar(String token)` | Marca `revocado = true` (logout). |

✏️ Modificable: la duración vía `app.jwt.refresh-expiration-ms`.

#### `LoginAttemptService.java`
Contador **en memoria** (`ConcurrentHashMap`), no usa BD. ⚠️ Consecuencia a mencionar en la exposición: al reiniciar el servidor los contadores se pierden, y en un despliegue con varias instancias cada una llevaría su propia cuenta.

| Método | Qué hace |
|---|---|
| `segundosBloqueo(username)` | Segundos que faltan; 0 si no está bloqueado. Si ya pasó, limpia el estado. Redondea hacia arriba con `Math.ceil`. |
| `registrarFallo(username)` | Suma un fallo; al llegar a `maxIntentos` activa el bloqueo y reinicia el contador. |
| `intentosRestantes(username)` | Para el mensaje "te quedan N intentos". |
| `limpiar(username)` | Se llama al loguear correctamente. |

La clave del mapa se normaliza (`trim().toLowerCase()`), así que "Admin123" y "admin123 " cuentan como el mismo usuario.

#### `LoginBloqueadoException.java`
`RuntimeException` simple que lleva `segundosRestantes`. `GlobalExceptionHandler` la traduce a **429 Too Many Requests** incluyendo ese número, para que el front muestre una cuenta regresiva.

---

## 4. Modelo de datos (`model/`)

### 4.1 Diagrama de relaciones

```
   ┌──────────────┐            ┌───────────────┐
   │   usuario    │ 1        N │ refresh_token │
   │  (login)     │────────────│               │
   └──────────────┘            └───────────────┘
        rol: enum ADMIN|USUARIO

   ┌──────────────┐            ┌───────────────┐
   │   cliente    │ 1        N │    pedido     │
   │  DNI único   │────────────│ estado, total │
   └──────────────┘            └───────┬───────┘
                                       │ 1
                        ┌──────────────┼──────────────┐
                        │ N            │ N            │ 1
                 ┌──────────────┐  ┌────────┐  ┌──────────────┐
                 │ pedido_item  │  │  pago  │  │ comprobante  │
                 └──────┬───────┘  └────────┘  │  (boleta)    │
                        │ N                    └──────────────┘
                        │
                 ┌──────────────┐  N        1  ┌──────────────┐
                 │   producto   │──────────────│  categoria   │
                 │ nombre único │              │ nombre único │
                 └──────────────┘              └──────────────┘
```

Camino de una compra: `cliente → pedido → (pedido_item × N) → pago → comprobante`.

### 4.2 Entidad por entidad

#### `Producto` → tabla `producto`
| Campo | Tipo / restricción |
|---|---|
| `id` | PK autoincremental |
| `nombre` | `nullable=false, unique=true` ⚠️ |
| `descripcion` | `length=500` |
| `precio` | `double`, obligatorio |
| `imagen` | `length=500` — guarda la **ruta** (`/images/productos/xxx.jpg`), no el binario |
| `stock` | `int` |
| `categoria` | `@ManyToOne(FetchType.EAGER)`, FK `categoria_id` (**nullable**) |

#### `Categoria` → tabla `categoria`
`id` + `nombre` (`nullable=false, unique=true`). Tiene constructor `Categoria(String nombre)` que usa el `DataSeeder`.

#### `Cliente` → tabla `cliente`
| Campo | Detalle |
|---|---|
| `dni` | `unique=true, nullable=false, length=8` ⚠️ es la clave natural del cliente |
| `nombres`, `apellidos` | obligatorios, 100 |
| `telefono` (15), `email` (120), `direccion` (200) | opcionales |
| `fechaRegistro` | `@PrePersist onCreate()` la fija a `LocalDateTime.now()` si viene nula |

**Getter derivado:** `getNombreCompleto()` → `nombres + " " + apellidos`.
⚠️ Ojo: `email` **no** tiene `unique=true` a nivel de columna; la unicidad se valida en `ClienteService` y `CheckoutService`.

#### `Pedido` → tabla `pedido`
| Campo | Detalle |
|---|---|
| `cliente` | `@ManyToOne` FK `cliente_id`, obligatorio |
| `fecha` | fijada por `@PrePersist` |
| `estado` | `length=30`, por defecto `"PENDIENTE"` |
| `total` | `double` |
| `metodoPago` | `length=30` |
| `items` | `@OneToMany(mappedBy="pedido", cascade=ALL, orphanRemoval=true, fetch=EAGER)` |

⚠️ El `cascade = ALL` es lo que permite guardar el pedido y sus líneas con un solo `save()`, y borrar las líneas al borrar el pedido.

**Getters derivados:** `getCodigo()` → `PED-0001` (`String.format("PED-%04d", id)`); `getCantidadTotal()` → suma de cantidades.

Estados usados en el código: `PENDIENTE` (al crear), `PAGADO` (al aprobarse el pago), `CANCELADO` (bloquea el cobro).

#### `PedidoItem` → tabla `pedido_item`
FK a `pedido` y a `producto`, más `cantidad` y `precioUnitario`. ⚠️ Guarda el precio **al momento de la venta** (copiado de `producto.precio` en `PedidoService.crear`), así que cambiar el precio del catálogo no altera las ventas históricas.
**Getter derivado:** `getSubtotal()` → `cantidad * precioUnitario`.

#### `Pago` → tabla `pago`
| Campo | Detalle |
|---|---|
| `pedido` | `@ManyToOne` obligatorio |
| `metodo` | `"YAPE"` o `"TARJETA"` |
| `monto`, `estado` | `"APROBADO"` / `"RECHAZADO"` |
| `referencia` | obligatorio, 60 — N° de operación de Yape, código de autorización simulado, o el `PaymentIntent id` de Stripe |
| `tarjetaUlt4` (4), `titular` (100), `voucher` (300) | opcionales |
| `fecha` | `@PrePersist` |

**Getter derivado:** `getCodigo()` → `PAG-0001`.

#### `Comprobante` → tabla `comprobante`
| Campo | Detalle |
|---|---|
| `tipo` | por defecto `"BOLETA"` (solo se emiten boletas) |
| `serie` (6) + `numero` (int) | el correlativo se lleva **por serie** |
| `pedido` | `@ManyToOne` obligatorio |
| `clienteNombre`, `clienteDni`, `clienteDireccion` | ⚠️ **snapshot**: se copian al emitir. Si luego editas el cliente, la boleta emitida no cambia — que es exactamente lo que debe pasar con un comprobante. |
| `subtotal`, `igv`, `total` | desglose calculado |
| `moneda` | `"PEN"` |
| `metodoPago`, `referenciaPago` | copiados del pago |
| `estado` | `"EMITIDO"` |
| `fechaEmision` | `@PrePersist` |

**Getter derivado:** `getCodigo()` → `serie + "-" + String.format("%08d", numero)` → `B001-00000001`.

#### `Usuario` → tabla `usuario`
`username` (único, 50), `email` (único, 120), `nombre`, `password` (hash BCrypt), `telefono`, `fechaRegistro`, `rol` (`@Enumerated(EnumType.STRING)`, por defecto `ADMIN`). `@PrePersist` fija fecha y rol.

#### `Rol` (enum) → `USUARIO`, `ADMIN`
⚠️ Se guarda como **texto** en la BD (`EnumType.STRING`). Si renombras un valor, los registros existentes dejan de mapear.

#### `RefreshToken` → tabla `refresh_token`
`token` (único, 100), `usuario` (FK), `expiraEn` (`Instant`), `revocado` (boolean).
**Getter derivado:** `estaVigente()` → `!revocado && expiraEn.isAfter(Instant.now())`.

---

## 5. Repositorios (`repository/`)

### Cómo Spring Data genera las consultas

No escribes SQL. Declaras un método en la interfaz y **Spring lee su nombre** para generar la consulta:

```
findBy   Categoria Nombre    IgnoreCase   And   Nombre Containing IgnoreCase
  │           │        │          │        │        │        │
verbo    propiedad  propiedad  modificador  unión  propiedad  LIKE %...%
         (relación)  anidada
```

Palabras clave que aparecen en este proyecto: `findBy`, `existsBy`, `countBy`, `And`, `IgnoreCase`, `Containing` (→ `LIKE %x%`), `LessThanEqual`, `IdNot` (→ `id <> ?`), `OrderBy...Asc/Desc`.

Cuando el nombre no alcanza, se usa `@Query` con JPQL (así están `ultimoNumero`, `sumTotal`, `topProductos` y `revocarTodosDe`).

### `ProductoRepository`
| Método | Consulta | Dónde se usa |
|---|---|---|
| `findByCategoriaNombreIgnoreCase(String)` | Productos de una categoría por nombre | `ProductoService.filtrar/porCategoria` |
| `findByNombreContainingIgnoreCase(String)` | Búsqueda por texto | `ProductoService.filtrar` |
| `findByCategoriaNombreIgnoreCaseAndNombreContainingIgnoreCase(...)` | Ambos filtros combinados | `ProductoService.filtrar` |
| `findByStockLessThanEqualOrderByStockAsc(int)` | Stock bajo, del más crítico al menos | `ProductoService.stockBajo` → dashboard |
| `existsByCategoriaId(Long)` | ¿La categoría tiene productos? | `CategoriaService.eliminar` (bloquea el borrado) |
| `existsByNombreIgnoreCase(String)` | Nombre duplicado al crear | `ProductoService.crear/existeNombre` |
| `existsByNombreIgnoreCaseAndIdNot(String, Long)` | Nombre duplicado al editar | `ProductoService.actualizar/existeNombre` |

### `CategoriaRepository`
| Método | Consulta | Dónde se usa |
|---|---|---|
| `findByNombre(String)` | Categoría exacta | disponible para uso general |
| `existsByNombreIgnoreCase(String)` | Duplicado al crear | `CategoriaService.crear` |
| `existsByNombreIgnoreCaseAndIdNot(String, Long)` | Duplicado al editar | `CategoriaService.actualizar` |

### `ClienteRepository`
| Método | Consulta | Dónde se usa |
|---|---|---|
| `findByDni(String)` | Cliente por DNI | `CheckoutService.obtenerOCrearCliente` y `verificarCliente` |
| `existsByDni` / `existsByDniAndIdNot` | DNI único (alta / edición) | `ClienteService` |
| `existsByEmailIgnoreCase` / `existsByEmailIgnoreCaseAndIdNot` | Email único | `ClienteService`, `CheckoutService` |
| `findAllByOrderByApellidosAscNombresAsc()` | Listado ordenado | `ClienteService.listar` |

### `PedidoRepository`
| Método | Consulta | Dónde se usa |
|---|---|---|
| `findAllByOrderByFechaDesc()` | Todos, más recientes primero | `PedidoService.todos` |
| `sumTotal()` | `SELECT COALESCE(SUM(p.total), 0) FROM Pedido p` | KPI de ventas del dashboard |
| `topProductos(Pageable)` | Agrupa `PedidoItem` por producto y suma cantidades, orden descendente. Devuelve `List<Object[]>` | `AdminDashboardController` (top 5) |

### `PagoRepository`
| Método | Consulta | Dónde se usa |
|---|---|---|
| `findAllByOrderByFechaDesc()` | Listado de pagos | `PagoService.listar` |
| `existsByReferenciaAndMetodo(String, String)` | Evita registrar dos veces la misma operación Yape | `PagoService.pagarConYape` |

### `ComprobanteRepository`
| Método | Consulta | Dónde se usa |
|---|---|---|
| `findAllByOrderByFechaEmisionDesc()` | Registro de ventas | `ComprobanteService.listar` |
| `findByPedidoId(Long)` | Boleta de un pedido (a lo sumo una) | `emitir` (idempotencia) y `porPedido` |
| `ultimoNumero(String serie)` | `SELECT COALESCE(MAX(c.numero), 0) ... WHERE c.serie = :serie` | cálculo del correlativo |

### `UsuarioRepository`
| Método | Consulta | Dónde se usa |
|---|---|---|
| `findByUsername` / `findByEmail` | Login con cualquiera de los dos | `CustomUserDetailsService`, `UsuarioService.buscar` |
| `existsByUsername` / `existsByEmail` | Únicos al crear | `UsuarioService.crear` |
| `existsByUsernameAndIdNot` / `existsByEmailAndIdNot` | Únicos al editar | `UsuarioService.actualizar` |
| `existsByRol(Rol)` | ¿Hay algún admin? | `DataSeeder` (crear admin por defecto) |
| `countByRol(Rol)` | Cuántos admins hay | `UsuarioService.eliminar` (proteger el último) |
| `findAllByOrderByUsernameAsc()` | Listado del panel | `UsuarioService.listar` |

### `RefreshTokenRepository`
| Método | Consulta | Dónde se usa |
|---|---|---|
| `findByToken(String)` | Buscar el token | `RefreshTokenService.rotar/revocar` |
| `revocarTodosDe(Usuario)` | `@Modifying @Query` UPDATE que revoca todos los activos del usuario | disponible para invalidar todas las sesiones |

---

## 6. Servicios (`service/`) — el corazón

### 6.1 `PagoService` ⭐ (el más importante)

**Responsabilidad:** cobrar un pedido y, si el cobro se aprueba, marcarlo como PAGADO y disparar la emisión de la boleta.

| Método público | Qué hace |
|---|---|
| `listar()` | Todos los pagos, más reciente primero |
| `porId(Long)` | Un pago |
| `pagarConTarjeta(pedidoId, numero, titular, vencimiento, cvv)` | Pasarela **simulada** |
| `pagarConStripe(pedidoId, paymentMethodId)` | Pasarela **real** |
| `pagarConYape(pedidoId, numeroOperacion, voucher)` | Cobro por Yape |

**Reglas de negocio:**

1. **Validación previa del pedido** (`validarPedido`, se aplica a los tres métodos): existe, no está `PAGADO` (409 "El pedido ya está pagado") ni `CANCELADO` (409 "El pedido está cancelado").

2. **Tarjeta simulada** — cuatro validaciones en cadena:
   - Formato: 13 a 19 dígitos tras quitar espacios y guiones.
   - **Algoritmo de Luhn** (`luhn()`): valida que el número sea matemáticamente consistente, igual que un banco real.
   - Vencimiento `MM/AA`, mes 1-12, y que el último día de ese mes no sea anterior a hoy.
   - CVV de 3 o 4 dígitos.

3. **Rechazo simulado** — ✏️ esta es la regla que se enseña en la demo:
```java
// Simulación del banco: los números que terminan en 0002 siempre se rechazan.
boolean rechazado = limpio.endsWith("0002");
```
   Cambiar el sufijo `"0002"` cambia qué tarjeta de prueba se rechaza.

4. **Tope de Yape** — ✏️ configurable con `app.yape.monto-maximo`:
```java
if (pedido.getTotal() > yapeMontoMaximo) {
    throw new ResponseStatusException(HttpStatus.CONFLICT, String.format(
        "Yape solo permite pagos de hasta S/ %,.2f. Usa tarjeta para montos mayores.", yapeMontoMaximo));
}
```

5. **Anti-duplicado en Yape** — el número de operación debe tener 6 a 20 dígitos y no haber sido usado antes (`existsByReferenciaAndMetodo`) → 409 "Ese número de operación ya fue registrado".

6. **Aprobación** (`aprobarPedido`, privado) — el punto donde se enlazan pago, pedido y boleta:
```java
private void aprobarPedido(Pedido pedido, Pago pago) {
    pedido.setEstado("PAGADO");
    pedido.setMetodoPago(pago.getMetodo());
    pedidoRepo.save(pedido);
    comprobanteService.emitir(pedido, pago);   // ← la boleta nace aquí
}
```

7. **Código de autorización** — `generarCodigoAutorizacion()` produce 6 caracteres alfanuméricos con `SecureRandom`. Solo para la pasarela simulada; con Stripe la referencia es el id del `PaymentIntent`.

**✏️ Qué se puede modificar:** el tope de Yape (properties), el sufijo de tarjeta rechazada, la longitud del código de autorización, el rango de dígitos del N° de operación, todos los mensajes de error.
**⚠️ Delicado:** quitar `@Transactional` de los métodos de pago, o quitar la llamada a `comprobanteService.emitir()` (los pedidos quedarían pagados sin boleta).

### 6.2 `StripeService`

**Responsabilidad:** cobrar de verdad contra Stripe en modo prueba. El navegador tokeniza la tarjeta (los datos de tarjeta **nunca pasan por nuestro servidor**) y aquí solo llega un `paymentMethodId`.

| Método | Qué hace |
|---|---|
| `estaActivo()` | `enabled && secretKey != null && !secretKey.isBlank()` — el interruptor que decide real vs. simulado |
| `cobrar(paymentMethodId, monto, descripcion)` | Devuelve el record `ResultadoStripe(aprobado, referencia, ult4, marca, mensaje, titular)` |

**Cómo funciona `cobrar`:**
1. `PaymentMethod.retrieve(paymentMethodId)` → obtiene últimos 4 dígitos, marca y titular (los necesita el comprobante).
2. Crea y confirma un `PaymentIntent` con `.setConfirm(true)` y `AllowRedirects.NEVER` (sin 3-D Secure, la demo no maneja redirecciones).
3. ⚠️ **Conversión de moneda:** `.setAmount(Math.round(monto * 100))` — Stripe trabaja en **céntimos**.
4. Si `status == "succeeded"` → aprobado.

**Manejo de errores, dos casos distintos:**
- `CardException` (tarjeta rechazada por el banco) → **no es un error del sistema**: se loguea como `warn` y se devuelve `ResultadoStripe(false, ...)`.
- `StripeException` (no se pudo contactar la pasarela) → `log.error` + **502 Bad Gateway**.

**✏️ Modificable:** la moneda (`app.stripe.currency`), la descripción del cargo, habilitar redirecciones si algún día se quiere 3-D Secure.
**⚠️ Delicado:** `Stripe.apiKey = secretKey` se asigna en cada llamada (campo estático del SDK). Nunca expongas `secretKey` en un DTO.

### 6.3 `CheckoutService`

**Responsabilidad:** orquestar la compra pública completa. Es el único punto donde se juntan cliente + pedido + pago + boleta.

| Método público | Qué hace |
|---|---|
| `comprar(CheckoutRequest)` | Todo el flujo. `@Transactional` |
| `verificarCliente(dni, email)` | Identifica a un comprador recurrente |

**Flujo de `comprar()`:**
```
1. obtenerOCrearCliente(req.cliente())     → busca por DNI; actualiza o crea
2. pedidoService.crear(...)                → valida y descuenta stock, calcula total
3. cobrar(pedido.getId(), req.pago())      → Yape / Stripe / simulado
4. si RECHAZADO → excepción → ROLLBACK TOTAL
5. busca el código de la boleta ya emitida
6. devuelve CheckoutResponse
```

**Regla clave — el rollback transaccional.** Es el mejor ejemplo de `@Transactional` para explicar en la exposición:
```java
if ("RECHAZADO".equals(pago.getEstado())) {
    throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED,
        "Pago rechazado por el banco. Revisa los datos de tu tarjeta e inténtalo de nuevo.");
}
```
Al lanzar la excepción dentro de un método `@Transactional`, Spring revierte **toda** la transacción: no queda pedido creado, **el stock descontado se restaura** y el pago rechazado tampoco se guarda. El comprador puede reintentar sin dejar basura en la BD.

**Regla — verificación de cliente recurrente.** `verificarCliente` exige **DNI + email coincidentes**. Es una decisión de privacidad deliberada: con solo el DNI cualquiera podría recuperar los datos de un tercero. Si no coinciden → 404 "…Puedes continuar como invitado."

**Regla — el email nunca rompe la compra.** Tanto al crear como al actualizar el cliente, si el email ya pertenece a otro cliente simplemente **se omite** en lugar de fallar:
```java
if (!clienteRepository.existsByEmailIgnoreCaseAndIdNot(c.email().trim(), cliente.getId())) {
    cliente.setEmail(c.email());
}
```

**Regla — selección de pasarela** (`cobrar`, privado): si Stripe está activo **y** llegó `paymentMethodId`, cobra real; si no, cae al simulado. Yape va por su rama. Cualquier otro método → 400.

**✏️ Modificable:** los mensajes, qué campos del cliente se actualizan en compras sucesivas, agregar métodos de pago al `switch`.

### 6.4 `ComprobanteService`

**Responsabilidad:** emitir la boleta y alimentar el registro de ventas.

| Método público | Qué hace |
|---|---|
| `emitir(Pedido, Pago)` | Crea la boleta. `@Transactional` |
| `listar()` | Todas, más reciente primero |
| `porId(Long)` / `porPedido(Long)` / `porPedidoCodigo(String)` | Búsquedas |
| `filtrar(desde, hasta)` | Filtro por rango de fechas, inclusive |
| `resumen(List<Comprobante>)` | Totales para el panel |

**Regla 1 — idempotencia.** Si el pedido ya tiene boleta, la devuelve tal cual. Emitir dos veces es imposible.

**Regla 2 — correlativo por serie:**
```java
int numero = comprobanteRepo.ultimoNumero(serie) + 1;
```
⚠️ El propio código documenta la limitación: en producción esto necesitaría `SELECT ... FOR UPDATE` o una secuencia dedicada, porque dos pagos simultáneos podrían calcular el mismo número. Para una demo académica es aceptable — **menciónalo en la exposición, demuestra que entienden concurrencia**.

**Regla 3 — el IGV va incluido en el precio** (como en el retail peruano). No se suma, **se desglosa hacia atrás**:
```java
double total = pedido.getTotal();
double subtotal = redondear(total / (1 + igvTasa));
double igv = redondear(total - subtotal);
```
Ejemplo: total S/ 118.00 → subtotal 100.00, IGV 18.00. `redondear()` usa `Math.round(valor * 100) / 100.0`.

**Regla 4 — snapshot del cliente.** Se copian nombre, DNI y dirección al comprobante: la boleta no cambia aunque después edites al cliente.

**Nota de implementación:** `porPedidoCodigo` recorre la lista en memoria filtrando por `getCodigo()` porque no existe índice por código de pedido. Aceptable con pocas boletas; ⚠️ no escala.

**✏️ Modificable:** serie e IGV vía properties; el tipo de comprobante (hoy siempre `"BOLETA"`) si algún día se agregara factura.

### 6.5 `BoletaPdfService`

**Responsabilidad:** convertir un `Comprobante` en un PDF, usando `templates/boleta.html` como plantilla.

Método público único: `generar(Comprobante c, List<PedidoItem> items) → byte[]`.

**Cómo funciona:**
1. Arma un `Context` de Thymeleaf con las variables `empresa` (mapa con `razonSocial`, `ruc`, `direccion`), `c` (el comprobante), `items`, `enLetras`, `qr` y `fechaTexto`.
2. `templateEngine.process("boleta", ctx)` → HTML como String.
3. `PdfRendererBuilder` de openhtmltopdf renderiza ese HTML+CSS a PDF (`useFastMode()`).

**El QR — formato SUNAT** (`textoQr`, privado):
```java
return ruc + "|03|" + c.getSerie() + "|" + c.getNumero() + "|"
     + String.format("%.2f", c.getIgv()) + "|" + String.format("%.2f", c.getTotal()) + "|"
     + fecha + "|1|" + dni + "|";
```
Campos separados por `|`: RUC emisor · **`03`** (código SUNAT de boleta de venta) · serie · correlativo · IGV · total · fecha `yyyy-MM-dd` · **`1`** (tipo de documento del cliente = DNI) · número de documento.

**Generación de la imagen** (`generarQrDataUri`): ZXing `QRCodeWriter` produce un `BitMatrix` de 220×220 con margen 1, se pinta píxel a píxel en un `BufferedImage` y se codifica en Base64 como **data URI** (`data:image/png;base64,...`) que va directo en el `src` del `<img>`. Así el PDF no necesita ningún archivo externo.

⚠️ El propio Javadoc advierte: la boleta **no se transmite a SUNAT** (haría falta RUC real y certificado digital), por eso no tiene validez tributaria y el PDF lleva un aviso rojo obligatorio.

### 6.6 `ReniecService`

**Responsabilidad:** autocompletar nombres y apellidos a partir del DNI consultando apiperu.dev.

Método público: `consultarDni(String dni) → Optional<ReniecPersona>`.

**Filosofía "best-effort" — nunca rompe el flujo.** Devuelve `Optional.empty()` (sin lanzar nada) si: el feature está apagado, falta el token, el DNI no tiene 8 dígitos, o **cualquier excepción de red/parseo** (se atrapa y se loguea como `warn`).

**⚠️ Timeouts** — detalle importante, porque el `DataSeeder` llama a este servicio al arrancar:
```java
factory.setConnectTimeout(2000);
factory.setReadTimeout(4000);
```
Sin esto, un apiperu.dev caído colgaría el arranque de la aplicación.

**Mapeo del JSON:** dos `record` privados. `ApiPeruData` usa `@JsonProperty` porque la API devuelve snake_case (`apellido_paterno`, `apellido_materno`, `nombre_completo`). Los apellidos se concatenan: `(paterno + " " + materno).trim()`.

**✏️ Modificable:** los timeouts, el proveedor (`app.apidevperu.base-url` + ajustar los records), apagarlo con `enabled=false`.

### 6.7 `PedidoService`

**Responsabilidad:** alta de pedidos con cálculo de total y control de stock, más los datos para reportes.

| Método público | Qué hace |
|---|---|
| `todos()`, `porId(Long)`, `total()` | Consultas básicas |
| `totalVentas()` | Suma de todos los totales (KPI) |
| `topProductos(int limite)` | Ranking con `PageRequest.of(0, limite)` |
| `reporte(desde, hasta, estado)` | Filtro en memoria para el PDF |
| `crear(clienteId, metodoPago, items)` | Alta completa. `@Transactional` |
| `actualizar(id, estado, metodoPago)` | Edición desde el panel |
| `eliminar(id)` | Borrado |

**Regla 1 — métodos de pago permitidos.** Solo `TARJETA` o `YAPE`. El comentario del código explica por qué: `EFECTIVO`/`PLIN`/`TRANSFERENCIA` eran del sistema anterior (venta cotizada por WhatsApp, sin pago ni boleta) y ya no aplican.

**Regla 2 — validación y descuento de stock**, la misma para el panel admin y el checkout público:
```java
if (prod.getStock() < it.cantidad()) {
    throw new ResponseStatusException(HttpStatus.CONFLICT,
        "Stock insuficiente para " + prod.getNombre() + " (quedan " + prod.getStock() + ")");
}
PedidoItem item = new PedidoItem(pedido, prod, it.cantidad(), prod.getPrecio());
pedido.getItems().add(item);
total += item.getSubtotal();
prod.setStock(prod.getStock() - it.cantidad());
```

**Regla 3 — el precio se congela.** `prod.getPrecio()` se copia al `PedidoItem`; el total se calcula sumando subtotales, nunca se confía en un total enviado por el cliente. ⚠️ Esto es también una medida de seguridad: el front no puede manipular el importe.

**✏️ Modificable:** los métodos de pago aceptados, los mensajes de stock.

### 6.8 `ProductoService`

| Método | Regla |
|---|---|
| `todos()`, `porId()`, `porCategoria()`, `categorias()`, `total()` | Consultas |
| `filtrar(categoria, q)` | Elige el repo adecuado según qué filtros llegaron |
| `stockBajo(int umbral)` | Productos con `stock <= umbral` |
| `crear(...)` | ⚠️ **Nombre único** (409 "Ya existe un producto con ese nombre"). Fuerza `stock = Math.max(0, stock)` |
| `actualizar(...)` | Actualización **parcial**: solo aplica los campos no nulos / no vacíos. Valida nombre único excluyendo el propio id. `precio` solo si `> 0`, `stock` solo si `>= 0` |
| `ajustarStock(id, delta)` | Suma o resta; nunca deja negativo (`Math.max(0, ...)`) |
| `eliminar(id)` | Borrado directo |
| `existeNombre(nombre, id)` | Para la validación en vivo del formulario |

### 6.9 `CategoriaService`

| Método | Regla |
|---|---|
| `crear(nombre)` | Nombre obligatorio y único (`IllegalArgumentException` → 400) |
| `actualizar(id, nombre)` | Nombre único excluyendo el propio id |
| `eliminar(id)` | ⚠️ **Bloqueo por integridad**: `if (productoRepo.existsByCategoriaId(id)) throw ...` — "No se puede eliminar: la categoría tiene productos asociados" |
| `existeNombre(nombre, id)` | Validación en vivo |

### 6.10 `ClienteService`

| Método | Regla |
|---|---|
| `crear(...)` | DNI obligatorio; **DNI único** y **email único** → 409 |
| `actualizar(...)` | Valida DNI y email únicos excluyendo el propio id. Nombres/apellidos solo se pisan si vienen no vacíos; teléfono/email/dirección se asignan siempre |
| `eliminar(id)` | Borrado. ⚠️ Si el cliente tiene pedidos, la FK lo impide y `GlobalExceptionHandler` lo traduce a 409 |
| `existeDni` / `existeEmail` | Validación en vivo |

### 6.11 `UsuarioService`

| Método | Regla |
|---|---|
| `autenticar(loginId, password)` | Busca por username, luego por email, y compara con `passwordEncoder.matches()` |
| `buscar(loginId)` | Username o email — se usa tras el login para armar el token |
| `crear(...)` | Contraseña obligatoria (400); username y email únicos (409). ⚠️ **Siempre** guarda `passwordEncoder.encode(password)`, nunca texto plano |
| `actualizar(...)` | La contraseña **solo se cambia si viene no vacía** (así el formulario de edición puede dejarla en blanco) |
| `eliminar(id)` | ⚠️ **Protección del último admin**: |
| `parseRol(String)` | Convierte el texto a enum; si no es válido → 400 "Rol inválido" |

```java
if (u.getRol() == Rol.ADMIN && repo.countByRol(Rol.ADMIN) <= 1) {
    throw new ResponseStatusException(HttpStatus.CONFLICT, "No puedes eliminar el último administrador");
}
```

### 6.12 `PagoComprobanteService`

Genera con **OpenPDF** (programático, sin plantilla HTML) el comprobante de un pago: título, tabla clave/valor (fecha, cliente, pedido, método, referencia, tarjeta si aplica, estado) y el monto destacado a la derecha.

✏️ **Modificable:** los colores `ACCENT` (`new Color(99, 102, 241)`) y `HEAD_BG` (`new Color(30, 27, 75)`), el formato de fecha `FMT`, y las filas que se agregan con `addFila(...)`.
⚠️ No confundir con la **boleta** (`BoletaPdfService`): son dos PDF distintos. Este es el voucher del pago; aquel es el comprobante fiscal.

### 6.13 `PedidoReporteService`

Reporte PDF de pedidos, también con OpenPDF: título, línea de filtros aplicados, tabla de 7 columnas (Código, Cliente, Fecha, Estado, Método, Ítems, Total) y pie con total de ventas y cantidad de pedidos.

✏️ **Modificable:** las columnas (array `cols` + los anchos en `new PdfPTable(new float[]{...})` — deben tener la misma longitud), los colores, el formato de fecha.

---

## 7. Controladores y endpoints (`controller/`)

### 7.1 Públicos (tienda, sin login)

| Método | Ruta | Acceso | Qué hace | Service |
|---|---|---|---|---|
| GET | `/api/productos` | Público | Catálogo, filtros opcionales `?categoria=&q=` | `ProductoService.filtrar` |
| GET | `/api/productos/{id}` | Público | Detalle + hasta 4 relacionados de la misma categoría | `ProductoService` |
| GET | `/api/categorias` | Público | Categorías para los filtros | `CategoriaService.listar` |
| GET | `/api/config` | Público | Nombre de tienda, WhatsApp, datos Yape, clave **pública** de Stripe | lee properties |
| GET | `/api/reniec/{dni}` | Público | Autocompletar por DNI (404 si no hay dato) | `ReniecService` |
| POST | `/api/checkout` | Público | **Compra completa**: cliente + pedido + pago + boleta | `CheckoutService.comprar` |
| POST | `/api/checkout/cliente` | Público | Identificar comprador recurrente (DNI + email) | `CheckoutService.verificarCliente` |
| GET | `/api/checkout/boleta/{pedidoCodigo}?dni=` | Público **verificado** | Descarga la boleta en PDF | `ComprobanteService` + `BoletaPdfService` |

⚠️ La descarga de boleta es pública pero **no abierta**: el controller compara el DNI del query param con `c.getClienteDni()` y responde **403** si no coincide, para que nadie descargue la boleta de otro adivinando el código.

### 7.2 Autenticación

| Método | Ruta | Acceso | Qué hace | Service |
|---|---|---|---|---|
| POST | `/api/auth/login` | Público | Valida credenciales, emite access + refresh. 429 si está bloqueado | `AuthenticationManager`, `JwtService`, `RefreshTokenService`, `LoginAttemptService` |
| POST | `/api/auth/refresh` | Público | Rota el refresh y emite access nuevo | `RefreshTokenService.rotar` |
| POST | `/api/auth/logout` | Autenticado | Revoca el refresh (204) | `RefreshTokenService.revocar` |
| GET | `/api/auth/me` | Autenticado | Datos del usuario del token | `UsuarioService.buscar` |

### 7.3 Admin / ERP (todos exigen `ROLE_ADMIN`)

**Productos** — `AdminProductoController`
| Método | Ruta | Qué hace |
|---|---|---|
| GET | `/api/admin/productos` | Lista todos (incluidos sin stock) |
| POST | `/api/admin/productos` | Crea (valida `ProductoRequest`) |
| PUT | `/api/admin/productos/{id}` | Actualiza |
| PATCH | `/api/admin/productos/{id}/stock?delta=` | Suma o resta stock |
| DELETE | `/api/admin/productos/{id}` | Elimina (204) |
| GET | `/api/admin/productos/existe?nombre=&id=` | Validación en vivo de duplicado |

**Categorías** — `AdminCategoriaController`: GET, POST, PUT `/{id}`, DELETE `/{id}`, GET `/existe?nombre=&id=` sobre `/api/admin/categorias`.

**Clientes** — `AdminClienteController`
| Método | Ruta | Qué hace |
|---|---|---|
| GET / POST / PUT `/{id}` / DELETE `/{id}` | `/api/admin/clientes` | CRUD |
| GET | `/api/admin/clientes/reniec/{dni}` | Autocompletar desde RENIEC |
| GET | `/api/admin/clientes/existe?dni=&email=&id=` | Valida DNI **y** email |

**Usuarios** — `AdminUsuarioController`: GET, POST, PUT `/{id}` (204), DELETE `/{id}` (204), GET `/existe?username=&email=&id=` sobre `/api/admin/usuarios`.

**Pedidos** — `AdminPedidoController`
| Método | Ruta | Qué hace |
|---|---|---|
| GET | `/api/admin/pedidos` | Lista |
| GET | `/api/admin/pedidos/{id}` | Detalle |
| POST | `/api/admin/pedidos` | Crea (descuenta stock) |
| PUT | `/api/admin/pedidos/{id}` | Cambia estado / método |
| DELETE | `/api/admin/pedidos/{id}` | Elimina |
| GET | `/api/admin/pedidos/reporte.pdf?desde=&hasta=&estado=` | Reporte PDF (fechas `yyyy-MM-dd`) |

**Pagos** — `AdminPagoController`
| Método | Ruta | Qué hace |
|---|---|---|
| GET | `/api/admin/pagos` | Lista de pagos |
| POST | `/api/admin/pagos/tarjeta` | Cobro con tarjeta (Stripe si está activo y llega token; si no, simulado) |
| POST | `/api/admin/pagos/yape` | Cobro con Yape |
| GET | `/api/admin/pagos/config` | Cuenta Yape + estado y clave pública de Stripe |
| GET | `/api/admin/pagos/{id}/comprobante.pdf` | Voucher del pago en PDF |

**Comprobantes** — `AdminComprobanteController`
| Método | Ruta | Qué hace |
|---|---|---|
| GET | `/api/admin/comprobantes?desde=&hasta=` | Registro de ventas |
| GET | `/api/admin/comprobantes/resumen?desde=&hasta=` | Cantidad, subtotal, IGV, total |
| GET | `/api/admin/comprobantes/{id}/pdf` | Boleta por id |
| GET | `/api/admin/comprobantes/pedido/{pedidoId}/pdf` | Boleta por pedido |

**Dashboard** — `AdminDashboardController`: `GET /api/admin/dashboard` devuelve los KPIs (totales de productos, categorías, clientes, pedidos, ventas), el **top 5** de productos y los productos con stock bajo.
✏️ El umbral es una constante en el propio controller: `private static final int UMBRAL_STOCK_BAJO = 10;`

**Uploads** — `UploadController`: `POST /api/admin/uploads` (multipart, campo `file`). Valida que no esté vacío y que el content-type empiece por `image/`; guarda con nombre UUID y extensión según el tipo (`.png`, `.webp`, `.gif`, por defecto `.jpg`); devuelve `{"url": "/images/productos/<uuid>.<ext>"}`.

### 7.4 `GlobalExceptionHandler`

`@RestControllerAdvice` que convierte toda excepción en JSON `{"error": "..."}`.

| Excepción | HTTP | Mensaje / comportamiento |
|---|---|---|
| `MethodArgumentNotValidException` | **400** | Junta los mensajes de `@Valid` separados por `". "` y añade el mapa `errores` campo→mensaje |
| `ResponseStatusException` | **el que traiga** | Reenvía `e.getReason()` — es la vía normal desde los services |
| `IllegalArgumentException` | **400** | El mensaje de la excepción, o "Solicitud inválida" |
| `NoSuchElementException` | **404** | "Recurso no encontrado" (viene de los `orElseThrow()` sin argumento) |
| `AuthenticationException` | **401** | "Usuario o contraseña incorrectos" |
| `LoginBloqueadoException` | **429** | Mensaje + campo extra `segundosRestantes` |
| `DataIntegrityViolationException` | **409** | Red de seguridad: inspecciona el mensaje raíz. Si contiene `foreign key`/`referential integrity`/`fk_`/`constraint` → "No se puede eliminar: el registro está siendo usado por otros datos". Si contiene `unique`/`duplicate entry`/`already exists` → "Ya existe un registro con esos datos". Si no → "Operación no permitida: conflicto de datos" |
| `MaxUploadSizeExceededException` | **413** | "La imagen supera el tamaño máximo (5MB)" |
| `DateTimeParseException` | **400** | "Fecha inválida" (parámetros de reportes mal formados) |

✏️ **Todos estos mensajes son editables aquí.** Es el sitio correcto para cambiar la redacción de los errores genéricos; los específicos de negocio están en cada Service.

---

## 8. DTOs y Mappers

### ¿Por qué existen?

1. **No exponer las entidades.** Si devolvieras `Usuario` directamente, el JSON incluiría el **hash de la contraseña**. Con `UsuarioDTO` decides exactamente qué sale.
2. **Evitar bucles infinitos.** `Pedido` → `PedidoItem` → `Pedido` → … Jackson entraría en recursión. Los mappers cortan el ciclo aplanando a `productoNombre`, `clienteNombre`, etc.
3. **Contrato estable.** Puedes renombrar una columna sin romper el front.
4. **Validación en la frontera.** Los `*Request` llevan `@NotBlank`, `@Pattern`, `@Email`, `@Min`, `@Size`… y `@Valid` en el controller las dispara antes de llegar al Service.
5. **Inmutabilidad.** Todos son `record` de Java 17: sin setters, sin estado mutable.

**Convención de nombres:** `*Request` = entra · `*DTO` / `*Response` = sale.

### Familia: autenticación
| DTO | Para qué se usa |
|---|---|
| `LoginRequest` | username + password (ambos `@NotBlank`) |
| `LoginResponse` | accessToken, refreshToken, username, nombre, rol |
| `RefreshRequest` | refreshToken (`@NotBlank`) — también lo usa logout |
| `TokenResponse` | par access + refresh tras el refresh |
| `AuthUser` | respuesta de `/api/auth/me` |

### Familia: producto y categoría
| DTO | Para qué se usa |
|---|---|
| `ProductoDTO` | Producto aplanado (incluye `categoriaId` y `categoriaNombre`) |
| `ProductoDetalleDTO` | Producto + lista de relacionados |
| `ProductoRequest` | Alta/edición: nombre ≤100, descripción ≤500, precio `@DecimalMin("0.01")`, stock `@Min(0)`, categoría `@NotNull` |
| `CategoriaDTO` | id + nombre |
| `CategoriaRequest` | nombre `@NotBlank` ≤60 |

### Familia: cliente
| DTO | Para qué se usa |
|---|---|
| `ClienteDTO` | Cliente completo + `nombreCompleto` + `fechaRegistro` |
| `ClienteRequest` | Alta/edición: DNI `@Pattern("\\d{8}")`, nombres y apellidos obligatorios, teléfono `\\d{0,15}`, email `@Email` |
| `ReniecPersona` | nombres, apellidos, nombreCompleto (respuesta de apiperu) |

### Familia: pedido
| DTO | Para qué se usa |
|---|---|
| `PedidoDTO` | Pedido + `codigo`, `clienteNombre`, `cantidadTotal` y sus líneas |
| `PedidoItemDTO` | Línea con `productoNombre` y `subtotal` |
| `PedidoRequest` | clienteId `@NotNull`, metodoPago, items `@NotEmpty @Valid` |
| `PedidoItemRequest` | productoId `@NotNull`, cantidad `@Min(1)` |
| `PedidoUpdateRequest` | estado `@NotBlank` + metodoPago opcional |

### Familia: pago
| DTO | Para qué se usa |
|---|---|
| `PagoDTO` | Pago aplanado con `pedidoCodigo` y `clienteNombre` |
| `PagoTarjetaRequest` | pedidoId obligatorio; numero/titular/vencimiento/cvv (simulado) y `paymentMethodId` (Stripe) **todos opcionales**: cada flujo usa un subconjunto, y la validación real la hace `PagoService` |
| `PagoYapeRequest` | pedidoId + numeroOperacion obligatorios, voucher opcional |
| `YapeConfigDTO` | numero, titular, qr, montoMaximo, stripeEnabled, stripePublicKey |

### Familia: comprobante y ventas
| DTO | Para qué se usa |
|---|---|
| `ComprobanteDTO` | Boleta completa con `codigo` y `pedidoCodigo` |
| `ResumenVentasDTO` | cantidad, subtotal, igv, total |
| `DashboardDTO` | KPIs + `stockBajo` + `topProductos` |
| `TopProductoDTO` | Fila del ranking de más vendidos |

### Familia: checkout
| DTO | Para qué se usa |
|---|---|
| `CheckoutRequest` | cliente + items + pago, los tres con `@Valid` anidado |
| `CheckoutClienteDTO` | Datos del comprador (mismas validaciones que `ClienteRequest`) |
| `CheckoutPagoDTO` | metodo `@NotBlank` + campos de tarjeta / Yape / Stripe |
| `CheckoutResponse` | Confirmación: códigos de pedido, pago y comprobante, estado, referencia, total, nombre |
| `VerificarClienteRequest` | DNI + email para identificar al comprador recurrente |

### Familia: configuración y utilidades
| DTO | Para qué se usa |
|---|---|
| `ConfigDTO` | Config pública de la tienda |
| `ExisteDTO` | `{existe, mensaje}` de las validaciones en vivo |
| `UploadResponse` | URL de la imagen subida |
| `UsuarioDTO` / `UsuarioRequest` | Usuario del panel (el DTO **nunca** incluye password) |

### Los 7 mappers

Todos son `@Component` con un método `toDTO(...)`. Los interesantes:

| Mapper | Qué aplana |
|---|---|
| `ProductoMapper` | `categoria` → `categoriaId` + `categoriaNombre` (con guarda de null) |
| `PedidoMapper` | Tiene además `toItemDTO()`; convierte cada `PedidoItem` y resuelve `clienteNombre` |
| `PagoMapper` | Navega `pago → pedido → cliente` para sacar `pedidoCodigo` y `clienteNombre` |
| `ComprobanteMapper` | Resuelve `pedidoId` y `pedidoCodigo` |
| `ClienteMapper` | Añade el derivado `nombreCompleto` |
| `UsuarioMapper` | ⚠️ **Omite deliberadamente el password** |
| `CategoriaMapper` | id + nombre |

⚠️ **Si agregas un campo a una entidad y quieres verlo en el front, tienes que tocar también el DTO y el mapper** — la entidad sola no basta.

---

## 9. La boleta en PDF

Tres piezas trabajan juntas:

```
ComprobanteService.emitir()  →  crea el registro en BD (números, snapshot)
            ▼
BoletaPdfService.generar()   →  arma el Context + genera el QR (ZXing)
            ▼
templates/boleta.html        →  Thymeleaf rellena la plantilla
            ▼
openhtmltopdf                →  HTML + CSS  →  PDF (byte[])
            ▼
AdminComprobanteController / CheckoutController  →  descarga
```

### 9.1 Anatomía de `templates/boleta.html`

El archivo está dividido en **7 bloques comentados**, tanto en el `<style>` como en el `<body>`:

| # | Bloque | Contenido |
|---|---|---|
| 1 | Cabecera | Razón social, dirección y RUC a la izquierda; recuadro con RUC, franja "BOLETA DE VENTA ELECTRÓNICA" y el código (`B001-00000001`) a la derecha |
| 2 | Tarjeta cliente | Señor(es), DNI, dirección · fecha de emisión, moneda, forma de pago y referencia |
| 3 | Tabla de ítems | CANT. (10%) · DESCRIPCIÓN (48%) · P. UNIT. (21%) · IMPORTE (21%), con filas alternadas |
| 4 | Totales | OP. GRAVADA, I.G.V. (18%), IMPORTE TOTAL (fila destacada) |
| 5 | Importe en letras | Recuadro punteado con el resultado de `NumeroALetras` |
| 6 | Pie | Texto legal + QR de 115×115 px |
| 7 | Aviso DEMO | Recuadro rojo: "DOCUMENTO DE DEMOSTRACIÓN — SIN VALIDEZ TRIBUTARIA" |

### 9.2 Cómo modificar el diseño ✏️

Es **HTML y CSS normal**, todo dentro del mismo archivo. La paleta actual:

| Color | Dónde se usa | Cómo cambiarlo |
|---|---|---|
| `#4f46e5` (índigo) | Nombre de la empresa | `.empresa-nombre { color: ... }` |
| `#1e1b4b` (azul muy oscuro) | Borde y franja del recuadro, cabecera de la tabla, fila del total | Aparece en `.recuadro-boleta`, `table.items thead th`, `table.totales tr.fila-total` |
| `#f8fafc` (gris muy claro) | Fondo de la tarjeta del cliente y filas alternadas | `.tarjeta-cliente`, `tr.par` |
| `#ef4444` / `#fef2f2` | Aviso DEMO | `.aviso-demo` |

**Recetas concretas:**
- **Cambiar el tamaño de hoja o márgenes** → `@page { size: A4; margin: 14mm; }`
- **Cambiar los anchos de columna** → los `style="width:XX%"` de los `<th>` del bloque 3.
- **Quitar el aviso DEMO** → borrar el `<div class="aviso-demo">`. ⚠️ Recomendable mantenerlo mientras el proyecto sea académico.
- **Agregar una columna a la tabla de ítems** → añade un `<th>` y un `<td th:text="...">`, y reajusta los porcentajes para que sumen 100.
- **Cambiar el texto "I.G.V. (18%)"** → está **escrito a mano** en la línea del bloque 4. ⚠️ Si cambias `app.comprobante.igv`, este literal no se actualiza solo.
- **Agregar un dato nuevo** (ej. el teléfono de la empresa) → hay que hacerlo en **dos sitios**: añadirlo al `Map.of(...)` de `ctx.setVariable("empresa", ...)` en `BoletaPdfService` y luego usarlo con `th:text="${empresa.telefono}"` en la plantilla.

⚠️ **Limitaciones de openhtmltopdf:** no es un navegador. Evita flexbox y grid; por eso el layout usa **tablas** (`table.encabezado`, `table.tarjeta-cols`, `table.pie`). Si metes CSS moderno puede que simplemente no se renderice.

### 9.3 `NumeroALetras`

Clase utilitaria `final` con constructor privado (no se instancia). Método público único:
`NumeroALetras.convertir(double monto) → "SON: MIL NOVENTA Y OCHO CON 50/100 SOLES"`.

**Detalles del algoritmo que vale la pena conocer:**
- Redondea a centavos **antes** de separar para no arrastrar errores de coma flotante: `long centavosTotales = Math.round(Math.max(monto, 0) * 100);`
- `UNIDADES[]` cubre 0–29 de corrido, porque en español los "teens" y los veinte-tantos son irregulares (ONCE, DIECISÉIS, VEINTIDÓS…). A partir de 30 se arma con `Y`: "TREINTA Y UNO".
- Casos especiales del español resueltos: `100` → **CIEN** (no "CIENTO"), `1000` → **MIL** (no "UN MIL"), `1.000.000` → **UN MILLÓN**.
- `apocopar()` convierte "VEINTIUNO" → "VEINTIUN" cuando antecede a MIL/MILLONES. El último grupo **no** se apocopa porque le sigue "CON".
- Soporta hasta 999.999.999.

✏️ Para cambiar la moneda (ej. a dólares) hay que editar el literal `"/100 SOLES"` en `convertir()`.

---

## 10. Datos iniciales (`DataSeeder`)

`@Component` que implementa `CommandLineRunner`: Spring Boot ejecuta `run()` automáticamente al arrancar.

### Qué siembra

| Bloque | Condición (idempotencia) | Contenido |
|---|---|---|
| **Categorías** | `categoriaRepo.count() == 0` | 10: Tarjetas Graficas, Procesadores, Placas Madre, Memorias RAM, Almacenamiento, Monitores, Perifericos, Audio, Sillas Gamer, Consolas |
| **Productos** | `productoRepo.count() == 0` | ~28 productos reales de tecnología con precios en soles, stock e imagen local |
| **Clientes** | `clienteRepo.count() == 0` | 6 clientes con DNI, teléfono, email y dirección |
| **Admin** | `!usuarioRepo.existsByRol(Rol.ADMIN)` | `admin123` / `gamerstore123` |
| **Pedidos** | — | ⚠️ **Ya no se siembran.** El comentario del código lo explica: toda venta debe entrar por el checkout con pago real y boleta; sembrar ventas ficticias sin pago ni comprobante rompía esa coherencia |

**Idempotencia:** cada bloque comprueba si su tabla ya tiene datos. Puedes reiniciar la app cuantas veces quieras sin duplicar nada. Si el bloque de categorías se salta, igual carga el mapa `cats` desde la BD para que los productos puedan enlazarse.

**Integración con RENIEC:** el helper `clienteReal(...)` consulta apiperu.dev por DNI y, si responde, usa los nombres reales; si no, usa los de respaldo pasados por parámetro. Por eso `ReniecService` tiene timeouts cortos: un servicio caído no puede colgar el arranque.

### Cómo agregar o cambiar datos ✏️

**Agregar un producto** — añade una línea en el bloque de productos:
```java
productoRepo.save(new Producto("Nombre del producto", "Descripción corta",
        1299.00, img("slug-imagen"), 12, cats.get("Perifericos")));
```
El helper `img("slug")` construye `"/images/productos/slug.jpg"`, así que coloca el archivo `slug.jpg` en `uploads/productos/`. El último parámetro debe ser una clave **exacta** del mapa `cats` (⚠️ los nombres van sin tildes: `"Tarjetas Graficas"`, `"Perifericos"`).

**Agregar una categoría** — añádela al array `nombres` del bloque de categorías.

**⚠️ Para ver los cambios**, la tabla debe estar vacía: como el seeder solo actúa con `count() == 0`, tendrás que vaciar `producto` (o `categoria`) desde phpMyAdmin antes de reiniciar. Recuerda respetar el orden de las FK: primero `pedido_item`/`comprobante`/`pago`, luego `pedido`, luego `producto`.

**Cambiar las credenciales del admin** — edita el bloque final. ⚠️ Solo se crea si **no existe ningún** usuario con rol ADMIN; si ya tienes uno, cambia la contraseña desde el panel.

---

## 11. Guía rápida: "quiero cambiar X, ¿dónde toco?"

| # | Quiero… | Archivo / propiedad | Detalle |
|---|---|---|---|
| 1 | **Cambiar el IGV** | `application.properties` → `app.comprobante.igv` | ✏️ Ej. `0.18` → `0.10`. Lo lee `ComprobanteService`, que desglosa `subtotal = total / (1 + igv)`. ⚠️ Actualiza también el literal "I.G.V. (18%)" en `templates/boleta.html`, que está escrito a mano. |
| 2 | **Cambiar la serie de boleta** | `app.comprobante.serie` | ✏️ `B001` → `B002`. Cambia el prefijo del código (`B002-00000001`). ⚠️ El correlativo se lleva **por serie**, así que la nueva arranca en 1. |
| 3 | **Cambiar los datos de la empresa** | `app.empresa.razon-social`, `app.empresa.ruc`, `app.empresa.direccion` | ✏️ Los lee `BoletaPdfService` y los pasa a la plantilla. El RUC además es el **primer campo del QR**. |
| 4 | **Cambiar el tope de Yape** | `app.yape.monto-maximo` | ✏️ Un solo valor. Backend: `PagoService.pagarConYape` responde 409. Front: llega por `/api/config` y deshabilita la opción. |
| 5 | **Cambiar el número o el QR de Yape** | `app.yape.numero`, `app.yape.titular`, `app.yape.qr` + archivo en `uploads/qr/` | ✏️ La imagen física va en `uploads/qr/` (ruta fija en `WebConfig`), publicada bajo `/images/qr/**`. |
| 6 | **Cambiar intentos de login o bloqueo** | `app.login.max-intentos`, `app.login.bloqueo-segundos` | ✏️ Los lee `LoginAttemptService` con `@Value`. Contador **en memoria**: se pierde al reiniciar. |
| 7 | **Cambiar la duración del token** | `app.jwt.access-expiration-ms` (y `refresh-expiration-ms`) | ✏️ En **milisegundos**: 5 min = `300000`, 1 h = `3600000`. Lo lee `JwtService` por constructor. |
| 8 | **Agregar un producto o categoría al seeder** | `config/DataSeeder.java` | ✏️ Nueva línea `productoRepo.save(new Producto(...))` o entrada en el array `nombres`. ⚠️ Solo corre si la tabla está vacía (`count() == 0`). |
| 9 | **Hacer pública una ruta nueva** | `config/security/SecurityConfig.java` → `authorizeHttpRequests` | ✏️ Añade `.requestMatchers(HttpMethod.GET, "/api/mi-ruta/**").permitAll()`. ⚠️ **Antes** de `.requestMatchers("/api/**").authenticated()` — las reglas se evalúan en orden. |
| 10 | **Cambiar el diseño de la boleta** | `src/main/resources/templates/boleta.html` | ✏️ HTML + CSS en un solo archivo, 7 bloques comentados. Colores en el `<style>`. ⚠️ Sin flexbox ni grid: openhtmltopdf no los soporta bien, por eso el layout usa tablas. |
| 11 | **Agregar un campo a un producto** | 5 archivos en cadena | `model/Producto.java` (campo + `@Column` + getter/setter) → `dto/ProductoDTO.java` → `dto/ProductoRequest.java` (con su validación) → `mapper/ProductoMapper.java` → `service/ProductoService.java` (`crear` y `actualizar`) → `controller/AdminProductoController.java` (pasar el nuevo parámetro) → y el formulario del front. ⚠️ Con `ddl-auto=update` Hibernate agrega la columna solo. |
| 12 | **Cambiar el umbral de stock bajo** | `controller/AdminDashboardController.java` | ✏️ `private static final int UMBRAL_STOCK_BAJO = 10;` — está hardcodeado, no es una property. |
| 13 | **Pasar Stripe a producción** | `application-local.properties` | ⚠️ Reemplaza `pk_test_...`/`sk_test_...` por las claves **live** del dashboard de Stripe. ⚠️ Con claves live los cobros son **dinero real**. Nunca subas la clave secreta a git. |
| 14 | **Cambiar el tamaño máximo de imagen** | `spring.servlet.multipart.max-file-size` y `max-request-size` | ✏️ Ej. `5MB` → `10MB` (sube también `max-request-size`). ⚠️ Actualiza el mensaje "(5MB)" en `GlobalExceptionHandler.tooLarge()`. |
| 15 | **Cambiar los tipos de imagen aceptados** | `controller/UploadController.java` | ✏️ El `switch (ct)` que mapea content-type → extensión, y la validación `ct.startsWith("image/")`. |
| 16 | **Cambiar un mensaje de error** | El Service correspondiente, o `GlobalExceptionHandler` | ✏️ Mensajes de negocio → dentro del `ResponseStatusException` del Service (ej. "Stock insuficiente para…" en `PedidoService.crear`). Mensajes genéricos (404, 409, 413) → `GlobalExceptionHandler`. Mensajes de 401/403 de seguridad → `SecurityConfig`. |
| 17 | **Cambiar qué tarjeta se rechaza en la demo** | `service/PagoService.java` | ✏️ `boolean rechazado = limpio.endsWith("0002");` |
| 18 | **Cambiar el nombre de la tienda o el WhatsApp** | `app.tienda.nombre`, `app.whatsapp.numero` | ✏️ Llegan al front por `GET /api/config`. |
| 19 | **Permitir otro origen (CORS)** | `app.cors.allowed-origins` | ✏️ Admite varios separados por coma. Lo leen `WebConfig` y `SecurityConfig`. ⚠️ Nunca uses `*` en producción. |
| 20 | **Cambiar los colores de los PDF de pago/reporte** | `PagoComprobanteService.java`, `PedidoReporteService.java` | ✏️ Constantes `ACCENT = new Color(99, 102, 241)` y `HEAD_BG = new Color(30, 27, 75)`. Son PDF programáticos (OpenPDF), no usan plantilla HTML. |
| 21 | **Cambiar las columnas del reporte de pedidos** | `service/PedidoReporteService.java` | ✏️ El array `cols` y los anchos de `new PdfPTable(new float[]{...})`. ⚠️ Ambos deben tener la misma longitud. |
| 22 | **Cambiar los métodos de pago aceptados** | `service/PedidoService.crear` + `CheckoutService.cobrar` | ✏️ La validación que hoy solo admite `TARJETA`/`YAPE`, y el `switch` de `CheckoutService`. |
| 23 | **Desactivar el autocompletado por DNI** | `app.apidevperu.enabled=false` | ✏️ `ReniecService` deja de llamar a la API y devuelve `Optional.empty()`; nada más se rompe. |
| 24 | **Ver el SQL que genera Hibernate** | `spring.jpa.show-sql=true` | ✏️ Muy útil para depurar y para explicar en la exposición qué consulta genera cada método del repositorio. |

---

## Resumen para la exposición

Si tienes que quedarte con cinco ideas del backend:

1. **Capas estrictas.** El controller no calcula nada; el service tiene todas las reglas; el repositorio no sabe de negocio. Ninguna entidad sale al front sin pasar por un mapper.
2. **Seguridad stateless.** No hay sesión de servidor: cada request se autentica con su JWT. El refresh token sí vive en BD y **rota** (un solo uso), y hay bloqueo por intentos fallidos.
3. **Transaccionalidad real.** Si el banco rechaza el pago, `@Transactional` revierte pedido, stock y pago de una sola vez. No quedan datos a medias.
4. **El IGV va incluido y se desglosa hacia atrás** (`total / 1.18`), como en el retail peruano — no se suma al final.
5. **Degradación elegante.** Sin token de RENIEC no se autocompleta pero se compra igual; sin claves de Stripe la pasarela cae a la simulada con Luhn. La demo nunca se cae por una dependencia externa.
