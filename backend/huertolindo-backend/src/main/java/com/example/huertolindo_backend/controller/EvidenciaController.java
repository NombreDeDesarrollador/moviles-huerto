package com.example.huertolindo_backend.controller;

import com.example.huertolindo_backend.config.FileStorageService;
import com.example.huertolindo_backend.dto.ApiDtos.ApiError;
import com.example.huertolindo_backend.dto.ApiDtos.EvidenciaResponse;
import com.example.huertolindo_backend.service.HuertoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/evidencias")
@Tag(name = "Evidencias", description = "Subida y consulta de evidencias fotográficas de tareas")
public class EvidenciaController {
    private final HuertoService service;
    private final FileStorageService fileStorageService;

    public EvidenciaController(HuertoService service, FileStorageService fileStorageService) {
        this.service = service;
        this.fileStorageService = fileStorageService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Subir evidencia fotográfica", description = "Sube un archivo de imagen como evidencia del cumplimiento de una tarea")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Evidencia subida exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = EvidenciaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Archivo no válido o datos incorrectos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "No existe una tarea con el tareaId proporcionado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "La tarea no está asignada a este usuario",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "413", description = "El archivo supera el tamaño máximo permitido (10 MB)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public EvidenciaResponse subir(@Parameter(description = "ID de la tarea a la cual asociar la evidencia", example = "5", required = true) @RequestParam Long tareaId,
                                   @Parameter(description = "Comentario u observación opcional sobre la evidencia", example = "Foto de avance del cultivo") @RequestParam(required = false) String comentario,
                                   @Parameter(description = "Archivo de imagen (PNG, JPG, etc.)", required = true) @RequestParam("file") MultipartFile file,
                                   Authentication authentication) throws IOException {
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        service.validarAccesoTarea(tareaId, authentication.getName(), admin);
        String path = fileStorageService.store(file);
        return service.crearEvidencia(tareaId, comentario, path, authentication.getName(), admin);
    }

    @GetMapping
    @Operation(summary = "Listar evidencias", description = "Retorna la lista de todas las evidencias fotográficas registradas")
    @ApiResponse(responseCode = "200", description = "Lista de evidencias obtenida exitosamente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = EvidenciaResponse.class))))
    public List<EvidenciaResponse> listar() {
        return service.evidencias();
    }
}
