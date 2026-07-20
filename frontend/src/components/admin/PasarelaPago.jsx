import { useEffect, useState, useMemo } from 'react'
import { loadStripe } from '@stripe/stripe-js'
import { Elements, CardElement, useStripe, useElements } from '@stripe/react-stripe-js'
import { AdminAPI, PagosAPI } from '../../api/endpoints.js'
import { downloadBlob } from '../../api/client.js'
import { money } from '../../utils/format.js'
import { useToast } from '../ui/Toast.jsx'
import Modal from '../ui/Modal.jsx'
import Alert from '../ui/Alert.jsx'

// Algoritmo de Luhn: valida (en el navegador, antes de llamar al backend) que un
// numero de tarjeta sea matematicamente consistente.
function luhn(numero) {
  const digitos = numero.replace(/\D/g, '')
  if (digitos.length < 13) return false
  let suma = 0
  let alternar = false
  for (let i = digitos.length - 1; i >= 0; i--) {
    let d = Number(digitos[i])
    if (alternar) {
      d *= 2
      if (d > 9) d -= 9
    }
    suma += d
    alternar = !alternar
  }
  return suma % 10 === 0
}

// Formatea el numero de tarjeta en grupos de 4 mientras se escribe (1234 5678 9012 3456)
function formatearTarjeta(valor) {
  const digitos = valor.replace(/\D/g, '').slice(0, 19)
  return digitos.replace(/(.{4})/g, '$1 ').trim()
}

// Formatea el vencimiento como MM/AA, insertando la "/" automaticamente
function formatearVencimiento(valor) {
  const digitos = valor.replace(/\D/g, '').slice(0, 4)
  return digitos.length <= 2 ? digitos : digitos.slice(0, 2) + '/' + digitos.slice(2)
}

const EMPTY_YAPE = { numeroOperacion: '', voucher: '' }
const EMPTY_TARJETA = { numero: '', titular: '', vencimiento: '', cvv: '' }

// Subcomponente que SOLO existe dentro de <Elements> (los hooks useStripe/useElements lo
// exigen). A diferencia del checkout publico, aca el modal es de una sola vista: el propio
// boton "Pagar" tokeniza la tarjeta Y llama a PagosAPI.pagarTarjeta en el mismo click.
function FormularioTarjetaStripe({ pedido, titular, setTitular, procesando, setProcesando, setError, setResultado, onPagado, toast }) {
  const stripe = useStripe()
  const elements = useElements()
  const [completo, setCompleto] = useState(false)
  const [errorCard, setErrorCard] = useState('')
  const [enfocado, setEnfocado] = useState(false)

  const pagar = async () => {
    if (!stripe || !elements) return
    setProcesando(true)
    setError('')
    try {
      const { paymentMethod, error: errStripe } = await stripe.createPaymentMethod({
        type: 'card',
        card: elements.getElement(CardElement),
        billing_details: { name: titular || undefined },
      })
      if (errStripe) {
        setError(errStripe.message)
        toast.error(errStripe.message)
        return
      }
      const pago = await PagosAPI.pagarTarjeta({ pedidoId: pedido.id, paymentMethodId: paymentMethod.id })
      setResultado(pago)
      if (pago.estado === 'APROBADO') {
        toast.success('Pago aprobado')
        onPagado()
      }
    } catch (err) {
      setError(err.message)
      toast.error(err.message)
    } finally {
      setProcesando(false)
    }
  }

  return (
    <>
      <div className="field">
        <label className="label">Titular *</label>
        <input
          className="input"
          value={titular}
          disabled={procesando}
          onChange={(e) => setTitular(e.target.value.toUpperCase())}
          placeholder="NOMBRE COMO FIGURA EN LA TARJETA"
        />
      </div>

      <div className="field">
        <label className="label">Datos de la tarjeta *</label>
        <div className={'stripe-card' + (enfocado ? ' focus' : '') + (errorCard ? ' err' : '')}>
          <CardElement
            options={{
              hidePostalCode: true,
              disabled: procesando,
              style: {
                base: { fontSize: '16px', color: '#1e293b', '::placeholder': { color: '#94a3b8' } },
                invalid: { color: '#ef4444' },
              },
            }}
            onChange={(e) => {
              setCompleto(e.complete)
              setErrorCard(e.error?.message || '')
            }}
            onFocus={() => setEnfocado(true)}
            onBlur={() => setEnfocado(false)}
          />
        </div>
        {errorCard && <small className="pasarela-error-txt">{errorCard}</small>}
      </div>

      <div className="aviso-demo aviso-demo-info">
        <i className="bi bi-info-circle-fill" />
        <span>
          Pasarela real (Stripe) en modo prueba: tu tarjeta se tokeniza en el navegador y nunca pasa por nuestro servidor. No se realizan
          cobros reales.
          <br />
          <small className="text-muted">Tarjeta de prueba: 4242 4242 4242 4242 · cualquier fecha futura · CVC 123</small>
        </span>
      </div>

      <button
        className="btn btn-primary btn-block"
        style={{ marginTop: '1rem' }}
        disabled={!completo || titular.trim() === '' || procesando}
        onClick={pagar}
      >
        <i className="bi bi-lock-fill" /> {procesando ? 'Procesando...' : 'Pagar ' + money(pedido.total)}
      </button>
    </>
  )
}

