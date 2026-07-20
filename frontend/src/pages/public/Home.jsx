import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { PublicAPI } from '../../api/endpoints.js'
import ProductCard from '../../components/public/ProductCard.jsx'
import ProductGridSkeleton from '../../components/public/ProductGridSkeleton.jsx'

// Página de inicio pública: hero de bienvenida, razones para comprar y productos destacados
export default function Home() {
  const [destacados, setDestacados] = useState(null)

  // Al montar, trae los productos y muestra solo los primeros 8 como "destacados"
  useEffect(() => {
    PublicAPI.productos()
      .then((list) => setDestacados(list.slice(0, 8)))
      .catch(() => setDestacados([]))
  }, [])

  return (
    <>
      {/* HERO */}
      <section className="hero">
        <div className="container hero-grid">
          <div>
            <span className="eyebrow">
              <i className="bi bi-lightning-charge-fill" /> Tienda gamer en Perú
            </span>
            <h1>
              Lleva tu setup al <span className="accent">siguiente nivel</span>
            </h1>
            <p className="lead">
              Consolas, periféricos pro y monitores 240Hz. Compra online y recibe en 24-48h.
            </p>
            <div className="hero-actions">
              <Link to="/productos" className="btn btn-primary">
                <i className="bi bi-grid-3x3-gap-fill" /> Explorar catálogo
              </Link>
            </div>
          </div>
          <div>
            <div className="hero-visual">
              <i className="bi bi-controller" />
            </div>
          </div>
        </div>
      </section>

      {/* FEATURES */}
      <section className="section container">
        <h2 className="section-title">Por qué GamerStore</h2>
        <div className="feature-grid">
          <div className="feature">
            <div className="feature-icon"><i className="bi bi-lightning-charge-fill" /></div>
            <h3>Envío Express</h3>
            <p>Recibe tu equipo en 24-48h en Lima y 3 días en provincia.</p>
          </div>
          <div className="feature">
            <div className="feature-icon"><i className="bi bi-shield-fill-check" /></div>
            <h3>Productos originales</h3>
            <p>Solo trabajamos con marcas oficiales. Garantía real respaldada.</p>
          </div>
          <div className="feature">
            <div className="feature-icon"><i className="bi bi-credit-card-2-front-fill" /></div>
            <h3>Pago 100% online</h3>
            <p>Paga con Yape o tarjeta y recibe tu boleta al instante.</p>
          </div>
        </div>
      </section>

      {/* DESTACADOS */}
      <section className="section" style={{ borderTop: '1px solid var(--border)', borderBottom: '1px solid var(--border)' }}>
        <div className="container">
          <h2 className="section-title">Productos destacados</h2>
          {destacados === null ? (
            <ProductGridSkeleton count={8} cols="grid-4" />
          ) : (
            <div className="product-grid grid-4">
              {destacados.map((p) => (
                <ProductCard key={p.id} p={p} />
              ))}
            </div>
          )}
          <div className="text-center" style={{ marginTop: '2.5rem' }}>
            <Link to="/productos" className="btn btn-outline">
              <i className="bi bi-arrow-right-circle" /> Ver todo el catálogo
            </Link>
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="section container">
        <div
          className="text-center"
          style={{
            background: 'var(--surface)',
            border: '1px solid var(--border)',
            borderRadius: 'var(--radius)',
            padding: '3rem 1.5rem',
            boxShadow: 'var(--shadow-sm)',
          }}
        >
          <div className="feature-icon" style={{ background: 'var(--accent-soft)', color: 'var(--accent)' }}>
            <i className="bi bi-controller" />
          </div>
          <h3 style={{ fontSize: '1.5rem' }}>¿Listo para armar tu setup?</h3>
          <p className="text-muted" style={{ margin: '0.5rem 0 1.5rem' }}>
            Explora el catálogo completo y compra en línea con pago seguro.
          </p>
          <Link to="/productos" className="btn btn-primary btn-lg">
            <i className="bi bi-grid-3x3-gap-fill" /> Explorar catálogo
          </Link>
        </div>
      </section>
    </>
  )
}
