import { createContext, useContext, useState, useEffect } from 'react'
import { AuthAPI } from '../api/endpoints'
import { saveSession, saveUser, clearSession, getUser, getToken, getRefreshToken } from '../api/client'

// Contexto de sesion del admin: expone el usuario logueado y las acciones
// login/logout. Cualquier componente lo lee con el hook useAuth() de abajo.
const AuthContext = createContext(null)

// Provee la sesion a toda la app (envuelto en main.jsx). Guarda el usuario en
// estado de React y en localStorage (via client.js) para sobrevivir un refresh.
export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => getUser())

  // Al arrancar, si hay sesion guardada, la validamos contra /auth/me
  // (si el access expiro pero el refresh es valido, client.js lo renueva solo).
  useEffect(() => {
    if (getToken() || getRefreshToken()) {
      AuthAPI.me()
        .then((u) => {
          setUser(u)
          saveUser(u)
        })
        .catch(() => {
          clearSession()
          setUser(null)
        })
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // Llama a /auth/login, guarda access+refresh token y el usuario, y actualiza el estado
  const login = async (username, password) => {
    const r = await AuthAPI.login(username, password)
    const u = { username: r.username, nombre: r.nombre, rol: r.rol }
    saveSession({ accessToken: r.accessToken, refreshToken: r.refreshToken, user: u })
    setUser(u)
    return u
  }

  // Avisa al backend para invalidar el refresh token y limpia la sesion local
  // (aunque la llamada al backend falle, igual se cierra sesion en el navegador)
  const logout = async () => {
    try {
      await AuthAPI.logout()
    } catch {
      /* aunque falle, limpiamos localmente */
    }
    clearSession()
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, login, logout, isAuth: !!user }}>
      {children}
    </AuthContext.Provider>
  )
}

// Hook de conveniencia para leer { user, login, logout, isAuth } desde cualquier componente
export const useAuth = () => useContext(AuthContext)
