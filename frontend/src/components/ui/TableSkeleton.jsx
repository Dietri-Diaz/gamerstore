import Skeleton from './Skeleton.jsx'

// Esqueleto de carga para las tablas del admin (buscador + filas).
export default function TableSkeleton({ rows = 6 }) {
  return (
    <div className="table-wrap">
      <div className="table-toolbar">
        <Skeleton style={{ height: 38, width: 280, borderRadius: 'var(--r-sm)' }} />
      </div>
      <div style={{ padding: '1rem' }}>
        {Array.from({ length: rows }).map((_, i) => (
          <Skeleton key={i} className="sk-row" />
        ))}
      </div>
    </div>
  )
}
