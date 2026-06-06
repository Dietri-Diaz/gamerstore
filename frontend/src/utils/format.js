// Formatea un precio como "S/ 1,234.50".
export function money(value) {
  const n = Number(value || 0)
  return 'S/ ' + n.toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// Construye un enlace de WhatsApp con texto opcional.
export function waUrl(numero, texto) {
  const base = `https://wa.me/${numero}`
  return texto ? `${base}?text=${encodeURIComponent(texto)}` : base
}

// SKU visible de un producto.
export const sku = (id) => `GS-${id}`
