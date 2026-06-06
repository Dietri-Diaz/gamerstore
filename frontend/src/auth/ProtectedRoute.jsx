import { Navigate, Outlet } from 'react-router-dom'
import { getToken } from '../api/client'

// Si no hay token, redirige al login del panel.
export default function ProtectedRoute() {
  return getToken() ? <Outlet /> : <Navigate to="/admin/login" replace />
}
