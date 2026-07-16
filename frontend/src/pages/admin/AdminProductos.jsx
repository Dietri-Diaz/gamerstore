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

// Página admin: CRUD de productos del catálogo (crear, editar, eliminar y subir imagen)
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
  const [subiendo, setSubiendo] = useState(false)

  // Tabla: búsqueda + orden + paginación
  const t = useTableControls(productos || [], {
    searchKeys: ['nombre', 'categoriaNombre'],
    pageSize: 8,
    initialSort: { key: 'nombre', dir: 'asc' },
  })

  const cargar = () => AdminAPI.productos().then(setProductos).catch(() => setProductos([]))

  // Al montar, carga los productos y las categorías (estas últimas alimentan el <select> del formulario)
  useEffect(() => {
    cargar()
    AdminAPI.categorias().then(setCategorias).catch(() => setCategorias([]))
  }, [])

  // Abre el modal en modo "crear": deja el formulario vacío
  const abrirCrear = () => {
    setEditing(null)
    setForm(EMPTY)
    setFormError('')
    setShowModal(true)
  }

  // Abre el modal en modo "editar": precarga el formulario con los datos del producto elegido
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

  // Envía el formulario: crea o actualiza el producto según si estamos editando, y refresca la tabla
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
      toast.error(err.message)
    } finally {
      setSaving(false)
    }
  }

  // Sube la imagen elegida al backend (como FormData) y guarda la URL devuelta en el formulario
  const subirImagen = async (e) => {
    const file = e.target.files?.[0]
    if (!file) return
    setSubiendo(true)
    setFormError('')
    try {
      const fd = new FormData()
      fd.append('file', file)
      const { url } = await AdminAPI.subirImagen(fd)
      setForm((f) => ({ ...f, imagen: url }))
      toast.success('Imagen subida')
    } catch (err) {
      setFormError(err.message)
      toast.error(err.message)
    } finally {
      setSubiendo(false)
    }
  }

  // Pide confirmación y elimina el producto si el usuario acepta
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
              {/* Subida de imagen: muestra vista previa si ya hay una, y permite elegir un archivo nuevo */}
              <div className="field full">
                <label className="label">Imagen del producto</label>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                  {form.imagen ? (
                    <img
                      src={form.imagen}
                      alt="preview"
                      style={{ width: 64, height: 64, objectFit: 'cover', borderRadius: 8, border: '1px solid var(--border)' }}
                    />
                  ) : (
                    <div style={{ width: 64, height: 64, borderRadius: 8, background: 'var(--border)', display: 'grid', placeItems: 'center' }}>
                      <i className="bi bi-image text-muted" />
                    </div>
                  )}
                  <label className="btn btn-outline" style={{ cursor: 'pointer', margin: 0 }}>
                    <i className="bi bi-upload" /> {subiendo ? 'Subiendo...' : 'Subir imagen'}
                    <input type="file" accept="image/*" hidden onChange={subirImagen} disabled={subiendo} />
                  </label>
                </div>
                <small className="text-muted">Se guarda en el proyecto (uploads/productos). Máx 5MB.</small>
              </div>
            </div>
            <button type="submit" hidden />
          </form>
        </Modal>
      )}
    </>
  )
}
