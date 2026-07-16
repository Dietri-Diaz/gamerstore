package com.gamerstore.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Provee el codificador BCrypt para guardar y comparar contrasenas hasheadas.
 * Es solo el utilitario de cifrado, no el framework de seguridad.
 */
@Configuration
public class PasswordConfig {

    // Bean inyectable en toda la app (DataSeeder, servicios de auth, etc.) para
    // encriptar contrasenas al guardarlas y verificarlas al hacer login.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
