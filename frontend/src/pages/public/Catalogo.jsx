import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { PublicAPI } from '../../api/endpoints.js'
import ProductCard from '../../components/public/ProductCard.jsx'
import ProductGridSkeleton from '../../components/public/ProductGridSkeleton.jsx'

// Página de catálogo público: lista productos filtrando por categoría y/o búsqueda de texto (query params)
export default function Catalogo() {
  const [searchParams, setSearchParams] = useSearchParams()
  const categoria = searchParams.get('categoria') || ''
  const q = searchParams.get('q') || ''

  const [categorias, setCategorias] = useState([])
  const [productos, setProductos] = useState(null)
  const [search, setSearch] = useState(q)

  // Carga la lista de categorías una sola vez al montar, para armar el filtro lateral
  useEffect(() => {
    PublicAPI.categorias()
      .then((list) => setCategorias(list.map((c) => c.nombre)))
      .catch(() => setCategorias([]))
  }, [])

  // Recarga los productos cada vez que cambia la categoría o el texto de búsqueda en la URL
  useEffect(() => {
    setProductos(null)
    PublicAPI.productos(categoria, q)
      .then(setProductos)
      .catch(() => setProductos([]))
    setSearch(q)
  }, [categoria, q])

  // Actualiza los query params al elegir una categoría desde el filtro lateral
  const aplicarCategoria = (cat) => {
    const next = {}
    if (cat) next.categoria = cat
    if (q) next.q = q
    setSearchParams(next)
  }

  // Maneja el submit del formulario de búsqueda: actualiza los query params con el texto ingresado
  const buscar = (e) => {
    e.preventDefault()
    const next = {}
    if (categoria) next.categoria = categoria
    if (search) next.q = search
    setSearchParams(next)
  }

  return (
    <section className="section container">
      <div style={{ marginBottom: '1.5rem' }}>
        <span className="eyebrow">
          <i className="bi bi-grid-3x3-gap-fill" /> Catálogo completo
        </span>
        <h1 style={{ fontSize: '1.8rem', marginTop: '0.75rem' }}>
          Encuentra tu <span className="accent">arma ideal</span>
        </h1>
        {q && (
          <p className="text-muted">
            Resultados para: <strong style={{ color: 'var(--text-strong)' }}>{q}</strong>
          </p>
        )}
      </div>

      <div className="catalog-layout">
        {/* FILTROS */}
        <aside>
          <div className="filter-card">
            <h5>
              <i className="bi bi-funnel" style={{ color: 'var(--accent)' }} /> Filtros
            </h5>

            <form onSubmit={buscar} style={{ marginBottom: '1rem' }}>
              <label className="label">Búsqueda</label>
              <input
                className="input"
                type="search"
                placeholder="Nombre del producto..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
              <button type="submit" className="btn btn-primary btn-sm btn-block" style={{ marginTop: '0.5rem' }}>
                <i className="bi bi-search" /> Buscar
              </button>
            </form>

            <hr className="divider" />

            <label className="label">Categorías</label>
            <div className="filter-list">
              <button
                className={'filter-link' + (!categoria ? ' active' : '')}
                onClick={() => aplicarCategoria('')}
              >
                <i className="bi bi-grid" />
                <span>Todas</span>
              </button>
              {categorias.map((c) => (
                <button
                  key={c}
                  className={'filter-link' + (categoria.toLowerCase() === c.toLowerCase() ? ' active' : '')}
                  onClick={() => aplicarCategoria(c)}
                >
                  <i className="bi bi-tag" />
                  <span>{c}</span>
                </button>
              ))}
            </div>
          </div>
        </aside>

        {/* GRID */}
        <div>
          {productos === null ? (
            <ProductGridSkeleton count={6} cols="grid-3" />
          ) : productos.length === 0 ? (
            <div className="empty">
              <i className="bi bi-search" />
              <h3 style={{ fontSize: '1.2rem' }}>Sin resultados</h3>
              <p>Intenta con otra búsqueda o categoría.</p>
            </div>
          ) : (
            <>
              <p className="text-muted" style={{ marginBottom: '1rem' }}>
                <span className="badge badge-accent">
                  <i className="bi bi-box-seam" /> {productos.length} productos
                </span>
              </p>
              <div className="product-grid grid-3">
                {productos.map((p) => (
                  <ProductCard key={p.id} p={p} />
                ))}
              </div>
            </>
          )}
        </div>
      </div>
    </section>
  )
}
