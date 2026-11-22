package com.example.huertolindo.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface HuertoApiService {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequestDto): Response<BackendAuthResponseDto>

    @POST("auth/registro")
    suspend fun registrarEnBackend(@Body request: BackendRegistroRequestDto): Response<BackendUsuarioDto>

    @POST("usuarios")
    suspend fun registrar(@Body usuario: BackendUsuarioRequestDto): Response<BackendUsuarioDto>

    @GET("usuarios")
    suspend fun obtenerUsuariosBackend(): Response<List<BackendUsuarioDto>>

    @GET("usuarios")
    suspend fun obtenerUsuarios(): Response<List<UsuarioDto>>

    @GET("usuarios/{id}")
    suspend fun obtenerUsuarioPorId(@Path("id") id: String): Response<UsuarioDto>

    @PUT("usuarios/{id}")
    suspend fun actualizarUsuario(@Path("id") id: String, @Body usuario: UsuarioDto): Response<UsuarioDto>

    @DELETE("usuarios/{id}")
    suspend fun eliminarUsuario(@Path("id") id: String): Response<Void>

    // Sectores
    @GET("sectores")
    suspend fun obtenerSectores(): Response<List<SectorDto>>

    @GET("sectores")
    suspend fun obtenerSectoresBackend(): Response<List<BackendSectorDto>>

    @GET("sectores/{id}")
    suspend fun obtenerSectorPorId(@Path("id") id: String): Response<SectorDto>

    @POST("sectores")
    suspend fun guardarSector(@Body sector: SectorDto): Response<SectorDto>

    @POST("sectores")
    suspend fun guardarSectorBackend(@Body sector: BackendSectorRequestDto): Response<BackendSectorDto>

    @PUT("sectores/{id}")
    suspend fun actualizarSector(@Path("id") id: String, @Body sector: SectorDto): Response<SectorDto>

    @PUT("sectores/{id}")
    suspend fun actualizarSectorBackend(
        @Path("id") id: String,
        @Body sector: BackendSectorRequestDto
    ): Response<BackendSectorDto>

    @DELETE("sectores/{id}")
    suspend fun eliminarSector(@Path("id") id: String): Response<Void>

    // Cultivos
    @GET("cultivos")
    suspend fun obtenerCultivos(): Response<List<CultivoDto>>

    @GET("cultivos")
    suspend fun obtenerCultivosBackend(): Response<List<BackendCultivoDto>>

    @GET("cultivos/{id}")
    suspend fun obtenerCultivoPorId(@Path("id") id: String): Response<CultivoDto>

    @POST("cultivos")
    suspend fun guardarCultivo(@Body cultivo: CultivoDto): Response<CultivoDto>

    @POST("cultivos")
    suspend fun guardarCultivoBackend(
        @Body cultivo: BackendCultivoRequestDto
    ): Response<BackendCultivoDto>

    @PUT("cultivos/{id}")
    suspend fun actualizarCultivo(@Path("id") id: String, @Body cultivo: CultivoDto): Response<CultivoDto>

    @PUT("cultivos/{id}")
    suspend fun actualizarCultivoBackend(
        @Path("id") id: String,
        @Body cultivo: BackendCultivoRequestDto
    ): Response<BackendCultivoDto>

    @DELETE("cultivos/{id}")
    suspend fun eliminarCultivo(@Path("id") id: String): Response<Void>

    @PATCH("cultivos/{id}/estado")
    suspend fun actualizarEstadoCultivo(@Path("id") id: String, @Body estadoMap: Map<String, String>): Response<CultivoDto>

    // Tareas
    @GET("tareas")
    suspend fun obtenerTareas(): Response<List<TareaDto>>

    @GET("tareas")
    suspend fun obtenerTareasBackend(): Response<List<BackendTareaDto>>

    @GET("tareas/{id}")
    suspend fun obtenerTareaPorId(@Path("id") id: String): Response<TareaDto>

    @POST("tareas")
    suspend fun guardarTarea(@Body tarea: TareaDto): Response<TareaDto>

    @POST("tareas")
    suspend fun guardarTareaBackend(
        @Body tarea: BackendTareaRequestDto
    ): Response<BackendTareaDto>

    @POST("tareas/{id}/actividades")
    suspend fun agregarActividadTarea(
        @Path("id") id: String,
        @Body actividad: BackendActividadRequestDto
    ): Response<BackendActividadDto>

    @GET("tareas/{id}/actividades")
    suspend fun obtenerActividadesTarea(
        @Path("id") id: String
    ): Response<List<BackendActividadDto>>

    @PUT("tareas/{id}")
    suspend fun actualizarTarea(@Path("id") id: String, @Body tarea: TareaDto): Response<TareaDto>

    @PUT("tareas/{id}")
    suspend fun actualizarTareaBackend(
        @Path("id") id: String,
        @Body tarea: BackendTareaRequestDto
    ): Response<BackendTareaDto>

    @DELETE("tareas/{id}")
    suspend fun eliminarTarea(@Path("id") id: String): Response<Void>

    @PATCH("tareas/{id}/estado")
    suspend fun actualizarEstadoTarea(
        @Path("id") id: String,
        @Body estadoMap: Map<String, String>
    ): Response<BackendTareaDto>

    // Evidencias
    @GET("evidencias")
    suspend fun obtenerEvidencias(): Response<List<EvidenciaDto>>

    @GET("evidencias")
    suspend fun obtenerEvidenciasBackend(
        @Query("tareaId") tareaId: Long? = null,
        @Query("cultivoId") cultivoId: Long? = null,
        @Query("reporteId") reporteId: Long? = null
    ): Response<List<BackendEvidenciaDto>>

    @Multipart
    @POST("evidencias/upload")
    suspend fun subirEvidencia(
        @Part file: MultipartBody.Part,
        @Part("tareaId") tareaId: RequestBody?,
        @Part("cultivoId") cultivoId: RequestBody?,
        @Part("reporteId") reporteId: RequestBody?,
        @Part("comentario") comentario: RequestBody?,
    ): Response<BackendEvidenciaDto>

    @DELETE("evidencias/{id}")
    suspend fun eliminarEvidencia(@Path("id") id: String): Response<Void>

    // Validaciones
    @GET("validaciones")
    suspend fun obtenerValidaciones(): Response<List<ValidacionDto>>

    @GET("validaciones")
    suspend fun obtenerValidacionesBackend(
        @Query("tareaId") tareaId: Long? = null
    ): Response<List<BackendValidacionDto>>

    @GET("validaciones/{id}")
    suspend fun obtenerValidacionPorId(@Path("id") id: String): Response<ValidacionDto>

    @POST("validaciones")
    suspend fun guardarValidacion(@Body validacion: ValidacionDto): Response<ValidacionDto>

    @POST("validaciones")
    suspend fun guardarValidacionBackend(
        @Body validacion: BackendValidacionRequestDto
    ): Response<BackendValidacionDto>

    @PUT("validaciones/{id}")
    suspend fun actualizarValidacion(@Path("id") id: String, @Body validacion: ValidacionDto): Response<ValidacionDto>

    @DELETE("validaciones/{id}")
    suspend fun eliminarValidacion(@Path("id") id: String): Response<Void>

    // Reportes
    @GET("reportes")
    suspend fun obtenerReportes(): Response<List<ReporteProblemaDto>>

    @GET("reportes/{id}")
    suspend fun obtenerReportePorId(@Path("id") id: String): Response<ReporteProblemaDto>

    @POST("reportes")
    suspend fun guardarReporte(@Body reporte: ReporteProblemaDto): Response<ReporteProblemaDto>

    @POST("reportes")
    suspend fun guardarReporteBackend(
        @Body reporte: BackendReporteRequestDto
    ): Response<BackendReporteDto>

    @GET("reportes")
    suspend fun obtenerReportesBackend(): Response<List<BackendReporteDto>>

    @PUT("reportes/{id}")
    suspend fun actualizarReporteBackend(
        @Path("id") id: String,
        @Body reporte: BackendReporteRequestDto
    ): Response<BackendReporteDto>

    @PUT("reportes/{id}")
    suspend fun actualizarReporte(@Path("id") id: String, @Body reporte: ReporteProblemaDto): Response<ReporteProblemaDto>

    @DELETE("reportes/{id}")
    suspend fun eliminarReporte(@Path("id") id: String): Response<Void>
}
