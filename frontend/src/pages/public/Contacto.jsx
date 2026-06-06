import { useConfig } from '../../config/ConfigContext.jsx'
import { waUrl } from '../../utils/format.js'

const info = [
  { icon: 'bi-geo-alt-fill', label: 'Ubicación', value: 'Lima, Perú', sub: 'Atención presencial con cita previa' },
  { icon: 'bi-envelope-fill', label: 'Email', value: 'hola@gamerstore.gg', sub: 'Respuesta en 24h hábiles' },
  { icon: 'bi-clock-fill', label: 'Horario', value: 'Lun-Sáb · 9:00 AM - 8:00 PM', sub: 'Domingos cerrado' },
  { icon: 'bi-truck', label: 'Envíos', value: 'Todo Perú', sub: '24-48h en Lima · 3 días provincia' },
]

const pasos = [
  { n: '1', t: 'Elige tu producto', d: 'Explora el catálogo y elige el equipo que necesitas.' },
  { n: '2', t: 'Cotiza por WhatsApp', d: 'Haz click en "Cotizar" y nuestro asesor te confirma stock y precio final.' },
  { n: '3', t: 'Paga y recibe', d: 'Coordinamos pago, envío o recojo en tienda. ¡Listo para jugar!' },
]

export default function Contacto() {
  const { whatsappNumero } = useConfig()

  return (
    <>
      <section className="hero" style={{ padding: '3.5rem 0 2rem' }}>
        <div className="container text-center">
          <span className="eyebrow">
            <i className="bi bi-headset" /> Estamos aquí para ayudarte
          </span>
          <h1 style={{ fontSize: '2.5rem', margin: '1rem auto', maxWidth: 700 }}>
            Hablemos del <span className="accent">setup ideal</span>
          </h1>
          <p className="lead" style={{ margin: '0 auto' }}>
            La forma más rápida de comprar en GamerStore es por WhatsApp. Te asesoramos, cotizamos y
            coordinamos entrega o recojo en tienda.
          </p>
        </div>
      </section>

      <section className="section container">
        <div className="detail-grid">
          {/* WhatsApp */}
          <div className="spec-card text-center" style={{ margin: 0 }}>
            <div className="feature-icon" style={{ background: 'var(--success-soft)', color: 'var(--whatsapp)' }}>
              <i className="bi bi-whatsapp" />
            </div>
            <h3 style={{ fontSize: '1.3rem' }}>WhatsApp</h3>
            <p className="text-muted">Asesor gamer en línea. Respuesta inmediata.</p>
            <h4 style={{ margin: '0.75rem 0 1rem' }}>+51 986 969 024</h4>
            <a
              href={waUrl(whatsappNumero, 'Hola GamerStore, quiero información sobre sus productos')}
              target="_blank"
              rel="noreferrer"
              className="btn btn-whatsapp btn-lg"
            >
              <i className="bi bi-whatsapp" /> Abrir chat
            </a>
          </div>

          {/* Info */}
          <div className="spec-card" style={{ margin: 0 }}>
            <h3 style={{ fontSize: '1.2rem', marginBottom: '1.25rem' }}>
              <i className="bi bi-shop" style={{ color: 'var(--accent)' }} /> Información
            </h3>
            {info.map((item) => (
              <div key={item.label} style={{ display: 'flex', gap: '0.85rem', marginBottom: '1rem' }}>
                <div className="stat-icon" style={{ background: 'var(--accent-soft)', color: 'var(--accent)', width: 40, height: 40, fontSize: '1rem' }}>
                  <i className={'bi ' + item.icon} />
                </div>
                <div>
                  <small className="text-muted">{item.label}</small>
                  <h6 style={{ color: 'var(--text-strong)' }}>{item.value}</h6>
                  <small className="text-muted">{item.sub}</small>
                </div>
              </div>
            ))}
          </div>
        </div>

        <div style={{ marginTop: '3rem' }}>
          <h2 className="section-title">¿Cómo comprar?</h2>
          <div className="feature-grid">
            {pasos.map((p) => (
              <div key={p.n} className="feature">
                <div className="feature-icon">{p.n}</div>
                <h3>{p.t}</h3>
                <p>{p.d}</p>
              </div>
            ))}
          </div>
        </div>
      </section>
    </>
  )
}
