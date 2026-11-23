package com.example.huertolindo_backend.controller;

import com.example.huertolindo_backend.dto.ApiDtos.ApiError;
import com.example.huertolindo_backend.dto.ApiDtos.SectorRequest;
import com.example.huertolindo_backend.dto.ApiDtos.SectorResponse;
import com.example.huertolindo_backend.service.HuertoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sectores")
@Tag(name = "Sectores", description = "Operaciones CRUD para la administración de sectores del huerto")
public class SectorController {
    private final HuertoService service;

    public SectorController(HuertoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar sectores", description = "Retorna la lista de todos los sectores registrados en el huerto")
    @ApiResponse(responseCode = "200", description = "Lista de sectores obtenida exitosamente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = SectorResponse.class))))
    public List<SectorResponse> listar() {
        return service.sectores();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener sector por ID", description = "Retorna el detalle de un sector específico según su identificador")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sector encontrado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SectorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe un sector con el id proporcionado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "400", description = "El ID proporcionado tiene un formato numérico inválido",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public SectorResponse obtener(@Parameter(description = "ID único del sector a consultar", example = "3", required = true) @PathVariable Long id) {
        return service.sector(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear nuevo sector", description = "Crea un nuevo sector en el huerto (Requiere rol ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Sector creado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SectorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de solicitud inválidos o campos incompletos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere permisos de administrador)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public SectorResponse crear(@Valid @RequestBody SectorRequest request) {
        return service.crearSector(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar sector", description = "Actualiza el nombre, descripción y ubicación de un sector existente (Requiere rol ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sector actualizado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SectorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de solicitud inválidos o ID con formato incorrecto",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "No existe un sector con el id proporcionado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere permisos de administrador)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public SectorResponse actualizar(@Parameter(description = "ID del sector a actualizar", example = "3", required = true) @PathVariable Long id,
                                     @Valid @RequestBody SectorRequest request) {
        return service.actualizarSector(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar sector", description = "Elimina un sector por su ID (Requiere rol ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Sector eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "No existe un sector con el id proporcionado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere permisos de administrador)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public void eliminar(@Parameter(description = "ID del sector a eliminar", example = "3", required = true) @PathVariable Long id) {
        service.eliminarSector(id);
    }
}
