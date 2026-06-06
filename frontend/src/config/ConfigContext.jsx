import { createContext, useContext, useEffect, useState } from 'react'
import { PublicAPI } from '../api/endpoints'

const DEFAULTS = { tiendaNombre: 'GamerStore', whatsappNumero: '51986969024' }

const ConfigContext = createContext(DEFAULTS)

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

export const useConfig = () => useContext(ConfigContext)
