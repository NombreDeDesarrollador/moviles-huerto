package com.example.huertolindo

import com.example.huertolindo.data.remote.BackendCultivoDto
import com.example.huertolindo.data.remote.BackendSectorDto
import com.example.huertolindo.data.remote.BackendTareaDto
import com.example.huertolindo.data.remote.toBackendRequest as cultivoToBackendRequest
import com.example.huertolindo.data.remote.toBackendRequest
import com.example.huertolindo.data.remote.toDomain
import com.example.huertolindo.domain.model.Tarea
import com.example.huertolindo.domain.model.TipoTarea
import org.junit.Assert.assertEquals
import org.junit.Test

class BackendApiDtosTest {

    @Test
    fun mapeaSectorRemotoConIdNumerico() {
        val sector = BackendSectorDto(
            id = 4L,
            nombre = "Invernadero",
            descripcion = "Zona protegida",
            ubicacion = "Patio norte"
        ).toDomain()

        assertEquals("4", sector.id)
        assertEquals("Invernadero", sector.nombre)
        assertEquals("Patio norte", sector.ubicacion)
    }

    @Test
    fun mapeaIdsDelBackendYAsociacionesDeTarea() {
        val cultivo = BackendCultivoDto(
            id = 12L,
            nombre = "Lechuga",
            especie = null,
            fechaSiembra = "2026-10-01",
            estado = "SEMBRADO",
            sectorId = 4L,
            sectorNombre = "Invernadero"
        ).toDomain()

        val tarea = BackendTareaDto(
            id = 31L,
            titulo = "Regar",
            descripcion = "Riego matutino",
            tipo = "RIEGO",
            fechaProgramada = "2026-10-15",
            completada = false,
            cultivoId = 12L,
            cultivoNombre = "Lechuga",
            responsableId = 8L,
            responsableNombre = "Estudiante",
            estado = "PENDIENTE_VALIDACION"
        ).toDomain(cultivo)

        assertEquals("31", tarea.id)
        assertEquals("4", tarea.sectorId)
        assertEquals("12", tarea.cultivoId)
        assertEquals("8", tarea.asignadoA)
        assertEquals(TipoTarea.RIEGO, tarea.tipo)
        assertEquals("Por validar", tarea.estado)
    }

    @Test
    fun convierteTareaAlContratoNumericoDeLaApi() {
        val request = Tarea(
            id = "31",
            titulo = "Regar",
            descripcion = "Riego matutino",
            sectorId = "4",
            asignadoA = "8",
            estado = "Pendiente",
            fechaProgramada = "2026-10-15",
            tipo = TipoTarea.RIEGO,
            cultivoId = "12"
        ).toBackendRequest()

        assertEquals(12L, request.cultivoId)
        assertEquals(8L, request.responsableId)
        assertEquals("RIEGO", request.tipo)
        assertEquals("PENDIENTE", request.estado)
    }

    @Test(expected = IllegalArgumentException::class)
    fun noFingeCompatibilidadConCultivoPlanificadoNoAdmitidoPorLaApi() {
        com.example.huertolindo.domain.model.Cultivo(
            id = "12",
            nombreComun = "Lechuga",
            sectorId = "4",
            fechaSiembra = "2026-10-01",
            estadoDesarrollo = "Planificado",
            frecuenciaRiego = "2"
        ).cultivoToBackendRequest()
    }

    @Test(expected = IllegalArgumentException::class)
    fun rechazaCrearTareaConIdsLocalesUuid() {
        Tarea(
            id = "local-task",
            titulo = "Regar",
            descripcion = "Riego matutino",
            sectorId = "local-sector",
            asignadoA = "local-user",
            estado = "Pendiente",
            fechaProgramada = "2026-10-15",
            tipo = TipoTarea.RIEGO,
            cultivoId = "local-crop"
        ).toBackendRequest()
    }
}
