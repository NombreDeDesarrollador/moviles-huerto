package com.example.huertolindo_backend.dto;

import com.example.huertolindo_backend.model.EstadoCultivo;
import com.example.huertolindo_backend.model.Rol;
import com.example.huertolindo_backend.model.TipoTarea;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "Contenedor de DTOs del sistema HuertoLindo")
public final class ApiDtos {
    private ApiDtos() {}

    @Schema(description = "Datos requeridos para iniciar sesión")
    public record LoginRequest(
            @Schema(description = "Correo electrónico registrado", example = "profesor@huertolindo.local")
            @NotBlank(message = "El correo es obligatorio")
            @Email(message = "El formato del correo es inválido")
            String correo,

            @Schema(description = "Contraseña de acceso", example = "Password123!")
            @NotBlank(message = "La contraseña es obligatoria")
            String password
    ) {}

    @Schema(description = "Datos para el autoregistro de un nuevo estudiante")
    public record RegistroRequest(
            @Schema(description = "Nombre completo del estudiante", example = "Daniel Cifuentes")
            @NotBlank(message = "El nombre es obligatorio")
            String nombre,

            @Schema(description = "Correo electrónico institucional", example = "daniel.cifuentes@estudiante.cl")
            @NotBlank(message = "El correo es obligatorio")
            @Email(message = "El formato del correo es inválido")
            String correo,

            @Schema(description = "Contraseña de al menos 8 caracteres", example = "Estudiante2026!")
            @NotBlank(message = "La contraseña es obligatoria")
            @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
            String password
    ) {}

    @Schema(description = "Datos para crear o actualizar un usuario por parte de un administrador")
    public record UsuarioRequest(
            @Schema(description = "Nombre completo del usuario", example = "Christian Acuña")
            @NotBlank(message = "El nombre es obligatorio")
            String nombre,

            @Schema(description = "Correo electrónico", example = "christian.acunaa@profesor.cl")
            @NotBlank(message = "El correo es obligatorio")
            @Email(message = "El formato del correo es inválido")
            String correo,

            @Schema(description = "Contraseña (opcional al actualizar, obligatoria al crear)", example = "Profesor2026!")
            @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
            String password,

            @Schema(description = "Rol del usuario en el sistema", example = "ADMIN")
            @NotNull(message = "El rol es obligatorio")
            Rol rol,

            @Schema(description = "Indica si la cuenta se encuentra activa", example = "true")
            boolean activo
    ) {}

    @Schema(description = "Información pública de un usuario")
    public record UsuarioResponse(
            @Schema(description = "Identificador único del usuario", example = "1")
            Long id,

            @Schema(description = "Nombre completo", example = "Christian Acuña")
            String nombre,

            @Schema(description = "Correo electrónico", example = "christian.acunaa@profesor.cl")
            String correo,

            @Schema(description = "Rol asignado", example = "ADMIN")
            Rol rol,

            @Schema(description = "Estado activo del usuario", example = "true")
            boolean activo
    ) {}

    @Schema(description = "Respuesta de autenticación con los datos del usuario logueado")
    public record AuthResponse(
            @Schema(description = "Usuario autenticado")
            UsuarioResponse usuario
    ) {}

    @Schema(description = "Datos para crear o actualizar un sector del huerto")
    public record SectorRequest(
            @Schema(description = "Nombre representativo del sector", example = "Sector Invernadero Norte")
            @NotBlank(message = "El nombre del sector es obligatorio")
            String nombre,

            @Schema(description = "Descripción del sector y sus condiciones", example = "Invernadero con control de temperatura para hortalizas de hoja")
            String descripcion,

            @Schema(description = "Ubicación física en el predio escolar", example = "Pabellón B, costado oriente")
            String ubicacion
    ) {}

    @Schema(description = "Información detallada de un sector")
    public record SectorResponse(
            @Schema(description = "Identificador único del sector", example = "3")
            Long id,

            @Schema(description = "Nombre del sector", example = "Sector Invernadero Norte")
            String nombre,

            @Schema(description = "Descripción del sector", example = "Invernadero con control de temperatura para hortalizas de hoja")
            String descripcion,

            @Schema(description = "Ubicación física", example = "Pabellón B, costado oriente")
            String ubicacion
    ) {}

