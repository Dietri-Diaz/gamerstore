import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import App from './App.jsx'
import { ConfigProvider } from './config/ConfigContext.jsx'
import { AuthProvider } from './auth/AuthContext.jsx'
import { ToastProvider } from './components/ui/Toast.jsx'
import { ConfirmProvider } from './components/ui/Confirm.jsx'
import { CarritoProvider } from './carrito/CarritoContext.jsx'
import './index.css'

// Punto de entrada de la app: monta <App /> dentro de una cadena de providers.
// El orden de anidado importa porque cada provider expone un contexto que los
// hijos consumen (por ejemplo AuthProvider necesita estar antes de App para
// que las rutas admin puedan leer el usuario logueado).
ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    {/* Habilita las rutas (Routes/Route) definidas en App.jsx */}
    <BrowserRouter>
      {/* Config de la tienda (nombre, whatsapp, etc.) traida del backend */}
      <ConfigProvider>
        {/* Sesion del admin: usuario, login/logout, tokens */}
        <AuthProvider>
          {/* Notificaciones tipo toast disponibles en toda la app */}
          <ToastProvider>
            {/* Dialogos de confirmacion (ej. "seguro que deseas eliminar?") */}
            <ConfirmProvider>
              {/* Carrito de compras de la tienda publica, persistido en localStorage */}
              <CarritoProvider>
                <App />
              </CarritoProvider>
            </ConfirmProvider>
          </ToastProvider>
        </AuthProvider>
      </ConfigProvider>
    </BrowserRouter>
  </React.StrictMode>
)
