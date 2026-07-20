import { createContext, useContext, useEffect, useState } from 'react'

// Contexto del carrito de compras de la tienda publica: guarda los productos
// elegidos por el visitante y persiste todo en localStorage para que no se
// pierda si recarga la pagina o cierra el navegador.
const CarritoContext = createContext(null)
const STORAGE_KEY = 'gs_carrito'

// Topa una cantidad entre 1 y el stock disponible del producto
const limitar = (cantidad, stock) => Math.max(1, Math.min(cantidad, stock))

export function CarritoProvider({ children }) {
  // Carga el estado inicial desde localStorage (si no hay nada o esta corrupto, empieza vacio)
  const [items, setItems] = useState(() => {
    try {
      const raw = localStorage.getItem(STORAGE_KEY)
      return raw ? JSON.parse(raw) : []
    } catch {
      return []
    }
  })

  // Cada vez que cambian los items, se guarda una copia en localStorage
  useEffect(() => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(items))
    } catch {
      /* localStorage no disponible (modo privado, cuota llena, etc.) */
    }
  }, [items])

  // Agrega un producto al carrito; si ya estaba, suma la cantidad sin superar el stock.
  // Si el producto no tiene stock, no hace nada.
  const agregar = (producto, cantidad = 1) => {
    if (!producto || producto.stock <= 0) return
    setItems((lista) => {
      const existente = lista.find((i) => i.id === producto.id)
      if (existente) {
        return lista.map((i) =>
          i.id === producto.id
            ? { ...i, cantidad: limitar(i.cantidad + cantidad, producto.stock) }
            : i
        )
      }
      return [
        ...lista,
        {
          id: producto.id,
          nombre: producto.nombre,
          precio: producto.precio,
          imagen: producto.imagen,
          stock: producto.stock,
          cantidad: limitar(cantidad, producto.stock),
        },
      ]
    })
  }

  // Quita un producto por completo del carrito
  const quitar = (id) => {
    setItems((lista) => lista.filter((i) => i.id !== id))
  }

  // Cambia la cantidad de un item puntual; si la nueva cantidad es <= 0, lo quita
  const cambiarCantidad = (id, cantidad) => {
    if (cantidad <= 0) {
      quitar(id)
      return
    }
    setItems((lista) =>
      lista.map((i) => (i.id === id ? { ...i, cantidad: limitar(cantidad, i.stock) } : i))
    )
  }

  // Vacia el carrito por completo (ej. justo despues de confirmar una compra)
  const limpiar = () => setItems([])

  const total = items.reduce((acc, i) => acc + i.precio * i.cantidad, 0)
  const cantidadTotal = items.reduce((acc, i) => acc + i.cantidad, 0)

  return (
    <CarritoContext.Provider
      value={{ items, agregar, quitar, cambiarCantidad, limpiar, total, cantidadTotal }}
    >
      {children}
    </CarritoContext.Provider>
  )
}

// Hook de conveniencia para leer/mutar el carrito desde cualquier componente
export const useCarrito = () => useContext(CarritoContext)
