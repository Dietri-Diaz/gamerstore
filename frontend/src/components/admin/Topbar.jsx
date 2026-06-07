import { useLocation } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext.jsx'

const TITLES = {
  '/admin': 'Dashboard',
  '/admin/productos': 'Productos',
  '/admin/categorias': 'Categorías',
  '/admin/clientes': 'Clientes',
  '/admin/pedidos': 'Pedidos',
}

export default function Topbar() {
  const { user } = useAuth()
  const { pathname } = useLocation()
  const title = TITLES[pathname] || 'Panel'
  const inicial = (user?.nombre || 'A').charAt(0).toUpperCase()

  return (
    <header className="topbar">
      <div>
        <h1 className="topbar-title">{title}</h1>
        <span className="topbar-sub">Panel administrativo</span>
      </div>
      <div className="userchip">
        <div className="userchip-avatar">{inicial}</div>
        <div>
          <div className="userchip-name">{user?.nombre || 'Administrador'}</div>
          <div className="userchip-role">{user?.rol || 'ADMIN'}</div>
        </div>
      </div>
    </header>
  )
}
