package com.example.huertolindo_backend.controller;

import com.example.huertolindo_backend.dto.ApiDtos.ApiError;
import com.example.huertolindo_backend.dto.ApiDtos.UsuarioRequest;
import com.example.huertolindo_backend.dto.ApiDtos.UsuarioResponse;
import com.example.huertolindo_backend.model.Rol;
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
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Usuarios", description = "Operaciones CRUD para la gestión de usuarios del sistema (Requiere rol ADMIN)")
public class UsuarioController {
    private final HuertoService service;

    public UsuarioController(HuertoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar usuarios", description = "Retorna la lista de usuarios, opcionalmente filtrada por rol")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de usuarios obtenida exitosamente",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = UsuarioResponse.class)))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere permisos de administrador)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public List<UsuarioResponse> listar(@Parameter(description = "Rol para filtrar (ADMIN o ESTUDIANTE)", example = "ESTUDIANTE") @RequestParam(required = false) Rol rol) {
        return service.usuarios(rol);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener usuario por ID", description = "Retorna la información de un usuario específico")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario encontrado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuarioResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe un usuario con el id proporcionado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere permisos de administrador)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "400", description = "El ID proporcionado tiene un formato numérico inválido",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public UsuarioResponse obtener(@Parameter(description = "ID del usuario a consultar", example = "2", required = true) @PathVariable Long id) {
        return service.usuario(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear nuevo usuario", description = "Crea un usuario (estudiante o administrador) con su contraseña encriptada")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario creado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuarioResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de usuario inválidos o correo ya registrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere permisos de administrador)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public UsuarioResponse crear(@Valid @RequestBody UsuarioRequest request) {
        return service.crearUsuario(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar usuario", description = "Actualiza los datos (nombre, correo, rol, activo) de un usuario existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario actualizado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UsuarioResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o correo duplicado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "No existe un usuario con el id proporcionado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere permisos de administrador)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public UsuarioResponse actualizar(@Parameter(description = "ID del usuario a actualizar", example = "2", required = true) @PathVariable Long id,
                                      @Valid @RequestBody UsuarioRequest request) {
        return service.actualizarUsuario(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar usuario", description = "Elimina un usuario por su ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Usuario eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "No existe un usuario con el id proporcionado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere permisos de administrador)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    public void eliminar(@Parameter(description = "ID del usuario a eliminar", example = "2", required = true) @PathVariable Long id) {
        service.eliminarUsuario(id);
    }
}
