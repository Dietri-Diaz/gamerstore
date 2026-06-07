import { NavLink, Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext.jsx'

const links = [
  { to: '/admin', label: 'Dashboard', icon: 'bi-grid-1x2-fill', end: true },
  { to: '/admin/productos', label: 'Productos', icon: 'bi-box-seam-fill' },
  { to: '/admin/categorias', label: 'Categorías', icon: 'bi-tags-fill' },
  { to: '/admin/clientes', label: 'Clientes', icon: 'bi-people-fill' },
  { to: '/admin/pedidos', label: 'Pedidos', icon: 'bi-bag-check-fill' },
]

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

      <nav className="sidebar-nav">
        {links.map((l) => (
          <NavLink key={l.to} to={l.to} end={l.end} className="sidebar-link" title={l.label}>
            <i className={'bi ' + l.icon} />
            <span>{l.label}</span>
          </NavLink>
        ))}
      </nav>

      <div className="sidebar-foot">
        <Link to="/" className="sidebar-link" title="Volver a la tienda">
          <i className="bi bi-arrow-left-short" />
          <span>Volver a la tienda</span>
        </Link>
        <button
          className="sidebar-link"
          title="Cerrar sesión"
          onClick={() => {
            logout()
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
