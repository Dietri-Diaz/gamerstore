import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { PublicAPI } from '../../api/endpoints.js'
import { useCarrito } from '../../carrito/CarritoContext.jsx'
import { useToast } from '../../components/ui/Toast.jsx'
import { money, sku } from '../../utils/format.js'
import ProductCard from '../../components/public/ProductCard.jsx'
import Spinner from '../../components/ui/Spinner.jsx'

// Página de detalle de un producto público: info completa, especificaciones y productos relacionados
export default function ProductoDetalle() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { agregar } = useCarrito()
  const toast = useToast()
  const [data, setData] = useState(null)
  const [error, setError] = useState(false)
  const [cantidad, setCantidad] = useState(1)

  // Carga el producto (y sus relacionados) cada vez que cambia el id en la URL
  useEffect(() => {
    setData(null)
    setError(false)
    setCantidad(1)
    window.scrollTo(0, 0)
    PublicAPI.producto(id)
      .then(setData)
      .catch(() => setError(true))
  }, [id])

  // Si el producto no existe o falló la carga, mostramos un estado de "no encontrado"
  if (error) {
    return (
      <section className="section container">
        <div className="empty">
          <i className="bi bi-emoji-frown" />
          <h3>Producto no encontrado</h3>
          <Link to="/productos" className="btn btn-outline" style={{ marginTop: '1rem' }}>
            Volver al catálogo
          </Link>
        </div>
      </section>
    )
  }

  // Mientras no lleguen los datos del producto, mostramos un spinner de carga
  if (!data) {
    return (
      <section className="section container">
        <Spinner />
      </section>
    )
  }

  const { producto: p, relacionados } = data
  const sinStock = p.stock === 0

  // Agrega el producto (con la cantidad elegida) al carrito y avisa con un toast
  const handleAgregar = () => {
    agregar(p, cantidad)
    toast.success('Agregado al carrito')
  }

  // Agrega el producto y va directo al checkout, sin quedarse en esta página
  const handleComprarAhora = () => {
    agregar(p, cantidad)
    navigate('/checkout')
  }

  return (
    <section className="section container">
      <nav className="breadcrumb">
        <Link to="/">Inicio</Link>
        <span className="sep">/</span>
        <Link to="/productos">Catálogo</Link>
        {p.categoriaNombre && (
          <>
            <span className="sep">/</span>
            <Link to={`/productos?categoria=${encodeURIComponent(p.categoriaNombre)}`}>
              {p.categoriaNombre}
            </Link>
          </>
        )}
        <span className="sep">/</span>
        <span>{p.nombre}</span>
      </nav>

      <div className="detail-grid">
        <div className="detail-img-wrap">
          <img className="detail-img" src={p.imagen} alt={p.nombre} />
        </div>

        <div>
          {p.categoriaNombre && <span className="badge badge-cat">{p.categoriaNombre}</span>}
          <h1 style={{ fontSize: '2rem', margin: '0.75rem 0' }}>{p.nombre}</h1>

          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '1rem' }}>
            <span className="detail-price">{money(p.precio)}</span>
            {p.stock > 0 ? (
              <span className="badge badge-ok">
                <i className="bi bi-check-circle-fill" /> En stock
              </span>
            ) : (
              <span className="badge badge-warn">
                <i className="bi bi-exclamation-triangle-fill" /> Agotado
              </span>
            )}
          </div>

          <p className="text-muted" style={{ marginBottom: '1rem' }}>{p.descripcion}</p>

          {/* Selector de cantidad y acciones de compra */}
          <div className="detail-buy">
            <div className="qty-selector">
              <label className="label">Cantidad</label>
              <input
                className="input qty-input"
                type="number"
                min={1}
                max={p.stock}
                value={cantidad}
                disabled={sinStock}
                onChange={(e) => setCantidad(Math.max(1, Math.min(Number(e.target.value) || 1, p.stock)))}
              />
            </div>
            <div className="detail-buy-actions">
              <button className="btn btn-outline btn-lg" disabled={sinStock} onClick={handleAgregar}>
                <i className="bi bi-cart-plus" /> {sinStock ? 'Sin stock' : 'Agregar al carrito'}
              </button>
              <button className="btn btn-primary btn-lg" disabled={sinStock} onClick={handleComprarAhora}>
                <i className="bi bi-lightning-charge-fill" /> {sinStock ? 'Sin stock' : 'Comprar ahora'}
              </button>
            </div>
          </div>

          <div className="spec-card">
            <h5 style={{ color: 'var(--accent)', marginBottom: '1rem' }}>
              <i className="bi bi-info-circle" /> Especificaciones
            </h5>
            <div className="spec-grid">
              <div>
                <small>Categoría</small>
                <strong>{p.categoriaNombre || '—'}</strong>
              </div>
              <div>
                <small>Stock disponible</small>
                <strong>{p.stock} unidades</strong>
              </div>
              <div>
                <small>SKU</small>
                <strong>{sku(p.id)}</strong>
              </div>
              <div>
                <small>Envío</small>
                <strong style={{ color: 'var(--success)' }}>24-48h Lima</strong>
              </div>
            </div>
          </div>

          <p className="text-center text-muted" style={{ fontSize: '0.85rem', marginTop: '0.5rem' }}>
            <i className="bi bi-shield-check" /> Compra segura · Pago con Yape o tarjeta · Boleta automática
          </p>
        </div>
      </div>

      {/* Sección de productos relacionados, solo se muestra si el backend devolvió alguno */}
      {relacionados.length > 0 && (
        <div style={{ marginTop: '3rem', paddingTop: '2rem', borderTop: '1px solid var(--border)' }}>
          <h2 className="section-title">También te puede interesar</h2>
          <div className="product-grid grid-4">
            {relacionados.map((r) => (
              <ProductCard key={r.id} p={r} />
            ))}
          </div>
        </div>
      )}
    </section>
  )
}
