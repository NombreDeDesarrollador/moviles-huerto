package com.example.huertolindo_backend.controller;

import com.example.huertolindo_backend.dto.ApiDtos.ApiError;
import com.example.huertolindo_backend.dto.ApiDtos.ValidacionRequest;
import com.example.huertolindo_backend.dto.ApiDtos.ValidacionResponse;
import com.example.huertolindo_backend.service.HuertoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/validaciones")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Validaciones", description = "Aprobación y retroalimentación docente para tareas y evidencias (Requiere rol ADMIN)")
public class ValidacionController {
    private final HuertoService service;

    public ValidacionController(HuertoService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear validación docente", description = "Aprueba o rechaza una tarea/evidencia con comentarios u observaciones pedagógicas (Requiere rol ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Validación registrada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ValidacionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Debe especificar exactamente una tarea o evidencia",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "No existe una tarea o evidencia con el id proporcionado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere permisos de administrador)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ValidacionResponse crear(@Valid @RequestBody ValidacionRequest request, Authentication authentication) {
        return service.crearValidacion(request, authentication.getName());
    }

    @GetMapping
    @Operation(summary = "Listar validaciones", description = "Retorna la lista de todas las validaciones registradas por docentes (Requiere rol ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de validaciones obtenida exitosamente",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ValidacionResponse.class)))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere permisos de administrador)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public List<ValidacionResponse> listar() {
        return service.validaciones();
    }
}
