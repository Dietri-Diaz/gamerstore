# GamerStore ERP — Cómo ejecutar en otra PC

Guía rápida para dejar el proyecto corriendo desde cero.

## 1. Requisitos (instalar una sola vez)

- **JDK 17 o superior** (Java). Verifica con `java -version`.
- **Node.js 18 o superior** (incluye npm). Verifica con `node -v`.
- **XAMPP** (para MySQL/MariaDB). Solo se necesita el módulo **MySQL**.

> No hace falta instalar Maven: el proyecto trae el "wrapper" (`mvnw.cmd`).

## 2. Base de datos

1. Abre **XAMPP** y pulsa **Start** en **MySQL**.
2. Importa la base de datos incluida en este proyecto: el archivo **`tienda_pc.sql`** (en la raíz).
   - **Opción A — phpMyAdmin:** abre `http://localhost/phpmyadmin` → pestaña **Importar** → selecciona `tienda_pc.sql` → **Continuar**.
   - **Opción B — consola:** `C:\xampp\mysql\bin\mysql.exe -u root < tienda_pc.sql`

   Esto crea la base `tienda_pc` con TODOS los datos (28 productos, 6 clientes con nombres reales, 40 pedidos, etc.).

> **Alternativa:** si no importas el `.sql`, no pasa nada — al arrancar el backend, el sistema **crea las tablas y carga datos automáticamente**. (Pero importar el `.sql` garantiza los mismos datos reales sin depender de internet.)

## 3. Backend (API — puerto 8080)

En la carpeta raíz del proyecto, abre una terminal y ejecuta:

```
mvnw.cmd spring-boot:run
```

La **primera vez** descarga las dependencias de Maven (puede tardar unos minutos). Cuando veas `Started GamerStoreApplication`, la API está lista en `http://localhost:8080`.

## 4. Frontend (panel — puerto 5173)

En **otra** terminal, entra a la carpeta `frontend/` y ejecuta:

```
cd frontend
npm install
npm run dev
```

La **primera vez**, `npm install` descarga las dependencias (crea `node_modules/`). Luego abre `http://localhost:5173`.

## 5. Ingresar al panel

- URL: **http://localhost:5173/admin**
- Usuario: **admin123**
- Contraseña: **gamerstore123**

## Notas

- **Datos reales por DNI (RENIEC):** el sistema consulta la API de apiperu.dev. El token ya viene configurado en `src/main/resources/application.properties` (`app.apidevperu.token`). Si el token se agota o no hay internet, el sistema sigue funcionando con los datos que ya están en la base.
- **Puertos ocupados:** si el 8080 o el 5173 están en uso, cierra la aplicación que los use o cambia el puerto (8080 en `application.properties`, 5173 en `frontend/vite.config.js`).
- **Documentación del proyecto:** en la carpeta `docs/` está la guía completa de la exposición, los flujos por módulo y el informe.

## Resumen ultra rápido

```
1) XAMPP  -> Start MySQL   (e importar tienda_pc.sql)
2) raíz   -> mvnw.cmd spring-boot:run        (backend :8080)
3) frontend/ -> npm install && npm run dev   (panel :5173)
4) http://localhost:5173/admin   ->   admin123 / gamerstore123
```
