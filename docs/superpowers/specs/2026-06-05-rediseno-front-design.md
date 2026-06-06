# Rediseño del frontend de GamerStore (mejora visual + UX)

**Fecha:** 2026-06-05 · **Estado:** Aprobado

## Objetivo

Elevar todo el front React a nivel "premium" (estilo claro tipo Stripe/Linear),
con foco en el **panel admin**. Solo cambia el frontend; la API/JWT no se tocan.

**Restricción clave:** el código debe quedar **simple y fácil de explicar**
(los compañeros del usuario exponen el proyecto sin haberlo construido). Componentes
claros, de propósito único, comentados en español. Nada de abstracciones rebuscadas.

## Decisiones acordadas

- Estilo: **claro premium** (no oscuro). Mismo tema en tienda y admin.
- Mejoras admin (todas): tablas pro, toasts + confirmaciones, gráficos, detalles premium.

## Qué se hace

**1. Sistema de diseño (`index.css`)** — escala de tokens más fina: neutros (slate),
acento índigo con toque de gradiente, sombras tipo Linear, radios/espaciados
consistentes, foco accesible y transiciones suaves.

**2. Componentes reutilizables nuevos** (cada uno en su archivo, comentado):
- `ToastContext` + `Toaster` — notificaciones en esquina (éxito/error), auto-cierre. Hook `useToast`.
- `ConfirmContext` + hook `useConfirm()` — modal de confirmación (reemplaza `window.confirm`).
- `Skeleton` — placeholders de carga.
- `useTableControls` (hook) — búsqueda + orden + paginación sobre un arreglo.
- `Pagination` + `TableToolbar` — controles visuales que usan ese hook.

**3. Dashboard con gráficos** — dependencia **recharts**. Gráficos con datos reales
calculados en el front desde la lista de productos: "productos por categoría" (barras)
y "estado de stock" (dona: ok/bajo/agotado). Stat-cards refinadas + lista de stock bajo.

**4. Páginas**
- Admin: Dashboard (charts + KPIs); Productos/Categorías/Clientes con tablas pro
  (buscador, orden, paginación), toasts, confirmación modal y skeletons.
- Público: Navbar, Footer, Home (hero con gradiente), Catálogo (filtros + skeletons),
  Detalle y Contacto — pulidos al mismo nivel.
- Sidebar admin colapsable (recuerda la preferencia en localStorage).

**5. Sin cambios en backend** ni en la lógica de negocio.

## Verificación

`npm run build` debe pasar; las pruebas de integración del backend siguen verdes;
se reinicia el backend para ver el nuevo look en http://localhost:8080.

## Fuera de alcance

Backend, módulo de pedidos/POS, modo oscuro con switch.