    @Schema(description = "Datos para crear o actualizar un cultivo")
    public record CultivoRequest(
            @Schema(description = "Nombre común del cultivo", example = "Tomate Cherry")
            @NotBlank(message = "El nombre del cultivo es obligatorio")
            String nombre,

            @Schema(description = "Especie biológica o variedad", example = "Solanum lycopersicum var. cerasiforme")
            String especie,

            @Schema(description = "Fecha de siembra (AAAA-MM-DD)", example = "2026-09-15")
            LocalDate fechaSiembra,

            @Schema(description = "Estado fenológico o de desarrollo del cultivo", example = "CRECIMIENTO")
            @NotNull(message = "El estado del cultivo es obligatorio")
            EstadoCultivo estado,

            @Schema(description = "Identificador del sector al que pertenece el cultivo", example = "3")
            @NotNull(message = "El ID del sector es obligatorio")
            Long sectorId
    ) {}

    @Schema(description = "Información detallada de un cultivo")
    public record CultivoResponse(
            @Schema(description = "Identificador único del cultivo", example = "10")
            Long id,

            @Schema(description = "Nombre del cultivo", example = "Tomate Cherry")
            String nombre,

            @Schema(description = "Especie o variedad", example = "Solanum lycopersicum var. cerasiforme")
            String especie,

            @Schema(description = "Fecha de siembra", example = "2026-09-15")
            LocalDate fechaSiembra,

            @Schema(description = "Estado actual del cultivo", example = "CRECIMIENTO")
            EstadoCultivo estado,

            @Schema(description = "ID del sector asociado", example = "3")
            Long sectorId,

            @Schema(description = "Nombre del sector asociado", example = "Sector Invernadero Norte")
            String sectorNombre
    ) {}

    @Schema(description = "Solicitud para actualizar el estado de desarrollo de un cultivo")
    public record EstadoCultivoRequest(
            @Schema(description = "Nuevo estado del cultivo", example = "COSECHA")
            @NotNull(message = "El nuevo estado es obligatorio")
            EstadoCultivo estado
    ) {}

    @Schema(description = "Datos para crear o actualizar una tarea agrícola")
    public record TareaRequest(
            @Schema(description = "Título o nombre de la tarea", example = "Riego por goteo matutino")
            @NotBlank(message = "El título es obligatorio")
            String titulo,

            @Schema(description = "Instrucciones detalladas de la tarea", example = "Verificar presión y activar sistema durante 20 minutos")
            String descripcion,

            @Schema(description = "Tipo o categoría de la labor agrícola", example = "RIEGO")
            @NotNull(message = "El tipo de tarea es obligatorio")
            TipoTarea tipo,

            @Schema(description = "Fecha programada de ejecución (AAAA-MM-DD)", example = "2026-11-25")
            @NotNull(message = "La fecha programada es obligatoria")
            LocalDate fechaProgramada,

            @Schema(description = "Indica si la tarea fue completada", example = "false")
            boolean completada,

            @Schema(description = "Identificador del cultivo asociado", example = "10")
            @NotNull(message = "El cultivo es obligatorio")
            Long cultivoId,

            @Schema(description = "Identificador del usuario asignado como responsable", example = "2")
            @NotNull(message = "El responsable es obligatorio")
            Long responsableId,

            @Schema(description = "Estado de flujo de la tarea", example = "PENDIENTE")
            String estado
    ) {
        public TareaRequest(@NotBlank String titulo, String descripcion, @NotNull TipoTarea tipo,
                            @NotNull LocalDate fechaProgramada, boolean completada,
                            @NotNull Long cultivoId, @NotNull Long responsableId) {
            this(titulo, descripcion, tipo, fechaProgramada, completada, cultivoId, responsableId, null);
        }
    }

