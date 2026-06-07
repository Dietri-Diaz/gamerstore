import { api } from './client'

function qs(params) {
  const sp = new URLSearchParams()
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') sp.append(k, v)
  })
  const s = sp.toString()
  return s ? `?${s}` : ''
}

export const AuthAPI = {
  login: (username, password) => api.post('/auth/login', { username, password }),
}

export const PublicAPI = {
  config: () => api.get('/config'),
  productos: (categoria, q) => api.get('/productos' + qs({ categoria, q })),
  producto: (id) => api.get('/productos/' + id),
  categorias: () => api.get('/categorias'),
}

export const AdminAPI = {
  dashboard: () => api.get('/admin/dashboard'),

  productos: () => api.get('/admin/productos'),
  crearProducto: (data) => api.post('/admin/productos', data),
  actualizarProducto: (id, data) => api.put('/admin/productos/' + id, data),
  eliminarProducto: (id) => api.del('/admin/productos/' + id),

  categorias: () => api.get('/admin/categorias'),
  crearCategoria: (data) => api.post('/admin/categorias', data),
  actualizarCategoria: (id, data) => api.put('/admin/categorias/' + id, data),
  eliminarCategoria: (id) => api.del('/admin/categorias/' + id),

  clientes: () => api.get('/admin/clientes'),
  crearCliente: (data) => api.post('/admin/clientes', data),
  actualizarCliente: (id, data) => api.put('/admin/clientes/' + id, data),
  eliminarCliente: (id) => api.del('/admin/clientes/' + id),

  pedidos: () => api.get('/admin/pedidos'),
  crearPedido: (data) => api.post('/admin/pedidos', data),
  actualizarPedido: (id, data) => api.put('/admin/pedidos/' + id, data),
  eliminarPedido: (id) => api.del('/admin/pedidos/' + id),
}
