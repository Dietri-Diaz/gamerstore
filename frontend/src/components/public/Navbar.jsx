import { useState } from 'react'
import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext.jsx'

export default function Navbar() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [q, setQ] = useState('')
  const [open, setOpen] = useState(false)

  const submit = (e) => {
    e.preventDefault()
    navigate('/productos' + (q ? '?q=' + encodeURIComponent(q) : ''))
    setOpen(false)
  }

  const close = () => setOpen(false)

  return (
    <nav className="nav">
      <div className="container nav-inner">
        <Link to="/" className="nav-brand">
          <i className="bi bi-controller" />
          GamerStore
        </Link>

        <form className="nav-search" onSubmit={submit}>
          <input
            className="input"
            type="search"
            placeholder="Buscar productos..."
            value={q}
            onChange={(e) => setQ(e.target.value)}
          />
        </form>

        <button className="nav-toggle" onClick={() => setOpen((o) => !o)} aria-label="Menú">
          <i className="bi bi-list" />
        </button>

        <div className={'nav-links' + (open ? ' open' : '')}>
          <NavLink to="/" end className="nav-link" onClick={close}>
            Inicio
          </NavLink>
          <NavLink to="/productos" className="nav-link" onClick={close}>
            Catálogo
          </NavLink>
          <NavLink to="/contacto" className="nav-link" onClick={close}>
            Contacto
          </NavLink>

          {user ? (
            <>
              <Link to="/admin" className="nav-link" onClick={close}>
                <i className="bi bi-speedometer2" /> Panel
              </Link>
              <button
                className="btn btn-outline btn-sm"
                onClick={() => {
                  logout()
                  navigate('/')
                  close()
                }}
              >
                <i className="bi bi-box-arrow-right" /> Salir
              </button>
            </>
          ) : (
            <Link to="/admin/login" className="btn btn-outline btn-sm" onClick={close}>
              <i className="bi bi-shield-lock" /> Admin
            </Link>
          )}
        </div>
      </div>
    </nav>
  )
}
