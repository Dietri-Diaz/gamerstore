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
  reniec: (dni) => api.get('/reniec/' + dni),
  verificarCliente: (data) => api.post('/checkout/cliente', data),
  checkout: (data) => api.post('/checkout', data),
  // Boleta publica de un pedido, verificada por DNI del comprador
  boletaUrl: (pedidoCodigo, dni) => '/checkout/boleta/' + pedidoCodigo + qs({ dni }),
  // Seguimiento publico del pedido: sin login, pero se verifica con el DNI del comprador
  // (igual que la boleta) para que nadie pueda espiar pedidos ajenos adivinando codigos.
  seguimiento: (codigo, dni) => api.get('/checkout/seguimiento/' + codigo + qs({ dni })),
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
  // Anular una venta ya pagada: el backend repone el stock, devuelve el dinero
  // y anula la boleta, todo en una sola transaccion. No se puede deshacer.
  anularPedido: (id, motivo) => api.post('/admin/pedidos/' + id + '/anular', { motivo }),

  // Comprobantes (boletas) emitidos: registro de ventas + resumen + descarga en PDF
  comprobantes: (params) => api.get('/admin/comprobantes' + qs(params || {})),
  resumenVentas: (params) => api.get('/admin/comprobantes/resumen' + qs(params || {})),
  boletaUrl: (id) => '/admin/comprobantes/' + id + '/pdf',
  boletaPedidoUrl: (pedidoId) => '/admin/comprobantes/pedido/' + pedidoId + '/pdf',

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

  // Verificacion de duplicados en vivo (mientras el usuario escribe en el formulario)
  existeProducto: (nombre, id) => api.get('/admin/productos/existe' + qs({ nombre, id })),
  existeCategoria: (nombre, id) => api.get('/admin/categorias/existe' + qs({ nombre, id })),
  existeCliente: (params) => api.get('/admin/clientes/existe' + qs(params || {})),
  existeUsuario: (params) => api.get('/admin/usuarios/existe' + qs(params || {})),
}

// Pasarela de pagos: cobro con Yape (QR + N° de operación) o con tarjeta.
export const PagosAPI = {
  listar: () => api.get('/admin/pagos'),
  config: () => api.get('/admin/pagos/config'),
  pagarTarjeta: (data) => api.post('/admin/pagos/tarjeta', data),
  pagarYape: (data) => api.post('/admin/pagos/yape', data),
  comprobanteUrl: (id) => '/admin/pagos/' + id + '/comprobante.pdf',
}
