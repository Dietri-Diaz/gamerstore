import { useEffect, useState } from 'react'
import { AdminAPI } from '../../api/endpoints.js'
import { useTableControls } from '../../hooks/useTableControls.js'
import { useAutoClear } from '../../hooks/useAutoClear.js'
import { useDuplicado } from '../../hooks/useDuplicado.js'
import { useToast } from '../../components/ui/Toast.jsx'
import { useConfirm } from '../../components/ui/Confirm.jsx'
import Modal from '../../components/ui/Modal.jsx'
import Alert from '../../components/ui/Alert.jsx'
import TableToolbar from '../../components/ui/TableToolbar.jsx'
import TableSkeleton from '../../components/ui/TableSkeleton.jsx'
import Pagination from '../../components/ui/Pagination.jsx'

// Ya no se elige rol: todos los usuarios son ADMIN. La tienda pública no necesita login
// (se compra sin cuenta), así que un rol "USUARIO" que igual entraba a todo el panel no
// aportaba nada y solo daba una falsa sensación de permisos limitados.
const EMPTY = { username: '', email: '', nombre: '', password: '', telefono: '', rol: 'ADMIN' }

// Página admin: CRUD de usuarios del sistema (accesos al panel, todos con rol ADMIN)
export default function AdminUsuarios() {
  const toast = useToast()
  const confirm = useConfirm()

  const [usuarios, setUsuarios] = useState(null)
  const [showModal, setShowModal] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState(EMPTY)
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState('')

  const t = useTableControls(usuarios || [], {
    searchKeys: ['username', 'email', 'nombre'],
    pageSize: 8,
    initialSort: { key: 'username', dir: 'asc' },
  })

  // El mensaje de error del formulario se borra solo a los 5s (no se queda estático).
  useAutoClear(formError, setFormError)

  // Aviso en vivo si el usuario ya existe.
  const dupUser = useDuplicado(
    form.username,
    (v) => AdminAPI.existeUsuario({ username: v, id: editing?.id }),
    { activo: showModal, minLargo: 3 }
  )
  // Aviso en vivo si el email ya está registrado por otro usuario.
  const dupEmail = useDuplicado(
    form.email,
    (v) => AdminAPI.existeUsuario({ email: v, id: editing?.id }),
    { activo: showModal, minLargo: 5 }
  )

  const cargar = () => AdminAPI.usuarios().then(setUsuarios).catch(() => setUsuarios([]))
  // Carga el listado de usuarios al montar el componente
  useEffect(() => { cargar() }, [])

  // Abre el modal en modo "crear": deja el formulario vacío
  const abrirCrear = () => {
    setEditing(null)
    setForm(EMPTY)
    setFormError('')
    setShowModal(true)
  }
  // Abre el modal en modo "editar": precarga el formulario (la contraseña se deja vacía)
  const abrirEditar = (u) => {
    setEditing(u)
    // El rol siempre viaja como ADMIN: el backend lo fuerza igual, aquí solo somos coherentes.
    setForm({ username: u.username, email: u.email, nombre: u.nombre, password: '', telefono: u.telefono || '', rol: 'ADMIN' })
    setFormError('')
    setShowModal(true)
  }
  const cambiar = (campo) => (e) => setForm((f) => ({ ...f, [campo]: e.target.value }))

  // Envía el formulario: crea o actualiza el usuario según si estamos editando, y refresca la tabla
  const guardar = async (e) => {
    e.preventDefault()
    setSaving(true)
    setFormError('')
    try {
      if (editing) {
        await AdminAPI.actualizarUsuario(editing.id, form)
        toast.success('Usuario actualizado')
      } else {
        await AdminAPI.crearUsuario(form)
        toast.success('Usuario creado correctamente')
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

  // Pide confirmación y elimina el usuario si el usuario admin acepta
  const eliminar = async (u) => {
    const ok = await confirm({
      title: 'Eliminar usuario',
      message: `¿Eliminar al usuario "${u.username}"? Esta acción no se puede deshacer.`,
      confirmText: 'Eliminar',
      danger: true,
    })
    if (!ok) return
    try {
      await AdminAPI.eliminarUsuario(u.id)
      toast.success('Usuario eliminado')
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
          <h2>Usuarios del sistema</h2>
          <p>Administra los accesos al panel</p>
        </div>
        <button className="btn btn-primary" onClick={abrirCrear}>
          <i className="bi bi-person-plus" /> Nuevo usuario
        </button>
      </div>

      {usuarios === null ? (
        <TableSkeleton />
      ) : (
        <div className="table-wrap">
          <TableToolbar query={t.query} onSearch={t.onSearch} total={t.total} />
          <div className="table-scroll">
            <table className="table">
              <thead>
                <tr>
                  <Th label="Usuario" col="username" />
                  <Th label="Nombre" col="nombre" />
                  <Th label="Email" col="email" />
                  <Th label="Rol" col="rol" />
                  <th style={{ textAlign: 'right' }}>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {t.paged.length === 0 && (
                  <tr>
                    <td colSpan={5}>
                      <div className="empty"><i className="bi bi-people" /><div>No hay usuarios</div></div>
                    </td>
                  </tr>
                )}
                {t.paged.map((u) => (
                  <tr key={u.id}>
                    <td className="fw-bold">{u.username}</td>
                    <td>{u.nombre}</td>
                    <td>{u.email}</td>
                    <td>
                      {/* Badge fijo: todo usuario del sistema es administrador */}
                      <span className="badge badge-accent">ADMIN</span>
                    </td>
                    <td>
                      <div className="cell-actions">
                        <button className="btn btn-outline btn-icon" onClick={() => abrirEditar(u)} title="Editar">
                          <i className="bi bi-pencil" />
                        </button>
                        <button className="btn btn-danger btn-icon" onClick={() => eliminar(u)} title="Eliminar">
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
          title={editing ? 'Editar usuario' : 'Nuevo usuario'}
          icon={editing ? 'bi-pencil-square' : 'bi-person-plus'}
          onClose={() => setShowModal(false)}
          footer={
            <>
              <button className="btn btn-ghost" onClick={() => setShowModal(false)}>Cancelar</button>
              <button className="btn btn-primary" onClick={guardar} disabled={saving || dupUser.duplicado || dupEmail.duplicado}>
                <i className="bi bi-check2" /> {saving ? 'Guardando...' : 'Guardar'}
              </button>
            </>
          }
        >
          {formError && <Alert type="error">{formError}</Alert>}
          <form onSubmit={guardar}>
            <div className="form-grid">
              <div className="field">
                <label className="label">Usuario *</label>
                <input className={'input' + (dupUser.duplicado ? ' input-error' : '')} value={form.username} onChange={cambiar('username')} required />
                {dupUser.duplicado && (
                  <small className="campo-error"><i className="bi bi-exclamation-triangle-fill" /> {dupUser.mensaje}</small>
                )}
              </div>
              <div className="field">
                <label className="label">Email *</label>
                <input className={'input' + (dupEmail.duplicado ? ' input-error' : '')} type="email" value={form.email} onChange={cambiar('email')} required />
                {dupEmail.duplicado && (
                  <small className="campo-error"><i className="bi bi-exclamation-triangle-fill" /> {dupEmail.mensaje}</small>
                )}
              </div>
              <div className="field full">
                <label className="label">Nombre *</label>
                <input className="input" value={form.nombre} onChange={cambiar('nombre')} required />
              </div>
              <div className="field">
                <label className="label">Teléfono</label>
                <input className="input" value={form.telefono} onChange={cambiar('telefono')} />
              </div>
              <div className="field">
                {/* Sin selector de rol: todos los usuarios del panel son administradores */}
                <label className="label">Rol</label>
                <div style={{ paddingTop: '0.35rem' }}>
                  <span className="badge badge-accent">ADMIN</span>
                </div>
              </div>
              <div className="field full">
                <label className="label">{editing ? 'Contraseña (dejar vacío para no cambiar)' : 'Contraseña *'}</label>
                <input className="input" type="password" value={form.password} onChange={cambiar('password')} required={!editing} placeholder="••••••••" />
              </div>
            </div>
            <button type="submit" hidden />
          </form>
        </Modal>
      )}
    </>
  )
}
