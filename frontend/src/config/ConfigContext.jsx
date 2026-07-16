import { createContext, useContext, useEffect, useState } from 'react'
import { PublicAPI } from '../api/endpoints'

// Valores de respaldo mientras se carga (o si falla) la config real del backend
const DEFAULTS = { tiendaNombre: 'GamerStore', whatsappNumero: '51986969024' }

// Contexto con los datos generales de la tienda (nombre, whatsapp, etc.)
const ConfigContext = createContext(DEFAULTS)

// Al montar, pide la config publica a la API y la reemplaza sobre los DEFAULTS;
// si la peticion falla, la app sigue funcionando con los valores por defecto.
export function ConfigProvider({ children }) {
  const [config, setConfig] = useState(DEFAULTS)

  useEffect(() => {
    PublicAPI.config()
      .then((c) => c && setConfig(c))
      .catch(() => {
        /* si falla, se usan los valores por defecto */
      })
  }, [])

  return <ConfigContext.Provider value={config}>{children}</ConfigContext.Provider>
}

// Hook de conveniencia para leer la config de la tienda desde cualquier componente
export const useConfig = () => useContext(ConfigContext)
