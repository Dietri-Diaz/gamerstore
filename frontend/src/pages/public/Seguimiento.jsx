import { useState } from 'react'
import { Link } from 'react-router-dom'
import { PublicAPI } from '../../api/endpoints.js'
import { downloadBlob } from '../../api/client.js'
import { useToast } from '../../components/ui/Toast.jsx'
import { useAutoClear } from '../../hooks/useAutoClear.js'
import { money } from '../../utils/format.js'
import Alert from '../../components/ui/Alert.jsx'

// Clase de color del badge segun el estado del pedido (mismos colores que el panel admin,
// para que el cliente y el vendedor vean el pedido "del mismo color").
function badgeEstado(estado) {
  if (estado === 'PAGADO' || estado === 'ENTREGADO') return 'badge badge-ok'
  if (estado === 'ANULADO' || estado === 'CANCELADO') return 'badge badge-danger'
  if (estado === 'ENVIADO') return 'badge badge-accent'
  return 'badge badge-warn' // PENDIENTE
}

// Pagina publica de seguimiento: el comprador escribe el codigo de su pedido (PED-0047)
// y su DNI, y ve en que punto del proceso esta. No requiere login: el DNI hace de
// "contrasena" del pedido, igual que en la descarga de la boleta.
export default function Seguimiento() {
  const toast = useToast()

  const [codigo, setCodigo] = useState('')
  const [dni, setDni] = useState('')
  const [buscando, setBuscando] = useState(false)
  const [error, setError] = useState('')
  const [pedido, setPedido] = useState(null) // SeguimientoDTO devuelto por la API
  const [descargando, setDescargando] = useState(false)

  // El mensaje de error se borra solo a los 5s (mismo comportamiento que el resto del proyecto).
  useAutoClear(error, setError)

  // Consulta el pedido en la API. El backend responde el MISMO 404 tanto si el codigo
  // no existe como si el DNI no coincide, asi que aqui solo mostramos su mensaje:
  // nunca revelamos si un codigo ajeno existe o no.
  const buscar = async (e) => {
    e.preventDefault()
    setError('')
    if (codigo.trim() === '' || !/^\d{8}$/.test(dni)) {
      setError('Ingresa el código de tu pedido y tu DNI de 8 dígitos')
      return
    }
    setBuscando(true)
    setPedido(null)
    try {
      const data = await PublicAPI.seguimiento(codigo.trim(), dni)
      setPedido(data)
    } catch (err) {
      // 404 = no encontrado (o DNI que no corresponde): mensaje amable y sin pistas.
      setError(
        err.status === 404
          ? 'No encontramos un pedido con ese código y DNI. Revisa que estén tal cual te los dimos al comprar.'
          : err.message
      )
    } finally {
      setBuscando(false)
    }
  }

  // Descarga la boleta del pedido consultado (el backend la verifica otra vez con el DNI)
  const descargarBoleta = async () => {
    setDescargando(true)
    try {
      await downloadBlob(PublicAPI.boletaUrl(pedido.codigo, dni), 'boleta-' + pedido.comprobanteCodigo + '.pdf')
    } catch (err) {
      toast.error(err.message)
    } finally {
      setDescargando(false)
    }
  }

  const anulado = pedido && pedido.estado === 'ANULADO'

  return (
    <section className="section container">
      <div style={{ marginBottom: '1.5rem' }}>
        <span className="eyebrow">
          <i className="bi bi-truck" /> Seguimiento
        </span>
        <h1 style={{ fontSize: '1.8rem', marginTop: '0.75rem' }}>
          ¿Dónde está mi <span className="accent">pedido</span>?
        </h1>
      </div>

      {/* Formulario de consulta: codigo del pedido + DNI del comprador */}
      <div className="panel seguimiento-form">
        <div className="panel-head">
          <h5>
            <i className="bi bi-search" style={{ color: 'var(--accent)' }} /> Consulta tu pedido
          </h5>
        </div>

        {error && <Alert type="error">{error}</Alert>}

        <form onSubmit={buscar}>
          <div className="form-grid">
            <div className="field">
              <label className="label">Código del pedido *</label>
              <input
                className="input"
                value={codigo}
                onChange={(e) => setCodigo(e.target.value.toUpperCase())}
                maxLength={20}
                placeholder="Ej. PED-0047"
              />
            </div>
            <div className="field">
              <label className="label">DNI del comprador *</label>
              <input
                className="input"
                value={dni}
                onChange={(e) => setDni(e.target.value.replace(/\D/g, '').slice(0, 8))}
                maxLength={8}
                placeholder="8 dígitos"
              />
            </div>
          </div>

          <button type="submit" className="btn btn-primary" disabled={buscando}>
            <i className="bi bi-search" /> {buscando ? 'Buscando...' : 'Buscar mi pedido'}
          </button>
        </form>

        <p className="text-muted" style={{ marginTop: '0.9rem', fontSize: '0.82rem' }}>
          El código te lo dimos al terminar la compra (también está en tu boleta).
        </p>
      </div>

      {/* Resultado: solo aparece cuando la API devolvió un pedido */}
      {pedido && (
        <div className="seguimiento-resultado">
          {/* Pedido anulado: se avisa arriba de todo y en rojo, antes que cualquier otro dato */}
          {anulado && (
            <div className="seguimiento-anulado">
              <i className="bi bi-x-octagon-fill" />
              <div>
                <strong>Este pedido fue anulado</strong>
                <span>{pedido.motivoAnulacion || 'Sin motivo registrado'}</span>
                <small>Si ya habías pagado, el dinero se devuelve al medio de pago que usaste.</small>
              </div>
            </div>
          )}

          {/* Cabecera con los datos generales del pedido */}
          <div className="panel">
            <div className="seguimiento-cabecera">
              <div>
                <span className="seguimiento-codigo">{pedido.codigo}</span>
                <span className="text-muted" style={{ fontSize: '0.85rem' }}>
                  {pedido.fecha ? new Date(pedido.fecha).toLocaleString('es-PE') : '—'}
                </span>
              </div>
              <span className={badgeEstado(pedido.estado)}>{pedido.estado}</span>
            </div>

            <div className="detalle-datos-grid">
              <div className="detalle-dato">
                <span>Cliente</span>
                <strong>{pedido.clienteNombre}</strong>
              </div>
              <div className="detalle-dato">
                <span>Método de pago</span>
                <strong>{pedido.metodoPago || '—'}</strong>
              </div>
              <div className="detalle-dato">
                <span>Total</span>
                <strong style={{ color: 'var(--accent)' }}>{money(pedido.total)}</strong>
              </div>
              {pedido.comprobanteCodigo && (
                <div className="detalle-dato">
                  <span>Boleta</span>
                  <strong>{pedido.comprobanteCodigo}</strong>
                </div>
              )}
            </div>
          </div>

          {/* Linea de tiempo vertical: los pasos vienen ya calculados del backend
              (no hay tabla de historial, se derivan del estado actual del pedido). */}
          <div className="panel">
            <div className="panel-head">
              <h5>
                <i className="bi bi-signpost-split" style={{ color: 'var(--accent)' }} /> Estado de tu pedido
              </h5>
            </div>

            <ol className="linea-tiempo">
              {(pedido.historial || []).map((paso, i) => (
                <li
                  key={i}
                  className={
                    'lt-paso' +
                    (paso.completado ? ' done' : '') +
                    (paso.actual ? ' actual' : '') +
                    (paso.estado === 'ANULADO' || paso.estado === 'CANCELADO' ? ' cancel' : '')
                  }
                >
                  <span className="lt-punto">
                    {paso.estado === 'ANULADO' || paso.estado === 'CANCELADO' ? (
                      <i className="bi bi-x-lg" />
                    ) : paso.completado ? (
                      <i className="bi bi-check-lg" />
                    ) : (
                      <i className="bi bi-circle" />
                    )}
                  </span>
                  <div className="lt-cuerpo">
                    <strong>{paso.titulo}</strong>
                    {paso.descripcion && <small>{paso.descripcion}</small>}
                    {paso.actual && <span className="lt-actual">Aquí estás</span>}
                  </div>
                </li>
              ))}
            </ol>
          </div>

          {/* Tipo de entrega: si es delivery se muestra a dónde va; si es recojo, no hay dirección */}
          <div className="panel">
            <div className="panel-head">
              <h5>
                <i className={'bi ' + (pedido.tipoEntrega === 'DELIVERY' ? 'bi-truck' : 'bi-shop')} style={{ color: 'var(--accent)' }} />{' '}
                {pedido.tipoEntrega === 'DELIVERY' ? 'Envío a domicilio' : 'Recojo en tienda'}
              </h5>
            </div>

            {pedido.tipoEntrega === 'DELIVERY' ? (
              <div className="detalle-datos-grid">
                <div className="detalle-dato">
                  <span>Dirección de envío</span>
                  <strong>{pedido.direccionEnvio || '—'}</strong>
                </div>
                {pedido.referenciaEnvio && (
                  <div className="detalle-dato">
                    <span>Referencia</span>
                    <strong>{pedido.referenciaEnvio}</strong>
                  </div>
                )}
              </div>
            ) : (
              <p className="detalle-vacio">
                Recoge tu pedido en nuestra tienda presentando tu DNI y el código <strong>{pedido.codigo}</strong>.
              </p>
            )}
          </div>

          {/* Ítems comprados */}
          <div className="panel">
            <div className="panel-head">
              <h5>
                <i className="bi bi-bag" style={{ color: 'var(--accent)' }} /> Productos
              </h5>
            </div>

            <div className="table-wrap">
              <div className="table-scroll">
                <table className="table">
                  <thead>
                    <tr>
                      <th>Producto</th>
                      <th>Cantidad</th>
                      <th>P. Unit.</th>
                      <th>Subtotal</th>
                    </tr>
                  </thead>
                  <tbody>
                    {(pedido.items || []).map((it, i) => (
                      <tr key={i}>
                        <td>{it.producto}</td>
                        <td>{it.cantidad}</td>
                        <td>{money(it.precioUnitario)}</td>
                        <td>{money(it.subtotal)}</td>
                      </tr>
                    ))}
                    <tr>
                      <td colSpan={3} style={{ textAlign: 'right' }}><strong>Total</strong></td>
                      <td><strong>{money(pedido.total)}</strong></td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          {/* Acciones finales: la boleta solo se ofrece si el backend emitió una para este pedido */}
          <div className="compra-ok-actions">
            {pedido.comprobanteCodigo && (
              <button type="button" className="btn btn-primary" onClick={descargarBoleta} disabled={descargando}>
                <i className="bi bi-file-earmark-pdf" /> {descargando ? 'Generando...' : 'Descargar boleta'}
              </button>
            )}
            <Link to="/productos" className="btn btn-outline">
              <i className="bi bi-grid-3x3-gap-fill" /> Seguir comprando
            </Link>
          </div>
        </div>
      )}
    </section>
  )
}
