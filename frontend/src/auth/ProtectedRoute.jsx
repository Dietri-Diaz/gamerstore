import { Navigate, Outlet } from 'react-router-dom'
import { getUser } from '../api/client'

// Si no hay un usuario logueado, redirige al login del panel.
export default function ProtectedRoute() {
  return getUser() ? <Outlet /> : <Navigate to="/admin/login" replace />
}
