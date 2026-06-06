// Placeholder de carga con efecto "shimmer".
// Uso: <Skeleton className="sk-card" />  o  <Skeleton className="sk-line" style={{ width: '60%' }} />
export default function Skeleton({ className = '', style }) {
  return <div className={'skeleton ' + className} style={style} />
}
