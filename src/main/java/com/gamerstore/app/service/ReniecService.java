package com.gamerstore.app.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gamerstore.app.dto.ReniecPersona;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/** Consulta datos reales de personas por DNI en apiperu.dev (RENIEC). Best-effort: nunca rompe el flujo. */
@Service
public class ReniecService {

    private static final Logger log = LoggerFactory.getLogger(ReniecService.class);

    private final boolean enabled;
    private final String token;
    private final RestClient client;

    // Lee de application.properties si el servicio está habilitado, la URL base y el token de apiperu.dev.
    public ReniecService(@Value("${app.apidevperu.enabled:true}") boolean enabled,
                         @Value("${app.apidevperu.base-url:https://apiperu.dev/api}") String baseUrl,
                         @Value("${app.apidevperu.token:}") String token) {
        this.enabled = enabled;
        this.token = token;
        // Timeouts para que un apiperu.dev lento/caído NUNCA cuelgue el arranque (seeder) ni el endpoint.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(4000);
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    /**
     * Consulta el DNI en apiperu.dev y arma nombres/apellidos. Si el feature está apagado, falta el token
     * o el DNI no tiene 8 dígitos, no llama a la API. Cualquier error de red/parseo se atrapa y devuelve
     * vacío: es "best-effort", nunca debe romper el registro de un cliente.
     */
    public Optional<ReniecPersona> consultarDni(String dni) {
        if (!enabled || token == null || token.isBlank() || dni == null || !dni.matches("\\d{8}")) {
            return Optional.empty();
        }
        try {
            ApiPeruResponse resp = client.get()
                    .uri("/dni/{dni}", dni)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(ApiPeruResponse.class);
            if (resp != null && resp.success() && resp.data() != null) {
                ApiPeruData d = resp.data();
                String ap = d.apellidoPaterno() != null ? d.apellidoPaterno() : "";
                String am = d.apellidoMaterno() != null ? d.apellidoMaterno() : "";
                String apellidos = (ap + " " + am).trim();
                return Optional.of(new ReniecPersona(d.nombres(), apellidos, d.nombreCompleto()));
            }
        } catch (Exception e) {
            log.warn("RENIEC: no se pudo consultar el DNI {} ({})", dni, e.getMessage());
        }
        return Optional.empty();
    }

    // ---- Estructura de la respuesta de apiperu.dev ----
    // Mapea el JSON { success, data } de la API.
    private record ApiPeruResponse(boolean success, ApiPeruData data) {}
    // Mapea los datos de la persona (nombres y apellidos vienen en snake_case en el JSON).
    private record ApiPeruData(
            String nombres,
            @JsonProperty("apellido_paterno") String apellidoPaterno,
            @JsonProperty("apellido_materno") String apellidoMaterno,
            @JsonProperty("nombre_completo") String nombreCompleto) {}
}
