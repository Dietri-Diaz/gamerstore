## 1. Introducción

**GamerStore** es un sistema de gestión (ERP) para una tienda de productos de tecnología (componentes de PC, periféricos, monitores, consolas, etc.). El sistema está compuesto por una **API REST** desarrollada en Spring Boot y un **panel de administración web** construido en React, respaldados por una base de datos MySQL.

El componente central del proyecto es el **panel administrativo (ERP)**, que centraliza la gestión del catálogo (productos y categorías), los clientes, las ventas (pedidos) y los usuarios del sistema, además de ofrecer un tablero con indicadores del negocio y reportes descargables. El sistema incorpora **seguridad real con JWT**, **datos reales** obtenidos por integración con servicios externos y un diseño **por capas** que facilita su mantenimiento.

Este informe documenta los objetivos, el alcance, las tecnologías empleadas, la arquitectura, el modelo de datos, los módulos funcionales y el funcionamiento interno del sistema.

## 2. Objetivos

**Objetivo general:** Desarrollar un sistema web de administración (ERP) para una tienda de tecnología, con una API REST segura y un panel de gestión completo.

**Objetivos específicos:**
- Modelar la base de datos mediante entidades y mapeo objeto-relacional (JPA/Hibernate).
- Implementar autenticación y autorización con **JSON Web Tokens (JWT)** y refresh tokens.
- Desarrollar el CRUD completo de productos, categorías, clientes, pedidos y usuarios.
- Incorporar **datos reales** de clientes mediante consulta por DNI (RENIEC).
- Generar **reportes en PDF** de las ventas con filtros.
- Almacenar las **imágenes de productos en el proyecto** (no en la base de datos).
- Presentar un **dashboard** con indicadores (KPIs) y productos más vendidos.
- Aplicar **validaciones** de datos únicos con retroalimentación clara al usuario.

## 3. Alcance del sistema

**Incluye:**
- Panel de administración (ERP) con los módulos: Dashboard, Productos, Categorías, Clientes, Pedidos y Usuarios.
- API REST bajo la ruta `/api/**`, con endpoints públicos y protegidos.
- Base de datos MySQL con carga automática de datos de prueba al iniciar.
- Seguridad con JWT (access + refresh), roles y cifrado de contraseñas (BCrypt).
- Integración con **apiperu.dev** (RENIEC) para autocompletar clientes por DNI.
- Generación de reportes PDF (OpenPDF).
- Almacenamiento de imágenes en la carpeta del proyecto.

**Nota sobre la tienda pública:** el sistema también cuenta con una vitrina pública (catálogo de productos con cotización por WhatsApp), pero el foco de este proyecto y de la presentación es el **panel administrativo (ERP)**.

**No incluye:** pasarela de pago en línea ni carrito/checkout público (la compra se gestiona mediante cotización por WhatsApp y el registro de pedidos se realiza desde el panel).

## 4. Tecnologías utilizadas

| Capa / Área | Tecnología | Uso en el proyecto |
|---|---|---|
| Lenguaje backend | Java 17 | Lenguaje principal de la API |
| Framework backend | Spring Boot 3.5.6 | Base del servidor y la API REST |
| Acceso a datos | Spring Data JPA (Hibernate) | Mapeo objeto-relacional y consultas |
| Seguridad | Spring Security + jjwt 0.12 | Autenticación/autorización con JWT |
| Reportes | OpenPDF 1.3.35 | Generación del PDF de pedidos |
| Base de datos | MySQL / MariaDB (XAMPP) | Persistencia de la información |
| Construcción | Maven | Gestión de dependencias y build |
| Frontend | React 18 + Vite 5 | Interfaz del panel (SPA) |
| Ruteo / gráficos | React Router + Recharts | Navegación y visualización del dashboard |
| Integración externa | apiperu.dev (RENIEC) | Datos reales de clientes por DNI |
| Pruebas | JUnit + Spring Boot Test + H2 | Pruebas de integración de la API |

## 5. Arquitectura del sistema

El sistema se ejecuta en **dos servidores** durante el desarrollo:
- **Backend (API REST):** Spring Boot en `http://localhost:8080`, expone las rutas `/api/**`.
- **Frontend (SPA del panel):** React + Vite en `http://localhost:5173`; en desarrollo reenvía (proxy) las rutas `/api` e `/images` al backend.

El backend está organizado siguiendo una **arquitectura por capas**, donde cada capa tiene una única responsabilidad:

```
Pantalla (React)  ->  Controller (/api)  ->  Service (reglas)  ->  Repository  ->  Base de datos
      DTO   <-------  Mapper (Entidad->DTO)  <-------------------------------------------┘
```

- **Controller:** recibe la petición HTTP, valida el formato de entrada y delega en el Service.
- **Service:** contiene las reglas de negocio (validaciones, cálculos, restricciones).
- **Repository:** interfaz de Spring Data JPA que accede a la base de datos sin escribir SQL manual.
- **Mapper / DTO:** convierten las entidades a objetos de transferencia (DTO) que se envían como JSON, desacoplando la base de datos de la API.

