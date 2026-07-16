import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { PublicAPI } from '../../api/endpoints.js'
import { useConfig } from '../../config/ConfigContext.jsx'
import { waUrl } from '../../utils/format.js'
import ProductCard from '../../components/public/ProductCard.jsx'
import ProductGridSkeleton from '../../components/public/ProductGridSkeleton.jsx'

// Página de inicio pública: hero de bienvenida, razones para comprar y productos destacados
export default function Home() {
  const { whatsappNumero } = useConfig()
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
              Consolas, periféricos pro y monitores 240Hz. Cotiza por WhatsApp y recibe en 24-48h.
            </p>
            <div className="hero-actions">
              <Link to="/productos" className="btn btn-primary">
                <i className="bi bi-grid-3x3-gap-fill" /> Explorar catálogo
              </Link>
              <a
                href={waUrl(whatsappNumero, 'Hola GamerStore, quiero cotizar un producto')}
                target="_blank"
                rel="noreferrer"
                className="btn btn-whatsapp"
              >
                <i className="bi bi-whatsapp" /> Cotizar ahora
              </a>
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
            <div className="feature-icon"><i className="bi bi-whatsapp" /></div>
            <h3>Atención personalizada</h3>
            <p>Cotiza por WhatsApp con un especialista gamer en tiempo real.</p>
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
          <div className="feature-icon" style={{ background: 'var(--success-soft)', color: 'var(--whatsapp)' }}>
            <i className="bi bi-whatsapp" />
          </div>
          <h3 style={{ fontSize: '1.5rem' }}>¿Listo para cotizar?</h3>
          <p className="text-muted" style={{ margin: '0.5rem 0 1.5rem' }}>
            Habla con un asesor gamer por WhatsApp. Te ayudamos a armar tu setup ideal.
          </p>
          <a
            href={waUrl(whatsappNumero, 'Hola GamerStore, necesito una cotización')}
            target="_blank"
            rel="noreferrer"
            className="btn btn-whatsapp btn-lg"
          >
            <i className="bi bi-whatsapp" /> Chatear con asesor
          </a>
        </div>
      </section>
    </>
  )
}
