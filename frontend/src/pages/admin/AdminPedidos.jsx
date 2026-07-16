import { useEffect, useState } from 'react'
import { AdminAPI } from '../../api/endpoints.js'
import { downloadBlob } from '../../api/client.js'
import { money } from '../../utils/format.js'
import { useTableControls } from '../../hooks/useTableControls.js'
import { useToast } from '../../components/ui/Toast.jsx'
import { useConfirm } from '../../components/ui/Confirm.jsx'
import Modal from '../../components/ui/Modal.jsx'
import Alert from '../../components/ui/Alert.jsx'
import TableToolbar from '../../components/ui/TableToolbar.jsx'
import TableSkeleton from '../../components/ui/TableSkeleton.jsx'
import Pagination from '../../components/ui/Pagination.jsx'

const ESTADOS = ['PENDIENTE', 'PAGADO', 'ENVIADO', 'ENTREGADO', 'CANCELADO']
const METODOS = ['EFECTIVO', 'TARJETA', 'YAPE', 'PLIN', 'TRANSFERENCIA']

// Clase de color para el badge según el estado del pedido
function badgeEstado(estado) {
  if (estado === 'PAGADO' || estado === 'ENTREGADO') return 'badge badge-ok'
  if (estado === 'CANCELADO') return 'badge badge-danger'
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
  const [metodoPago, setMetodoPago] = useState('EFECTIVO')
  const [items, setItems] = useState([])
  const [draft, setDraft] = useState({ productoId: '', cantidad: 1 })
  const [formError, setFormError] = useState('')
  const [saving, setSaving] = useState(false)

  // Modal "editar estado"
  const [editing, setEditing] = useState(null)
  const [estadoEdit, setEstadoEdit] = useState('PENDIENTE')
  const [metodoEdit, setMetodoEdit] = useState('')

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
    setMetodoPago('EFECTIVO')
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
    </>
  )
}
