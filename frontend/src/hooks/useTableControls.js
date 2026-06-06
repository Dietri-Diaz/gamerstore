import { useMemo, useState } from 'react'

/**
 * Lógica reutilizable para tablas: búsqueda + orden + paginación.
 * Recibe el arreglo de filas y devuelve solo la "página" lista para pintar,
 * más los controles. Así cada tabla del admin comparte la misma lógica
 * sin repetir código.
 *
 *   const t = useTableControls(productos, { searchKeys: ['nombre'], pageSize: 8 })
 *   t.paged          -> filas de la página actual
 *   t.query/onSearch -> texto del buscador
 *   t.sort/toggleSort('nombre') -> orden por columna
 *   t.page/setPage/totalPages/total -> paginación
 */
export function useTableControls(rows, { searchKeys = [], pageSize = 8, initialSort = null } = {}) {
  const [query, setQuery] = useState('')
  const [sort, setSort] = useState(initialSort) // { key, dir: 'asc' | 'desc' }
  const [page, setPage] = useState(1)

  // 1) Filtrar por el texto del buscador
  const filtradas = useMemo(() => {
    const q = query.trim().toLowerCase()
    if (!q) return rows
    return rows.filter((row) => searchKeys.some((k) => String(row[k] ?? '').toLowerCase().includes(q)))
  }, [rows, query, searchKeys])

  // 2) Ordenar por la columna elegida
  const ordenadas = useMemo(() => {
    if (!sort) return filtradas
    const copia = [...filtradas]
    copia.sort((a, b) => {
      const av = a[sort.key]
      const bv = b[sort.key]
      if (av == null) return 1
      if (bv == null) return -1
      if (typeof av === 'number' && typeof bv === 'number') return av - bv
      return String(av).localeCompare(String(bv), 'es', { numeric: true })
    })
    if (sort.dir === 'desc') copia.reverse()
    return copia
  }, [filtradas, sort])

  // 3) Paginar
  const totalPages = Math.max(1, Math.ceil(ordenadas.length / pageSize))
  const paginaActual = Math.min(page, totalPages)
  const paged = ordenadas.slice((paginaActual - 1) * pageSize, paginaActual * pageSize)

  const toggleSort = (key) => {
    setPage(1)
    setSort((s) => (s && s.key === key ? { key, dir: s.dir === 'asc' ? 'desc' : 'asc' } : { key, dir: 'asc' }))
  }

  const onSearch = (valor) => {
    setQuery(valor)
    setPage(1)
  }

  return {
    paged,
    query,
    onSearch,
    sort,
    toggleSort,
    page: paginaActual,
    setPage,
    totalPages,
    total: ordenadas.length,
  }
}
