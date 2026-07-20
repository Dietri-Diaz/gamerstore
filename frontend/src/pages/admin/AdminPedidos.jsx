import { useEffect, useState } from 'react'
import { AdminAPI, PagosAPI } from '../../api/endpoints.js'
import { downloadBlob } from '../../api/client.js'
import { money } from '../../utils/format.js'
import { useTableControls } from '../../hooks/useTableControls.js'
import { useAutoClear } from '../../hooks/useAutoClear.js'
import { useToast } from '../../components/ui/Toast.jsx'
import { useConfirm } from '../../components/ui/Confirm.jsx'
import Modal from '../../components/ui/Modal.jsx'
import Alert from '../../components/ui/Alert.jsx'
import TableToolbar from '../../components/ui/TableToolbar.jsx'
import TableSkeleton from '../../components/ui/TableSkeleton.jsx'
import Pagination from '../../components/ui/Pagination.jsx'
import PasarelaPago from '../../components/admin/PasarelaPago.jsx'

const ESTADOS = ['PENDIENTE', 'PAGADO', 'ENVIADO', 'ENTREGADO', 'CANCELADO', 'ANULADO']
// Solo estos dos: toda venta pasa por la pasarela (Stripe) o Yape.
const METODOS = ['TARJETA', 'YAPE']
// Límites del motivo de anulación (los mismos que valida el backend)
const MOTIVO_MIN = 5
const MOTIVO_MAX = 200

// Clase de color para el badge según el estado del pedido
function badgeEstado(estado) {
  if (estado === 'PAGADO' || estado === 'ENTREGADO') return 'badge badge-ok'
  if (estado === 'CANCELADO' || estado === 'ANULADO') return 'badge badge-danger'
  if (estado === 'ENVIADO') return 'badge badge-accent'
  return 'badge badge-warn' // PENDIENTE
}

