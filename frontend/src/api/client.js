// Cliente HTTP minimo sobre fetch.
// - Adjunta el JWT (si existe) en cada peticion.
// - Ante un 401 limpia la sesion y manda al login.
// - Normaliza los errores a Error(mensaje) leyendo { "error": "..." } del backend.

const BASE = '/api'

export const TOKEN_KEY = 'gs_token'
export const USER_KEY = 'gs_user'

export const getToken = () => localStorage.getItem(TOKEN_KEY)

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

async function request(path, { method = 'GET', body } = {}) {
  const headers = {}
  const token = getToken()
  if (token) headers['Authorization'] = `Bearer ${token}`
  if (body !== undefined) headers['Content-Type'] = 'application/json'

  const res = await fetch(BASE + path, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })

  if (res.status === 401) {
    clearSession()
    if (window.location.pathname.startsWith('/admin')) {
      window.location.href = '/admin/login'
    }
    throw new Error('Sesión expirada. Inicia sesión nuevamente.')
  }

  if (!res.ok) {
    let msg = 'Ocurrió un error'
    try {
      const data = await res.json()
      if (data && data.error) msg = data.error
    } catch {
      /* sin cuerpo JSON */
    }
    throw new Error(msg)
  }

  if (res.status === 204) return null
  const ct = res.headers.get('content-type') || ''
  return ct.includes('application/json') ? res.json() : res.text()
}

export const api = {
  get: (p) => request(p),
  post: (p, body) => request(p, { method: 'POST', body }),
  put: (p, body) => request(p, { method: 'PUT', body }),
  patch: (p, body) => request(p, { method: 'PATCH', body }),
  del: (p) => request(p, { method: 'DELETE' }),
}
