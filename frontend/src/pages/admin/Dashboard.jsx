import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  ResponsiveContainer,
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip,
  PieChart, Pie, Cell,
} from 'recharts'
import { AdminAPI } from '../../api/endpoints.js'
import { money } from '../../utils/format.js'
import Skeleton from '../../components/ui/Skeleton.jsx'

// Colores para el gráfico de estado de stock
const COLORES_STOCK = ['#10b981', '#f59e0b', '#ef4444'] // ok, bajo, agotado

export default function Dashboard() {
  const [data, setData] = useState(null)
  const [productos, setProductos] = useState([])

  useEffect(() => {
    AdminAPI.dashboard()
      .then(setData)
      .catch(() => setData({ totalProductos: 0, totalCategorias: 0, totalClientes: 0, stockBajo: [] }))
    AdminAPI.productos()
      .then(setProductos)
      .catch(() => setProductos([]))
  }, [])

  // Cargando: mostramos skeletons con la misma forma
  if (!data) {
    return (
      <>
        <div className="stat-grid">
          {[1, 2, 3, 4].map((i) => (
            <Skeleton key={i} style={{ height: 86, borderRadius: 'var(--r)' }} />
          ))}
        </div>
        <div className="grid-2">
          <Skeleton style={{ height: 320, borderRadius: 'var(--r)' }} />
          <Skeleton style={{ height: 320, borderRadius: 'var(--r)' }} />
        </div>
      </>
    )
  }

  // KPIs
  const stats = [
    { label: 'Productos', value: data.totalProductos, icon: 'bi-box-seam-fill', bg: 'var(--accent-soft)', color: 'var(--accent)' },
    { label: 'Categorías', value: data.totalCategorias, icon: 'bi-tags-fill', bg: '#e0e7ff', color: 'var(--accent-hover)' },
    { label: 'Clientes', value: data.totalClientes, icon: 'bi-people-fill', bg: '#fce7f3', color: '#be185d' },
    { label: 'Pedidos', value: data.totalPedidos, icon: 'bi-bag-check-fill', bg: '#dcfce7', color: '#15803d' },
    { label: 'Ventas', value: money(data.totalVentas), icon: 'bi-cash-coin', bg: '#fef3c7', color: '#b45309' },
    { label: 'Stock bajo', value: data.stockBajo.length, icon: 'bi-exclamation-triangle-fill', bg: 'var(--warning-soft)', color: 'var(--warning-text)' },
  ]

  // Datos para "productos por categoría" (agrupamos en el front)
  const porCategoria = Object.values(
    productos.reduce((acc, p) => {
      const nombre = p.categoriaNombre || 'Sin categoría'
      acc[nombre] = acc[nombre] || { nombre, cantidad: 0 }
      acc[nombre].cantidad++
      return acc
    }, {})
  )

  // Datos para "estado de stock"
  const estadoStock = [
    { name: 'En stock (>10)', value: productos.filter((p) => p.stock > 10).length },
    { name: 'Stock bajo (1-10)', value: productos.filter((p) => p.stock > 0 && p.stock <= 10).length },
    { name: 'Agotado', value: productos.filter((p) => p.stock === 0).length },
  ]

  return (
    <>
      <div className="stat-grid">
        {stats.map((s) => (
          <div key={s.label} className="stat-card">
            <div className="stat-icon" style={{ background: s.bg, color: s.color }}>
              <i className={'bi ' + s.icon} />
            </div>
            <div>
              <div className="stat-label">{s.label}</div>
              <div className="stat-value">{s.value}</div>
            </div>
          </div>
        ))}
      </div>

      {/* Gráficos */}
      <div className="grid-2">
        <div className="panel">
          <div className="panel-head">
            <h3><i className="bi bi-bar-chart-fill" style={{ color: 'var(--accent)' }} /> Productos por categoría</h3>
          </div>
          <div className="chart-box">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={porCategoria} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" vertical={false} />
                <XAxis dataKey="nombre" tick={{ fontSize: 11, fill: 'var(--muted)' }} />
                <YAxis allowDecimals={false} tick={{ fontSize: 11, fill: 'var(--muted)' }} />
                <Tooltip cursor={{ fill: 'var(--accent-soft)' }} />
                <Bar dataKey="cantidad" fill="#6366f1" radius={[6, 6, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div className="panel">
          <div className="panel-head">
            <h3><i className="bi bi-pie-chart-fill" style={{ color: 'var(--accent)' }} /> Estado del stock</h3>
          </div>
          <div className="chart-box">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie data={estadoStock} dataKey="value" nameKey="name" cx="50%" cy="50%" innerRadius={55} outerRadius={90} paddingAngle={3}>
                  {estadoStock.map((entry, i) => (
                    <Cell key={i} fill={COLORES_STOCK[i]} />
                  ))}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          </div>
          <div className="chart-legend">
            {estadoStock.map((e, i) => (
              <span key={e.name} className="legend-item">
                <span className="legend-dot" style={{ background: COLORES_STOCK[i] }} />
                {e.name}: <strong style={{ color: 'var(--text-strong)' }}>{e.value}</strong>
              </span>
            ))}
          </div>
        </div>
      </div>

      {/* Stock bajo + acciones */}
      <div className="panel-grid">
        <div className="panel">
          <div className="panel-head">
            <h3><i className="bi bi-exclamation-triangle-fill" style={{ color: 'var(--warning)' }} /> Stock bajo</h3>
            <Link to="/admin/productos" style={{ fontSize: '0.88rem' }}>Gestionar</Link>
          </div>
          {data.stockBajo.length === 0 ? (
            <div className="empty">
              <i className="bi bi-check-circle-fill" style={{ color: 'var(--success)', opacity: 1 }} />
              <p>¡Todo el inventario está en buen nivel!</p>
            </div>
          ) : (
            <div className="stock-list">
              {data.stockBajo.map((p) => (
                <div key={p.id} className="stock-card">
                  <img src={p.imagen} alt={p.nombre} />
                  <div>
                    <div className="fw-bold" style={{ fontSize: '0.9rem' }}>{p.nombre}</div>
                    <div className="text-muted" style={{ fontSize: '0.82rem' }}>
                      Stock: <strong style={{ color: 'var(--warning)' }}>{p.stock}</strong> unidades
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="panel">
          <div className="panel-head">
            <h3><i className="bi bi-lightning-charge-fill" style={{ color: 'var(--accent)' }} /> Acciones rápidas</h3>
          </div>
          <div className="quick-actions">
            <Link to="/admin/productos" className="btn btn-outline"><i className="bi bi-box-seam" /> Gestionar productos</Link>
            <Link to="/admin/categorias" className="btn btn-outline"><i className="bi bi-tags" /> Gestionar categorías</Link>
            <Link to="/admin/clientes" className="btn btn-outline"><i className="bi bi-people" /> Gestionar clientes</Link>
            <Link to="/" className="btn btn-outline"><i className="bi bi-shop" /> Ver tienda pública</Link>
          </div>
        </div>
      </div>
    </>
  )
}