// Página admin: gestión de pedidos (crear con ítems, editar estado/método de pago, eliminar y
// descargar un reporte en PDF con filtros de fecha/estado)
export default function AdminPedidos() {
  const toast = useToast()
  const confirm = useConfirm()

  const [pedidos, setPedidos] = useState(null)
  const [clientes, setClientes] = useState([])
  const [productos, setProductos] = useState([])

  // Modal "nuevo pedido"
  const [showCrear, setShowCrear] = useState(false)
  const [clienteId, setClienteId] = useState('')
  const [metodoPago, setMetodoPago] = useState('TARJETA')
  const [items, setItems] = useState([])
  const [draft, setDraft] = useState({ productoId: '', cantidad: 1 })
  const [formError, setFormError] = useState('')
  const [saving, setSaving] = useState(false)

  // Modal "editar estado"
  const [editing, setEditing] = useState(null)
  const [estadoEdit, setEstadoEdit] = useState('PENDIENTE')
  const [metodoEdit, setMetodoEdit] = useState('')

  // Modal "cobrar" (pasarela de pagos): guarda el pedido que se esta cobrando
  const [cobrando, setCobrando] = useState(null)

  // Modal "ver detalle": pedido + el pago con el que se cobró + su boleta
  const [detalle, setDetalle] = useState(null)
  const [cargandoDetalle, setCargandoDetalle] = useState(false)

  // Modal "anular venta": guarda el pedido que se va a anular y el motivo escrito
  const [anulando, setAnulando] = useState(null)
  const [motivoAnulacion, setMotivoAnulacion] = useState('')
  const [anulandoSaving, setAnulandoSaving] = useState(false)

  // Reporte PDF
  const [repDesde, setRepDesde] = useState('')
  const [repHasta, setRepHasta] = useState('')
  const [repEstado, setRepEstado] = useState('')
  const [descargando, setDescargando] = useState(false)

  // Arma la URL del reporte con los filtros de fecha/estado elegidos y descarga el PDF resultante
  const descargarPDF = async () => {
    setDescargando(true)
    try {
      const url = AdminAPI.reportePedidosUrl({ desde: repDesde, hasta: repHasta, estado: repEstado })
      await downloadBlob(url, 'reporte-pedidos.pdf')
    } catch (err) {
      toast.error(err.message)
    } finally {
      setDescargando(false)
    }
  }

  const t = useTableControls(pedidos || [], {
    searchKeys: ['codigo', 'clienteNombre', 'estado'],
    pageSize: 8,
    initialSort: { key: 'id', dir: 'desc' },
  })

  // El mensaje de error del formulario se borra solo a los 5s (no se queda estático).
  useAutoClear(formError, setFormError)

  const cargar = () => AdminAPI.pedidos().then(setPedidos).catch(() => setPedidos([]))

  // Al montar, carga los pedidos y también clientes/productos (necesarios para el formulario de nuevo pedido)
  useEffect(() => {
    cargar()
    AdminAPI.clientes().then(setClientes).catch(() => setClientes([]))
    AdminAPI.productos().then(setProductos).catch(() => setProductos([]))
  }, [])

  // ---- Crear pedido ----
  const abrirCrear = () => {
    setClienteId('')
    setMetodoPago('TARJETA')
    setItems([])
    setDraft({ productoId: '', cantidad: 1 })
    setFormError('')
    setShowCrear(true)
  }

  // Agrega el producto y cantidad elegidos en el "draft" a la lista de ítems del pedido en construcción
  const agregarItem = () => {
    if (!draft.productoId) return
    const prod = productos.find((p) => String(p.id) === String(draft.productoId))
    if (!prod) return
    const cantidad = Math.max(1, Number(draft.cantidad) || 1)
    setItems((lista) => [...lista, { productoId: prod.id, nombre: prod.nombre, precio: prod.precio, cantidad }])
    setDraft({ productoId: '', cantidad: 1 })
  }

  // Quita un ítem de la lista del pedido en construcción por su posición
  const quitarItem = (idx) => setItems((lista) => lista.filter((_, i) => i !== idx))

  const totalPedido = items.reduce((acc, it) => acc + it.precio * it.cantidad, 0)

  // Valida y envía el pedido nuevo (cliente + ítems) a la API
  const guardar = async (e) => {
    e.preventDefault()
    setFormError('')
    if (!clienteId) return setFormError('Selecciona un cliente')
    if (items.length === 0) return setFormError('Agrega al menos un producto')
    setSaving(true)
    try {
      await AdminAPI.crearPedido({
        clienteId: Number(clienteId),
        metodoPago,
        items: items.map((it) => ({ productoId: it.productoId, cantidad: it.cantidad })),
      })
      toast.success('Pedido registrado correctamente')
      setShowCrear(false)
      cargar()
    } catch (err) {
      setFormError(err.message)
    } finally {
      setSaving(false)
    }
  }

  // ---- Editar estado ----
  const abrirEditar = (p) => {
    setEditing(p)
    setEstadoEdit(p.estado)
    setMetodoEdit(p.metodoPago || '')
  }

  // Guarda el nuevo estado y/o método de pago del pedido que se está editando
  const guardarEstado = async (e) => {
    e.preventDefault()
    setSaving(true)
    try {
      await AdminAPI.actualizarPedido(editing.id, { estado: estadoEdit, metodoPago: metodoEdit })
      toast.success('Pedido actualizado')
      setEditing(null)
      cargar()
    } catch (err) {
      toast.error(err.message)
    } finally {
      setSaving(false)
    }
  }

  // Descarga la boleta (PDF) del pedido ya pagado.
  const descargarBoleta = async (p) => {
    try {
      await downloadBlob(AdminAPI.boletaPedidoUrl(p.id), 'boleta-' + p.codigo + '.pdf')
    } catch (err) {
      toast.error(err.message)
    }
  }

  // Abre el detalle del pedido: sus ítems, el pago con el que se cobró y su boleta.
  const verDetalle = async (p) => {
    setCargandoDetalle(true)
    setDetalle({ pedido: p, pago: null, boleta: null })
    try {
      // Nota: el listado de pagos vive en PagosAPI (no en AdminAPI), pero pega al mismo /admin/pagos
      const [pagos, boletas] = await Promise.all([PagosAPI.listar(), AdminAPI.comprobantes()])
      const pago = pagos.filter((x) => x.pedidoId === p.id).sort((a, b) => b.id - a.id)[0] || null
      const boleta = boletas.find((c) => c.pedidoId === p.id) || null
      setDetalle({ pedido: p, pago, boleta })
    } catch (err) {
      toast.error(err.message)
    } finally {
      setCargandoDetalle(false)
    }
  }

  // Descarga el comprobante en PDF del pago mostrado en el detalle
  const descargarComprobantePago = async (pago) => {
    try {
      await downloadBlob(PagosAPI.comprobanteUrl(pago.id), 'comprobante-' + pago.codigo + '.pdf')
    } catch (err) {
      toast.error(err.message)
    }
  }

  // Descarga la boleta desde el detalle del pedido (usa el código propio de la boleta, no del pedido)
  const descargarBoletaDetalle = async (pedidoId, boletaCodigo) => {
    try {
      await downloadBlob(AdminAPI.boletaPedidoUrl(pedidoId), 'boleta-' + boletaCodigo + '.pdf')
    } catch (err) {
      toast.error(err.message)
    }
  }

  // ---- Anular venta ----
  // Abre el modal de anulación desde el detalle (se cierra el detalle para no apilar modales)
  const abrirAnular = (p) => {
    setDetalle(null)
    setMotivoAnulacion('')
    setAnulando(p)
  }

  // El motivo es obligatorio: el backend exige entre 5 y 200 caracteres.
  const motivoValido = motivoAnulacion.trim().length >= MOTIVO_MIN && motivoAnulacion.trim().length <= MOTIVO_MAX

  // Anula la venta: el backend repone el stock, devuelve el dinero (Stripe o manual en Yape)
  // y anula la boleta, todo en una sola transacción. Aquí solo refrescamos la lista.
  const anular = async () => {
    if (!motivoValido) return
    setAnulandoSaving(true)
    try {
      await AdminAPI.anularPedido(anulando.id, motivoAnulacion.trim())
      toast.success('Venta anulada: se repuso el stock y se devolvió el dinero')
      setAnulando(null)
      cargar()
    } catch (err) {
      toast.error(err.message)
    } finally {
      setAnulandoSaving(false)
    }
  }

  // Pide confirmación y elimina el pedido si el usuario acepta
  const eliminar = async (p) => {
    const ok = await confirm({
      title: 'Eliminar pedido',
      message: `¿Eliminar el pedido ${p.codigo}? Esta acción no se puede deshacer.`,
      confirmText: 'Eliminar',
      danger: true,
    })
    if (!ok) return
    try {
      await AdminAPI.eliminarPedido(p.id)
      toast.success('Pedido eliminado')
      cargar()
    } catch (err) {
      toast.error(err.message)
    }
  }

  const Th = ({ label, col }) => (
    <th className={'sortable' + (t.sort?.key === col ? ' is-sorted' : '')} onClick={() => t.toggleSort(col)}>
      {label}
      <span className="sort-ind">{t.sort?.key === col ? (t.sort.dir === 'asc' ? '▲' : '▼') : '↕'}</span>
    </th>
  )

  return (
    <>
      <div className="page-head">
        <div>
          <h2>Pedidos</h2>
          <p>Registra y gestiona las ventas de la tienda</p>
        </div>
        {/* Filtros de fecha/estado para el reporte, y botón para descargar el PDF con esos filtros */}
        <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'flex-end', flexWrap: 'wrap' }}>
          <div className="field" style={{ margin: 0 }}>
            <label className="label" style={{ fontSize: '0.72rem' }}>Desde</label>
            <input className="input" type="date" value={repDesde} onChange={(e) => setRepDesde(e.target.value)} />
          </div>
          <div className="field" style={{ margin: 0 }}>
            <label className="label" style={{ fontSize: '0.72rem' }}>Hasta</label>
            <input className="input" type="date" value={repHasta} onChange={(e) => setRepHasta(e.target.value)} />
          </div>
          <div className="field" style={{ margin: 0 }}>
            <label className="label" style={{ fontSize: '0.72rem' }}>Estado</label>
            <select className="select" value={repEstado} onChange={(e) => setRepEstado(e.target.value)}>
              <option value="">Todos</option>
              {ESTADOS.map((s) => <option key={s} value={s}>{s}</option>)}
            </select>
          </div>
          <button className="btn btn-outline" onClick={descargarPDF} disabled={descargando}>
            <i className="bi bi-file-earmark-pdf" /> {descargando ? 'Generando...' : 'Descargar PDF'}
          </button>
          <button className="btn btn-primary" onClick={abrirCrear}>
            <i className="bi bi-bag-plus" /> Nuevo pedido
          </button>
        </div>
      </div>

      {pedidos === null ? (
        <TableSkeleton />
      ) : (
        <div className="table-wrap">
          <TableToolbar query={t.query} onSearch={t.onSearch} total={t.total} />
          <div className="table-scroll">
            <table className="table">
              <thead>
                <tr>
                  <Th label="Código" col="codigo" />
                  <Th label="Cliente" col="clienteNombre" />
                  <Th label="Fecha" col="fecha" />
                  <th>Ítems</th>
                  <Th label="Total" col="total" />
                  <Th label="Estado" col="estado" />
                  <th style={{ textAlign: 'right' }}>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {t.paged.length === 0 && (
                  <tr>
                    <td colSpan={7}>
                      <div className="empty">
                        <i className="bi bi-bag" />
                        <div>No hay pedidos registrados</div>
                      </div>
                    </td>
                  </tr>
                )}
                {t.paged.map((p) => (
                  <tr key={p.id}>
                    <td><strong style={{ color: 'var(--accent)', fontFamily: 'monospace' }}>{p.codigo}</strong></td>
                    <td className="fw-bold">{p.clienteNombre}</td>
                    <td>{p.fecha ? new Date(p.fecha).toLocaleDateString('es-PE') : '—'}</td>
                    <td>{p.cantidadTotal}</td>
                    <td><strong>{money(p.total)}</strong></td>
                    <td><span className={badgeEstado(p.estado)}>{p.estado}</span></td>
                    <td>
                      <div className="cell-actions">
                        <button className="btn btn-outline btn-icon" onClick={() => verDetalle(p)} title="Ver detalle">
                          <i className="bi bi-eye" />
                        </button>
                        {/* Una venta anulada tampoco se vuelve a cobrar: ya se devolvió el dinero */}
                        {p.estado !== 'PAGADO' && p.estado !== 'CANCELADO' && p.estado !== 'ANULADO' && (
                          <button className="btn btn-success btn-icon" onClick={() => setCobrando(p)} title="Cobrar">
                            <i className="bi bi-credit-card" />
                          </button>
                        )}
                        {p.estado === 'PAGADO' && (
                          <button className="btn btn-outline btn-icon" onClick={() => descargarBoleta(p)} title="Ver boleta">
                            <i className="bi bi-receipt" />
                          </button>
                        )}
                        <button className="btn btn-outline btn-icon" onClick={() => abrirEditar(p)} title="Editar estado">
                          <i className="bi bi-pencil" />
                        </button>
                        <button className="btn btn-danger btn-icon" onClick={() => eliminar(p)} title="Eliminar">
                          <i className="bi bi-trash" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination page={t.page} totalPages={t.totalPages} total={t.total} onPage={t.setPage} />
        </div>
      )}

      {/* Modal nuevo pedido */}
      {showCrear && (
        <Modal
          title="Nuevo pedido"
          icon="bi-bag-plus"
          onClose={() => setShowCrear(false)}
          footer={
            <>
              <button className="btn btn-ghost" onClick={() => setShowCrear(false)}>Cancelar</button>
              <button className="btn btn-primary" onClick={guardar} disabled={saving}>
                <i className="bi bi-check2" /> {saving ? 'Guardando...' : 'Registrar'}
              </button>
            </>
          }
        >
          {formError && <Alert type="error">{formError}</Alert>}

          <div className="form-grid">
            <div className="field">
              <label className="label">Cliente *</label>
              <select className="select" value={clienteId} onChange={(e) => setClienteId(e.target.value)} required>
                <option value="">— Selecciona —</option>
                {clientes.map((c) => (
                  <option key={c.id} value={c.id}>{c.nombreCompleto} ({c.dni})</option>
                ))}
              </select>
            </div>
            <div className="field">
              <label className="label">Método de pago</label>
              <select className="select" value={metodoPago} onChange={(e) => setMetodoPago(e.target.value)}>
                {METODOS.map((m) => <option key={m} value={m}>{m}</option>)}
              </select>
            </div>
          </div>

          <label className="label">Agregar productos</label>
          <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'flex-end', marginBottom: '0.75rem' }}>
            <select className="select" value={draft.productoId} onChange={(e) => setDraft({ ...draft, productoId: e.target.value })}>
              <option value="">— Producto —</option>
              {productos.map((p) => (
                <option key={p.id} value={p.id}>{p.nombre} ({money(p.precio)})</option>
              ))}
            </select>
            <input
              className="input"
              type="number"
              min="1"
              style={{ width: 90 }}
              value={draft.cantidad}
              onChange={(e) => setDraft({ ...draft, cantidad: e.target.value })}
            />
            <button type="button" className="btn btn-outline" onClick={agregarItem}>
              <i className="bi bi-plus-lg" />
            </button>
          </div>

          {items.length > 0 && (
            <div className="table-wrap" style={{ marginBottom: '0.75rem' }}>
              <table className="table">
                <thead>
                  <tr><th>Producto</th><th>Cant.</th><th>Subtotal</th><th></th></tr>
                </thead>
                <tbody>
                  {items.map((it, idx) => (
                    <tr key={idx}>
                      <td>{it.nombre}</td>
                      <td>{it.cantidad}</td>
                      <td>{money(it.precio * it.cantidad)}</td>
                      <td style={{ textAlign: 'right' }}>
                        <button className="btn btn-danger btn-icon" onClick={() => quitarItem(idx)} title="Quitar">
                          <i className="bi bi-x-lg" />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          <div style={{ textAlign: 'right', fontSize: '1.1rem' }}>
            Total: <strong style={{ color: 'var(--accent)' }}>{money(totalPedido)}</strong>
          </div>
        </Modal>
      )}

      {/* Modal editar estado */}
      {editing && (
        <Modal
          title={`Editar ${editing.codigo}`}
          icon="bi-pencil-square"
          size="sm"
          onClose={() => setEditing(null)}
          footer={
            <>
              <button className="btn btn-ghost" onClick={() => setEditing(null)}>Cancelar</button>
              <button className="btn btn-primary" onClick={guardarEstado} disabled={saving}>
                <i className="bi bi-check2" /> {saving ? 'Guardando...' : 'Guardar'}
              </button>
            </>
          }
        >
          <form onSubmit={guardarEstado}>
            <div className="field">
              <label className="label">Estado</label>
              <select className="select" value={estadoEdit} onChange={(e) => setEstadoEdit(e.target.value)}>
                {ESTADOS.map((s) => <option key={s} value={s}>{s}</option>)}
              </select>
            </div>
            <div className="field">
              <label className="label">Método de pago</label>
              <select className="select" value={metodoEdit} onChange={(e) => setMetodoEdit(e.target.value)}>
                <option value="">— Sin especificar —</option>
                {METODOS.map((m) => <option key={m} value={m}>{m}</option>)}
              </select>
            </div>
            <button type="submit" hidden />
          </form>
        </Modal>
      )}

      {/* Modal de la pasarela de pagos (cobrar un pedido pendiente) */}
      {cobrando && (
        <PasarelaPago
          pedido={cobrando}
          onClose={() => setCobrando(null)}
          onPagado={() => { setCobrando(null); cargar() }}
        />
      )}

      {/* Modal "anular venta": exige escribir un motivo y advierte de las consecuencias.
          No es un confirm simple a propósito: anular mueve dinero, stock y contabilidad. */}
      {anulando && (
        <Modal
          title={`Anular venta ${anulando.codigo}`}
          icon="bi-x-octagon"
          size="sm"
          onClose={() => setAnulando(null)}
          footer={
            <>
              <button className="btn btn-ghost" onClick={() => setAnulando(null)}>Cancelar</button>
              <button className="btn btn-danger" onClick={anular} disabled={!motivoValido || anulandoSaving}>
                <i className="bi bi-x-octagon" /> {anulandoSaving ? 'Anulando...' : 'Anular venta'}
              </button>
            </>
          }
        >
          <Alert type="error">
            <span>
              Se devolverá el dinero al cliente, se repondrá el stock y la boleta quedará anulada.
              <strong> Esta acción no se puede deshacer.</strong>
            </span>
          </Alert>

          <div className="detalle-datos-grid" style={{ marginBottom: '1rem' }}>
            <div className="detalle-dato">
              <span>Cliente</span>
              <strong>{anulando.clienteNombre}</strong>
            </div>
            <div className="detalle-dato">
              <span>Monto a devolver</span>
              <strong>{money(anulando.total)}</strong>
            </div>
          </div>

          <div className="field">
            <label className="label">Motivo de la anulación *</label>
            <textarea
              className="textarea"
              rows={3}
              value={motivoAnulacion}
              onChange={(e) => setMotivoAnulacion(e.target.value.slice(0, MOTIVO_MAX))}
              maxLength={MOTIVO_MAX}
              placeholder="Ej. El cliente se arrepintió de la compra"
            />
            <small className="text-muted">
              {motivoAnulacion.trim().length}/{MOTIVO_MAX} — mínimo {MOTIVO_MIN} caracteres. Queda registrado en la boleta anulada.
            </small>
          </div>
        </Modal>
      )}

      {/* Modal "ver detalle": ítems del pedido + el pago con el que se cobró + su boleta, todo junto */}
      {detalle && (
        <Modal
          title={`Pedido ${detalle.pedido.codigo}`}
          icon="bi-eye"
          size="lg"
          onClose={() => setDetalle(null)}
          footer={
            <>
              {/* Anular solo tiene sentido en una venta ya cobrada: es lo que hay que devolver.
                  Incluye ENVIADO y ENTREGADO porque las devoluciones normalmente se piden
                  DESPUES de recibir el producto. Quedan fuera PENDIENTE (nunca se cobro, se
                  cancela), CANCELADO y ANULADO (ya no hay nada que devolver). */}
              {['PAGADO', 'ENVIADO', 'ENTREGADO'].includes(detalle.pedido.estado) && (
                <button className="btn btn-danger btn-sm" style={{ marginRight: 'auto' }} onClick={() => abrirAnular(detalle.pedido)}>
                  <i className="bi bi-x-octagon" /> Anular venta
                </button>
              )}
              <button className="btn btn-ghost" onClick={() => setDetalle(null)}>Cerrar</button>
            </>
          }
        >
          {/* Cabecera: estado y fecha */}
          <div className="detalle-cabecera">
            <span className={badgeEstado(detalle.pedido.estado)}>{detalle.pedido.estado}</span>
            <span className="detalle-fecha">
              {detalle.pedido.fecha ? new Date(detalle.pedido.fecha).toLocaleString('es-PE') : '—'}
            </span>
          </div>
          <div className="detalle-dato-simple"><strong>Cliente:</strong> {detalle.pedido.clienteNombre}</div>

          {/* Si la venta fue anulada, el motivo se ve arriba de todo */}
          {detalle.pedido.estado === 'ANULADO' && (
            <Alert type="error">
              <span>
                <strong>Venta anulada.</strong> {detalle.pedido.motivoAnulacion || 'Sin motivo registrado'}
              </span>
            </Alert>
          )}

          {/* Ítems del pedido: ya vienen incluidos en el pedido de la lista, no hace falta pedirlos aparte */}
          <div className="detalle-seccion">
            <h4><i className="bi bi-bag" /> Ítems</h4>
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
                    {(detalle.pedido.items || []).map((it) => (
                      <tr key={it.id}>
                        <td>{it.productoNombre}</td>
                        <td>{it.cantidad}</td>
                        <td>{money(it.precioUnitario)}</td>
                        <td>{money(it.subtotal)}</td>
                      </tr>
                    ))}
                    <tr>
                      <td colSpan={3} style={{ textAlign: 'right' }}><strong>Total</strong></td>
                      <td><strong>{money(detalle.pedido.total)}</strong></td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          {/* Pago: con qué se cobró el pedido (si ya se cobró) */}
          <div className="detalle-seccion">
            <h4><i className="bi bi-credit-card" /> Pago</h4>
            {cargandoDetalle ? (
              <p className="detalle-vacio">Cargando...</p>
            ) : detalle.pago ? (
              <>
                <div className="detalle-datos-grid">
                  <div className="detalle-dato">
                    <span>Código</span>
                    <strong>{detalle.pago.codigo}</strong>
                  </div>
                  <div className="detalle-dato">
                    <span>Método</span>
                    <span className={detalle.pago.metodo === 'YAPE' ? 'badge badge-yape' : 'badge badge-tarjeta'}>
                      {detalle.pago.metodo}
                    </span>
                  </div>
                  <div className="detalle-dato">
                    <span>Estado</span>
                    <span className={detalle.pago.estado === 'APROBADO' ? 'badge badge-ok' : 'badge badge-danger'}>
                      {detalle.pago.estado}
                    </span>
                  </div>
                  <div className="detalle-dato">
                    <span>Monto</span>
                    <strong>{money(detalle.pago.monto)}</strong>
                  </div>
                  <div className="detalle-dato">
                    <span>Referencia</span>
                    <strong>{detalle.pago.referencia || '—'}</strong>
                    <small className="detalle-nota">
                      {detalle.pago.referencia && detalle.pago.referencia.startsWith('pi_')
                        ? 'ID de la transacción en Stripe'
                        : 'N° de operación de Yape o código de autorización'}
                    </small>
                  </div>
                  {detalle.pago.tarjetaUlt4 && (
                    <div className="detalle-dato">
                      <span>Tarjeta</span>
                      <strong>•••• {detalle.pago.tarjetaUlt4}</strong>
                    </div>
                  )}
                </div>
                <button
                  className="btn btn-outline btn-sm"
                  style={{ marginTop: '0.75rem' }}
                  onClick={() => descargarComprobantePago(detalle.pago)}
                >
                  <i className="bi bi-file-earmark-pdf" /> Comprobante de pago
                </button>
              </>
            ) : (
              <div className="detalle-aviso">
                <span>Este pedido aún no ha sido cobrado</span>
                {detalle.pedido.estado !== 'PAGADO' && detalle.pedido.estado !== 'CANCELADO' && detalle.pedido.estado !== 'ANULADO' && (
                  <button
                    className="btn btn-success btn-sm"
                    onClick={() => { const pedido = detalle.pedido; setDetalle(null); setCobrando(pedido) }}
                  >
                    <i className="bi bi-credit-card" /> Cobrar ahora
                  </button>
                )}
              </div>
            )}
          </div>

          {/* Boleta: documento contable del pedido, se emite al aprobarse el pago */}
          <div className="detalle-seccion">
            <h4><i className="bi bi-receipt" /> Boleta</h4>
            {cargandoDetalle ? (
              <p className="detalle-vacio">Cargando...</p>
            ) : detalle.boleta ? (
              <>
                <div className="detalle-datos-grid">
                  <div className="detalle-dato">
                    <span>Código</span>
                    <strong>{detalle.boleta.codigo}</strong>
                  </div>
                  <div className="detalle-dato">
                    <span>Op. gravada</span>
                    <strong>{money(detalle.boleta.subtotal)}</strong>
                  </div>
                  <div className="detalle-dato">
                    <span>IGV (18%)</span>
                    <strong>{money(detalle.boleta.igv)}</strong>
                  </div>
                  <div className="detalle-dato">
                    <span>Total</span>
                    <strong>{money(detalle.boleta.total)}</strong>
                  </div>
                </div>
                <button
                  className="btn btn-outline btn-sm"
                  style={{ marginTop: '0.75rem' }}
                  onClick={() => descargarBoletaDetalle(detalle.pedido.id, detalle.boleta.codigo)}
                >
                  <i className="bi bi-download" /> Descargar boleta
                </button>
              </>
            ) : (
              <div className="detalle-aviso">
                <span>Aún no se ha emitido boleta (se emite al aprobarse el pago)</span>
              </div>
            )}
          </div>
        </Modal>
      )}
    </>
  )
}
