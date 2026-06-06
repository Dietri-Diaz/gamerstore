// Barra superior de una tabla: buscador en vivo + contador (o contenido a la derecha).
export default function TableToolbar({ query, onSearch, total, right }) {
  return (
    <div className="table-toolbar">
      <div className="table-search">
        <i className="bi bi-search" />
        <input
          className="input"
          type="search"
          placeholder="Buscar..."
          value={query}
          onChange={(e) => onSearch(e.target.value)}
        />
      </div>
      {right || <span className="table-count">{total} registros</span>}
    </div>
  )
}
