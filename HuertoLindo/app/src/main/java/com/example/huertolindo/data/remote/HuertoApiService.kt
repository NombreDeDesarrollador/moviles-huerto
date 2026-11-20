package com.example.huertolindo.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface HuertoApiService {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequestDto): Response<UsuarioDto>

    @POST("auth/registro")
    suspend fun registrar(@Body usuario: UsuarioDto): Response<UsuarioDto>

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

    @GET("sectores/{id}")
    suspend fun obtenerSectorPorId(@Path("id") id: String): Response<SectorDto>

    @POST("sectores")
    suspend fun guardarSector(@Body sector: SectorDto): Response<SectorDto>

    @PUT("sectores/{id}")
    suspend fun actualizarSector(@Path("id") id: String, @Body sector: SectorDto): Response<SectorDto>

    @DELETE("sectores/{id}")
    suspend fun eliminarSector(@Path("id") id: String): Response<Void>

    // Cultivos
    @GET("cultivos")
    suspend fun obtenerCultivos(): Response<List<CultivoDto>>

    @GET("cultivos/{id}")
    suspend fun obtenerCultivoPorId(@Path("id") id: String): Response<CultivoDto>

    @POST("cultivos")
    suspend fun guardarCultivo(@Body cultivo: CultivoDto): Response<CultivoDto>

    @PUT("cultivos/{id}")
    suspend fun actualizarCultivo(@Path("id") id: String, @Body cultivo: CultivoDto): Response<CultivoDto>

    @DELETE("cultivos/{id}")
    suspend fun eliminarCultivo(@Path("id") id: String): Response<Void>

    @PATCH("cultivos/{id}/estado")
    suspend fun actualizarEstadoCultivo(@Path("id") id: String, @Body estadoMap: Map<String, String>): Response<CultivoDto>

    // Tareas
    @GET("tareas")
    suspend fun obtenerTareas(): Response<List<TareaDto>>

    @GET("tareas/{id}")
    suspend fun obtenerTareaPorId(@Path("id") id: String): Response<TareaDto>

    @POST("tareas")
    suspend fun guardarTarea(@Body tarea: TareaDto): Response<TareaDto>

    @PUT("tareas/{id}")
    suspend fun actualizarTarea(@Path("id") id: String, @Body tarea: TareaDto): Response<TareaDto>

    @DELETE("tareas/{id}")
    suspend fun eliminarTarea(@Path("id") id: String): Response<Void>

    @PATCH("tareas/{id}/estado")
    suspend fun actualizarEstadoTarea(@Path("id") id: String, @Body estadoMap: Map<String, String>): Response<TareaDto>

    // Evidencias
    @GET("evidencias")
    suspend fun obtenerEvidencias(): Response<List<EvidenciaDto>>

    @Multipart
    @POST("evidencias/upload")
    suspend fun subirEvidencia(
        @Part file: MultipartBody.Part,
        @Part("tareaId") tareaId: RequestBody?,
        @Part("comentario") comentario: RequestBody?,
        @Part("tipoAsociacion") tipoAsociacion: RequestBody?,
        @Part("entidadId") entidadId: RequestBody?
    ): Response<EvidenciaDto>

    // Validaciones
    @GET("validaciones")
    suspend fun obtenerValidaciones(): Response<List<ValidacionDto>>

    @GET("validaciones/{id}")
    suspend fun obtenerValidacionPorId(@Path("id") id: String): Response<ValidacionDto>

    @POST("validaciones")
    suspend fun guardarValidacion(@Body validacion: ValidacionDto): Response<ValidacionDto>

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

    @PUT("reportes/{id}")
    suspend fun actualizarReporte(@Path("id") id: String, @Body reporte: ReporteProblemaDto): Response<ReporteProblemaDto>

    @DELETE("reportes/{id}")
    suspend fun eliminarReporte(@Path("id") id: String): Response<Void>
}
