package com.gamerstore.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de la API REST sobre una base H2 en memoria (perfil "test").
 * Verifica: arranque del contexto, endpoints publicos abiertos, proteccion
 * de /api/admin/** y el flujo login -> JWT -> acceso autorizado.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper om;

    @Test
    void endpointsPublicosAbiertos() throws Exception {
        mvc.perform(get("/api/productos")).andExpect(status().isOk());
        mvc.perform(get("/api/categorias")).andExpect(status().isOk());
        mvc.perform(get("/api/config")).andExpect(status().isOk());
    }

    @Test
    void adminSinTokenDevuelve401() throws Exception {
        mvc.perform(get("/api/admin/productos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/dashboard")).andExpect(status().isUnauthorized());
    }

    @Test
    void loginInvalidoDevuelve401() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin123\",\"password\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginEmiteTokenYDaAccesoAlAdmin() throws Exception {
        String respuesta = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin123\",\"password\":\"gamerstore123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.rol").value("ADMIN"))
                .andReturn().getResponse().getContentAsString();

        String token = om.readTree(respuesta).get("token").asText();

        mvc.perform(get("/api/admin/productos").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mvc.perform(get("/api/admin/dashboard").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.totalProductos").exists());
    }
}
