import { NavLink, Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext.jsx'

// Secciones del panel admin: ruta, texto e ícono de cada enlace del menú lateral.
const links = [
  { to: '/admin', label: 'Dashboard', icon: 'bi-grid-1x2-fill', end: true },
  { to: '/admin/productos', label: 'Productos', icon: 'bi-box-seam-fill' },
  { to: '/admin/categorias', label: 'Categorías', icon: 'bi-tags-fill' },
  { to: '/admin/clientes', label: 'Clientes', icon: 'bi-people-fill' },
  { to: '/admin/pedidos', label: 'Pedidos', icon: 'bi-bag-check-fill' },
  { to: '/admin/usuarios', label: 'Usuarios', icon: 'bi-person-badge-fill' },
]

// Menú lateral del panel admin: navegación entre secciones, botón para colapsar
// el sidebar y accesos para volver a la tienda o cerrar sesión.
export default function Sidebar({ onToggle, collapsed }) {
  const { logout } = useAuth()
  const navigate = useNavigate()

  return (
    <aside className="sidebar">
      <button className="sidebar-toggle" onClick={onToggle} aria-label="Colapsar menú">
        <i className={'bi ' + (collapsed ? 'bi-chevron-right' : 'bi-chevron-left')} />
      </button>

      <div className="sidebar-brand">
        <i className="bi bi-controller" />
        <div>
          <div className="sidebar-brand-name">GamerStore</div>
          <div className="sidebar-brand-sub">Panel admin</div>
        </div>
      </div>

      {/* Lista de enlaces del menú, generada a partir del arreglo "links" */}
      <nav className="sidebar-nav">
        {links.map((l) => (
          <NavLink key={l.to} to={l.to} end={l.end} className="sidebar-link" title={l.label}>
            <i className={'bi ' + l.icon} />
            <span>{l.label}</span>
          </NavLink>
        ))}
      </nav>

      {/* Accesos rápidos: volver al sitio público o cerrar la sesión actual */}
      <div className="sidebar-foot">
        <Link to="/" className="sidebar-link" title="Volver a la tienda">
          <i className="bi bi-arrow-left-short" />
          <span>Volver a la tienda</span>
        </Link>
        <button
          className="sidebar-link"
          title="Cerrar sesión"
          onClick={async () => {
            await logout()
            navigate('/admin/login')
          }}
        >
          <i className="bi bi-box-arrow-right" />
          <span>Cerrar sesión</span>
        </button>
      </div>
    </aside>
  )
}
