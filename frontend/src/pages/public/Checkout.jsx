import { useEffect, useState, useMemo, useRef, useCallback } from 'react'
import { Link } from 'react-router-dom'
import { loadStripe } from '@stripe/stripe-js'
import { Elements, CardElement, useStripe, useElements } from '@stripe/react-stripe-js'
import { PublicAPI } from '../../api/endpoints.js'
import { downloadBlob } from '../../api/client.js'
import { useConfig } from '../../config/ConfigContext.jsx'
import { useCarrito } from '../../carrito/CarritoContext.jsx'
import { useToast } from '../../components/ui/Toast.jsx'
import { useAutoClear } from '../../hooks/useAutoClear.js'
import { money } from '../../utils/format.js'
import Alert from '../../components/ui/Alert.jsx'

// Algoritmo de Luhn: valida (en el navegador, antes de llamar al backend) que un
// numero de tarjeta sea matematicamente consistente. Misma logica que usa el
// panel admin en PasarelaPago.jsx.
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

// Detecta la marca de la tarjeta a partir de sus primeros digitos (reglas estandar de BIN):
// Visa empieza en 4, Mastercard en 51-55 o 2221-2720, Amex en 34/37, Discover en 6011/65.
function detectarMarca(numeroLimpio) {
  if (!numeroLimpio) return null
  const n2 = Number(numeroLimpio.slice(0, 2))
  const n4 = Number(numeroLimpio.slice(0, 4))
  if (numeroLimpio[0] === '4') return 'VISA'
  if ((n2 >= 51 && n2 <= 55) || (n4 >= 2221 && n4 <= 2720)) return 'MASTERCARD'
  if (n2 === 34 || n2 === 37) return 'AMEX'
  if (numeroLimpio.startsWith('6011') || n2 === 65) return 'DISCOVER'
  return null
}

const EMPTY_CLIENTE = { dni: '', nombres: '', apellidos: '', telefono: '', email: '', direccion: '' }
const EMPTY_TARJETA = { numero: '', titular: '', vencimiento: '', cvv: '' }
const PASOS_LABEL = ['Identificación', 'Datos', 'Pago', 'Confirmar']

// Subcomponente que SOLO existe dentro de <Elements> (los hooks useStripe/useElements
// exigen ese contexto). Muestra el input de Titular + el CardElement de Stripe y, cada
// vez que cambia si el CardElement esta completo, le avisa al padre (via onListo) tanto
// ese estado como una funcion "tokenizar" para crear el PaymentMethod cuando se pulse Continuar.
function FormularioTarjetaStripe({ titular, setTitular, onListo, procesando }) {
  const stripe = useStripe()
  const elements = useElements()
  const [completo, setCompleto] = useState(false)
  const [errorCard, setErrorCard] = useState('')
  const [enfocado, setEnfocado] = useState(false)

  // Crea el PaymentMethod en Stripe a partir de lo escrito en el CardElement.
  // El backend nunca ve el numero de tarjeta: solo recibe este paymentMethodId.
  const tokenizar = async () => {
    if (!stripe || !elements) {
      return { paymentMethod: null, error: { message: 'La pasarela de pago aún está cargando, intenta de nuevo.' } }
    }
    return stripe.createPaymentMethod({
      type: 'card',
      card: elements.getElement(CardElement),
      billing_details: { name: titular || undefined },
    })
  }

  useEffect(() => {
    onListo({ completo, tokenizar })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [completo, titular, stripe, elements])

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
    </>
  )
}

