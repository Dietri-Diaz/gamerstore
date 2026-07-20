package com.gamerstore.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

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

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${app.uploads.dir}")
    private String uploadsDir;

    // Permite que el front en desarrollo (Vite, :5173) llame a la API en :8080.
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.split(","))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    }

    // Registra dos fuentes de archivos estaticos: las imagenes subidas (carpeta externa
    // configurable) y el build de React empaquetado en resources/static, este ultimo con
    // el resolver de fallback a index.html definido mas abajo.
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploads = Paths.get(uploadsDir).toAbsolutePath().normalize();
        String uploadsLocation = uploads.toUri().toString();
        if (!uploadsLocation.endsWith("/")) uploadsLocation += "/";
        registry.addResourceHandler("/images/productos/**")
                .addResourceLocations(uploadsLocation);

        // QR de Yape para la pasarela de pagos (carpeta física uploads/qr/).
        Path qrDir = Paths.get("uploads/qr").toAbsolutePath().normalize();
        String qrLocation = qrDir.toUri().toString();
        if (!qrLocation.endsWith("/")) qrLocation += "/";
        registry.addResourceHandler("/images/qr/**")
                .addResourceLocations(qrLocation);

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
