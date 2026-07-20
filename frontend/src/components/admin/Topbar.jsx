import { useLocation } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext.jsx'

// Título a mostrar en la barra superior según la ruta actual del panel admin.
// OJO: al agregar una pantalla nueva al panel, súmala también aquí; si no, la
// barra superior mostrará "Panel" en vez de su nombre.
const TITLES = {
  '/admin': 'Dashboard',
  '/admin/productos': 'Productos',
  '/admin/categorias': 'Categorías',
  '/admin/clientes': 'Clientes',
  '/admin/pedidos': 'Pedidos',
  '/admin/ventas': 'Ventas',
  '/admin/pagos': 'Pagos',
  '/admin/usuarios': 'Usuarios',
}

// Barra superior del panel admin: título de la sección actual y avatar/nombre
// del usuario logueado (a la derecha).
export default function Topbar() {
  const { user } = useAuth()
  const { pathname } = useLocation()
  const title = TITLES[pathname] || 'Panel' // ruta sin título propio -> "Panel"
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
