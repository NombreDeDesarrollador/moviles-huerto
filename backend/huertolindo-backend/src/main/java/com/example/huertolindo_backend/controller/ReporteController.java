package com.example.huertolindo_backend.controller;

import com.example.huertolindo_backend.dto.ApiDtos.ApiError;
import com.example.huertolindo_backend.dto.ApiDtos.ReporteRequest;
import com.example.huertolindo_backend.dto.ApiDtos.ReporteResponse;
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

@RestController
@RequestMapping("/api/reportes")
@Tag(name = "Reportes", description = "Operaciones CRUD y resolución de incidencias en cultivos")
public class ReporteController {
    private final HuertoService service;

    public ReporteController(HuertoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar reportes", description = "Retorna la lista completa de reportes e incidencias del huerto")
    @ApiResponse(responseCode = "200", description = "Lista de reportes obtenida exitosamente",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ReporteResponse.class))))
    public List<ReporteResponse> listar() {
        return service.reportes();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener reporte por ID", description = "Retorna el detalle de un reporte específico según su identificador")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reporte encontrado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ReporteResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe un reporte con el id proporcionado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "400", description = "El ID proporcionado tiene un formato numérico inválido",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ReporteResponse obtener(@Parameter(description = "ID del reporte", example = "4", required = true) @PathVariable Long id) {
        return service.reporte(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear reporte", description = "Registra una incidencia o problema detectado en un cultivo")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reporte registrado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ReporteResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de solicitud inválidos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "No existe un cultivo o usuario con el id proporcionado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "No puedes reportar en nombre de otro usuario",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ReporteResponse crear(@Valid @RequestBody ReporteRequest request, Authentication authentication) {
        return service.crearReporte(request, authentication.getName(), esAdmin(authentication));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar reporte", description = "Actualiza los datos (título, descripción, gravedad) de un reporte existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reporte actualizado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ReporteResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de solicitud inválidos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "No existe un reporte con el id proporcionado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "No puedes modificar este reporte (no eres el autor ni ADMIN)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ReporteResponse actualizar(@Parameter(description = "ID del reporte a actualizar", example = "4", required = true) @PathVariable Long id,
                                      @Valid @RequestBody ReporteRequest request,
                                      Authentication authentication) {
        return service.actualizarReporte(id, request, authentication.getName(), esAdmin(authentication));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar reporte", description = "Elimina un reporte por su ID (Requiere rol ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Reporte eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "No existe un reporte con el id proporcionado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere permisos de administrador)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public void eliminar(@Parameter(description = "ID del reporte a eliminar", example = "4", required = true) @PathVariable Long id) {
        service.eliminarReporte(id);
    }

    @PatchMapping("/{id}/resolver")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Resolver reporte", description = "Marca un reporte como resuelto (Requiere rol ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reporte marcado como resuelto exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ReporteResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe un reporte con el id proporcionado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere permisos de administrador)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public ReporteResponse resolver(@Parameter(description = "ID del reporte a resolver", example = "4", required = true) @PathVariable Long id) {
        return service.resolverReporte(id);
    }

    private boolean esAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
