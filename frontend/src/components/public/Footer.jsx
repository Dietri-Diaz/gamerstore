import { Link } from 'react-router-dom'
import { useConfig } from '../../config/ConfigContext.jsx'
import { waUrl } from '../../utils/format.js'

export default function Footer() {
  const { whatsappNumero } = useConfig()

  return (
    <footer className="footer">
      <div className="container">
        <div className="footer-grid">
          <div>
            <h5>
              <i className="bi bi-controller" /> GamerStore
            </h5>
            <p>
              Tu tienda gamer de confianza. Consolas, periféricos y monitores con envío rápido y
              garantía real.
            </p>
            <div className="footer-social">
              <a href="#" aria-label="Twitch"><i className="bi bi-twitch" /></a>
              <a href="#" aria-label="Discord"><i className="bi bi-discord" /></a>
              <a href="#" aria-label="YouTube"><i className="bi bi-youtube" /></a>
              <a href="#" aria-label="Instagram"><i className="bi bi-instagram" /></a>
            </div>
          </div>

          <div>
            <h5>Navegación</h5>
            <ul>
              <li><Link to="/">Inicio</Link></li>
              <li><Link to="/productos">Catálogo</Link></li>
              <li><Link to="/contacto">Contacto</Link></li>
            </ul>
          </div>

          <div>
            <h5>Categorías</h5>
            <ul>
              <li><Link to="/productos?categoria=Consolas">Consolas</Link></li>
              <li><Link to="/productos?categoria=Perifericos">Periféricos</Link></li>
              <li><Link to="/productos?categoria=Monitores">Monitores</Link></li>
              <li><Link to="/productos?categoria=VR">Realidad Virtual</Link></li>
            </ul>
          </div>

          <div>
            <h5>Contacto</h5>
            <ul>
              <li><i className="bi bi-geo-alt" /> Lima, Perú</li>
              <li>
                <a href={waUrl(whatsappNumero)} target="_blank" rel="noreferrer">
                  <i className="bi bi-whatsapp" style={{ color: 'var(--whatsapp)' }} /> +51 986 969 024
                </a>
              </li>
              <li><i className="bi bi-envelope" /> hola@gamerstore.gg</li>
            </ul>
          </div>
        </div>

        <div className="footer-bottom">
          <span>&copy; 2026 GamerStore. Todos los derechos reservados.</span>
          <span>
            <a href="#">Términos</a> &nbsp;·&nbsp; <a href="#">Privacidad</a>
          </span>
        </div>
      </div>
    </footer>
  )
}
