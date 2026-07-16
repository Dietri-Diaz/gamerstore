package com.gamerstore.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Punto de entrada de la aplicacion Spring Boot: @SpringBootApplication activa
// autoconfiguracion, escaneo de componentes (@Service, @Component, @RestController, etc.)
// y la configuracion de Spring en todo el paquete com.gamerstore.app.
@SpringBootApplication
public class GamerStoreApplication {
    // Arranca el contenedor de Spring (crea el contexto, levanta el servidor embebido y
    // ejecuta los CommandLineRunner como DataSeeder).
    public static void main(String[] args) {
        SpringApplication.run(GamerStoreApplication.class, args);
    }
}
