import { useEffect, useState } from 'react'
import { AdminAPI } from '../../api/endpoints.js'
import { money, sku } from '../../utils/format.js'
import { useTableControls } from '../../hooks/useTableControls.js'
import { useToast } from '../../components/ui/Toast.jsx'
import { useConfirm } from '../../components/ui/Confirm.jsx'
import Modal from '../../components/ui/Modal.jsx'
import Alert from '../../components/ui/Alert.jsx'
import TableToolbar from '../../components/ui/TableToolbar.jsx'
import TableSkeleton from '../../components/ui/TableSkeleton.jsx'
import Pagination from '../../components/ui/Pagination.jsx'

const EMPTY = { nombre: '', descripcion: '', precio: '', stock: '', imagen: '', categoriaId: '' }

export default function AdminProductos() {
  const toast = useToast()
  const confirm = useConfirm()

  const [productos, setProductos] = useState(null)
  const [categorias, setCategorias] = useState([])
  const [showModal, setShowModal] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState(EMPTY)
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState('')

  // Tabla: búsqueda + orden + paginación
  const t = useTableControls(productos || [], {
    searchKeys: ['nombre', 'categoriaNombre'],
    pageSize: 8,
    initialSort: { key: 'nombre', dir: 'asc' },
  })

  const cargar = () => AdminAPI.productos().then(setProductos).catch(() => setProductos([]))

  useEffect(() => {
    cargar()
    AdminAPI.categorias().then(setCategorias).catch(() => setCategorias([]))
  }, [])

  const abrirCrear = () => {
    setEditing(null)
    setForm(EMPTY)
    setFormError('')
    setShowModal(true)
  }

  const abrirEditar = (p) => {
    setEditing(p)
    setForm({
      nombre: p.nombre,
      descripcion: p.descripcion || '',
      precio: p.precio,
      stock: p.stock,
      imagen: p.imagen || '',
      categoriaId: p.categoriaId || '',
    })
    setFormError('')
    setShowModal(true)
  }

  const cambiar = (campo) => (e) => setForm((f) => ({ ...f, [campo]: e.target.value }))

  const guardar = async (e) => {
    e.preventDefault()
    setSaving(true)
    setFormError('')
    const payload = {
      nombre: form.nombre,
      descripcion: form.descripcion,
      precio: form.precio === '' ? null : Number(form.precio),
      stock: form.stock === '' ? null : Number(form.stock),
      imagen: form.imagen,
      categoriaId: form.categoriaId ? Number(form.categoriaId) : null,
    }
    try {
      if (editing) {
        await AdminAPI.actualizarProducto(editing.id, payload)
        toast.success('Producto actualizado')
      } else {
        await AdminAPI.crearProducto(payload)
        toast.success('Producto creado correctamente')
      }
      setShowModal(false)
      cargar()
    } catch (err) {
      setFormError(err.message)
    } finally {
      setSaving(false)
    }
  }

  const eliminar = async (p) => {
    const ok = await confirm({
      title: 'Eliminar producto',
      message: `¿Seguro que quieres eliminar "${p.nombre}"? Esta acción no se puede deshacer.`,
      confirmText: 'Eliminar',
      danger: true,
    })
    if (!ok) return
    try {
      await AdminAPI.eliminarProducto(p.id)
      toast.success('Producto eliminado')
      cargar()
    } catch (err) {
      toast.error(err.message)
    }
  }

  // Encabezado de columna ordenable
  const Th = ({ label, col, right }) => (
    <th
      className={'sortable' + (t.sort?.key === col ? ' is-sorted' : '')}
      style={right ? { textAlign: 'right' } : undefined}
      onClick={() => t.toggleSort(col)}
    >
      {label}
      <span className="sort-ind">{t.sort?.key === col ? (t.sort.dir === 'asc' ? '▲' : '▼') : '↕'}</span>
    </th>
  )

  return (
    <>
      <div className="page-head">
        <div>
          <h2>Catálogo de productos</h2>
          <p>Gestiona el inventario de la tienda</p>
        </div>
        <button className="btn btn-primary" onClick={abrirCrear}>
          <i className="bi bi-plus-circle" /> Nuevo producto
        </button>
      </div>

      {productos === null ? (
        <TableSkeleton />
      ) : (
        <div className="table-wrap">
          <TableToolbar query={t.query} onSearch={t.onSearch} total={t.total} />
          <div className="table-scroll">
            <table className="table">
              <thead>
                <tr>
                  <Th label="Producto" col="nombre" />
                  <Th label="Categoría" col="categoriaNombre" />
                  <Th label="Precio" col="precio" />
                  <Th label="Stock" col="stock" />
                  <th style={{ textAlign: 'right' }}>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {t.paged.length === 0 && (
                  <tr>
                    <td colSpan={5}>
                      <div className="empty">
                        <i className="bi bi-inbox" />
                        <div>No hay productos que coincidan</div>
                      </div>
                    </td>
                  </tr>
                )}
                {t.paged.map((p) => (
                  <tr key={p.id}>
                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                        <img className="thumb" src={p.imagen} alt={p.nombre} />
                        <div>
                          <div className="fw-bold">{p.nombre}</div>
                          <small className="text-muted">{sku(p.id)}</small>
                        </div>
                      </div>
                    </td>
                    <td><span className="badge badge-cat">{p.categoriaNombre || '—'}</span></td>
                    <td><strong style={{ color: 'var(--accent)' }}>{money(p.precio)}</strong></td>
                    <td>
                      {p.stock > 10 ? (
                        <span className="badge badge-ok">{p.stock}</span>
                      ) : p.stock > 0 ? (
                        <span className="badge badge-warn">{p.stock}</span>
                      ) : (
                        <span className="badge badge-danger">Sin stock</span>
                      )}
                    </td>
                    <td>
                      <div className="cell-actions">
                        <button className="btn btn-outline btn-icon" onClick={() => abrirEditar(p)} title="Editar">
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

      {showModal && (
        <Modal
          title={editing ? 'Editar producto' : 'Nuevo producto'}
          icon={editing ? 'bi-pencil-square' : 'bi-plus-circle'}
          onClose={() => setShowModal(false)}
          footer={
            <>
              <button className="btn btn-ghost" onClick={() => setShowModal(false)}>Cancelar</button>
              <button className="btn btn-primary" onClick={guardar} disabled={saving}>
                <i className="bi bi-check2" /> {saving ? 'Guardando...' : 'Guardar'}
              </button>
            </>
          }
        >
          {formError && <Alert type="error">{formError}</Alert>}
          <form onSubmit={guardar}>
            <div className="field">
              <label className="label">Nombre *</label>
              <input className="input" value={form.nombre} onChange={cambiar('nombre')} required />
            </div>
            <div className="field">
              <label className="label">Descripción</label>
              <textarea className="textarea" rows={2} value={form.descripcion} onChange={cambiar('descripcion')} />
            </div>
            <div className="form-grid">
              <div className="field">
                <label className="label">Precio (S/) *</label>
                <input className="input" type="number" step="0.01" min="0.01" value={form.precio} onChange={cambiar('precio')} required />
              </div>
              <div className="field">
                <label className="label">Stock *</label>
                <input className="input" type="number" min="0" value={form.stock} onChange={cambiar('stock')} required />
              </div>
              <div className="field full">
                <label className="label">Categoría *</label>
                <select className="select" value={form.categoriaId} onChange={cambiar('categoriaId')} required>
                  <option value="">— Selecciona —</option>
                  {categorias.map((c) => (
                    <option key={c.id} value={c.id}>{c.nombre}</option>
                  ))}
                </select>
              </div>
              <div className="field full">
                <label className="label">URL imagen</label>
                <input className="input" type="url" placeholder="https://..." value={form.imagen} onChange={cambiar('imagen')} />
              </div>
            </div>
            <button type="submit" hidden />
          </form>
        </Modal>
      )}
    </>
  )
}
