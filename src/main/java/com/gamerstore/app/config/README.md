# config/ — Configuración de Spring Boot

Aquí van las clases que **configuran el comportamiento** de la aplicación (beans, interceptores, datos iniciales). No son lógica de negocio ni controladores.

**Archivos:**

- **`SecurityConfig.java`** — Define el `PasswordEncoder` (BCrypt). Lo inyectamos en `UsuarioService` para hashear/verificar contraseñas. Es nuestra única pieza de seguridad — no usamos Spring Security completo, solo el encoder.

- **`AdminInterceptor.java`** — Implementa `HandlerInterceptor`. Antes de cualquier petición a `/admin/**`, verifica que la sesión tenga `rol = ADMIN`. Si no, redirige a `/auth/login`.

- **`WebConfig.java`** — Registra el `AdminInterceptor` con Spring para que se aplique a las rutas `/admin/**`.

- **`DataSeeder.java`** — Implementa `CommandLineRunner`. Se ejecuta automáticamente **una sola vez al arrancar la app** y siembra datos iniciales: 7 categorías, 12 productos, 5 clientes demo, y el admin por defecto (`admin123` / `gamerstore123`) con su password ya hasheado con BCrypt.

**Por qué están separados:**
Mantener la configuración aparte del código de negocio hace que sea fácil ver "¿cómo arranca esta app?" abriendo solo esta carpeta.
