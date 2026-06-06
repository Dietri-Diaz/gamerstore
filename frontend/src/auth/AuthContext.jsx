import { createContext, useContext, useState } from 'react'
import { AuthAPI } from '../api/endpoints'
import { TOKEN_KEY, USER_KEY, clearSession, getToken } from '../api/client'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? JSON.parse(raw) : null
  })

  const login = async (username, password) => {
    const res = await AuthAPI.login(username, password)
    localStorage.setItem(TOKEN_KEY, res.token)
    const u = { username: res.username, nombre: res.nombre, rol: res.rol }
    localStorage.setItem(USER_KEY, JSON.stringify(u))
    setUser(u)
    return u
  }

  const logout = () => {
    clearSession()
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, login, logout, isAuth: !!user && !!getToken() }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)
