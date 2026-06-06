import Skeleton from '../ui/Skeleton.jsx'

// Grilla de tarjetas "fantasma" mientras cargan los productos.
export default function ProductGridSkeleton({ count = 8, cols = 'grid-4' }) {
  return (
    <div className={'product-grid ' + cols}>
      {Array.from({ length: count }).map((_, i) => (
        <Skeleton key={i} className="sk-card" />
      ))}
    </div>
  )
}
