import { useEffect, useState } from 'react'
import { AdminAPI } from '../../api/endpoints.js'
import { downloadBlob } from '../../api/client.js'
import { money } from '../../utils/format.js'
import { useTableControls } from '../../hooks/useTableControls.js'
import { useToast } from '../../components/ui/Toast.jsx'
import TableToolbar from '../../components/ui/TableToolbar.jsx'
import TableSkeleton from '../../components/ui/TableSkeleton.jsx'
import Pagination from '../../components/ui/Pagination.jsx'

// Clase del badge segun el metodo de pago usado en la boleta
function badgeMetodo(metodo) {
  if (metodo === 'YAPE') return 'badge badge-yape'
  if (metodo === 'TARJETA') return 'badge badge-tarjeta'
  return 'badge badge-cat'
}

// Página admin: registro de ventas (boletas emitidas), con resumen y filtros de fecha —
// base para el futuro libro de ventas de la tienda.
export default function AdminVentas() {
  const toast = useToast()

  const [comprobantes, setComprobantes] = useState(null)
  const [resumen, setResumen] = useState({ cantidad: 0, subtotal: 0, igv: 0, total: 0 })

  // Filtros de fecha del reporte (vacíos = trae todo)
  const [desde, setDesde] = useState('')
  const [hasta, setHasta] = useState('')

  const t = useTableControls(comprobantes || [], {
    searchKeys: ['codigo', 'pedidoCodigo', 'clienteNombre', 'clienteDni'],
    pageSize: 8,
    initialSort: { key: 'id', dir: 'desc' },
  })

  // Carga en paralelo el listado de boletas y el resumen de ventas, con los mismos filtros
  const cargar = () => {
    const params = { desde, hasta }
    AdminAPI.comprobantes(params).then(setComprobantes).catch(() => setComprobantes([]))
    AdminAPI.resumenVentas(params)
      .then(setResumen)
      .catch(() => setResumen({ cantidad: 0, subtotal: 0, igv: 0, total: 0 }))
  }

  useEffect(() => {
    cargar()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // Descarga la boleta en PDF de un comprobante puntual
  const descargarBoleta = async (c) => {
    try {
      await downloadBlob(AdminAPI.boletaUrl(c.id), 'boleta-' + c.codigo + '.pdf')
    } catch (err) {
      toast.error(err.message)
    }
  }

  const Th = ({ label, col }) => (
    <th className={'sortable' + (t.sort?.key === col ? ' is-sorted' : '')} onClick={() => t.toggleSort(col)}>
      {label}
      <span className="sort-ind">{t.sort?.key === col ? (t.sort.dir === 'asc' ? '▲' : '▼') : '↕'}</span>
    </th>
  )

  return (
    <>
      <div className="page-head">
        <div>
          <h2>Registro de ventas</h2>
          {/* Los totales los calcula el backend excluyendo las anuladas: una venta anulada no es venta */}
          <p>Boletas emitidas — base para el libro de ventas. Los totales no incluyen las boletas anuladas.</p>
        </div>
        {/* Filtros de fecha del reporte: si se dejan vacíos, trae todas las boletas */}
        <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'flex-end', flexWrap: 'wrap' }}>
          <div className="field" style={{ margin: 0 }}>
            <label className="label" style={{ fontSize: '0.72rem' }}>Desde</label>
            <input className="input" type="date" value={desde} onChange={(e) => setDesde(e.target.value)} />
          </div>
          <div className="field" style={{ margin: 0 }}>
            <label className="label" style={{ fontSize: '0.72rem' }}>Hasta</label>
            <input className="input" type="date" value={hasta} onChange={(e) => setHasta(e.target.value)} />
          </div>
          <button className="btn btn-outline" onClick={cargar}>
            <i className="bi bi-funnel" /> Filtrar
          </button>
        </div>
      </div>

      {/* Tarjetas de resumen de ventas del periodo filtrado */}
      <div className="stat-grid">
        <div className="stat-card">
          <div className="stat-icon" style={{ background: 'var(--accent-soft)', color: 'var(--accent)' }}>
            <i className="bi bi-receipt-cutoff" />
          </div>
          <div>
            <div className="stat-label">Boletas emitidas</div>
            <div className="stat-value">{resumen.cantidad}</div>
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-icon" style={{ background: '#e0e7ff', color: 'var(--accent-hover)' }}>
            <i className="bi bi-cash" />
          </div>
          <div>
            <div className="stat-label">Op. gravada</div>
            <div className="stat-value">{money(resumen.subtotal)}</div>
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-icon" style={{ background: '#fef3c7', color: '#b45309' }}>
            <i className="bi bi-percent" />
          </div>
          <div>
            <div className="stat-label">IGV (18%)</div>
            <div className="stat-value">{money(resumen.igv)}</div>
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-icon" style={{ background: '#dcfce7', color: '#15803d' }}>
            <i className="bi bi-cash-coin" />
          </div>
          <div>
            <div className="stat-label">Total vendido</div>
            <div className="stat-value">{money(resumen.total)}</div>
          </div>
        </div>
      </div>

      {comprobantes === null ? (
        <TableSkeleton />
      ) : (
        <div className="table-wrap">
          <TableToolbar query={t.query} onSearch={t.onSearch} total={t.total} />
          <div className="table-scroll">
            <table className="table">
              <thead>
                <tr>
                  <Th label="Boleta" col="codigo" />
                  <Th label="Pedido" col="pedidoCodigo" />
                  <Th label="Cliente" col="clienteNombre" />
                  <Th label="Fecha" col="fechaEmision" />
                  <Th label="Op. gravada" col="subtotal" />
                  <Th label="IGV" col="igv" />
                  <Th label="Total" col="total" />
                  <th>Pago</th>
                  <Th label="Estado" col="estado" />
                  <th style={{ textAlign: 'right' }}>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {t.paged.length === 0 && (
                  <tr>
                    <td colSpan={10}>
                      <div className="empty">
                        <i className="bi bi-receipt-cutoff" />
                        <div>No hay boletas emitidas en el periodo</div>
                      </div>
                    </td>
                  </tr>
                )}
                {t.paged.map((c) => (
                  // Las boletas anuladas se atenúan pero NO se ocultan: el correlativo
                  // no se reutiliza, así que la boleta tiene que seguir en el registro.
                  <tr key={c.id} className={c.estado === 'ANULADO' ? 'fila-anulada' : ''}>
                    <td><strong style={{ color: 'var(--accent)', fontFamily: 'monospace' }}>{c.codigo}</strong></td>
                    <td>{c.pedidoCodigo}</td>
                    <td>
                      <div className="fw-bold">{c.clienteNombre}</div>
                      <small className="text-muted">DNI: {c.clienteDni}</small>
                    </td>
                    <td>{c.fechaEmision ? new Date(c.fechaEmision).toLocaleString('es-PE') : '—'}</td>
                    <td>{money(c.subtotal)}</td>
                    <td>{money(c.igv)}</td>
                    <td><strong>{money(c.total)}</strong></td>
                    <td><span className={badgeMetodo(c.metodoPago)}>{c.metodoPago}</span></td>
                    <td>
                      <span className={c.estado === 'EMITIDO' ? 'badge badge-ok' : 'badge badge-danger'}>
                        {c.estado === 'ANULADO' ? 'ANULADA' : c.estado}
                      </span>
                    </td>
                    <td>
                      <div className="cell-actions">
                        <button className="btn btn-outline btn-icon" onClick={() => descargarBoleta(c)} title="Descargar boleta">
                          <i className="bi bi-file-earmark-pdf" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination page={t.page} totalPages={t.totalPages} total={t.total} onPage={t.setPage} />
        </div>
      )}
    </>
  )
}
