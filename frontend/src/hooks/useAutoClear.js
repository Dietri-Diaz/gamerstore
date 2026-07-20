import { useEffect } from 'react'

/**
 * Limpia solo un mensaje (error/aviso) después de unos segundos.
 * Evita que un error quede estático en pantalla una vez que ya se leyó.
 */
export function useAutoClear(valor, limpiar, ms = 5000) {
  useEffect(() => {
    if (!valor) return
    const t = setTimeout(() => limpiar(''), ms)
    return () => clearTimeout(t)
  }, [valor, limpiar, ms])
}
