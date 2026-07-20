import { Link, useNavigate } from 'react-router-dom'
import { useCarrito } from '../../carrito/CarritoContext.jsx'
import { money } from '../../utils/format.js'

// Página del carrito de compras: lista los productos elegidos, permite ajustar
// cantidades (sin superar el stock) o quitarlos, y muestra el total antes de pasar al checkout.
export default function Carrito() {
  const { items, cambiarCantidad, quitar, total } = useCarrito()
  const navigate = useNavigate()

  // Carrito vacío: invita a volver al catálogo
  if (items.length === 0) {
    return (
      <section className="section container">
        <div className="empty">
          <i className="bi bi-cart-x" />
          <h3>Tu carrito está vacío</h3>
          <p>Agrega productos desde el catálogo para continuar.</p>
          <Link to="/productos" className="btn btn-primary" style={{ marginTop: '1rem' }}>
            <i className="bi bi-grid-3x3-gap-fill" /> Ver catálogo
          </Link>
        </div>
      </section>
    )
  }

  return (
    <section className="section container">
      <div style={{ marginBottom: '1.5rem' }}>
        <span className="eyebrow">
          <i className="bi bi-cart3" /> Tu carrito
        </span>
        <h1 style={{ fontSize: '1.8rem', marginTop: '0.75rem' }}>
          Revisa tu <span className="accent">pedido</span>
        </h1>
      </div>

      <div className="carrito-list">
        {items.map((i) => (
          <div className="carrito-item" key={i.id}>
            <img className="carrito-item-img" src={i.imagen} alt={i.nombre} />

            <div className="carrito-item-info">
              <strong>{i.nombre}</strong>
              <span className="text-muted">{money(i.precio)} c/u</span>
            </div>

            <div className="carrito-item-qty">
              <button
                type="button"
                className="btn btn-outline btn-icon"
                onClick={() => cambiarCantidad(i.id, i.cantidad - 1)}
                aria-label="Quitar una unidad"
              >
                <i className="bi bi-dash" />
              </button>
              <span>{i.cantidad}</span>
              <button
                type="button"
                className="btn btn-outline btn-icon"
                disabled={i.cantidad >= i.stock}
                onClick={() => cambiarCantidad(i.id, i.cantidad + 1)}
                aria-label="Agregar una unidad"
              >
                <i className="bi bi-plus" />
              </button>
            </div>

            <div className="carrito-item-subtotal">{money(i.precio * i.cantidad)}</div>

            <button
              type="button"
              className="btn btn-danger btn-icon"
              onClick={() => quitar(i.id)}
              title="Quitar del carrito"
            >
              <i className="bi bi-trash" />
            </button>
          </div>
        ))}
      </div>

      <div className="carrito-resumen">
        <div className="carrito-total">
          <span>Total</span>
          <strong>{money(total)}</strong>
        </div>
        <div className="carrito-resumen-actions">
          <Link to="/productos" className="btn btn-outline">
            <i className="bi bi-arrow-left" /> Seguir comprando
          </Link>
          <button type="button" className="btn btn-primary btn-lg" onClick={() => navigate('/checkout')}>
            Continuar compra <i className="bi bi-arrow-right" />
          </button>
        </div>
      </div>
    </section>
  )
}
