import { Outlet } from 'react-router-dom'
import Navbar from './Navbar.jsx'
import Footer from './Footer.jsx'

// Layout público: pone el Navbar arriba y el Footer abajo, y en medio el <Outlet />
// que React Router reemplaza por la página actual (Inicio, Catálogo, Contacto, etc).
export default function PublicLayout() {
  return (
    <>
      <Navbar />
      <main>
        <Outlet />
      </main>
      <Footer />
    </>
  )
}
