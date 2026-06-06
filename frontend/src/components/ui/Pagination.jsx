// Controles de paginación (anterior, números, siguiente).
export default function Pagination({ page, totalPages, total, onPage }) {
  if (total === 0) return null
  const numeros = []
  for (let i = 1; i <= totalPages; i++) numeros.push(i)

  return (
    <div className="pagination">
      <span className="page-info">
        Página {page} de {totalPages} · {total} registros
      </span>
      <div className="page-controls">
        <button className="page-btn" disabled={page <= 1} onClick={() => onPage(page - 1)} aria-label="Anterior">
          <i className="bi bi-chevron-left" />
        </button>
        {numeros.map((n) => (
          <button key={n} className={'page-btn' + (n === page ? ' active' : '')} onClick={() => onPage(n)}>
            {n}
          </button>
        ))}
        <button className="page-btn" disabled={page >= totalPages} onClick={() => onPage(page + 1)} aria-label="Siguiente">
          <i className="bi bi-chevron-right" />
        </button>
      </div>
    </div>
  )
}
