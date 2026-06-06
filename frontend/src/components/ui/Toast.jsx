import { createContext, useContext, useState, useCallback } from 'react'

/**
 * Sistema de notificaciones "toast" (avisos en la esquina superior derecha).
 * Uso:  const toast = useToast();  toast.success('Guardado');  toast.error('Falló')
 * Cada aviso se cierra solo a los 3.5s (o con la X).
 */
const ToastContext = createContext(null)
let contador = 0 // para dar un id único a cada toast

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([])

  const quitar = useCallback((id) => {
    setToasts((lista) => lista.filter((t) => t.id !== id))
  }, [])

  const mostrar = useCallback(
    (mensaje, tipo) => {
      const id = ++contador
      setToasts((lista) => [...lista, { id, mensaje, tipo }])
      setTimeout(() => quitar(id), 3500)
    },
    [quitar]
  )

  const toast = {
    success: (m) => mostrar(m, 'success'),
    error: (m) => mostrar(m, 'error'),
    info: (m) => mostrar(m, 'info'),
  }

  const iconos = {
    success: 'bi-check-circle-fill',
    error: 'bi-exclamation-triangle-fill',
    info: 'bi-info-circle-fill',
  }

  return (
    <ToastContext.Provider value={toast}>
      {children}
      <div className="toaster">
        {toasts.map((t) => (
          <div key={t.id} className={'toast toast-' + t.tipo}>
            <i className={'toast-icon bi ' + iconos[t.tipo]} />
            <span className="toast-msg">{t.mensaje}</span>
            <button className="toast-close" onClick={() => quitar(t.id)} aria-label="Cerrar">
              &times;
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

export const useToast = () => useContext(ToastContext)
