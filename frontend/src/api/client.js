// Cliente HTTP mínimo sobre fetch.
// Normaliza los errores a Error(mensaje) leyendo { "error": "..." } del backend.
// (La seguridad real con token/Spring Security se agregará en el avance final.)

const BASE = '/api'

export const USER_KEY = 'gs_user'

export const getUser = () => {
  const raw = localStorage.getItem(USER_KEY)
  return raw ? JSON.parse(raw) : null
}

export function clearSession() {
  localStorage.removeItem(USER_KEY)
}

async function request(path, { method = 'GET', body } = {}) {
  const headers = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'

  const res = await fetch(BASE + path, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })

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
