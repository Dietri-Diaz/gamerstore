// Agrupa todos los endpoints del backend en tres bloques: Auth (login/logout),
// Public (lo que ve cualquier visitante) y Admin (CRUD del panel, protegido).
// Cada funcion arma la ruta/metodo y delega en api.get/post/put/del de client.js.
import { api, getRefreshToken } from './client'

// Convierte un objeto plano en query string (?clave=valor&...), ignorando
// valores vacios/undefined/null. Usado para filtros opcionales en listados.
function qs(params) {
  const sp = new URLSearchParams()
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') sp.append(k, v)
  })
  const s = sp.toString()
  return s ? `?${s}` : ''
}

// --- Autenticacion: login, obtener usuario actual, logout ---
export const AuthAPI = {
  login: (username, password) => api.post('/auth/login', { username, password }),
  me: () => api.get('/auth/me'),
  logout: () => api.post('/auth/logout', { refreshToken: getRefreshToken() }),
}

// --- Publico: datos que se muestran en la tienda sin necesidad de login ---
export const PublicAPI = {
  config: () => api.get('/config'),
  productos: (categoria, q) => api.get('/productos' + qs({ categoria, q })),
  producto: (id) => api.get('/productos/' + id),
  categorias: () => api.get('/categorias'),
}

// --- Admin: CRUD completo del panel, requiere estar logueado (token JWT) ---
export const AdminAPI = {
  dashboard: () => api.get('/admin/dashboard'),

  // CRUD de productos
  productos: () => api.get('/admin/productos'),
  crearProducto: (data) => api.post('/admin/productos', data),
  actualizarProducto: (id, data) => api.put('/admin/productos/' + id, data),
  eliminarProducto: (id) => api.del('/admin/productos/' + id),

  // CRUD de categorias
  categorias: () => api.get('/admin/categorias'),
  crearCategoria: (data) => api.post('/admin/categorias', data),
  actualizarCategoria: (id, data) => api.put('/admin/categorias/' + id, data),
  eliminarCategoria: (id) => api.del('/admin/categorias/' + id),

  // CRUD de clientes + busqueda por DNI en RENIEC
  clientes: () => api.get('/admin/clientes'),
  crearCliente: (data) => api.post('/admin/clientes', data),
  actualizarCliente: (id, data) => api.put('/admin/clientes/' + id, data),
  eliminarCliente: (id) => api.del('/admin/clientes/' + id),
  buscarDni: (dni) => api.get('/admin/clientes/reniec/' + dni),

  // CRUD de pedidos + descarga de reporte en PDF
  pedidos: () => api.get('/admin/pedidos'),
  crearPedido: (data) => api.post('/admin/pedidos', data),
  actualizarPedido: (id, data) => api.put('/admin/pedidos/' + id, data),
  eliminarPedido: (id) => api.del('/admin/pedidos/' + id),
  reportePedidosUrl: (params) => '/admin/pedidos/reporte.pdf' + qs(params || {}),

  // CRUD de usuarios del panel admin
  usuarios: () => api.get('/admin/usuarios'),
  crearUsuario: (data) => api.post('/admin/usuarios', data),
  actualizarUsuario: (id, data) => api.put('/admin/usuarios/' + id, data),
  eliminarUsuario: (id) => api.del('/admin/usuarios/' + id),

  // Subida de imagenes (multipart/form-data): no usa api.post porque el body
  // es un FormData, no JSON; arma el header Authorization a mano.
  subirImagen: (formData) => fetch('/api/admin/uploads', {
    method: 'POST',
    headers: { Authorization: 'Bearer ' + (localStorage.getItem('gs_token') || '') },
    body: formData,
  }).then(async (res) => {
    if (!res.ok) {
      let msg = 'No se pudo subir la imagen'
      try { const d = await res.json(); if (d && d.error) msg = d.error } catch {}
      throw new Error(msg)
    }
    return res.json()
  }),
}
