# repository/ — Patrón Repository (acceso a datos)

**Interfaces** (no clases) que extienden `JpaRepository<Entidad, Long>`. Spring Data JPA genera la implementación automáticamente — solo declaramos los métodos.

**Lo que nos da gratis al extender `JpaRepository`:**
- `save(entidad)` → INSERT o UPDATE
- `findAll()` → SELECT *
- `findById(id)` → SELECT WHERE id = ?
- `deleteById(id)` → DELETE WHERE id = ?
- `count()` → SELECT COUNT(*)

**Archivos:**
- `ProductoRepository.java` — incluye métodos de búsqueda con filtros.
- `CategoriaRepository.java`
- `ClienteRepository.java` — incluye `findByDni(String)` y `existsByDni(String)`.
- `UsuarioRepository.java` — incluye `findByUsername(String)`.
- `PedidoRepository.java` — incluye queries de agregación (totales, top productos).

**Truco:** Spring Data lee el **nombre del método** y genera la consulta. Por ejemplo, `findByDni(String dni)` genera `SELECT * FROM cliente WHERE dni = ?`. **No escribimos SQL.**
