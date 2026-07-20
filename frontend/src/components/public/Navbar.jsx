import { useState } from 'react'
import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext.jsx'
import { useCarrito } from '../../carrito/CarritoContext.jsx'

// Barra de navegación pública: logo, buscador, enlaces del sitio y acceso al panel
// admin (o botón de salir si ya hay una sesión iniciada).
export default function Navbar() {
  const { user, logout } = useAuth()
  const { cantidadTotal } = useCarrito()
  const navigate = useNavigate()
  const [q, setQ] = useState('') // texto del buscador
  const [open, setOpen] = useState(false) // menú móvil abierto/cerrado

  // Al enviar el buscador, navega a /productos pasando el texto como query "q".
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

        {/* Botón hamburguesa: muestra/oculta el menú en pantallas chicas */}
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
          <NavLink to="/seguimiento" className="nav-link" onClick={close}>
            Seguimiento
          </NavLink>
          <NavLink to="/contacto" className="nav-link" onClick={close}>
            Contacto
          </NavLink>
          <NavLink to="/carrito" className="nav-link nav-cart" onClick={close}>
            <span className="nav-cart-icon">
              <i className="bi bi-cart3" />
              {/* Badge con la cantidad de items, solo visible si hay algo en el carrito */}
              {cantidadTotal > 0 && <span className="cart-badge">{cantidadTotal}</span>}
            </span>
            Carrito
          </NavLink>

          {/* Si hay sesión iniciada: link al panel y botón de salir. Si no: link al login admin */}
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