    @Schema(description = "Información detallada de una tarea agrícola")
    public record TareaResponse(
            @Schema(description = "Identificador único de la tarea", example = "5")
            Long id,

            @Schema(description = "Título de la tarea", example = "Riego por goteo matutino")
            String titulo,

            @Schema(description = "Instrucciones de la tarea", example = "Verificar presión y activar sistema durante 20 minutos")
            String descripcion,

            @Schema(description = "Tipo de labor agrícola", example = "RIEGO")
            TipoTarea tipo,

            @Schema(description = "Fecha programada", example = "2026-11-25")
            LocalDate fechaProgramada,

            @Schema(description = "Indica si está completada", example = "false")
            boolean completada,

            @Schema(description = "ID del cultivo asociado", example = "10")
            Long cultivoId,

            @Schema(description = "Nombre del cultivo", example = "Tomate Cherry")
            String cultivoNombre,

            @Schema(description = "ID del responsable", example = "2")
            Long responsableId,

            @Schema(description = "Nombre del responsable", example = "Daniel Cifuentes")
            String responsableNombre,

            @Schema(description = "Estado de la tarea (PENDIENTE, EN_PROCESO, POR_VALIDAR, VALIDADA, RECHAZADA)", example = "PENDIENTE")
            String estado
    ) {
        public TareaResponse(Long id, String titulo, String descripcion, TipoTarea tipo,
                             LocalDate fechaProgramada, boolean completada, Long cultivoId,
                             String cultivoNombre, Long responsableId, String responsableNombre) {
            this(id, titulo, descripcion, tipo, fechaProgramada, completada, cultivoId, cultivoNombre, responsableId, responsableNombre, completada ? "COMPLETADA" : "PENDIENTE");
        }
    }

    @Schema(description = "Datos para registrar un avance o bitácora en una tarea")
    public record ActividadRequest(
            @Schema(description = "Descripción de la actividad realizada", example = "Se completó la revisión de boquillas y se inició el riego")
            @NotBlank(message = "La descripción de la actividad es obligatoria")
            String descripcion
    ) {}

    @Schema(description = "Registro de actividad o bitácora de tarea")
    public record ActividadResponse(
            @Schema(description = "Identificador único de la actividad", example = "1")
            Long id,

            @Schema(description = "Descripción del avance", example = "Se completó la revisión de boquillas y se inició el riego")
            String descripcion,

            @Schema(description = "Fecha y hora del registro", example = "2026-11-23T14:30:00")
            LocalDateTime fechaRegistro,

            @Schema(description = "ID de la tarea relacionada", example = "5")
            Long tareaId,

            @Schema(description = "ID del usuario que registró la actividad", example = "2")
            Long usuarioId,

            @Schema(description = "Nombre del usuario", example = "Daniel Cifuentes")
            String usuarioNombre
    ) {}

    @Schema(description = "Detalle de evidencia fotográfica adjunta a una tarea")
    public record EvidenciaResponse(
            @Schema(description = "Identificador de la evidencia", example = "1")
            Long id,

            @Schema(description = "Ruta de acceso a la imagen almacenada", example = "/uploads/evidencia_1.jpg")
            String rutaImagen,

            @Schema(description = "Comentario de entrega", example = "Fotografía de la tarea completada en terreno")
            String comentario,

            @Schema(description = "Fecha y hora de subida", example = "2026-11-23T15:00:00")
            LocalDateTime fechaSubida,

            @Schema(description = "ID de la tarea vinculada", example = "5")
            Long tareaId,

            @Schema(description = "ID del estudiante que subió la evidencia", example = "2")
            Long usuarioId,

            @Schema(description = "Nombre del estudiante", example = "Daniel Cifuentes")
            String usuarioNombre,

            @Schema(description = "Ruta del archivo (compatibilidad)", example = "/uploads/evidencia_1.jpg")
            String rutaArchivo,

            @Schema(description = "Descripción (compatibilidad)", example = "Fotografía de la tarea completada en terreno")
            String descripcion,

            @Schema(description = "Fecha de creación formateada", example = "2026-11-23 15:00")
            String fechaCreacion,

            @Schema(description = "Tipo de entidad asociada", example = "TAREA")
            String tipoAsociacion,

            @Schema(description = "ID de la entidad asociada", example = "5")
            String entidadId
    ) {
        public EvidenciaResponse(Long id, String rutaImagen, String comentario, LocalDateTime fechaSubida,
                                 Long tareaId, Long usuarioId, String usuarioNombre) {
            this(id, rutaImagen, comentario, fechaSubida, tareaId, usuarioId, usuarioNombre,
                 rutaImagen, comentario, fechaSubida != null ? fechaSubida.toString() : "",
                 "TAREA", tareaId != null ? tareaId.toString() : null);
        }
    }

