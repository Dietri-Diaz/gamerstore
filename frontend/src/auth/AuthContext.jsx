import { createContext, useContext, useState } from 'react'
import { AuthAPI } from '../api/endpoints'
import { USER_KEY, clearSession, getUser } from '../api/client'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => getUser())

  const login = async (username, password) => {
    // El backend valida y devuelve { username, nombre, rol }
    const u = await AuthAPI.login(username, password)
    localStorage.setItem(USER_KEY, JSON.stringify(u))
    setUser(u)
    return u
  }

  const logout = () => {
    clearSession()
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, login, logout, isAuth: !!user }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)
