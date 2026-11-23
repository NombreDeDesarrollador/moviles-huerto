package com.example.huertolindo_backend.dto;

import com.example.huertolindo_backend.model.EstadoCultivo;
import com.example.huertolindo_backend.model.Rol;
import com.example.huertolindo_backend.model.TipoTarea;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class ApiDtos {
    private ApiDtos() {}

    public record LoginRequest(@NotBlank @Email String correo, @NotBlank String password) {}
    public record RegistroRequest(@NotBlank String nombre, @NotBlank @Email String correo,
                                  @NotBlank @Size(min = 8) String password) {}
    public record UsuarioRequest(@NotBlank String nombre, @NotBlank @Email String correo,
                                 @Size(min = 8) String password, @NotNull Rol rol, boolean activo) {}
    public record UsuarioResponse(Long id, String nombre, String correo, Rol rol, boolean activo) {}
    public record AuthResponse(UsuarioResponse usuario) {}

    public record SectorRequest(@NotBlank String nombre, String descripcion, String ubicacion) {}
    public record SectorResponse(Long id, String nombre, String descripcion, String ubicacion) {}

    public record CultivoRequest(@NotBlank String nombre, String especie, LocalDate fechaSiembra,
                                 @NotNull EstadoCultivo estado, @NotNull Long sectorId) {}
    public record CultivoResponse(Long id, String nombre, String especie, LocalDate fechaSiembra,
                                  EstadoCultivo estado, Long sectorId, String sectorNombre) {}
    public record EstadoCultivoRequest(@NotNull EstadoCultivo estado) {}

    public record TareaRequest(@NotBlank String titulo, String descripcion, @NotNull TipoTarea tipo,
                               @NotNull LocalDate fechaProgramada, boolean completada,
                               @NotNull Long cultivoId, @NotNull Long responsableId) {}
    public record TareaResponse(Long id, String titulo, String descripcion, TipoTarea tipo,
                                LocalDate fechaProgramada, boolean completada, Long cultivoId,
                                String cultivoNombre, Long responsableId, String responsableNombre) {}
    public record ActividadRequest(@NotBlank String descripcion) {}
    public record ActividadResponse(Long id, String descripcion, LocalDateTime fechaRegistro,
                                    Long tareaId, Long usuarioId, String usuarioNombre) {}

    public record EvidenciaResponse(Long id, String rutaImagen, String comentario, LocalDateTime fechaSubida,
                                    Long tareaId, Long usuarioId, String usuarioNombre) {}

    public record ReporteRequest(@NotBlank String titulo, @NotBlank String descripcion,
                                 @NotBlank String gravedad, @NotNull Long cultivoId,
                                 @NotNull Long usuarioId) {}
    public record ReporteResponse(Long id, String titulo, String descripcion, String gravedad,
                                  boolean resuelto, LocalDateTime fechaReporte, Long cultivoId,
                                  String cultivoNombre, Long usuarioId, String usuarioNombre) {}

    public record ValidacionRequest(boolean aprobado, String observacion, Long tareaId, Long evidenciaId) {}
    public record ValidacionResponse(Long id, boolean aprobado, String observacion, LocalDateTime fechaValidacion,
                                     Long validadorId, String validadorNombre, Long tareaId, Long evidenciaId) {}
    public record ApiError(LocalDateTime timestamp, int status, String error, String message,
                           java.util.Map<String, String> details) {}
}
