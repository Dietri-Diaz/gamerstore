package com.gamerstore.app.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

// Lleva la cuenta de intentos fallidos de login por usuario, en memoria (no
// necesita BD), y bloquea temporalmente al superar el maximo permitido.
@Service
public class LoginAttemptService {

    @Value("${app.login.max-intentos:3}")
    private int maxIntentos;

    @Value("${app.login.bloqueo-segundos:30}")
    private int bloqueoSegundos;

    private final ConcurrentHashMap<String, Intento> intentos = new ConcurrentHashMap<>();

    // Estado guardado por usuario: cuantos fallos lleva y, si ya se bloqueo, hasta cuando.
    private static class Intento {
        int fallos = 0;
        Instant bloqueadoHasta;
    }

    // Normaliza el username a minusculas y sin espacios para usarlo como clave del mapa.
    private String clave(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }

    /** Segundos que faltan para poder volver a intentar; 0 si no esta bloqueado. */
    public int segundosBloqueo(String username) {
        Intento i = intentos.get(clave(username));
        if (i == null || i.bloqueadoHasta == null) {
            return 0;
        }
        Duration restante = Duration.between(Instant.now(), i.bloqueadoHasta);
        if (restante.isNegative() || restante.isZero()) {
            // Ya paso el bloqueo: se limpia para que arranque de cero.
            i.bloqueadoHasta = null;
            i.fallos = 0;
            return 0;
        }
        // Redondea hacia arriba (p. ej. 29.2s -> 30s) para no cortar el bloqueo antes de tiempo.
        return (int) Math.ceil(restante.toNanos() / 1_000_000_000.0);
    }

    /** Registra un intento fallido; si llega al maximo, activa el bloqueo y reinicia el contador. */
    public void registrarFallo(String username) {
        Intento i = intentos.computeIfAbsent(clave(username), k -> new Intento());
        i.fallos++;
        if (i.fallos >= maxIntentos) {
            i.bloqueadoHasta = Instant.now().plusSeconds(bloqueoSegundos);
            i.fallos = 0;
        }
    }

    /** Intentos que le quedan antes de bloquearse. */
    public int intentosRestantes(String username) {
        Intento i = intentos.get(clave(username));
        int fallos = i == null ? 0 : i.fallos;
        return Math.max(0, maxIntentos - fallos);
    }

    /** Limpia el registro del usuario (se llama al loguear correctamente). */
    public void limpiar(String username) {
        intentos.remove(clave(username));
    }
}
