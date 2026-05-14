# GamerStore

Tienda online gaming con panel administrativo. Proyecto del curso Marcos de Desarrollo Web (UTP).

## Tecnologías

- Java 17
- Spring Boot 3.5.6
- Spring Web (MVC)
- Spring Data JPA + Hibernate
- Thymeleaf
- Bootstrap 5
- MariaDB (XAMPP)
- Maven
- BCrypt para passwords

## Cómo correr el proyecto

1. Iniciar XAMPP (Apache + MySQL).
2. En la terminal, dentro de la carpeta del proyecto:

```
./mvnw spring-boot:run
```

3. Abrir el navegador en `http://localhost:8080`.

La base de datos `tienda_pc` se crea sola al primer arranque y se llena con datos de ejemplo (categorías, productos, clientes).

## Acceso al panel admin

- URL: `http://localhost:8080/auth/login`
- Usuario: `admin123`
- Contraseña: `gamerstore123`

## Estructura del proyecto

```
gamerstore/
├── pom.xml                        Dependencias Maven
├── tienda_pc.sql                  Script SQL inicial
└── src/main/
    ├── java/com/gamerstore/app/
    │   ├── controller/            Controladores HTTP (MVC: C)
    │   ├── model/                 Entidades JPA (MVC: M)
    │   ├── repository/            Interfaces Spring Data JPA
    │   ├── service/               Lógica de negocio
    │   └── config/                Configuración (BCrypt, seeder)
    └── resources/
        ├── application.properties Configuración de BD
        ├── static/css/            Estilos
        └── templates/             Vistas Thymeleaf (MVC: V)
```

## Módulos del sistema

**Zona pública** (sin login):

- `/` — Landing con productos destacados
- `/productos` — Catálogo con filtros
- `/productos/{id}` — Detalle de producto con botón WhatsApp
- `/contacto` — Información de contacto

**Panel ERP** (requiere login admin):

- `/admin` — Dashboard con KPIs
- `/admin/productos` — CRUD de productos
- `/admin/categorias` — CRUD de categorías
- `/admin/clientes` — CRUD de clientes

## Base de datos

Tablas principales:

- `usuario` — admin del ERP
- `cliente` — clientes de la tienda
- `categoria` — categorías de productos
- `producto` — catálogo
- `pedido` y `pedido_item` — pendientes para el siguiente avance

Para ver las tablas: `http://localhost/phpmyadmin` → base `tienda_pc`.

## Configuración

Editar `src/main/resources/application.properties` si necesitas cambiar la URL o credenciales de la BD.

Valores por defecto (XAMPP):

```
spring.datasource.url=jdbc:mysql://localhost:3306/tienda_pc?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=
```