Esta separación permite entender, probar y modificar cada capa de forma independiente.

## 6. Modelo de datos

Las entidades (clases anotadas con `@Entity`) son convertidas automáticamente por Hibernate en tablas de la base de datos `tienda_pc`.

| Entidad | Tabla | Campos principales | Relaciones |
|---|---|---|---|
| Producto | producto | nombre (único), descripcion, precio, imagen (ruta), stock | pertenece a una Categoria (ManyToOne) |
| Categoria | categoria | nombre (único) | tiene muchos Productos |
| Cliente | cliente | dni (único), nombres, apellidos, telefono, email, direccion | referenciado por Pedido |
| Pedido | pedido | fecha, estado, total, metodoPago | pertenece a un Cliente; tiene muchos PedidoItem |
| PedidoItem | pedido_item | cantidad, precioUnitario | pertenece a un Pedido y a un Producto |
| Usuario | usuario | username (único), email (único), nombre, password (BCrypt), rol | — |
| RefreshToken | refresh_token | token (único), expiraEn, revocado | pertenece a un Usuario |

**Notas de diseño:**
- Los campos únicos (`producto.nombre`, `categoria.nombre`, `cliente.dni`, `usuario.username`, `usuario.email`) evitan duplicados a nivel de base de datos y de reglas de negocio.
- La imagen del producto se guarda como **ruta** (texto), no como archivo binario.
- El `Pedido` guarda sus líneas (`PedidoItem`) en cascada, y el total se calcula al registrarlo.

## 7. Seguridad

El acceso al panel está protegido con **autenticación basada en JWT (stateless)**:

- **Login:** el usuario envía sus credenciales; se validan con Spring Security y **BCrypt** (la contraseña nunca se guarda en texto plano). Si son correctas, se emite un **access token** (JWT firmado, vigencia de 5 minutos) y un **refresh token** persistido en la base de datos.
- **Autorización:** cada petición al panel envía la cabecera `Authorization: Bearer <token>`. Un **filtro** (`JwtAuthenticationFilter`) valida el token en cada solicitud, y `SecurityConfig` exige el rol `ADMIN` en las rutas `/api/admin/**`.
- **Refresh y rotación:** cuando el access token expira, el cliente solicita uno nuevo mediante el refresh token; este se **rota** (se revoca el anterior y se emite otro), lo que permite **revocar sesiones** (cierre de sesión real).
- **Contador de sesión:** el panel muestra de forma discreta el tiempo restante del token, que se renueva automáticamente sin interrumpir al usuario.

## 8. Módulos del sistema (ERP)

**8.1. Dashboard.** Muestra los indicadores del negocio (totales de productos, categorías, clientes y pedidos; total de ventas; productos con stock bajo) y un ranking de **productos más vendidos**, calculado a partir de las líneas de pedido. Incluye gráficos (Recharts).

**8.2. Productos.** Gestión completa (crear, listar, editar, eliminar) del catálogo. Permite **subir la imagen** del producto, que se almacena en la carpeta del proyecto y se referencia por su ruta. Valida que el **nombre no se repita**.

**8.3. Categorías.** Gestión de las categorías del catálogo. Valida nombres únicos y **bloquea la eliminación** de una categoría que tiene productos asociados.

**8.4. Clientes.** Registro y gestión de clientes. Incluye **autocompletado por DNI** consultando datos reales de RENIEC. Valida DNI y correo únicos.

**8.5. Pedidos.** Registro de ventas (estilo punto de venta): se selecciona el cliente, se agregan productos con cantidades y el sistema **calcula el total**. Permite cambiar el estado del pedido y **descargar un reporte en PDF** con filtros por fecha y estado.

**8.6. Usuarios.** Administración de los usuarios del sistema. Valida usuario y correo únicos, cifra la contraseña con BCrypt y **impide eliminar al último administrador** para no perder el acceso.

**8.7. Validaciones y manejo de errores (transversal).** Un manejador global de excepciones convierte los errores en respuestas JSON claras; el frontend los muestra como **notificaciones (toasts)**, por ejemplo al intentar registrar datos duplicados.

## 9. Integraciones externas

- **RENIEC (apiperu.dev):** el sistema consulta la API de apiperu.dev por número de DNI para obtener nombres y apellidos reales. La consulta es *best-effort* (con tiempos de espera y manejo de errores): si el servicio no responde, el flujo continúa sin interrumpirse. Se utiliza tanto en el registro de clientes (botón "Buscar") como en la carga inicial de datos de prueba.
- **OpenPDF:** librería utilizada para construir el reporte de pedidos en formato PDF (encabezado, tabla de pedidos y total de ventas).
- **Almacenamiento de imágenes:** las imágenes de los productos se guardan en la carpeta `uploads/productos/` del proyecto y se sirven mediante una ruta pública (`/images/**`); en la base de datos solo se guarda la ruta.

