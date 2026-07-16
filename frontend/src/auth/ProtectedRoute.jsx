import { Navigate, Outlet } from 'react-router-dom'
import { getUser } from '../api/client'

// Componente guardia (usado en App.jsx envolviendo las rutas /admin/*).
// Si hay sesion, deja pasar y <Outlet /> renderiza la ruta hija que React Router matcheo.
// Si no hay un usuario logueado, redirige al login del panel.
export default function ProtectedRoute() {
  return getUser() ? <Outlet /> : <Navigate to="/admin/login" replace />
}
