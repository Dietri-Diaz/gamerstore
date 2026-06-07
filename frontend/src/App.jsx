import { Routes, Route, Navigate } from 'react-router-dom'

import PublicLayout from './components/public/PublicLayout.jsx'
import Home from './pages/public/Home.jsx'
import Catalogo from './pages/public/Catalogo.jsx'
import ProductoDetalle from './pages/public/ProductoDetalle.jsx'
import Contacto from './pages/public/Contacto.jsx'

import ProtectedRoute from './auth/ProtectedRoute.jsx'
import AdminLayout from './components/admin/AdminLayout.jsx'
import Login from './pages/admin/Login.jsx'
import Dashboard from './pages/admin/Dashboard.jsx'
import AdminProductos from './pages/admin/AdminProductos.jsx'
import AdminCategorias from './pages/admin/AdminCategorias.jsx'
import AdminClientes from './pages/admin/AdminClientes.jsx'
import AdminPedidos from './pages/admin/AdminPedidos.jsx'

export default function App() {
  return (
    <Routes>
      {/* Zona publica */}
      <Route element={<PublicLayout />}>
        <Route path="/" element={<Home />} />
        <Route path="/productos" element={<Catalogo />} />
        <Route path="/productos/:id" element={<ProductoDetalle />} />
        <Route path="/contacto" element={<Contacto />} />
      </Route>

      {/* Login admin (sin layout) */}
      <Route path="/admin/login" element={<Login />} />

      {/* Panel admin (protegido por JWT) */}
      <Route element={<ProtectedRoute />}>
        <Route element={<AdminLayout />}>
          <Route path="/admin" element={<Dashboard />} />
          <Route path="/admin/productos" element={<AdminProductos />} />
          <Route path="/admin/categorias" element={<AdminCategorias />} />
          <Route path="/admin/clientes" element={<AdminClientes />} />
          <Route path="/admin/pedidos" element={<AdminPedidos />} />
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
