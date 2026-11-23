package com.example.huertolindo_backend.controller;

import com.example.huertolindo_backend.config.FileStorageService;
import com.example.huertolindo_backend.dto.ApiDtos.EvidenciaResponse;
import com.example.huertolindo_backend.service.HuertoService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/evidencias")
public class EvidenciaController {
    private final HuertoService service;
    private final FileStorageService fileStorageService;

    public EvidenciaController(HuertoService service, FileStorageService fileStorageService) {
        this.service = service;
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.CREATED)
    public EvidenciaResponse subir(@RequestParam Long tareaId,
                                   @RequestParam(required = false) String comentario,
                                   @RequestParam("file") MultipartFile file,
                                   Authentication authentication) throws IOException {
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        service.validarAccesoTarea(tareaId, authentication.getName(), admin);
        String path = fileStorageService.store(file);
        return service.crearEvidencia(tareaId, comentario, path, authentication.getName(), admin);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<EvidenciaResponse> listar() {
        return service.evidencias();
    }
}
