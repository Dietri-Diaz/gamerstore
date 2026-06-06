import { Link } from 'react-router-dom'
import { money } from '../../utils/format.js'

export default function ProductCard({ p }) {
  return (
    <Link to={`/productos/${p.id}`} className="product-card">
      <div className="product-card-imgwrap">
        <img className="product-card-img" src={p.imagen} alt={p.nombre} />
      </div>
      <div className="product-card-body">
        <span className="badge badge-cat">{p.categoriaNombre}</span>
        <h3 className="product-card-title">{p.nombre}</h3>
        <p className="product-card-desc">{p.descripcion}</p>
        <div className="product-card-foot">
          <span className="price">{money(p.precio)}</span>
          <span className="badge badge-accent">
            <i className="bi bi-eye" /> Ver
          </span>
        </div>
      </div>
    </Link>
  )
}
