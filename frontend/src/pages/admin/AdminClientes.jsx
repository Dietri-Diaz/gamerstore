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

const EMPTY = { dni: '', nombres: '', apellidos: '', telefono: '', email: '', direccion: '' }

// Página admin: CRUD de clientes, con autocompletar de nombres/apellidos consultando RENIEC por DNI
export default function AdminClientes() {
  const toast = useToast()
  const confirm = useConfirm()

  const [clientes, setClientes] = useState(null)
  const [showModal, setShowModal] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState(EMPTY)
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState('')
  const [buscandoDni, setBuscandoDni] = useState(false)

  const t = useTableControls(clientes || [], {
    searchKeys: ['nombreCompleto', 'dni', 'email'],
    pageSize: 8,
    initialSort: { key: 'nombreCompleto', dir: 'asc' },
  })

  const cargar = () => AdminAPI.clientes().then(setClientes).catch(() => setClientes([]))

  // Carga el listado de clientes al montar el componente
  useEffect(() => {
    cargar()
  }, [])

  // Abre el modal en modo "crear": deja el formulario vacío
  const abrirCrear = () => {
    setEditing(null)
    setForm(EMPTY)
    setFormError('')
    setShowModal(true)
  }

  // Abre el modal en modo "editar": precarga el formulario con los datos del cliente elegido
  const abrirEditar = (c) => {
    setEditing(c)
    setForm({
      dni: c.dni,
      nombres: c.nombres,
      apellidos: c.apellidos,
      telefono: c.telefono || '',
      email: c.email || '',
      direccion: c.direccion || '',
    })
    setFormError('')
    setShowModal(true)
  }

  const cambiar = (campo) => (e) => setForm((f) => ({ ...f, [campo]: e.target.value }))

  // Envía el formulario: crea o actualiza el cliente según si estamos editando, y refresca la tabla
  const guardar = async (e) => {
    e.preventDefault()
    setSaving(true)
    setFormError('')
    try {
      if (editing) {
        await AdminAPI.actualizarCliente(editing.id, form)
        toast.success('Cliente actualizado')
      } else {
        await AdminAPI.crearCliente(form)
        toast.success('Cliente registrado correctamente')
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

  // Valida el DNI (8 dígitos) y consulta RENIEC para autocompletar nombres y apellidos en el formulario
  const buscarDni = async () => {
    if (!/^\d{8}$/.test(form.dni)) {
      setFormError('Ingresa un DNI de 8 dígitos')
      return
    }
    setBuscandoDni(true)
    setFormError('')
    try {
      const p = await AdminAPI.buscarDni(form.dni)
      setForm((f) => ({ ...f, nombres: p.nombres, apellidos: p.apellidos }))
      toast.success('Datos obtenidos de RENIEC')
    } catch (err) {
      setFormError(err.message)
      toast.error(err.message)
    } finally {
      setBuscandoDni(false)
    }
  }

  // Pide confirmación y elimina el cliente si el usuario acepta
  const eliminar = async (c) => {
    const ok = await confirm({
      title: 'Eliminar cliente',
      message: `¿Eliminar al cliente "${c.nombreCompleto}"?`,
      confirmText: 'Eliminar',
      danger: true,
    })
    if (!ok) return
    try {
      await AdminAPI.eliminarCliente(c.id)
      toast.success('Cliente eliminado')
      cargar()
    } catch (err) {
      toast.error(err.message)
    }
  }

  const Th = ({ label, col }) => (
    <th
      className={'sortable' + (t.sort?.key === col ? ' is-sorted' : '')}
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
          <h2>Clientes</h2>
          <p>Base de datos de clientes de la tienda</p>
        </div>
        <button className="btn btn-primary" onClick={abrirCrear}>
          <i className="bi bi-person-plus" /> Nuevo cliente
        </button>
      </div>

      {clientes === null ? (
        <TableSkeleton />
      ) : (
        <div className="table-wrap">
          <TableToolbar query={t.query} onSearch={t.onSearch} total={t.total} />
          <div className="table-scroll">
            <table className="table">
              <thead>
                <tr>
                  <Th label="Cliente" col="nombreCompleto" />
                  <Th label="DNI" col="dni" />
                  <th>Contacto</th>
                  <th>Dirección</th>
                  <th style={{ textAlign: 'right' }}>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {t.paged.length === 0 && (
                  <tr>
                    <td colSpan={5}>
                      <div className="empty">
                        <i className="bi bi-people" />
                        <div>No hay clientes que coincidan</div>
                      </div>
                    </td>
                  </tr>
                )}
                {t.paged.map((c) => (
                  <tr key={c.id}>
                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                        <div className="userchip-avatar" style={{ width: 38, height: 38 }}>
                          <i className="bi bi-person-fill" />
                        </div>
                        <div>
                          <div className="fw-bold">{c.nombreCompleto}</div>
                          <small className="text-muted">{c.email || 'sin email'}</small>
                        </div>
                      </div>
                    </td>
                    <td><strong style={{ color: 'var(--accent)', fontFamily: 'monospace' }}>{c.dni}</strong></td>
                    <td>
                      {c.telefono ? (
                        <span><i className="bi bi-telephone-fill" style={{ color: 'var(--accent)' }} /> {c.telefono}</span>
                      ) : (
                        <span className="text-muted">—</span>
                      )}
                    </td>
                    <td>{c.direccion ? <small>{c.direccion}</small> : <span className="text-muted">—</span>}</td>
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
          title={editing ? 'Editar cliente' : 'Nuevo cliente'}
          icon={editing ? 'bi-pencil-square' : 'bi-person-plus'}
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
            <div className="form-grid">
              <div className="field">
                <label className="label">DNI *{editing && ' (no editable)'}</label>
                <div style={{ display: 'flex', gap: '0.4rem' }}>
                  <input
                    className="input"
                    value={form.dni}
                    onChange={cambiar('dni')}
                    pattern="[0-9]{8}"
                    maxLength={8}
                    placeholder="8 dígitos"
                    required
                    disabled={!!editing}
                  />
                  {/* Botón que consulta RENIEC con el DNI y autocompleta nombres/apellidos (solo al crear) */}
                  {!editing && (
                    <button
                      type="button"
                      className="btn btn-outline"
                      onClick={buscarDni}
                      disabled={buscandoDni}
                      title="Autocompletar con RENIEC"
                    >
                      <i className="bi bi-search" /> {buscandoDni ? '...' : 'Buscar'}
                    </button>
                  )}
                </div>
              </div>
              <div className="field">
                <label className="label">Teléfono</label>
                <input className="input" value={form.telefono} onChange={cambiar('telefono')} placeholder="9 dígitos" />
              </div>
              <div className="field">
                <label className="label">Nombres *</label>
                <input className="input" value={form.nombres} onChange={cambiar('nombres')} required />
              </div>
              <div className="field">
                <label className="label">Apellidos *</label>
                <input className="input" value={form.apellidos} onChange={cambiar('apellidos')} required />
              </div>
              <div className="field full">
                <label className="label">Email</label>
                <input className="input" type="email" value={form.email} onChange={cambiar('email')} />
              </div>
              <div className="field full">
                <label className="label">Dirección</label>
                <input className="input" value={form.direccion} onChange={cambiar('direccion')} />
              </div>
            </div>
            <button type="submit" hidden />
          </form>
        </Modal>
      )}
    </>
  )
}
