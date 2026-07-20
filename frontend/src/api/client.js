// Cliente HTTP sobre fetch con JWT: agrega Authorization, hace refresh silencioso
// del access token (5 min) y reintenta la peticion; guarda sesion en localStorage.

const BASE = '/api'

export const USER_KEY = 'gs_user'
export const TOKEN_KEY = 'gs_token'
export const REFRESH_KEY = 'gs_refresh'

// Lectores de la sesion actual guardada en localStorage (usuario, access token, refresh token)
export const getUser = () => {
  const raw = localStorage.getItem(USER_KEY)
  return raw ? JSON.parse(raw) : null
}
export const getToken = () => localStorage.getItem(TOKEN_KEY)
export const getRefreshToken = () => localStorage.getItem(REFRESH_KEY)

// Guarda toda la sesion de una vez (se usa justo despues de un login exitoso)
export function saveSession({ accessToken, refreshToken, user }) {
  if (accessToken) localStorage.setItem(TOKEN_KEY, accessToken)
  if (refreshToken) localStorage.setItem(REFRESH_KEY, refreshToken)
  if (user) localStorage.setItem(USER_KEY, JSON.stringify(user))
}
// Actualiza solo los datos del usuario (ej. tras refrescar /auth/me)
export function saveUser(user) {
  if (user) localStorage.setItem(USER_KEY, JSON.stringify(user))
}
// Actualiza solo los tokens (usado por el refresh silencioso, no toca el usuario)
function saveTokens(accessToken, refreshToken) {
  if (accessToken) localStorage.setItem(TOKEN_KEY, accessToken)
  if (refreshToken) localStorage.setItem(REFRESH_KEY, refreshToken)
}
// Borra toda la sesion (logout o cuando el refresh token ya no sirve)
export function clearSession() {
  localStorage.removeItem(USER_KEY)
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_KEY)
}

// Manda al usuario a la pantalla de login del admin (si no esta ya ahi)
function redirigirLogin() {
  if (!location.pathname.startsWith('/admin/login')) location.assign('/admin/login')
}

// --- refresh en un solo vuelo (evita disparos en paralelo) ---
// Si varias peticiones reciben 401 al mismo tiempo, todas comparten la misma
// promesa de refresh en vez de pedir un token nuevo cada una (single-flight).
let refreshPromise = null
function refreshAccessToken() {
  const rt = getRefreshToken()
  if (!rt) return Promise.resolve(null)
  if (!refreshPromise) {
    refreshPromise = fetch(BASE + '/auth/refresh', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: rt }),
    })
      .then(async (res) => {
        if (!res.ok) throw new Error('refresh failed')
        const data = await res.json()
        saveTokens(data.accessToken, data.refreshToken)
        return data.accessToken
      })
      .catch(() => {
        clearSession()
        return null
      })
      .finally(() => {
        refreshPromise = null
      })
  }
  return refreshPromise
}

// Arma y dispara un fetch crudo: agrega el header Authorization si hay token
// y serializa el body a JSON cuando corresponde. No maneja errores ni refresh.
function rawRequest(path, { method, body, token }) {
  const headers = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (token) headers['Authorization'] = 'Bearer ' + token
  return fetch(BASE + path, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })
}

// Funcion central que usan get/post/put/patch/del: hace la peticion con el
// access token actual y, si el backend responde 401 (token vencido), intenta
// renovarlo una sola vez con refreshAccessToken() y reintenta la peticion.
// Si el refresh tambien falla, limpia la sesion y manda al login.
async function request(path, { method = 'GET', body } = {}) {
  const noRefresh = path.startsWith('/auth/login') || path.startsWith('/auth/refresh')
  let res = await rawRequest(path, { method, body, token: getToken() })

  if (res.status === 401 && !noRefresh && getRefreshToken()) {
    const nuevo = await refreshAccessToken()
    if (nuevo) {
      res = await rawRequest(path, { method, body, token: nuevo })
    } else {
      clearSession()
      redirigirLogin()
      throw new Error('Sesión expirada')
    }
  }

  if (!res.ok) {
    if (res.status === 401 && !noRefresh) {
      clearSession()
      redirigirLogin()
    }
    let msg = 'Ocurrió un error'
    let data = null
    try {
      data = await res.json()
      if (data && data.error) msg = data.error
    } catch {
      /* sin cuerpo JSON */
    }
    // Adjuntamos status y cuerpo para que la pantalla pueda reaccionar (p. ej. el bloqueo 429 del login).
    const err = new Error(msg)
    err.status = res.status
    err.data = data
    throw err
  }

  // Sin contenido (ej. DELETE) o respuesta JSON/texto segun el content-type
  if (res.status === 204) return null
  const ct = res.headers.get('content-type') || ''
  return ct.includes('application/json') ? res.json() : res.text()
}

// Descarga un archivo (blob) autenticado, p. ej. el reporte PDF.
export async function downloadBlob(path, filename) {
  let res = await rawRequest(path, { method: 'GET', token: getToken() })
  if (res.status === 401 && getRefreshToken()) {
    const nuevo = await refreshAccessToken()
    if (nuevo) res = await rawRequest(path, { method: 'GET', token: nuevo })
  }
  if (!res.ok) {
    let msg = 'No se pudo generar el archivo'
    try {
      const d = await res.json()
      if (d && d.error) msg = d.error
    } catch {
      /* ignore */
    }
    throw new Error(msg)
  }
  const blob = await res.blob()
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(url)
}

// API publica del cliente HTTP: metodos cortos que usan endpoints.js
// para llamar al backend (todos pasan por request(), con auth y refresh incluidos).
export const api = {
  get: (p) => request(p),
  post: (p, body) => request(p, { method: 'POST', body }),
  put: (p, body) => request(p, { method: 'PUT', body }),
  patch: (p, body) => request(p, { method: 'PATCH', body }),
  del: (p) => request(p, { method: 'DELETE' }),
}
