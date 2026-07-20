package com.gamerstore.app.controller;

import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import com.gamerstore.app.config.security.LoginBloqueadoException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

/** Convierte las excepciones en respuestas JSON limpias: { "error": "..." }. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Helper interno: arma la respuesta JSON { "error": mensaje } con el status HTTP indicado.
    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String msg) {
        return ResponseEntity.status(status).body(Map.of("error", msg));
    }

    /** Errores de Spring Validator (@Valid): junta los mensajes de cada campo. */
    // Responde 400 Bad Request con el resumen de errores y el detalle por campo.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validacion(MethodArgumentNotValidException e) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError fe : e.getBindingResult().getFieldErrors()) {
            errores.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        String resumen = String.join(". ", errores.values());
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("error", resumen.isBlank() ? "Datos inválidos" : resumen);
        resp.put("errores", errores);
        return ResponseEntity.badRequest().body(resp);
    }

    // Reenvía el status y el mensaje que ya trae la propia excepción (usada con orElseThrow en los controllers).
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> estado(ResponseStatusException e) {
        String msg = e.getReason() != null ? e.getReason() : "Error";
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", msg));
    }

    // 400 Bad Request: argumentos inválidos lanzados manualmente en services/controllers (p. ej. validaciones propias).
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException e) {
        return body(HttpStatus.BAD_REQUEST, e.getMessage() != null ? e.getMessage() : "Solicitud inválida");
    }

    // 404 Not Found: se dispara cuando un orElseThrow() no encuentra el recurso pedido.
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, Object>> notFound(NoSuchElementException e) {
        return body(HttpStatus.NOT_FOUND, "Recurso no encontrado");
    }

    /** Credenciales inválidas en el login (Spring Security). */
    // 401 Unauthorized.
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> auth(AuthenticationException e) {
        return body(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos");
    }

    /** Login bloqueado por demasiados intentos fallidos (429). Incluye los segundos restantes. */
    @ExceptionHandler(LoginBloqueadoException.class)
    public ResponseEntity<Map<String, Object>> loginBloqueado(LoginBloqueadoException e) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("error", e.getMessage());
        resp.put("segundosRestantes", e.getSegundosRestantes());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(resp);
    }

    /**
     * Red de seguridad para violaciones de integridad que no fueron interceptadas
     * previamente en los servicios. Distingue entre restricción UNIQUE (duplicado)
     * y restricción de FK (registro referenciado por otro).
     */
    // Responde 409 Conflict en ambos casos (duplicado o referenciado), cambiando solo el mensaje.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> conflict(DataIntegrityViolationException e) {
        String rootMsg = (e.getRootCause() != null && e.getRootCause().getMessage() != null)
                ? e.getRootCause().getMessage().toLowerCase()
                : "";

        if (rootMsg.contains("foreign key") || rootMsg.contains("referential integrity")
                || rootMsg.contains("fk_") || rootMsg.contains("constraint")) {
            return body(HttpStatus.CONFLICT,
                    "No se puede eliminar: el registro está siendo usado por otros datos");
        }
        if (rootMsg.contains("unique") || rootMsg.contains("duplicate entry")
                || rootMsg.contains("already exists")) {
            return body(HttpStatus.CONFLICT,
                    "Ya existe un registro con esos datos");
        }
        return body(HttpStatus.CONFLICT, "Operación no permitida: conflicto de datos");
    }

    // 413 Payload Too Large: el archivo subido (p. ej. en UploadController) supera el límite configurado (5MB).
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> tooLarge(MaxUploadSizeExceededException e) {
        return body(HttpStatus.PAYLOAD_TOO_LARGE, "La imagen supera el tamaño máximo (5MB)");
    }

    /** Fechas mal formadas en parámetros (p. ej. el reporte de pedidos). */
    // 400 Bad Request.
    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<Map<String, Object>> fechaInvalida(DateTimeParseException e) {
        return body(HttpStatus.BAD_REQUEST, "Fecha inválida");
    }
}

