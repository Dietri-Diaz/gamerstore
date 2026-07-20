import { useEffect, useState } from 'react'
import { PagosAPI } from '../../api/endpoints.js'
import { downloadBlob } from '../../api/client.js'
import { money } from '../../utils/format.js'
import { useTableControls } from '../../hooks/useTableControls.js'
import { useToast } from '../../components/ui/Toast.jsx'
import TableToolbar from '../../components/ui/TableToolbar.jsx'
import TableSkeleton from '../../components/ui/TableSkeleton.jsx'
import Pagination from '../../components/ui/Pagination.jsx'

// Clase del badge segun el metodo de pago usado
function badgeMetodo(metodo) {
  return metodo === 'YAPE' ? 'badge badge-yape' : 'badge badge-tarjeta'
}

// Página admin: historial de pagos (cobros hechos desde la pasarela) con descarga de comprobante
export default function AdminPagos() {
  const toast = useToast()

  const [pagos, setPagos] = useState(null)

  const t = useTableControls(pagos || [], {
    searchKeys: ['codigo', 'pedidoCodigo', 'clienteNombre', 'referencia'],
    pageSize: 8,
    initialSort: { key: 'id', dir: 'desc' },
  })

  const cargar = () => PagosAPI.listar().then(setPagos).catch(() => setPagos([]))

  useEffect(() => {
    cargar()
  }, [])

  // Descarga el comprobante en PDF de un pago puntual
  const descargarComprobante = async (p) => {
    try {
      await downloadBlob(PagosAPI.comprobanteUrl(p.id), 'comprobante-' + p.codigo + '.pdf')
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
          <h2>Pagos</h2>
          <p>Historial de cobros del sistema</p>
        </div>
      </div>

      {pagos === null ? (
        <TableSkeleton />
      ) : (
        <div className="table-wrap">
          <TableToolbar query={t.query} onSearch={t.onSearch} total={t.total} />
          <div className="table-scroll">
            <table className="table">
              <thead>
                <tr>
                  <Th label="Código" col="codigo" />
                  <Th label="Pedido" col="pedidoCodigo" />
                  <Th label="Cliente" col="clienteNombre" />
                  <Th label="Método" col="metodo" />
                  <Th label="Monto" col="monto" />
                  <Th label="Estado" col="estado" />
                  <th>Referencia</th>
                  <Th label="Fecha" col="fecha" />
                  <th style={{ textAlign: 'right' }}>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {t.paged.length === 0 && (
                  <tr>
                    <td colSpan={9}>
                      <div className="empty">
                        <i className="bi bi-credit-card-2-front" />
                        <div>No hay pagos registrados</div>
                      </div>
                    </td>
                  </tr>
                )}
                {t.paged.map((p) => (
                  <tr key={p.id}>
                    <td><strong style={{ color: 'var(--accent)', fontFamily: 'monospace' }}>{p.codigo}</strong></td>
                    <td>{p.pedidoCodigo}</td>
                    <td className="fw-bold">{p.clienteNombre}</td>
                    <td><span className={badgeMetodo(p.metodo)}>{p.metodo}</span></td>
                    <td><strong>{money(p.monto)}</strong></td>
                    <td>
                      <span className={p.estado === 'APROBADO' ? 'badge badge-ok' : 'badge badge-danger'}>
                        {p.estado}
                      </span>
                    </td>
                    <td>{p.referencia || '—'}</td>
                    <td>{p.fecha ? new Date(p.fecha).toLocaleString('es-PE') : '—'}</td>
                    <td>
                      <div className="cell-actions">
                        <button className="btn btn-outline btn-icon" onClick={() => descargarComprobante(p)} title="Descargar comprobante">
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
