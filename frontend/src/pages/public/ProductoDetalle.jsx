import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { PublicAPI } from '../../api/endpoints.js'
import { useConfig } from '../../config/ConfigContext.jsx'
import { money, waUrl, sku } from '../../utils/format.js'
import ProductCard from '../../components/public/ProductCard.jsx'
import Spinner from '../../components/ui/Spinner.jsx'

export default function ProductoDetalle() {
  const { id } = useParams()
  const { whatsappNumero } = useConfig()
  const [data, setData] = useState(null)
  const [error, setError] = useState(false)

  useEffect(() => {
    setData(null)
    setError(false)
    window.scrollTo(0, 0)
    PublicAPI.producto(id)
      .then(setData)
      .catch(() => setError(true))
  }, [id])

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

  if (!data) {
    return (
      <section className="section container">
        <Spinner />
      </section>
    )
  }

  const { producto: p, relacionados } = data
  const mensaje = `Hola GamerStore, me interesa cotizar el producto *${p.nombre}* (SKU ${sku(p.id)}). ¿Está disponible?`

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

          <a href={waUrl(whatsappNumero, mensaje)} target="_blank" rel="noreferrer" className="btn btn-whatsapp btn-lg btn-block">
            <i className="bi bi-whatsapp" /> Cotizar por WhatsApp
          </a>
          <p className="text-center text-muted" style={{ fontSize: '0.85rem', marginTop: '0.5rem' }}>
            <i className="bi bi-shield-check" /> Respuesta inmediata · Asesor gamer · Sin compromiso
          </p>
        </div>
      </div>

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
