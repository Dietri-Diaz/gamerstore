# config/ — Configuración de Spring Boot

Aquí van las clases que **configuran el comportamiento** de la aplicación (beans, recursos, datos iniciales). No son lógica de negocio ni controladores.

**Archivos:**

- **`PasswordConfig.java`** — Define el `PasswordEncoder` (BCrypt). Lo inyectamos en `UsuarioService` para hashear/verificar contraseñas. Es solo el codificador, no el framework de seguridad (Spring Security se agrega en el avance final).

- **`WebConfig.java`** — Configura dos cosas: (1) **CORS**, para que el front en desarrollo (Vite, `:5173`) pueda llamar a la API en `:8080`; y (2) el **fallback de la SPA**, que reenvía las rutas del front (ej. `/admin/productos`) al `index.html` de React cuando la app va compilada dentro del backend.

- **`DataSeeder.java`** — Implementa `CommandLineRunner`. Se ejecuta automáticamente **una sola vez al arrancar la app** y siembra datos iniciales: 7 categorías, 12 productos, 5 clientes demo, y el admin por defecto (`admin123` / `gamerstore123`) con su password ya hasheado con BCrypt.

**Por qué están separados:**
Mantener la configuración aparte del código de negocio hace que sea fácil ver "¿cómo arranca esta app?" abriendo solo esta carpeta.
