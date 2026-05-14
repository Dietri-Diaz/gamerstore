# controller/ — Capa C del MVC

Aquí están los **controladores HTTP**. Reciben las peticiones del navegador (`@GetMapping`, `@PostMapping`), llaman a la capa `service/` para hacer la lógica, y retornan el nombre de una vista Thymeleaf de `resources/templates/`.

**Archivos:**
- `HomeController.java` — zona pública (landing, catálogo, detalle, contacto, login).
- `AdminController.java` — zona ERP (dashboard + CRUDs de productos, categorías y clientes).

**Patrón:**
```java
@GetMapping("/ruta")
public String metodo(Model model) {
    model.addAttribute("datos", servicio.metodo());
    return "nombre-template";  // → resources/templates/nombre-template.html
}
```
