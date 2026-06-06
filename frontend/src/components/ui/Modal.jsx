import { useEffect } from 'react'

// Modal propio (sin dependencias). Cierra con la X, click en el fondo o Escape.
export default function Modal({ title, icon, onClose, children, footer, size }) {
  useEffect(() => {
    const onKey = (e) => e.key === 'Escape' && onClose()
    document.addEventListener('keydown', onKey)
    document.body.style.overflow = 'hidden'
    return () => {
      document.removeEventListener('keydown', onKey)
      document.body.style.overflow = ''
    }
  }, [onClose])

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className={'modal' + (size === 'sm' ? ' modal-sm' : '')} onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h5>
            {icon && <i className={'bi ' + icon} />} {title}
          </h5>
          <button className="modal-close" onClick={onClose} aria-label="Cerrar">
            &times;
          </button>
        </div>
        <div className="modal-body">{children}</div>
        {footer && <div className="modal-footer">{footer}</div>}
      </div>
    </div>
  )
}