// Modal de cobro de un pedido: dos pestañas (Yape con QR, o tarjeta simulada).
// Al aprobarse el pago, avisa al padre (onPagado) para que refresque la lista de pedidos.
export default function PasarelaPago({ pedido, onClose, onPagado }) {
  const toast = useToast()

  const [tab, setTab] = useState('yape')
  const [cfg, setCfg] = useState(null)
  const [qrError, setQrError] = useState(false)
  const [copiado, setCopiado] = useState(false)

  // Promesa de Stripe.js: solo se crea si la config del backend trae Stripe activo
  const stripePromise = useMemo(
    () => (cfg?.stripeEnabled && cfg?.stripePublicKey ? loadStripe(cfg.stripePublicKey) : null),
    [cfg?.stripeEnabled, cfg?.stripePublicKey]
  )

  const [yape, setYape] = useState(EMPTY_YAPE)
  const [subiendoVoucher, setSubiendoVoucher] = useState(false)

  const [tarjeta, setTarjeta] = useState(EMPTY_TARJETA)

  const [procesando, setProcesando] = useState(false)
  const [error, setError] = useState('')
  const [resultado, setResultado] = useState(null)

  // Yape (la app) tiene tope de monto: si el pedido lo supera, esa pestaña se bloquea.
  const yapeBloqueado = pedido.total > (cfg?.montoMaximo || 500)

  // Al montar, trae el numero/titular/QR/tope de la cuenta Yape que recibe los pagos
  useEffect(() => {
    PagosAPI.config().then(setCfg).catch(() => setCfg(null))
  }, [])

  // Cuando llega la config, si el pedido supera el tope de Yape el modal debe abrir en tarjeta
  useEffect(() => {
    if (cfg && pedido.total > (cfg.montoMaximo || 500)) setTab('tarjeta')
  }, [cfg])

  const numeroLimpio = tarjeta.numero.replace(/\D/g, '')
  const tarjetaValida = numeroLimpio.length >= 13 && luhn(tarjeta.numero)

  // Copia el numero de Yape al portapapeles y muestra feedback breve
  const copiarNumero = () => {
    if (!cfg) return
    navigator.clipboard.writeText(cfg.numero)
    setCopiado(true)
    setTimeout(() => setCopiado(false), 1500)
  }

  // Sube la captura del voucher (igual que la subida de imagenes de productos) y
  // guarda la url devuelta para mandarla junto con el pago
  const subirVoucher = async (e) => {
    const file = e.target.files?.[0]
    if (!file) return
    setSubiendoVoucher(true)
    try {
      const fd = new FormData()
      fd.append('file', file)
      const { url } = await AdminAPI.subirImagen(fd)
      setYape((y) => ({ ...y, voucher: url }))
      toast.success('Voucher subido')
    } catch (err) {
      toast.error(err.message)
    } finally {
      setSubiendoVoucher(false)
    }
  }

  // Confirma el pago con Yape mandando el N° de operacion (y el voucher, si se subio)
  const confirmarYape = async () => {
    setProcesando(true)
    setError('')
    try {
      const pago = await PagosAPI.pagarYape({
        pedidoId: pedido.id,
        numeroOperacion: yape.numeroOperacion,
        voucher: yape.voucher || null,
      })
      setResultado(pago)
      if (pago.estado === 'APROBADO') {
        toast.success('Pago aprobado')
        onPagado()
      }
    } catch (err) {
      setError(err.message)
      toast.error(err.message)
    } finally {
      setProcesando(false)
    }
  }

  // Confirma el pago con tarjeta (el backend vuelve a validar Luhn, vencimiento y CVV)
  const confirmarTarjeta = async () => {
    setProcesando(true)
    setError('')
    try {
      const pago = await PagosAPI.pagarTarjeta({
        pedidoId: pedido.id,
        numero: numeroLimpio,
        titular: tarjeta.titular,
        vencimiento: tarjeta.vencimiento,
        cvv: tarjeta.cvv,
      })
      setResultado(pago)
      if (pago.estado === 'APROBADO') {
        toast.success('Pago aprobado')
        onPagado()
      }
    } catch (err) {
      setError(err.message)
      toast.error(err.message)
    } finally {
      setProcesando(false)
    }
  }

  // Descarga el comprobante en PDF del pago recien registrado
  const descargarComprobante = async () => {
    try {
      await downloadBlob(PagosAPI.comprobanteUrl(resultado.id), 'comprobante-' + resultado.codigo + '.pdf')
    } catch (err) {
      toast.error(err.message)
    }
  }

  return (
    <Modal title={'Cobrar ' + pedido.codigo} icon="bi-credit-card" onClose={onClose}>
      {/* Resumen del pedido que se va a cobrar */}
      <div className="pasarela-resumen">
        <span>Pedido <strong>{pedido.codigo}</strong></span>
        <span className="pasarela-monto">{money(pedido.total)}</span>
      </div>

      {resultado ? (
        resultado.estado === 'APROBADO' ? (
          <div className="pago-ok">
            <i className="bi bi-check-circle-fill" />
            <div className="pago-info">
              <div className="fw-bold">Pago aprobado</div>
              <small className="text-muted">Código {resultado.codigo} · Ref. {resultado.referencia}</small>
            </div>
            <button className="btn btn-outline" onClick={descargarComprobante}>
              <i className="bi bi-file-earmark-pdf" /> Descargar comprobante
            </button>
          </div>
        ) : (
          <div className="pago-fail">
            <i className="bi bi-x-circle-fill" />
            <div className="pago-info">
              <div className="fw-bold">Pago rechazado por el banco</div>
              <small className="text-muted">Intenta con otra tarjeta o revisa los datos</small>
            </div>
            <button className="btn btn-outline" onClick={() => setResultado(null)}>
              <i className="bi bi-arrow-repeat" /> Reintentar
            </button>
          </div>
        )
      ) : (
        <>
          {error && <Alert type="error">{error}</Alert>}

          {/* Pestañas: Yape o tarjeta */}
          <div className="pasarela-tabs">
            <button
              type="button"
              className={'pasarela-tab' + (tab === 'yape' ? ' active' : '') + (yapeBloqueado ? ' pasarela-tab-off' : '')}
              disabled={yapeBloqueado}
              title={yapeBloqueado ? `Yape solo permite pagos de hasta S/ ${cfg?.montoMaximo || 500}` : undefined}
              onClick={() => setTab('yape')}
            >
              <i className="bi bi-phone" /> Yape
            </button>
            <button type="button" className={'pasarela-tab' + (tab === 'tarjeta' ? ' active' : '')} onClick={() => setTab('tarjeta')}>
              <i className="bi bi-credit-card-2-front" /> Tarjeta
            </button>
          </div>

          {tab === 'yape' && (
            <div className="pasarela-yape">
              {yapeBloqueado && (
                <div className="aviso-demo aviso-demo-warning">
                  <i className="bi bi-exclamation-triangle-fill" />
                  <span>Yape solo permite pagos de hasta S/ {cfg?.montoMaximo || 500}. Usa tarjeta para este pedido.</span>
                </div>
              )}

              <div className="pasarela-qr">
                {cfg && cfg.qr && !qrError ? (
                  <img src={cfg.qr} alt="QR de Yape" onError={() => setQrError(true)} />
                ) : (
                  <div className="pasarela-qr-fallback">
                    <i className="bi bi-qr-code-scan" />
                    <small>Coloca tu QR de Yape en <code>uploads/qr/yape-qr.png</code></small>
                  </div>
                )}
              </div>

              {cfg && (
                <div className="pasarela-cuenta">
                  <div className="pasarela-numero">{cfg.numero}</div>
                  <div className="text-muted">{cfg.titular}</div>
                  <button type="button" className="btn btn-outline btn-sm" onClick={copiarNumero}>
                    <i className={'bi ' + (copiado ? 'bi-check2' : 'bi-clipboard')} /> {copiado ? 'Copiado' : 'Copiar'}
                  </button>
                </div>
              )}

              <p className="pasarela-guia">
                1) Escanea el QR o yapea al número. 2) Pega el N° de operación que te da Yape. 3) Confirma.
              </p>

              <div className="field">
                <label className="label">N° de operación *</label>
                <input
                  className="input"
                  value={yape.numeroOperacion}
                  onChange={(e) => setYape((y) => ({ ...y, numeroOperacion: e.target.value.replace(/\D/g, '') }))}
                  maxLength={20}
                  placeholder="Ej. 123456"
                />
              </div>

              <div className="field">
                <label className="label">Subir voucher (captura)</label>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                  {yape.voucher ? (
                    <img
                      src={yape.voucher}
                      alt="voucher"
                      style={{ width: 52, height: 52, objectFit: 'cover', borderRadius: 8, border: '1px solid var(--border)' }}
                    />
                  ) : (
                    <div style={{ width: 52, height: 52, borderRadius: 8, background: 'var(--border)', display: 'grid', placeItems: 'center' }}>
                      <i className="bi bi-image text-muted" />
                    </div>
                  )}
                  <label className="btn btn-outline" style={{ cursor: 'pointer', margin: 0 }}>
                    <i className="bi bi-upload" /> {subiendoVoucher ? 'Subiendo...' : 'Elegir imagen'}
                    <input type="file" accept="image/*" hidden onChange={subirVoucher} disabled={subiendoVoucher} />
                  </label>
                </div>
              </div>

              <button
                className="btn btn-primary btn-block"
                disabled={yape.numeroOperacion.length < 6 || procesando || yapeBloqueado}
                onClick={confirmarYape}
              >
                <i className="bi bi-check2-circle" /> {procesando ? 'Procesando...' : 'Ya yapeé — Confirmar pago'}
              </button>
            </div>
          )}

          {tab === 'tarjeta' && (
            <div className="pasarela-tarjeta">
              {/* Pasarela real: el formulario vive dentro de <Elements> porque los hooks useStripe()/useElements() del subcomponente lo requieren */}
              {cfg?.stripeEnabled && cfg?.stripePublicKey ? (
                <Elements stripe={stripePromise}>
                  <FormularioTarjetaStripe
                    pedido={pedido}
                    titular={tarjeta.titular}
                    setTitular={(v) => setTarjeta((t) => ({ ...t, titular: v }))}
                    procesando={procesando}
                    setProcesando={setProcesando}
                    setError={setError}
                    setResultado={setResultado}
                    onPagado={onPagado}
                    toast={toast}
                  />
                </Elements>
              ) : (
                <>
                  <div className="field">
                    <label className="label">Número de tarjeta *</label>
                    <div className="pasarela-input-check">
                      <input
                        className="input"
                        value={tarjeta.numero}
                        onChange={(e) => setTarjeta((t) => ({ ...t, numero: formatearTarjeta(e.target.value) }))}
                        maxLength={23}
                        placeholder="1234 5678 9012 3456"
                      />
                      {numeroLimpio.length >= 13 && (
                        tarjetaValida
                          ? <i className="bi bi-check-circle-fill check-icon ok" />
                          : <i className="bi bi-x-circle-fill check-icon fail" />
                      )}
                    </div>
                    {numeroLimpio.length >= 13 && !tarjetaValida && (
                      <small className="pasarela-error-txt">Número inválido</small>
                    )}
                  </div>

                  <div className="field">
                    <label className="label">Titular *</label>
                    <input
                      className="input"
                      value={tarjeta.titular}
                      onChange={(e) => setTarjeta((t) => ({ ...t, titular: e.target.value.toUpperCase() }))}
                      placeholder="NOMBRE COMO FIGURA EN LA TARJETA"
                    />
                  </div>

                  <div className="form-grid">
                    <div className="field">
                      <label className="label">Vencimiento *</label>
                      <input
                        className="input"
                        value={tarjeta.vencimiento}
                        onChange={(e) => setTarjeta((t) => ({ ...t, vencimiento: formatearVencimiento(e.target.value) }))}
                        maxLength={5}
                        placeholder="MM/AA"
                      />
                    </div>
                    <div className="field">
                      <label className="label">CVV *</label>
                      <input
                        className="input"
                        type="password"
                        value={tarjeta.cvv}
                        onChange={(e) => setTarjeta((t) => ({ ...t, cvv: e.target.value.replace(/\D/g, '').slice(0, 4) }))}
                        maxLength={4}
                        placeholder="123"
                      />
                    </div>
                  </div>

                  <small className="text-muted">Prueba: 4242 4242 4242 4242 aprueba · 4000 0000 0000 0002 rechaza</small>

                  <button
                    className="btn btn-primary btn-block"
                    style={{ marginTop: '1rem' }}
                    disabled={!tarjetaValida || procesando}
                    onClick={confirmarTarjeta}
                  >
                    <i className="bi bi-lock-fill" /> {procesando ? 'Procesando...' : 'Pagar ' + money(pedido.total)}
                  </button>
                </>
              )}
            </div>
          )}
        </>
      )}
    </Modal>
  )
}
