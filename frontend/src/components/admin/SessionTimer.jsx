import { useEffect, useState } from 'react'
import { getToken } from '../../api/client'

// Lee el claim exp del access token y cuenta atras hasta que expira.
function expDeToken(token) {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    return payload.exp ? payload.exp * 1000 : null
  } catch {
    return null
  }
}

// Indicador flotante de sesión: muestra una cuenta regresiva en vivo hasta que
// expire el access token. Como el tiempo restante se recalcula leyendo el token
// en cada segundo (no se guarda aparte), el contador se reinicia solo cuando el
// token se refresca (su exp cambia).
export default function SessionTimer() {
  const [restante, setRestante] = useState(null)

  useEffect(() => {
    // Se ejecuta cada segundo: relee el token, calcula su expiración (exp) y
    // actualiza los segundos restantes.
    const tick = () => {
      const token = getToken()
      const exp = token ? expDeToken(token) : null
      if (!exp) {
        setRestante(null)
        return
      }
      setRestante(Math.max(0, Math.floor((exp - Date.now()) / 1000)))
    }
    tick()
    const id = setInterval(tick, 1000)
    return () => clearInterval(id)
  }, [])

  // Sin token válido (o sin exp) no hay nada que contar: no se muestra el widget.
  if (restante === null) return null
  const m = Math.floor(restante / 60)
  const s = restante % 60
  const bajo = restante <= 60

  return (
    <div className={'session-timer' + (bajo ? ' session-timer-low' : '')} title="Tiempo hasta renovar la sesión">
      <i className="bi bi-clock-history" />
      <span>Sesión {m}:{String(s).padStart(2, '0')}</span>
    </div>
  )
}
