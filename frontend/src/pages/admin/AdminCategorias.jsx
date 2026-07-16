import { useEffect, useState } from 'react'
import { AdminAPI } from '../../api/endpoints.js'
import { useTableControls } from '../../hooks/useTableControls.js'
import { useToast } from '../../components/ui/Toast.jsx'
import { useConfirm } from '../../components/ui/Confirm.jsx'
import Modal from '../../components/ui/Modal.jsx'
import Alert from '../../components/ui/Alert.jsx'
import TableToolbar from '../../components/ui/TableToolbar.jsx'
import TableSkeleton from '../../components/ui/TableSkeleton.jsx'
import Pagination from '../../components/ui/Pagination.jsx'

// Página admin: CRUD de categorías de productos
export default function AdminCategorias() {
  const toast = useToast()
  const confirm = useConfirm()

  const [categorias, setCategorias] = useState(null)
  const [showModal, setShowModal] = useState(false)
  const [editing, setEditing] = useState(null)
  const [nombre, setNombre] = useState('')
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState('')

  const t = useTableControls(categorias || [], {
    searchKeys: ['nombre'],
    pageSize: 8,
    initialSort: { key: 'id', dir: 'asc' },
  })

  const cargar = () => AdminAPI.categorias().then(setCategorias).catch(() => setCategorias([]))

  // Carga las categorías al montar el componente
  useEffect(() => {
    cargar()
  }, [])

  // Abre el modal en modo "crear": deja el nombre vacío
  const abrirCrear = () => {
    setEditing(null)
    setNombre('')
    setFormError('')
    setShowModal(true)
  }

  // Abre el modal en modo "editar": precarga el nombre de la categoría elegida
  const abrirEditar = (c) => {
    setEditing(c)
    setNombre(c.nombre)
    setFormError('')
    setShowModal(true)
  }

  // Envía el formulario: crea o actualiza la categoría según si estamos editando, y refresca la tabla
  const guardar = async (e) => {
    e.preventDefault()
    setSaving(true)
    setFormError('')
    try {
      if (editing) {
        await AdminAPI.actualizarCategoria(editing.id, { nombre })
        toast.success('Categoría actualizada')
      } else {
        await AdminAPI.crearCategoria({ nombre })
        toast.success('Categoría creada')
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

  // Pide confirmación y elimina la categoría si el usuario acepta (falla si tiene productos asociados)
  const eliminar = async (c) => {
    const ok = await confirm({
      title: 'Eliminar categoría',
      message: `¿Eliminar la categoría "${c.nombre}"? No se podrá eliminar si tiene productos asociados.`,
      confirmText: 'Eliminar',
      danger: true,
    })
    if (!ok) return
    try {
      await AdminAPI.eliminarCategoria(c.id)
      toast.success('Categoría eliminada')
      cargar()
    } catch (err) {
      toast.error(err.message)
    }
  }

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
          <h2>Categorías</h2>
          <p>Organiza tu catálogo de productos</p>
        </div>
        <button className="btn btn-primary" onClick={abrirCrear}>
          <i className="bi bi-plus-circle" /> Nueva categoría
        </button>
      </div>

      {categorias === null ? (
        <TableSkeleton rows={4} />
      ) : (
        <div className="table-wrap">
          <TableToolbar query={t.query} onSearch={t.onSearch} total={t.total} />
          <div className="table-scroll">
            <table className="table">
              <thead>
                <tr>
                  <Th label="ID" col="id" />
                  <Th label="Nombre" col="nombre" />
                  <th style={{ textAlign: 'right', width: 160 }}>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {t.paged.length === 0 && (
                  <tr>
                    <td colSpan={3}>
                      <div className="empty">
                        <i className="bi bi-tags" />
                        <div>No hay categorías que coincidan</div>
                      </div>
                    </td>
                  </tr>
                )}
                {t.paged.map((c) => (
                  <tr key={c.id}>
                    <td><span className="text-muted">#{c.id}</span></td>
                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                        <div className="stat-icon" style={{ background: 'var(--accent-soft)', color: 'var(--accent)', width: 38, height: 38, fontSize: '1rem' }}>
                          <i className="bi bi-tag-fill" />
                        </div>
                        <span className="fw-bold">{c.nombre}</span>
                      </div>
                    </td>
                    <td>
                      <div className="cell-actions">
                        <button className="btn btn-outline btn-icon" onClick={() => abrirEditar(c)} title="Editar">
                          <i className="bi bi-pencil" />
                        </button>
                        <button className="btn btn-danger btn-icon" onClick={() => eliminar(c)} title="Eliminar">
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
          title={editing ? 'Editar categoría' : 'Nueva categoría'}
          icon={editing ? 'bi-pencil-square' : 'bi-plus-circle'}
          size="sm"
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
              <input
                className="input"
                value={nombre}
                onChange={(e) => setNombre(e.target.value)}
                placeholder="Ej: Consolas, Periféricos..."
                required
                autoFocus
              />
            </div>
            <button type="submit" hidden />
          </form>
        </Modal>
      )}
    </>
  )
}