// Página de checkout de la tienda pública: un asistente (wizard) de 4 pasos
// (identificación → datos → pago → revisar y pagar) con un resumen del pedido
// siempre visible al costado, pensado para transmitir confianza tipo e-commerce real.
export default function Checkout() {
  const { items, total, limpiar } = useCarrito()
  const { yapeNumero, yapeTitular, yapeQr, yapeMontoMaximo, stripeEnabled, stripePublicKey } = useConfig()
  const toast = useToast()

  // Promesa de Stripe.js: se crea UNA sola vez (no en cada render) y solo si el backend
  // tiene la pasarela real activa; si no, queda en null y se usa el formulario manual.
  const stripePromise = useMemo(
    () => (stripeEnabled && stripePublicKey ? loadStripe(stripePublicKey) : null),
    [stripeEnabled, stripePublicKey]
  )

  // Yape (la app) tiene tope de monto: si el total lo supera, esa opcion se bloquea.
  const yapeBloqueado = total > yapeMontoMaximo

  // Paso actual del wizard (1 a 4)
  const [step, setStep] = useState(1)

  // --- Paso 1: identificación (invitado o cliente ya registrado) ---
  const [modo, setModo] = useState(null) // 'invitado' | 'cliente'
  const [identDni, setIdentDni] = useState('')
  const [identEmail, setIdentEmail] = useState('')
  const [identificando, setIdentificando] = useState(false)
  const [identificado, setIdentificado] = useState(false) // ya se recuperaron datos de un cliente real
  const [errorIdent, setErrorIdent] = useState('')

  // --- Paso 2: datos del comprador ---
  const [cliente, setCliente] = useState(EMPTY_CLIENTE)
  const [buscandoDni, setBuscandoDni] = useState(false)

  // --- Paso 3: método de pago ---
  const [metodo, setMetodo] = useState(null) // 'yape' | 'tarjeta'
  const [numeroOperacion, setNumeroOperacion] = useState('')
  const [tarjeta, setTarjeta] = useState(EMPTY_TARJETA)
  const [qrError, setQrError] = useState(false)
  const [copiado, setCopiado] = useState(false)

  // --- Paso 3 con Stripe: el PaymentMethod ya tokenizado (se crea al salir del paso 3) ---
  const [paymentMethodId, setPaymentMethodId] = useState(null)
  const [tarjetaResumen, setTarjetaResumen] = useState(null) // { marca, ult4 } para mostrar en el paso 4
  const [stripeCompleto, setStripeCompleto] = useState(false)
  const [tokenizando, setTokenizando] = useState(false)
  // Guarda la ultima funcion "tokenizar" que expone el subcomponente de Stripe (no dispara
  // re-render por si solo; el estado "completo" si lo hace, para habilitar/deshabilitar el boton)
  const stripeInfoRef = useRef({ completo: false, tokenizar: null })

  // --- Paso 4: revisar, pagar y confirmación final ---
  const [procesando, setProcesando] = useState(false)
  const [pasoAnim, setPasoAnim] = useState(-1) // indice del paso de la animacion de progreso
  const [error, setError] = useState('')
  const [confirmacion, setConfirmacion] = useState(null)
  const [descargandoBoleta, setDescargandoBoleta] = useState(false)

  useAutoClear(error, setError)

  // Si el total supera el tope de Yape y el metodo elegido era Yape, lo cambia a tarjeta
  // (nunca debe quedar seleccionado un metodo que ya no se puede usar).
  useEffect(() => {
    if (yapeBloqueado && metodo === 'yape') setMetodo('tarjeta')
  }, [yapeBloqueado, metodo])

  const numeroLimpio = tarjeta.numero.replace(/\D/g, '')
  const tarjetaValida = numeroLimpio.length >= 13 && luhn(tarjeta.numero)
  const marca = detectarMarca(numeroLimpio)

  const datosValidos = /^\d{8}$/.test(cliente.dni) && cliente.nombres.trim() !== '' && cliente.apellidos.trim() !== ''
  const pagoValido =
    metodo === 'tarjeta'
      ? tarjetaValida && tarjeta.titular.trim() !== '' && tarjeta.vencimiento.length === 5 && tarjeta.cvv.length >= 3
      : metodo === 'yape'
        ? numeroOperacion.length >= 6
        : false

  // Paso 1: intenta recuperar los datos de un cliente ya registrado (por DNI + correo)
  const identificarCliente = async () => {
    if (!/^\d{8}$/.test(identDni) || !identEmail.trim()) {
      toast.error('Ingresa un DNI de 8 dígitos y tu correo')
      return
    }
    setIdentificando(true)
    setErrorIdent('')
    try {
      const c = await PublicAPI.verificarCliente({ dni: identDni, email: identEmail })
      setCliente({
        dni: c.dni,
        nombres: c.nombres || '',
        apellidos: c.apellidos || '',
        telefono: c.telefono || '',
        email: c.email || identEmail,
        direccion: c.direccion || '',
      })
      setIdentificado(true)
      setStep(2)
    } catch (err) {
      setErrorIdent(err.message)
    } finally {
      setIdentificando(false)
    }
  }

  // Salida de emergencia si no se encontró el cliente: sigue como invitado sin bloquear la compra
  const continuarComoInvitado = () => {
    setModo('invitado')
    setErrorIdent('')
    setIdentificado(false)
    setCliente((c) => ({ ...c, dni: c.dni || identDni, email: c.email || identEmail }))
    setStep(2)
  }

  // Paso 2: busca el DNI en RENIEC y autocompleta nombres/apellidos del comprador
  const buscarDni = async () => {
    if (!/^\d{8}$/.test(cliente.dni)) {
      toast.error('Ingresa un DNI de 8 dígitos')
      return
    }
    setBuscandoDni(true)
    try {
      const p = await PublicAPI.reniec(cliente.dni)
      setCliente((c) => ({ ...c, nombres: p.nombres, apellidos: p.apellidos }))
      toast.success('Datos obtenidos de RENIEC')
    } catch (err) {
      toast.error(err.message)
    } finally {
      setBuscandoDni(false)
    }
  }

  // Copia el numero de Yape al portapapeles y muestra feedback breve
  const copiarNumero = () => {
    if (!yapeNumero) return
    navigator.clipboard.writeText(yapeNumero)
    setCopiado(true)
    setTimeout(() => setCopiado(false), 1500)
  }

  // Descarga la boleta (PDF) del pedido recién confirmado; el backend la verifica con el DNI del comprador
  const descargarBoleta = async () => {
    setDescargandoBoleta(true)
    try {
      await downloadBlob(PublicAPI.boletaUrl(confirmacion.pedidoCodigo, cliente.dni), 'boleta-' + confirmacion.comprobanteCodigo + '.pdf')
    } catch (err) {
      toast.error(err.message)
    } finally {
      setDescargandoBoleta(false)
    }
  }

  // El subcomponente de Stripe llama a esto cada vez que cambia el estado del CardElement
  const manejarListoStripe = useCallback(({ completo, tokenizar }) => {
    stripeInfoRef.current = { completo, tokenizar }
    setStripeCompleto(completo)
  }, [])

  // Boton "Continuar" del paso 3 cuando el metodo es tarjeta con Stripe activo: tokeniza
  // la tarjeta AHORA (mientras el CardElement todavia existe) porque en el paso 4 ya no
  // se renderiza el formulario y el elemento se perderia.
  const continuarConStripe = async () => {
    if (!stripeInfoRef.current.tokenizar) return
    setTokenizando(true)
    setError('')
    try {
      const { paymentMethod, error: errStripe } = await stripeInfoRef.current.tokenizar()
      if (errStripe) {
        setError(errStripe.message)
        toast.error(errStripe.message)
        return
      }
      setPaymentMethodId(paymentMethod.id)
      setTarjetaResumen({ marca: paymentMethod.card.brand.toUpperCase(), ult4: paymentMethod.card.last4 })
      setStep(4)
    } finally {
      setTokenizando(false)
    }
  }

  // Paso 4: arma el body EXACTO del contrato de POST /api/checkout, dispara el pago real
  // y en paralelo anima 3 pasos de progreso para que la espera se sienta creible.
  const pagar = async () => {
    setProcesando(true)
    setPasoAnim(0)
    setError('')

    const body = {
      cliente: {
        dni: cliente.dni,
        nombres: cliente.nombres,
        apellidos: cliente.apellidos,
        telefono: cliente.telefono,
        email: cliente.email,
        direccion: cliente.direccion,
      },
      items: items.map((i) => ({ productoId: i.id, cantidad: i.cantidad })),
      pago:
        metodo === 'tarjeta'
          ? stripeEnabled && paymentMethodId
            ? // Con Stripe activo el backend NO necesita (ni recibe) numero/vencimiento/cvv:
              // esos datos nunca salen del navegador, solo viaja el paymentMethodId ya tokenizado.
              { metodo: 'TARJETA', paymentMethodId }
            : {
                // Respaldo: pasarela simulada (Stripe desactivado en el backend)
                metodo: 'TARJETA',
                numero: numeroLimpio,
                titular: tarjeta.titular,
                vencimiento: tarjeta.vencimiento,
                cvv: tarjeta.cvv,
                numeroOperacion: null,
                voucher: null,
              }
          : {
              metodo: 'YAPE',
              numero: null,
              titular: null,
              vencimiento: null,
              cvv: null,
              numeroOperacion,
              voucher: null,
            },
    }

    const esperar = (ms) => new Promise((resolve) => setTimeout(resolve, ms))
    // La animacion no es solo decorativa: corre EN PARALELO a la llamada real y
    // Promise.all espera a que ambas terminen antes de mostrar la confirmación.
    const animar = async () => {
      await esperar(600)
      setPasoAnim(1)
      await esperar(600)
      setPasoAnim(2)
      await esperar(600)
    }

    try {
      const [resp] = await Promise.all([PublicAPI.checkout(body), animar()])
      setConfirmacion(resp)
      limpiar()
    } catch (err) {
      setError(err.message)
      toast.error(err.message)
      // Un PaymentMethod de Stripe ya usado (pago rechazado) no se puede reutilizar:
      // se descarta para que "Continuar" del paso 3 tokenice uno nuevo con la tarjeta
      // que el usuario vuelva a ingresar.
      if (stripeEnabled && metodo === 'tarjeta') {
        setPaymentMethodId(null)
        setTarjetaResumen(null)
      }
      setStep(3)
    } finally {
      setProcesando(false)
      setPasoAnim(-1)
    }
  }

  // Pantalla de confirmación final: ya no importa si el carrito quedó vacío (se limpió a propósito)
  if (confirmacion) {
    return (
      <section className="section container">
        <div className="compra-ok">
          <i className="bi bi-check-circle-fill" />
          <h2>¡Compra realizada!</h2>
          <p className="text-muted">Guarda el código de tu pedido, te servirá para hacerle seguimiento.</p>

          <div className="compra-ok-codigo">{confirmacion.pedidoCodigo}</div>

          <div className="compra-ok-datos">
            <div>
              <small>Pago</small>
              <strong>{confirmacion.pagoCodigo}</strong>
            </div>
            <div>
              <small>Método</small>
              <strong>{confirmacion.metodo}</strong>
            </div>
            <div>
              <small>Referencia</small>
              <strong>{confirmacion.referencia}</strong>
            </div>
            <div>
              <small>Total</small>
              <strong>{money(confirmacion.total)}</strong>
            </div>
            <div className="full">
              <small>Cliente</small>
              <strong>{confirmacion.clienteNombre}</strong>
            </div>
            {confirmacion.comprobanteCodigo && (
              <div className="full">
                <small>Boleta</small>
                <strong>{confirmacion.comprobanteCodigo}</strong>
              </div>
            )}
          </div>

          <p className="text-muted" style={{ marginTop: '1.1rem', fontSize: '0.85rem' }}>
            Te contactaremos al número/correo registrado.
          </p>

          {/* Descarga de la boleta: solo si el backend emitió un comprobante para este pedido */}
          {confirmacion.comprobanteCodigo && (
            <>
              <div className="compra-ok-actions">
                <button type="button" className="btn btn-primary btn-lg" onClick={descargarBoleta} disabled={descargandoBoleta}>
                  <i className="bi bi-file-earmark-pdf" /> {descargandoBoleta ? 'Generando...' : 'Descargar boleta'}
                </button>
              </div>
              <p className="text-muted" style={{ fontSize: '0.78rem' }}>
                Documento de demostración, sin validez tributaria.
              </p>
            </>
          )}

          <div className="compra-ok-actions">
            <Link to="/productos" className="btn btn-primary btn-lg">
              <i className="bi bi-grid-3x3-gap-fill" /> Seguir comprando
            </Link>
            <Link to="/" className="btn btn-outline btn-lg">
              <i className="bi bi-house-door" /> Volver al inicio
            </Link>
          </div>
        </div>
      </section>
    )
  }

  // Carrito vacío y sin compra confirmada: no hay nada que pagar
  if (items.length === 0) {
    return (
      <section className="section container">
        <div className="empty">
          <i className="bi bi-cart-x" />
          <h3>Tu carrito está vacío</h3>
          <p>Agrega productos desde el catálogo antes de continuar.</p>
          <Link to="/productos" className="btn btn-primary" style={{ marginTop: '1rem' }}>
            <i className="bi bi-grid-3x3-gap-fill" /> Ver catálogo
          </Link>
        </div>
      </section>
    )
  }

  // Segundo paso de la animación de pago: cambia el texto según el método elegido
  const pasosAnimacion = ['Validando datos', metodo === 'yape' ? 'Verificando la operación' : 'Autorizando con el banco', 'Confirmando tu pedido']

  return (
    <section className="section container">
      <div style={{ marginBottom: '1.5rem' }}>
        <span className="eyebrow">
          <i className="bi bi-credit-card" /> Checkout
        </span>
        <h1 style={{ fontSize: '1.8rem', marginTop: '0.75rem' }}>
          Finaliza tu <span className="accent">compra</span>
        </h1>
      </div>

      {/* Stepper visual: en que paso del proceso esta el comprador (no hace falta que sea clickeable) */}
      <div className="stepper">
        {PASOS_LABEL.map((label, i) => {
          const n = i + 1
          const estado = n < step ? 'done' : n === step ? 'active' : ''
          return (
            <div className={'step ' + estado} key={n}>
              <span className="step-circle">{n < step ? <i className="bi bi-check-lg" /> : n}</span>
              <span className="step-label">{label}</span>
              {n < PASOS_LABEL.length && <span className="step-line" />}
            </div>
          )
        })}
      </div>

      {error && <Alert type="error">{error}</Alert>}

      <div className="checkout-grid">
        <div>
          {/* ============ PASO 1: IDENTIFICACIÓN ============ */}
          {step === 1 && (
            <div className="panel">
              <div className="panel-head">
                <h5>
                  <i className="bi bi-person-badge" style={{ color: 'var(--accent)' }} /> ¿Cómo quieres comprar?
                </h5>
              </div>

              <div className="opcion-cards">
                <button type="button" className={'opcion-card' + (modo === 'invitado' ? ' sel' : '')} onClick={() => setModo('invitado')}>
                  <i className="bi bi-person" />
                  <strong>Comprar como invitado</strong>
                  <span className="text-muted">Solo necesitas tus datos para la entrega.</span>
                </button>
                <button type="button" className={'opcion-card' + (modo === 'cliente' ? ' sel' : '')} onClick={() => setModo('cliente')}>
                  <i className="bi bi-person-check" />
                  <strong>Ya soy cliente</strong>
                  <span className="text-muted">Recupera tus datos con tu DNI y correo.</span>
                </button>
              </div>

              {modo === 'cliente' && (
                <div className="ident-form">
                  {errorIdent && (
                    <Alert type="error">
                      {errorIdent}
                      <button type="button" className="btn btn-outline btn-sm" style={{ marginLeft: '0.6rem' }} onClick={continuarComoInvitado}>
                        Continuar como invitado
                      </button>
                    </Alert>
                  )}

                  <div className="form-grid">
                    <div className="field">
                      <label className="label">DNI *</label>
                      <input
                        className="input"
                        value={identDni}
                        onChange={(e) => setIdentDni(e.target.value.replace(/\D/g, '').slice(0, 8))}
                        maxLength={8}
                        placeholder="8 dígitos"
                      />
                    </div>
                    <div className="field">
                      <label className="label">Correo *</label>
                      <input
                        className="input"
                        type="email"
                        value={identEmail}
                        onChange={(e) => setIdentEmail(e.target.value)}
                        placeholder="tu@correo.com"
                      />
                    </div>
                  </div>

                  <button type="button" className="btn btn-primary" disabled={identificando} onClick={identificarCliente}>
                    <i className="bi bi-box-arrow-in-right" /> {identificando ? 'Verificando...' : 'Identificarme'}
                  </button>
                </div>
              )}

              {modo === 'invitado' && (
                <div className="wizard-nav">
                  <span />
                  <button type="button" className="btn btn-primary" onClick={() => setStep(2)}>
                    Continuar <i className="bi bi-arrow-right" />
                  </button>
                </div>
              )}
            </div>
          )}

          {/* ============ PASO 2: DATOS DEL COMPRADOR ============ */}
          {step === 2 && (
            <div className="panel">
              <div className="panel-head">
                <h5>
                  <i className="bi bi-person" style={{ color: 'var(--accent)' }} /> Datos del comprador
                </h5>
              </div>

              {identificado && (
                <Alert type="success">¡Hola, {cliente.nombres}! Cargamos tus datos.</Alert>
              )}

              <div className="field">
                <label className="label">DNI *</label>
                <div style={{ display: 'flex', gap: '0.4rem' }}>
                  <input
                    className="input"
                    value={cliente.dni}
                    readOnly={identificado}
                    onChange={(e) => setCliente((c) => ({ ...c, dni: e.target.value.replace(/\D/g, '').slice(0, 8) }))}
                    maxLength={8}
                    placeholder="8 dígitos"
                  />
                  {!identificado && (
                    <button type="button" className="btn btn-outline" onClick={buscarDni} disabled={buscandoDni}>
                      <i className="bi bi-search" /> {buscandoDni ? '...' : 'Buscar'}
                    </button>
                  )}
                </div>
                {identificado && <small className="text-muted">Identificado como cliente</small>}
              </div>

              <div className="form-grid">
                <div className="field">
                  <label className="label">Nombres *</label>
                  <input
                    className="input"
                    value={cliente.nombres}
                    onChange={(e) => setCliente((c) => ({ ...c, nombres: e.target.value }))}
                  />
                </div>
                <div className="field">
                  <label className="label">Apellidos *</label>
                  <input
                    className="input"
                    value={cliente.apellidos}
                    onChange={(e) => setCliente((c) => ({ ...c, apellidos: e.target.value }))}
                  />
                </div>
                <div className="field">
                  <label className="label">Teléfono</label>
                  <input
                    className="input"
                    value={cliente.telefono}
                    onChange={(e) => setCliente((c) => ({ ...c, telefono: e.target.value.replace(/\D/g, '').slice(0, 9) }))}
                    placeholder="987654321"
                  />
                </div>
                <div className="field">
                  <label className="label">Email</label>
                  <input
                    className="input"
                    type="email"
                    value={cliente.email}
                    onChange={(e) => setCliente((c) => ({ ...c, email: e.target.value }))}
                    placeholder="tu@correo.com"
                  />
                </div>
                <div className="field full">
                  <label className="label">Dirección de entrega</label>
                  <input
                    className="input"
                    value={cliente.direccion}
                    onChange={(e) => setCliente((c) => ({ ...c, direccion: e.target.value }))}
                    placeholder="Av. Ejemplo 123, Lima"
                  />
                </div>
              </div>

              <div className="wizard-nav">
                <button type="button" className="btn btn-outline" onClick={() => setStep(1)}>
                  <i className="bi bi-arrow-left" /> Atrás
                </button>
                <button type="button" className="btn btn-primary" disabled={!datosValidos} onClick={() => setStep(3)}>
                  Continuar <i className="bi bi-arrow-right" />
                </button>
              </div>
            </div>
          )}

          {/* ============ PASO 3: MÉTODO DE PAGO ============ */}
          {step === 3 && (
            <div className="panel">
              <div className="panel-head">
                <h5>
                  <i className="bi bi-credit-card" style={{ color: 'var(--accent)' }} /> Método de pago
                </h5>
              </div>

              <div className="opcion-cards">
                <button
                  type="button"
                  className={'opcion-card' + (metodo === 'yape' ? ' sel' : '') + (yapeBloqueado ? ' opcion-card-off' : '')}
                  disabled={yapeBloqueado}
                  onClick={() => setMetodo('yape')}
                >
                  <i className="bi bi-phone" style={{ color: '#7e22ce' }} />
                  <strong>Yape</strong>
                  {yapeBloqueado ? (
                    <span className="aviso-monto-max">Solo para pagos hasta S/ {yapeMontoMaximo} — usa tarjeta</span>
                  ) : (
                    <span className="text-muted">Escanea el QR y paga al instante.</span>
                  )}
                </button>
                <button type="button" className={'opcion-card' + (metodo === 'tarjeta' ? ' sel' : '')} onClick={() => setMetodo('tarjeta')}>
                  <i className="bi bi-credit-card-2-front" />
                  <strong>Tarjeta de crédito/débito</strong>
                  <span className="text-muted">Visa, Mastercard, Amex y más.</span>
                </button>
              </div>

              {/* El QR SOLO aparece cuando se elige Yape */}
              {metodo === 'yape' && (
                <div className="pasarela-yape">
                  <div className="pasarela-qr">
                    {yapeQr && !qrError ? (
                      <img src={yapeQr} alt="QR de Yape" onError={() => setQrError(true)} />
                    ) : (
                      <div className="pasarela-qr-fallback">
                        <i className="bi bi-qr-code-scan" />
                        <small>Pide el QR al vendedor</small>
                      </div>
                    )}
                  </div>

                  {yapeNumero && (
                    <div className="pasarela-cuenta">
                      <div className="pasarela-numero">{yapeNumero}</div>
                      {yapeTitular && <div className="text-muted">{yapeTitular}</div>}
                      <button type="button" className="btn btn-outline btn-sm" onClick={copiarNumero}>
                        <i className={'bi ' + (copiado ? 'bi-check2' : 'bi-clipboard')} /> {copiado ? 'Copiado' : 'Copiar'}
                      </button>
                    </div>
                  )}

                  <p className="pasarela-guia">
                    1) Escanea el QR o yapea al número. 2) Copia el N° de operación que te da Yape. 3) Pégalo aquí.
                  </p>

                  <div className="field">
                    <label className="label">N° de operación *</label>
                    <input
                      className="input"
                      value={numeroOperacion}
                      onChange={(e) => setNumeroOperacion(e.target.value.replace(/\D/g, '').slice(0, 20))}
                      maxLength={20}
                      placeholder="Ej. 123456"
                    />
                  </div>

                  <div className="aviso-demo aviso-demo-warning">
                    <i className="bi bi-exclamation-triangle-fill" />
                    <span>
                      Pago de prueba: este QR es real (cuenta de {yapeTitular}). Si yapeas por error, el monto será devuelto.
                    </span>
                  </div>
                </div>
              )}

              {metodo === 'tarjeta' && (
                <div className="pasarela-tarjeta">
                  {/* Pasarela real: el formulario de tarjeta vive dentro de <Elements> porque los hooks useStripe()/useElements() del subcomponente lo requieren */}
                  {stripeEnabled ? (
                    <Elements stripe={stripePromise}>
                      <FormularioTarjetaStripe
                        titular={tarjeta.titular}
                        setTitular={(v) => setTarjeta((t) => ({ ...t, titular: v }))}
                        onListo={manejarListoStripe}
                        procesando={tokenizando}
                      />
                    </Elements>
                  ) : (
                    <>
                      <div className="field">
                        <label className="label">
                          Número de tarjeta * {marca && <span className={'marca-badge marca-' + marca.toLowerCase()}>{marca}</span>}
                        </label>
                        <div className="pasarela-input-check">
                          <input
                            className="input"
                            value={tarjeta.numero}
                            onChange={(e) => setTarjeta((t) => ({ ...t, numero: formatearTarjeta(e.target.value) }))}
                            maxLength={23}
                            placeholder="1234 5678 9012 3456"
                          />
                          {numeroLimpio.length >= 13 &&
                            (tarjetaValida ? (
                              <i className="bi bi-check-circle-fill check-icon ok" />
                            ) : (
                              <i className="bi bi-x-circle-fill check-icon fail" />
                            ))}
                        </div>
                        {numeroLimpio.length >= 13 && !tarjetaValida && <small className="pasarela-error-txt">Número inválido</small>}
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
                    </>
                  )}

                  {stripeEnabled ? (
                    <div className="aviso-demo aviso-demo-info">
                      <i className="bi bi-info-circle-fill" />
                      <span>
                        Pasarela real (Stripe) en modo prueba: tu tarjeta se tokeniza en el navegador y nunca pasa por nuestro servidor. No
                        se realizan cobros reales.
                        <br />
                        <small className="text-muted">Tarjeta de prueba: 4242 4242 4242 4242 · cualquier fecha futura · CVC 123</small>
                      </span>
                    </div>
                  ) : (
                    <div className="aviso-demo aviso-demo-info">
                      <i className="bi bi-info-circle-fill" />
                      <span>
                        Modo demostración educativa: la validación es real (algoritmo de Luhn, vencimiento y CVV), pero no se realizan cobros
                        reales.
                        <br />
                        <small className="text-muted">Prueba: 4242 4242 4242 4242 aprueba · 4000 0000 0000 0002 rechaza</small>
                      </span>
                    </div>
                  )}
                </div>
              )}

              <div className="wizard-nav">
                <button type="button" className="btn btn-outline" onClick={() => setStep(2)}>
                  <i className="bi bi-arrow-left" /> Atrás
                </button>
                {metodo === 'tarjeta' && stripeEnabled ? (
                  <button
                    type="button"
                    className="btn btn-primary"
                    disabled={!stripeCompleto || tarjeta.titular.trim() === '' || tokenizando}
                    onClick={continuarConStripe}
                  >
                    {tokenizando ? 'Verificando...' : 'Continuar'} <i className="bi bi-arrow-right" />
                  </button>
                ) : (
                  <button type="button" className="btn btn-primary" disabled={!pagoValido} onClick={() => setStep(4)}>
                    Continuar <i className="bi bi-arrow-right" />
                  </button>
                )}
              </div>
            </div>
          )}

          {/* ============ PASO 4: REVISAR Y PAGAR ============ */}
          {step === 4 && (
            <div className="panel">
              <div className="panel-head">
                <h5>
                  <i className="bi bi-clipboard-check" style={{ color: 'var(--accent)' }} /> Revisa tu pedido
                </h5>
              </div>

              {!procesando ? (
                <>
                  <div className="revision-bloque">
                    <h6>Comprador</h6>
                    <div className="revision-datos">
                      <div>
                        <small>Nombre</small>
                        <strong>
                          {cliente.nombres} {cliente.apellidos}
                        </strong>
                      </div>
                      <div>
                        <small>DNI</small>
                        <strong>{cliente.dni}</strong>
                      </div>
                      <div>
                        <small>Teléfono</small>
                        <strong>{cliente.telefono || '—'}</strong>
                      </div>
                      <div>
                        <small>Email</small>
                        <strong>{cliente.email || '—'}</strong>
                      </div>
                      <div className="full">
                        <small>Dirección</small>
                        <strong>{cliente.direccion || '—'}</strong>
                      </div>
                    </div>
                  </div>

                  <div className="revision-bloque">
                    <h6>Método de pago</h6>
                    {metodo === 'yape' ? (
                      <div className="revision-metodo">
                        <i className="bi bi-phone" /> Yape · N° de operación <strong>{numeroOperacion}</strong>
                      </div>
                    ) : stripeEnabled && tarjetaResumen ? (
                      <div className="revision-metodo">
                        <i className="bi bi-credit-card-2-front" />{' '}
                        <span className={'marca-badge marca-' + tarjetaResumen.marca.toLowerCase()}>{tarjetaResumen.marca}</span> ••••{' '}
                        {tarjetaResumen.ult4}
                      </div>
                    ) : (
                      <div className="revision-metodo">
                        <i className="bi bi-credit-card-2-front" /> Tarjeta{' '}
                        {marca && <span className={'marca-badge marca-' + marca.toLowerCase()}>{marca}</span>} **** {numeroLimpio.slice(-4)}
                      </div>
                    )}
                  </div>

                  <div className="wizard-nav">
                    <button type="button" className="btn btn-outline" onClick={() => setStep(3)}>
                      <i className="bi bi-arrow-left" /> Atrás
                    </button>
                  </div>

                  <button type="button" className="btn btn-primary btn-lg btn-block" style={{ marginTop: '1rem' }} onClick={pagar}>
                    <i className="bi bi-lock-fill" /> Pagar {money(total)}
                  </button>
                </>
              ) : (
                <div className="procesando-pasos">
                  {pasosAnimacion.map((label, i) => (
                    <div className={'procesando-paso' + (i < pasoAnim ? ' done' : i === pasoAnim ? ' active' : '')} key={i}>
                      {i < pasoAnim ? (
                        <i className="bi bi-check-circle-fill" />
                      ) : i === pasoAnim ? (
                        <span className="spinner-sm" />
                      ) : (
                        <i className="bi bi-circle" />
                      )}
                      <span>{label}</span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>

        {/* Resumen del pedido: visible en TODOS los pasos del wizard */}
        <div className="panel checkout-resumen">
          <div className="panel-head">
            <h5>
              <i className="bi bi-receipt" style={{ color: 'var(--accent)' }} /> Resumen del pedido
            </h5>
          </div>

          <div className="checkout-items">
            {items.map((i) => (
              <div className="checkout-item" key={i.id}>
                <span>
                  {i.nombre} <span className="text-muted">× {i.cantidad}</span>
                </span>
                <strong>{money(i.precio * i.cantidad)}</strong>
              </div>
            ))}
          </div>

          <div className="checkout-total">
            <span>Total</span>
            <strong>{money(total)}</strong>
          </div>

          {!procesando && (
            <p className="text-muted" style={{ fontSize: '0.82rem' }}>
              {/* Recordatorio de en que paso esta el comprador, sin repetir el stepper */}
              Paso {step} de 4 — completa los pasos para confirmar tu compra.
            </p>
          )}
        </div>
      </div>
    </section>
  )
}
