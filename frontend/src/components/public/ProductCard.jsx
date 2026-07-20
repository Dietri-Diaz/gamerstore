import { Link } from 'react-router-dom'
import { money } from '../../utils/format.js'
import { useCarrito } from '../../carrito/CarritoContext.jsx'
import { useToast } from '../ui/Toast.jsx'

// Tarjeta de un producto para el catálogo: imagen, categoría, nombre, precio,
// un botón para agregarlo directo al carrito y el enlace hacia su página de detalle.
export default function ProductCard({ p }) {
  const { agregar } = useCarrito()
  const toast = useToast()

  // Agrega el producto al carrito sin salir del catálogo (la tarjeta entera es un
  // <Link>, asi que frenamos la navegacion para que el click quede en el boton)
  const handleAgregar = (e) => {
    e.preventDefault()
    e.stopPropagation()
    agregar(p, 1)
    toast.success('Agregado al carrito')
  }

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
          <div className="product-card-actions">
            <button
              type="button"
              className="btn btn-primary btn-sm"
              disabled={p.stock === 0}
              onClick={handleAgregar}
            >
              <i className="bi bi-cart-plus" /> {p.stock === 0 ? 'Sin stock' : 'Agregar'}
            </button>
            <span className="badge badge-accent">
              <i className="bi bi-eye" /> Ver
            </span>
          </div>
        </div>
      </div>
    </Link>
  )
}
