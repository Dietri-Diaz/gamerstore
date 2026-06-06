import { createContext, useContext, useState, useCallback } from 'react'
import Modal from './Modal.jsx'

/**
 * Diálogo de confirmación bonito (reemplaza al window.confirm del navegador).
 * Uso:
 *   const confirm = useConfirm()
 *   const ok = await confirm({ message: '¿Eliminar?', confirmText: 'Eliminar', danger: true })
 *   if (!ok) return
 * Devuelve una promesa con true (aceptar) o false (cancelar).
 */
const ConfirmContext = createContext(null)

export function ConfirmProvider({ children }) {
  const [estado, setEstado] = useState(null) // { opciones, resolver }

  const confirm = useCallback((opciones) => {
    return new Promise((resolver) => setEstado({ opciones, resolver }))
  }, [])

  const cerrar = (resultado) => {
    if (estado) estado.resolver(resultado)
    setEstado(null)
  }

  return (
    <ConfirmContext.Provider value={confirm}>
      {children}
      {estado && (
        <Modal
          title={estado.opciones.title || 'Confirmar'}
          icon={estado.opciones.danger ? 'bi-exclamation-triangle-fill' : 'bi-question-circle'}
          size="sm"
          onClose={() => cerrar(false)}
          footer={
            <>
              <button className="btn btn-ghost" onClick={() => cerrar(false)}>
                Cancelar
              </button>
              <button
                className={'btn ' + (estado.opciones.danger ? 'btn-danger' : 'btn-primary')}
                onClick={() => cerrar(true)}
                autoFocus
              >
                {estado.opciones.confirmText || 'Confirmar'}
              </button>
            </>
          }
        >
          <p style={{ color: 'var(--text)' }}>{estado.opciones.message}</p>
        </Modal>
      )}
    </ConfirmContext.Provider>
  )
}

export const useConfirm = () => useContext(ConfirmContext)
