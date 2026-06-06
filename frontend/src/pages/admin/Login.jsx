import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext.jsx'
import { getUser } from '../../api/client.js'
import Alert from '../../components/ui/Alert.jsx'

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (getUser()) navigate('/admin', { replace: true })
  }, [navigate])

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await login(username, password)
      navigate('/admin')
    } catch (err) {
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

          {error && <Alert type="error">{error}</Alert>}

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
                  placeholder="admin123"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  required
                  autoFocus
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
                />
              </div>
            </div>

            <button type="submit" className="btn btn-primary btn-lg btn-block" disabled={loading}>
              <i className="bi bi-box-arrow-in-right" /> {loading ? 'Ingresando...' : 'Ingresar al ERP'}
            </button>
          </form>

          <div className="login-demo">
            <strong style={{ color: 'var(--accent)' }}>
              <i className="bi bi-info-circle" /> Demo:
            </strong>
            <div className="text-muted" style={{ marginTop: '0.25rem' }}>
              Usuario: <code>admin123</code> · Contraseña: <code>gamerstore123</code>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
