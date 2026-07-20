import { useEffect, useState } from 'react'

/**
 * Verifica EN VIVO (mientras el usuario escribe) si un valor ya existe en la base de datos.
 * Espera 500 ms sin teclear (debounce) y recién ahí pregunta al backend: no hace falta
 * hacer click ni presionar Enter. Devuelve { duplicado, mensaje, verificando }.
 */
export function useDuplicado(valor, verificar, { delay = 500, minLargo = 3, activo = true } = {}) {
  const [estado, setEstado] = useState({ duplicado: false, mensaje: '', verificando: false })

  useEffect(() => {
    const v = (valor || '').trim()
    if (!activo || v.length < minLargo) {
      setEstado({ duplicado: false, mensaje: '', verificando: false })
      return
    }
    let cancelado = false
    setEstado((e) => ({ ...e, verificando: true }))
    const t = setTimeout(async () => {
      try {
        const r = await verificar(v)
        if (!cancelado) setEstado({ duplicado: !!r.existe, mensaje: r.mensaje || '', verificando: false })
      } catch {
        if (!cancelado) setEstado({ duplicado: false, mensaje: '', verificando: false })
      }
    }, delay)
    return () => { cancelado = true; clearTimeout(t) }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [valor, activo, delay, minLargo])

  return estado
}
