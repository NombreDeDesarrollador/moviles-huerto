package com.example.huertolindo_backend.controller;

import com.example.huertolindo_backend.dto.ApiDtos.*;
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
@RequestMapping("/api/cultivos")
@Tag(name = "Cultivos", description = "Operaciones CRUD y cambio de estado para cultivos del huerto")
public class CultivoController {
    private final HuertoService service;

    public CultivoController(HuertoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar cultivos", description = "Retorna la lista de cultivos, opcionalmente filtrada por sector")
    @ApiResponse(responseCode = "200", description = "Lista de cultivos obtenida exitosamente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = CultivoResponse.class))))
    public List<CultivoResponse> listar(@Parameter(description = "ID opcional del sector para filtrar cultivos", example = "3") @RequestParam(required = false) Long sectorId) {
        return service.cultivos(sectorId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener cultivo por ID", description = "Retorna la información detallada de un cultivo específico")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cultivo encontrado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CultivoResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe un cultivo con el id proporcionado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "400", description = "El ID proporcionado tiene un formato numérico inválido",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public CultivoResponse obtener(@Parameter(description = "ID del cultivo a consultar", example = "10", required = true) @PathVariable Long id) {
        return service.cultivo(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear nuevo cultivo", description = "Registra un nuevo cultivo asociado a un sector del huerto (Requiere rol ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cultivo creado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CultivoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de solicitud inválidos o campos incompletos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "No existe un sector con el sectorId proporcionado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere permisos de administrador)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public CultivoResponse crear(@Valid @RequestBody CultivoRequest request) {
        return service.crearCultivo(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar cultivo", description = "Actualiza los datos de un cultivo existente (Requiere rol ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cultivo actualizado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CultivoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de solicitud inválidos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "No existe un cultivo o sector con el id proporcionado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere permisos de administrador)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public CultivoResponse actualizar(@Parameter(description = "ID del cultivo a actualizar", example = "10", required = true) @PathVariable Long id,
                                      @Valid @RequestBody CultivoRequest request) {
        return service.actualizarCultivo(id, request);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cambiar estado de un cultivo", description = "Actualiza el estado de crecimiento o ciclo de vida del cultivo (Requiere rol ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado del cultivo actualizado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CultivoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Estado inválido",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "No existe un cultivo con el id proporcionado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere permisos de administrador)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public CultivoResponse cambiarEstado(@Parameter(description = "ID del cultivo", example = "10", required = true) @PathVariable Long id,
                                         @Valid @RequestBody EstadoCultivoRequest request) {
        return service.cambiarEstadoCultivo(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar cultivo", description = "Elimina un cultivo por su ID (Requiere rol ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Cultivo eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "No existe un cultivo con el id proporcionado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere permisos de administrador)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public void eliminar(@Parameter(description = "ID del cultivo a eliminar", example = "10", required = true) @PathVariable Long id) {
        service.eliminarCultivo(id);
    }
}
