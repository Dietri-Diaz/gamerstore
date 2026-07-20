import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext.jsx'
import { getUser } from '../../api/client.js'
import { useAutoClear } from '../../hooks/useAutoClear.js'
import Alert from '../../components/ui/Alert.jsx'

// Página de login del panel admin (ERP)
export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [bloqueo, setBloqueo] = useState(0) // segundos restantes del bloqueo por intentos fallidos (429)
  const prevBloqueo = useRef(0)

  // Si ya hay una sesión guardada, saltamos directo al panel sin mostrar el formulario
  useEffect(() => {
    if (getUser()) navigate('/admin', { replace: true })
  }, [navigate])

  // Cuenta atrás del bloqueo: mientras queden segundos, baja de a uno cada 1s
  useEffect(() => {
    if (bloqueo <= 0) return
    const t = setInterval(() => setBloqueo((s) => s - 1), 1000)
    return () => clearInterval(t)
  }, [bloqueo])

  // Cuando el bloqueo llega a 0 (detecta la transición desde >0), limpia el formulario
  // automáticamente para que el usuario reintente con los campos en blanco.
  useEffect(() => {
    if (prevBloqueo.current > 0 && bloqueo === 0) {
      setUsername('')
      setPassword('')
      setError('')
    }
    prevBloqueo.current = bloqueo
  }, [bloqueo])

  // Mientras no haya bloqueo activo, el mensaje de error se borra solo a los 5s
  useAutoClear(error, setError)

  // Envía usuario/contraseña, y si el login es correcto redirige al panel admin
  const submit = async (e) => {
    e.preventDefault()
    if (bloqueo > 0) return
    setError('')
    setLoading(true)
    try {
      await login(username, password)
      navigate('/admin')
    } catch (err) {
      if (err.status === 429 && err.data?.segundosRestantes) {
        setBloqueo(err.data.segundosRestantes)
      }
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-wrap">
      <div className="login-card">
        <div className="login-side">
          <i className="bi bi-shield-lock" />
          <h2>Panel ERP</h2>
          <p>Acceso exclusivo para administradores de GamerStore.</p>
          <Link to="/">
            <i className="bi bi-arrow-left" /> Volver a la tienda
          </Link>
        </div>

        <div className="login-form">
          <h1 style={{ fontSize: '1.5rem' }}>
            <i className="bi bi-controller" style={{ color: 'var(--accent)' }} /> GamerStore
          </h1>
          <p className="text-muted" style={{ marginBottom: '1.5rem' }}>
            Inicia sesión para gestionar productos, clientes y ventas.
          </p>

          {/* Mientras hay bloqueo activo mostramos el contador; si no, el error normal (se borra solo a los 5s) */}
          {bloqueo > 0 ? (
            <Alert type="error">Demasiados intentos fallidos. Espera {bloqueo} segundo(s).</Alert>
          ) : (
            error && <Alert type="error">{error}</Alert>
          )}

          <form onSubmit={submit}>
            <div className="field">
              <label className="label">Usuario o Email</label>
              <div className="input-group">
                <span className="input-addon">
                  <i className="bi bi-person-fill" />
                </span>
                <input
                  className="input"
                  type="text"
                  placeholder="Tu usuario o email"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  required
                  autoFocus
                  disabled={bloqueo > 0}
                />
              </div>
            </div>

            <div className="field">
              <label className="label">Contraseña</label>
              <div className="input-group">
                <span className="input-addon">
                  <i className="bi bi-lock-fill" />
                </span>
                <input
                  className="input"
                  type="password"
                  placeholder="••••••••"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                  disabled={bloqueo > 0}
                />
              </div>
            </div>

            <button type="submit" className="btn btn-primary btn-lg btn-block" disabled={loading || bloqueo > 0}>
              <i className="bi bi-box-arrow-in-right" />{' '}
              {bloqueo > 0 ? `Bloqueado (${bloqueo}s)` : loading ? 'Ingresando...' : 'Ingresar al ERP'}
            </button>
          </form>
        </div>
      </div>
    </div>
  )
}
