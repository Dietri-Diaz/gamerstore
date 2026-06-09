package com.gamerstore.app.controller;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/** Convierte las excepciones en respuestas JSON limpias: { "error": "..." }. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String msg) {
        return ResponseEntity.status(status).body(Map.of("error", msg));
    }

    /** Errores de Spring Validator (@Valid): junta los mensajes de cada campo. */
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

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> estado(ResponseStatusException e) {
        String msg = e.getReason() != null ? e.getReason() : "Error";
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", msg));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException e) {
        return body(HttpStatus.BAD_REQUEST, e.getMessage() != null ? e.getMessage() : "Solicitud inválida");
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, Object>> notFound(NoSuchElementException e) {
        return body(HttpStatus.NOT_FOUND, "Recurso no encontrado");
    }

    /**
     * Red de seguridad para violaciones de integridad que no fueron interceptadas
     * previamente en los servicios. Distingue entre restricción UNIQUE (duplicado)
     * y restricción de FK (registro referenciado por otro).
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> conflict(DataIntegrityViolationException e) {
        String rootMsg = e.getRootCause() != null
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
}