## 10. Flujo general de una petición

Todas las funcionalidades del panel siguen el mismo recorrido:

1. La **pantalla** (React) dispara una acción (por ejemplo, "Guardar") y llama a la función correspondiente de la capa de API (`endpoints.js`).
2. El cliente HTTP (`client.js`) realiza la solicitud agregando la cabecera de autorización con el token.
3. En el backend, el **Controller** recibe la petición en su ruta `/api/...` y valida los datos de entrada.
4. El **Service** aplica las reglas de negocio (validaciones de únicos, cálculos, etc.).
5. El **Repository** consulta o modifica la **base de datos**.
6. De regreso, el **Mapper** convierte la entidad en un **DTO**, que el Controller devuelve como **JSON**.
7. El frontend actualiza su estado y la **pantalla se refresca** mostrando el resultado (o una notificación de error).

## 11. Datos de prueba (carga inicial)

Al iniciar el backend, un componente sembrador (`DataSeeder`) llena la base de datos de forma automática e **idempotente** (solo si las tablas están vacías) con:
- **10 categorías** de tecnología.
- **28 productos** reales con precios en soles (S/) e imágenes locales.
- **6 clientes** con **nombres reales obtenidos de RENIEC** a partir de su DNI.
- **Aproximadamente 40 pedidos** con fechas repartidas en los últimos 6 meses (para dar historia al dashboard).
- Un **usuario administrador** por defecto para el acceso.

## 12. Pruebas y verificación

- **Pruebas de integración automatizadas** (`ApiIntegrationTest`) que se ejecutan sobre una base de datos H2 en memoria: verifican el arranque del sistema, los endpoints públicos, el login válido/ inválido y las validaciones de los formularios.
- **Verificación funcional** de extremo a extremo: inicio de sesión con JWT, operaciones CRUD de todos los módulos, subida de imágenes, generación del reporte PDF y visualización del dashboard, comprobando que el sistema responde correctamente y sin errores.

## 13. Instalación y ejecución

1. Iniciar **MySQL** (XAMPP). La base de datos `tienda_pc` se crea automáticamente si no existe.
2. **Backend:** desde la raíz del proyecto ejecutar `mvnw.cmd spring-boot:run` (levanta en el puerto 8080 y carga los datos de prueba).
3. **Frontend:** en la carpeta `frontend/` ejecutar `npm install` (la primera vez) y luego `npm run dev` (abre en el puerto 5173).
4. Ingresar al panel en `http://localhost:5173/admin` con el usuario **admin123** y la contraseña **gamerstore123**.

## 14. Conclusiones

El proyecto GamerStore cumple con el objetivo de construir un **sistema de administración (ERP)** completo y funcional para una tienda de tecnología. Se logró implementar una **API REST segura** con autenticación JWT y refresh tokens, un **modelo de datos** consistente con validaciones de integridad, y un conjunto de **módulos de gestión** (catálogo, clientes, ventas, usuarios y dashboard) con una interfaz clara.

Destacan como valores agregados la **integración con datos reales** (RENIEC vía apiperu.dev), la **generación de reportes en PDF**, el **almacenamiento de imágenes en el proyecto** y un **dashboard** con indicadores del negocio. La arquitectura por capas y el manejo centralizado de errores hacen que el sistema sea ordenado, comprensible y fácil de mantener y ampliar.

## 15. Anexos

**Credenciales de acceso:** usuario `admin123` · contraseña `gamerstore123`.

**Estructura del backend (por capas):**
```
src/main/java/com/gamerstore/app/
  model/        (entidades = tablas)
  repository/   (acceso a datos, Spring Data JPA)
  service/      (reglas de negocio, validaciones)
  controller/   (endpoints /api/...)
  dto/  mapper/ (objetos de transferencia + conversión)
  config/       (carga de datos, seguridad JWT, web)
```

**Principales endpoints de la API:**

| Método | Ruta | Descripción |
|---|---|---|
| POST | /api/auth/login | Inicia sesión y entrega los tokens |
| POST | /api/auth/refresh | Renueva el access token |
| GET | /api/admin/dashboard | Indicadores del panel |
| GET/POST/PUT/DELETE | /api/admin/productos | Gestión de productos |
| GET/POST/PUT/DELETE | /api/admin/categorias | Gestión de categorías |
| GET/POST/PUT/DELETE | /api/admin/clientes | Gestión de clientes |
| GET | /api/admin/clientes/reniec/{dni} | Consulta de datos por DNI (RENIEC) |
| GET/POST/PUT/DELETE | /api/admin/pedidos | Gestión de pedidos |
| GET | /api/admin/pedidos/reporte.pdf | Descarga del reporte de ventas |
| GET/POST/PUT/DELETE | /api/admin/usuarios | Gestión de usuarios |
| POST | /api/admin/uploads | Subida de imágenes |
