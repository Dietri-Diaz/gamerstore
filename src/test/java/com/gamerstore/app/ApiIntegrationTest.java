package com.gamerstore.app;

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
 * Verifica: arranque del contexto, endpoints publicos, login y, sobre todo,
 * la validacion de datos (Spring Validator) en los formularios.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void endpointsPublicosResponden() throws Exception {
        mvc.perform(get("/api/productos")).andExpect(status().isOk());
        mvc.perform(get("/api/categorias")).andExpect(status().isOk());
        mvc.perform(get("/api/config")).andExpect(status().isOk());
    }

    @Test
    void loginValido() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin123\",\"password\":\"gamerstore123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("ADMIN"));
    }

    @Test
    void loginInvalidoDevuelve401() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin123\",\"password\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginConCamposVaciosFallaValidacion() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearProductoInvalidoFallaValidacion() throws Exception {
        // nombre vacio, precio 0 y stock negativo -> debe rechazarse con 400
        mvc.perform(post("/api/admin/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\",\"precio\":0,\"stock\":-1,\"categoriaId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores").exists());
    }

    @Test
    void crearClienteConDniInvalidoFallaValidacion() throws Exception {
        // DNI no numerico / longitud incorrecta -> 400
        mvc.perform(post("/api/admin/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dni\":\"abc\",\"nombres\":\"Juan\",\"apellidos\":\"Perez\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearPedidoValido() throws Exception {
        // cliente 1 y producto 1 vienen del DataSeeder
        mvc.perform(post("/api/admin/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\":1,\"metodoPago\":\"EFECTIVO\"," +
                                "\"items\":[{\"productoId\":1,\"cantidad\":2}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.total").value(4998.0));
    }

    @Test
    void crearPedidoSinItemsFallaValidacion() throws Exception {
        mvc.perform(post("/api/admin/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\":1,\"items\":[]}"))
                .andExpect(status().isBadRequest());
    }
}
