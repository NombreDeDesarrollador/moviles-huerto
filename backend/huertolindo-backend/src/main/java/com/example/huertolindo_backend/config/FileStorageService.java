package com.example.huertolindo_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

@Service
public class FileStorageService {
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif"
    );

    private final Path uploadDirectory;

    public FileStorageService(@Value("${app.upload-dir:uploads}") String uploadDirectory) {
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    public String store(MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        if (file.isEmpty() || contentType == null || !EXTENSIONS.containsKey(contentType)) {
            throw new IllegalArgumentException("El archivo debe ser una imagen JPEG, PNG, WebP o GIF no vacía");
        }

        Files.createDirectories(uploadDirectory);
        String filename = UUID.randomUUID() + EXTENSIONS.get(contentType);
        Path destination = uploadDirectory.resolve(filename).normalize();
        if (!destination.getParent().equals(uploadDirectory)) {
            throw new IllegalArgumentException("Nombre de archivo inválido");
        }
        file.transferTo(destination);
        return "/uploads/" + filename;
    }
}
