package com.gamerstore.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Sirve la SPA de React compilada (src/main/resources/static) y hace el "fallback":
 * cualquier ruta que NO sea un archivo estatico ni empiece por /api se resuelve al
 * index.html, para que React Router maneje el enrutado del lado del cliente al
 * recargar URLs como /admin/productos.
 *
 * Las rutas /api/** las atienden los @RestController (tienen mayor prioridad que
 * los resource handlers), asi que esto no interfiere con la API.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requested = location.createRelative(resourcePath);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        // No es un archivo real: si es una llamada a la API, no interceptar (404 real);
                        // si no, devolver index.html para que React Router se encargue.
                        if (resourcePath.startsWith("api/")) {
                            return null;
                        }
                        return new ClassPathResource("/static/index.html");
                    }
                });
    }
}
