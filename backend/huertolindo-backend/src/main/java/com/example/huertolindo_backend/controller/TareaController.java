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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tareas")
@Tag(name = "Tareas", description = "Operaciones CRUD, cambio de estado, finalización y actividades de tareas agrícolas")
public class TareaController {
    private final HuertoService service;

    public TareaController(HuertoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar tareas", description = "Retorna la lista de tareas. Los estudiantes ven sus tareas asignadas y los administradores ven todas o filtran por usuario/cultivo")
    @ApiResponse(responseCode = "200", description = "Lista de tareas obtenida exitosamente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = TareaResponse.class))))
    public List<TareaResponse> listar(@Parameter(description = "ID opcional del usuario responsable", example = "2") @RequestParam(required = false) Long usuarioId,
                                      @Parameter(description = "ID opcional del cultivo asociado", example = "10") @RequestParam(required = false) Long cultivoId,
                                      Authentication authentication) {
        return service.tareas(usuarioId, cultivoId, authentication.getName(), esAdmin(authentication));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener tarea por ID", description = "Retorna la información detallada de una tarea específica")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tarea encontrada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TareaResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe una tarea con el id proporcionado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "La tarea no está asignada a este usuario",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "400", description = "El ID proporcionado tiene un formato numérico inválido",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public TareaResponse obtener(@Parameter(description = "ID de la tarea", example = "5", required = true) @PathVariable Long id,
                                 Authentication authentication) {
        return service.tarea(id, authentication.getName(), esAdmin(authentication));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear nueva tarea", description = "Crea una nueva tarea asignándola a un estudiante y a un cultivo (Requiere rol ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tarea creada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TareaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de solicitud inválidos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "No existe un cultivo o usuario con el id proporcionado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere permisos de administrador)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public TareaResponse crear(@Valid @RequestBody TareaRequest request) {
        return service.crearTarea(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar tarea", description = "Actualiza los datos de una tarea existente (Requiere rol ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tarea actualizada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TareaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de solicitud inválidos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "No existe una tarea, cultivo o usuario con el id proporcionado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere permisos de administrador)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public TareaResponse actualizar(@Parameter(description = "ID de la tarea a actualizar", example = "5", required = true) @PathVariable Long id,
                                   @Valid @RequestBody TareaRequest request) {
        return service.actualizarTarea(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar tarea", description = "Elimina una tarea por su ID (Requiere rol ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Tarea eliminada exitosamente"),
            @ApiResponse(responseCode = "404", description = "No existe una tarea con el id proporcionado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere permisos de administrador)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public void eliminar(@Parameter(description = "ID de la tarea a eliminar", example = "5", required = true) @PathVariable Long id) {
        service.eliminarTarea(id);
    }

    @PatchMapping("/{id}/completar")
    @Operation(summary = "Completar tarea", description = "Marca la tarea como completada y pasa a revisión docente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tarea completada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TareaResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe una tarea con el id proporcionado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "La tarea no está asignada a este usuario",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public TareaResponse completar(@Parameter(description = "ID de la tarea a completar", example = "5", required = true) @PathVariable Long id,
                                   Authentication authentication) {
        return service.completarTarea(id, authentication.getName(), esAdmin(authentication));
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Actualizar estado de tarea", description = "Actualiza el estado de la tarea (ej: PENDIENTE, EN_PROCESO, POR_VALIDAR, VALIDADA, RECHAZADA)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado de tarea actualizado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TareaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Estado inválido o no proporcionado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "No existe una tarea con el id proporcionado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "La tarea no está asignada a este usuario",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public TareaResponse actualizarEstado(@Parameter(description = "ID de la tarea", example = "5", required = true) @PathVariable Long id,
                                          @RequestBody Map<String, String> body,
                                          Authentication authentication) {
        String estado = body != null ? body.get("estado") : null;
        return service.actualizarEstadoTarea(id, estado, authentication.getName(), esAdmin(authentication));
    }

    @PostMapping("/{id}/actividades")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar actividad en tarea", description = "Agrega una bitácora o nota de avance a la tarea especificada")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Actividad registrada exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ActividadResponse.class))),
            @ApiResponse(responseCode = "400", description = "Descripción inválida o vacía",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "No existe una tarea con el id proporcionado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "La tarea no está asignada a este usuario",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ActividadResponse agregarActividad(@Parameter(description = "ID de la tarea", example = "5", required = true) @PathVariable Long id,
                                              @Valid @RequestBody ActividadRequest request,
                                              Authentication authentication) {
        return service.agregarActividad(id, request, authentication.getName(), esAdmin(authentication));
    }

    private boolean esAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
