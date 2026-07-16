package com.gamerstore.app.controller;

import com.gamerstore.app.dto.UploadResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/** Guarda la imagen subida en la carpeta del proyecto (uploads/productos) y devuelve su ruta pública. */
@RestController
@RequestMapping("/api/admin/uploads")
public class UploadController {

    private final Path dir;

    // La carpeta física de destino viene de application.properties (app.uploads.dir), no queda hardcodeada.
    public UploadController(@Value("${app.uploads.dir}") String uploadsDir) {
        this.dir = Paths.get(uploadsDir).toAbsolutePath().normalize();
    }

    // POST /api/admin/uploads: recibe la imagen como multipart/form-data, valida que no esté vacía y
    // que su content-type sea "image/*"; luego la guarda en disco (carpeta app.uploads.dir) con un
    // nombre aleatorio (UUID) y la extensión según el tipo, y devuelve la ruta pública para usarla en el front.
    @PostMapping
    public UploadResponse subir(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío");
        }
        String ct = file.getContentType();
        if (ct == null || !ct.startsWith("image/")) {
            throw new IllegalArgumentException("El archivo debe ser una imagen");
        }
        Files.createDirectories(dir);
        // Determina la extensión del archivo según el content-type recibido (por defecto .jpg).
        String ext = switch (ct) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
        // Genera un nombre único (UUID sin guiones) para no pisar archivos existentes.
        String name = UUID.randomUUID().toString().replace("-", "") + ext;
        Files.copy(file.getInputStream(), dir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        return new UploadResponse("/images/productos/" + name);
    }
}