    @Schema(description = "Datos para emitir un reporte de incidencia o plaga")
    public record ReporteRequest(
            @Schema(description = "Título descriptivo del problema", example = "Presencia de pulgones en hojas basales")
            @NotBlank(message = "El título es obligatorio")
            String titulo,

            @Schema(description = "Descripción detallada del síntoma o problema detectado", example = "Se aprecian colonias de pulgón negro en el envés de las hojas inferiores")
            @NotBlank(message = "La descripción es obligatoria")
            String descripcion,

            @Schema(description = "Nivel de gravedad (LEVE, MODERADA, ALTA)", example = "MODERADA")
            @NotBlank(message = "La gravedad es obligatoria")
            String gravedad,

            @Schema(description = "Identificador del cultivo afectado", example = "10")
            @NotNull(message = "El ID del cultivo es obligatorio")
            Long cultivoId,

            @Schema(description = "Identificador del usuario informante", example = "2")
            @NotNull(message = "El ID del usuario es obligatorio")
            Long usuarioId
    ) {}

    @Schema(description = "Información de un reporte de incidencia")
    public record ReporteResponse(
            @Schema(description = "Identificador del reporte", example = "4")
            Long id,

            @Schema(description = "Título del reporte", example = "Presencia de pulgones en hojas basales")
            String titulo,

            @Schema(description = "Descripción del problema", example = "Se aprecian colonias de pulgón negro en el envés de las hojas inferiores")
            String descripcion,

            @Schema(description = "Nivel de gravedad", example = "MODERADA")
            String gravedad,

            @Schema(description = "Indica si la incidencia fue resuelta", example = "false")
            boolean resuelto,

            @Schema(description = "Fecha y hora del reporte", example = "2026-11-23T11:20:00")
            LocalDateTime fechaReporte,

            @Schema(description = "ID del cultivo afectado", example = "10")
            Long cultivoId,

            @Schema(description = "Nombre del cultivo", example = "Tomate Cherry")
            String cultivoNombre,

            @Schema(description = "ID del usuario informante", example = "2")
            Long usuarioId,

            @Schema(description = "Nombre del usuario informante", example = "Daniel Cifuentes")
            String usuarioNombre
    ) {}

    @Schema(description = "Datos para la validación o rechazo docente de una tarea o evidencia")
    public record ValidacionRequest(
            @Schema(description = "Indica si la entrega fue aprobada (true) o rechazada (false)", example = "true")
            boolean aprobado,

            @Schema(description = "Observaciones o retroalimentación pedagógica para el estudiante", example = "Excelente aplicación del abono orgánico, labor impecable.")
            String observacion,

            @Schema(description = "ID de la tarea a validar (opcional si se valida por evidencia)", example = "5")
            Long tareaId,

            @Schema(description = "ID de la evidencia a validar (opcional si se valida por tarea)", example = "1")
            Long evidenciaId
    ) {}

    @Schema(description = "Detalle del registro de validación docente")
    public record ValidacionResponse(
            @Schema(description = "Identificador de la validación", example = "2")
            Long id,

            @Schema(description = "Indica si fue aprobada", example = "true")
            boolean aprobado,

            @Schema(description = "Observación o comentario del profesor", example = "Excelente aplicación del abono orgánico, labor impecable.")
            String observacion,

            @Schema(description = "Fecha de la validación", example = "2026-11-23T16:45:00")
            LocalDateTime fechaValidacion,

            @Schema(description = "ID del profesor validador", example = "1")
            Long validadorId,

            @Schema(description = "Nombre del profesor", example = "Christian Acuña")
            String validadorNombre,

            @Schema(description = "ID de la tarea validada", example = "5")
            Long tareaId,

            @Schema(description = "ID de la evidencia validada", example = "1")
            Long evidenciaId
    ) {}

    @Schema(description = "Estructura estándar de respuesta de error del API")
    public record ApiError(
            @Schema(description = "Marca temporal del suceso", example = "2026-11-23T10:15:30")
            LocalDateTime timestamp,

            @Schema(description = "Código de estado HTTP", example = "404")
            int status,

            @Schema(description = "Motivo del estado HTTP", example = "Not Found")
            String error,

            @Schema(description = "Mensaje descriptivo del error", example = "No existe un sector con el id 3")
            String message,

            @Schema(description = "Mapa de campos con errores de validación (si aplica)", example = "{\"nombre\": \"El nombre del sector es obligatorio\"}")
            Map<String, String> details
    ) {}
}
