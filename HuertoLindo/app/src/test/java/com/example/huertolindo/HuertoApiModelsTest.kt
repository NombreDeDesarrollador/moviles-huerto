package com.example.huertolindo

import com.example.huertolindo.data.remote.*
import com.example.huertolindo.domain.model.*
import com.example.huertolindo.domain.security.PermisosHuerto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class HuertoApiModelsTest {

    @Test
    fun usuarioDtoToDomainMapeaRolesCorrectamente() {
        val adminDto = UsuarioDto(
            id = "u-admin",
            nombre = "Docente",
            apellido = "Huerto",
            correo = "docente@huertolindo.cl",
            perfil = "ADMIN",
            tallerAsignado = "Taller A",
            passwordSalt = "salt1",
            passwordHash = "hash1"
        )
        val adminDomain = adminDto.toDomain()
        assertEquals("u-admin", adminDomain.id)
        assertEquals(PermisosHuerto.ADMINISTRADOR, adminDomain.perfil)
        assertEquals("Docente Huerto", adminDomain.nombreCompleto)

        val estudianteDto = UsuarioDto(
            id = "u-estudiante",
            nombre = "Estudiante",
            apellido = "Uno",
            correo = "estudiante1@huertolindo.cl",
            perfil = "ESTUDIANTE",
            tallerAsignado = null,
            passwordSalt = null,
            passwordHash = null
        )
        val estudianteDomain = estudianteDto.toDomain()
        assertEquals(PermisosHuerto.ESTUDIANTE, estudianteDomain.perfil)
        assertEquals("", estudianteDomain.tallerAsignado)
    }

    @Test
    fun usuarioToDtoConvierteRolesCorrectamente() {
        val admin = Usuario(
            id = "1",
            nombre = "Admin",
            correo = "admin@huertolindo.cl",
            perfil = PermisosHuerto.ADMINISTRADOR,
            tallerAsignado = "Taller",
            apellido = "Test"
        )
        val adminDto = admin.toDto()
        assertEquals("ADMIN", adminDto.perfil)

        val estudiante = Usuario(
            id = "2",
            nombre = "Estudiante",
            correo = "estudiante@huertolindo.cl",
            perfil = PermisosHuerto.ESTUDIANTE,
            tallerAsignado = "Taller"
        )
        val estudianteDto = estudiante.toDto()
        assertEquals("ESTUDIANTE", estudianteDto.perfil)
    }

    @Test
    fun cultivoDtoMapeaTodosLosEstadosExactosDelBackend() {
        val estadosBackend = listOf(
            "PLANIFICADO" to "Planificado",
            "SEMBRADO" to "Sembrado",
            "EN_CRECIMIENTO" to "En crecimiento",
            "LISTO_COSECHA" to "Listo para cosechar",
            "COSECHADO" to "Cosechado",
            "CON_PROBLEMAS" to "Con problemas",
            "FINALIZADO" to "Finalizado"
        )

        estadosBackend.forEach { (backend, esperadoDomain) ->
            val dto = CultivoDto(
                id = "c1",
                sectorId = "s1",
                nombre = "Tomate",
                especie = "Solanum",
                variedad = "Cherry",
                fechaSiembra = "2026-09-01",
                fechaCosechaEstimada = "2026-12-01",
                frecuenciaRiego = "2",
                estadoDesarrollo = backend,
                observaciones = null
            )
            val domain = dto.toDomain()
            assertEquals(esperadoDomain, domain.estadoDesarrollo)
            assertEquals("Tomate", domain.nombreComun)
        }
    }

    @Test
    fun cultivoToDtoMapeaEstadosAlBackendCorrectamente() {
        val estadosDomain = listOf(
            "Planificado" to "PLANIFICADO",
            "Sembrado" to "SEMBRADO",
            "En crecimiento" to "EN_CRECIMIENTO",
            "Desarrollo" to "EN_CRECIMIENTO",
            "Listo para cosechar" to "LISTO_COSECHA",
            "Cosechado" to "COSECHADO",
            "Con problemas" to "CON_PROBLEMAS",
            "Finalizado" to "FINALIZADO"
        )

        estadosDomain.forEach { (domainEstado, esperadoBackend) ->
            val cultivo = Cultivo(
                id = "c1",
                nombreComun = "Lechuga",
                sectorId = "s1",
                fechaSiembra = "2026-09-01",
                estadoDesarrollo = domainEstado,
                frecuenciaRiego = "3",
                fechaCosechaEstimada = "2026-11-01"
            )
            val dto = cultivo.toDto()
            assertEquals(esperadoBackend, dto.estadoDesarrollo)
            assertEquals(esperadoBackend, cultivo.toCultivoEstadoBackend())
        }
    }

    @Test
    fun tareaDtoMapeaTodosLosEstadosExactosDelBackend() {
        val estadosBackend = listOf(
            "PENDIENTE" to "Pendiente",
            "EN_DESARROLLO" to "En desarrollo",
            "PENDIENTE_VALIDACION" to "Por validar",
            "VALIDADA" to "Validada",
            "RECHAZADA" to "Rechazada",
            "ATRASADA" to "Atrasada"
        )

        estadosBackend.forEach { (backend, esperadoDomain) ->
            val dto = TareaDto(
                id = "t1",
                titulo = "Regar",
                descripcion = "Regar lechugas",
                sectorId = "s1",
                cultivoId = "c1",
                asignadoAId = "u1",
                fechaCreacion = "2026-10-01",
                fechaVencimiento = "2026-10-15",
                estado = backend,
                tipoTarea = "Riego",
                prioridad = "Alta"
            )
            val domain = dto.toDomain()
            assertEquals(esperadoDomain, domain.estado)
            assertEquals("u1", domain.asignadoA)
            assertEquals("2026-10-15", domain.fechaProgramada)
        }
    }

    @Test
    fun tareaToDtoMapeaEstadosAlBackendCorrectamente() {
        val estadosDomain = listOf(
            "Pendiente" to "PENDIENTE",
            "En desarrollo" to "EN_DESARROLLO",
            "Por validar" to "PENDIENTE_VALIDACION",
            "Validada" to "VALIDADA",
            "Rechazada" to "RECHAZADA",
            "Atrasada" to "ATRASADA",
            "Completada" to "VALIDADA"
        )

        estadosDomain.forEach { (domainEstado, esperadoBackend) ->
            val tarea = Tarea(
                id = "t1",
                titulo = "Desmalezar",
                descripcion = "Retirar maleza",
                sectorId = "s1",
                asignadoA = "u1",
                estado = domainEstado,
                fechaProgramada = "2026-10-10",
                tipo = "Mantenimiento"
            )
            val dto = tarea.toDto()
            assertEquals(esperadoBackend, dto.estado)
            assertEquals(esperadoBackend, tarea.toTareaEstadoBackend())
        }
    }

    @Test
    fun sectorDtoYDomainRoundTrip() {
        val dto = SectorDto(
            id = "sec-1",
            nombre = "Sector Invernadero",
            descripcion = "Zona protegida",
            tipoSuelo = "Franco arcilloso",
            superficieMetros = 45.5,
            ubicacion = "Zona Este"
        )
        val domain = dto.toDomain()
        assertEquals("sec-1", domain.id)
        assertEquals("Sector Invernadero", domain.nombre)
        assertEquals("Zona Este", domain.ubicacion)

        val deVueltaDto = domain.toDto()
        assertEquals("sec-1", deVueltaDto.id)
        assertEquals("Sector Invernadero", deVueltaDto.nombre)
    }

    @Test
    fun validacionDtoYDomainMapeo() {
        val validadaDto = ValidacionDto("v1", "t1", "a1", "VALIDADA", "Buen trabajo", "2026-10-10 12:00:00")
        val validadaDomain = validadaDto.toDomain()
        assertEquals("Validada", validadaDomain.resultado)
        assertEquals("Buen trabajo", validadaDomain.observaciones)
        assertEquals("VALIDADA", validadaDomain.toDto().estado)

        val rechazadaDto = ValidacionDto("v2", "t2", "a1", "RECHAZADA", "Falta foto", "2026-10-11 10:00:00")
        val rechazadaDomain = rechazadaDto.toDomain()
        assertEquals("Rechazada", rechazadaDomain.resultado)
        assertEquals("RECHAZADA", rechazadaDomain.toDto().estado)
    }

    @Test
    fun reporteProblemaDtoYDomainMapeo() {
        val dto = ReporteProblemaDto(
            id = "rep-1",
            sectorId = "sec-1",
            cultivoId = "cul-1",
            usuarioId = "usr-1",
            titulo = "Plaga de pulgones",
            descripcion = "Hojas marchitas",
            estado = "EN_REVISION",
            fechaReporte = "2026-10-12",
            fotografiaUri = null
        )
        val domain = dto.toDomain()
        assertEquals("rep-1", domain.id)
        assertEquals("Plaga de pulgones", domain.tipoProblema)
        assertEquals("En revisión", domain.estado)

        val deVueltaDto = domain.toDto()
        assertEquals("EN_REVISION", deVueltaDto.estado)
        assertEquals("Plaga de pulgones", deVueltaDto.titulo)
    }

    @Test
    fun evidenciaDtoYDomainMapeo() {
        val dto = EvidenciaDto(
            id = "ev-1",
            tareaId = "t-1",
            tipoAsociacion = "TAREA",
            entidadId = "t-1",
            rutaArchivo = "/storage/ev1.jpg",
            descripcion = "Foto de avance",
            fechaCreacion = "2026-10-10 15:30"
        )
        val domain = dto.toDomain()
        assertEquals("ev-1", domain.id)
        assertEquals("t-1", domain.tareaId)
        assertNull(domain.cultivoId)
        assertNull(domain.reporteId)

        val deVueltaDto = domain.toDto()
        assertEquals("TAREA", deVueltaDto.tipoAsociacion)
        assertEquals("t-1", deVueltaDto.entidadId)
    }
}
