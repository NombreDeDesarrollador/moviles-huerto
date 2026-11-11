package com.example.huertolindo

import com.example.huertolindo.domain.model.Tarea
import com.example.huertolindo.domain.model.TipoTarea
import com.example.huertolindo.domain.security.PermisosHuerto
import com.example.huertolindo.domain.security.ReglasHuerto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermisosHuertoTest {
    private val tareas = listOf(
        Tarea("1", "Riego", "Regar", "sector-1", "estudiante-1", "En desarrollo"),
        Tarea("2", "Poda", "Podar", "sector-2", "estudiante-2", "Pendiente"),
        Tarea("3", "Revisión", "Revisar", "sector-1", "estudiante-1", "Por validar")
    )

    @Test
    fun administradorPuedeConsultarTodasLasTareas() {
        assertEquals(
            tareas,
            PermisosHuerto.tareasVisibles(PermisosHuerto.ADMINISTRADOR, "admin-1", tareas)
        )
    }

    @Test
    fun estudianteSoloPuedeConsultarSusTareas() {
        assertEquals(
            listOf(tareas[0], tareas[2]),
            PermisosHuerto.tareasVisibles(PermisosHuerto.ESTUDIANTE, "estudiante-1", tareas)
        )
    }

    @Test
    fun estudiantePuedeEnviarSoloSuTareaEnDesarrollo() {
        assertTrue(PermisosHuerto.puedeEnviarTarea("estudiante", "estudiante-1", tareas[0]))
        assertFalse(PermisosHuerto.puedeEnviarTarea("estudiante", "estudiante-2", tareas[0]))
        assertFalse(
            PermisosHuerto.puedeEnviarTarea(
                "estudiante",
                "estudiante-1",
                Tarea("pendiente", "Riego", "Regar", "sector-1", "estudiante-1", "Pendiente")
            )
        )
        assertFalse(PermisosHuerto.puedeEnviarTarea("administrador", "admin-1", tareas[0]))
    }

    @Test
    fun soloEstudianteResponsablePuedeAdjuntarEvidenciaDeTareaEnCursoORechazada() {
        assertTrue(
            PermisosHuerto.puedeAdjuntarEvidenciaTarea(
                PermisosHuerto.ESTUDIANTE,
                "estudiante-1",
                tareas[0]
            )
        )
        assertTrue(
            PermisosHuerto.puedeAdjuntarEvidenciaTarea(
                PermisosHuerto.ESTUDIANTE,
                "estudiante-1",
                tareas[2].copy(estado = "Rechazada")
            )
        )
        assertFalse(
            PermisosHuerto.puedeAdjuntarEvidenciaTarea(
                PermisosHuerto.ADMINISTRADOR,
                "admin-1",
                tareas[0]
            )
        )
        assertFalse(
            PermisosHuerto.puedeAdjuntarEvidenciaTarea(
                PermisosHuerto.ESTUDIANTE,
                "estudiante-2",
                tareas[0]
            )
        )
        assertFalse(
            PermisosHuerto.puedeAdjuntarEvidenciaTarea(
                PermisosHuerto.ESTUDIANTE,
                "estudiante-2",
                tareas[1]
            )
        )
    }

    @Test
    fun soloEstudianteResponsablePuedeEliminarEvidenciaDeTarea() {
        assertTrue(
            PermisosHuerto.puedeEliminarEvidenciaTarea(
                PermisosHuerto.ESTUDIANTE,
                "estudiante-1",
                tareas[0]
            )
        )
        assertFalse(
            PermisosHuerto.puedeEliminarEvidenciaTarea(
                PermisosHuerto.ADMINISTRADOR,
                "admin-1",
                tareas[0]
            )
        )
        assertFalse(
            PermisosHuerto.puedeEliminarEvidenciaTarea(
                PermisosHuerto.ESTUDIANTE,
                "estudiante-2",
                tareas[0]
            )
        )
    }

    @Test
    fun tareaRechazadaPuedeReiniciarseSoloPorSuResponsable() {
        val rechazada = Tarea(
            "rechazada", "Poda", "Podar", "sector-1", "estudiante-1", "Rechazada"
        )
        assertTrue(
            PermisosHuerto.puedeIniciarTarea(
                PermisosHuerto.ESTUDIANTE,
                "estudiante-1",
                rechazada
            )
        )
        assertFalse(
            PermisosHuerto.puedeIniciarTarea(
                PermisosHuerto.ESTUDIANTE,
                "estudiante-2",
                rechazada
            )
        )
        assertFalse(
            PermisosHuerto.puedeIniciarTarea(
                PermisosHuerto.ADMINISTRADOR,
                "admin-1",
                rechazada
            )
        )
    }

    @Test
    fun soloAdministradorPuedeResolverTareasEnRevisionYRechazoExigeMotivo() {
        assertTrue(PermisosHuerto.puedeValidarTarea(PermisosHuerto.ADMINISTRADOR, "Por validar"))
        assertFalse(PermisosHuerto.puedeValidarTarea(PermisosHuerto.ESTUDIANTE, "Por validar"))
        assertFalse(PermisosHuerto.puedeValidarTarea(PermisosHuerto.ADMINISTRADOR, "En desarrollo"))
        assertTrue(
            PermisosHuerto.puedeRechazarTarea(
                PermisosHuerto.ADMINISTRADOR,
                "Por validar",
                "Falta evidencia"
            )
        )
        assertFalse(
            PermisosHuerto.puedeRechazarTarea(
                PermisosHuerto.ADMINISTRADOR,
                "Por validar",
                "  "
            )
        )
        assertFalse(
            PermisosHuerto.puedeRechazarTarea(
                PermisosHuerto.ESTUDIANTE,
                "Por validar",
                "Falta evidencia"
            )
        )
        assertFalse(
            PermisosHuerto.puedeRechazarTarea(
                PermisosHuerto.ADMINISTRADOR,
                "Pendiente",
                "Falta evidencia"
            )
        )
    }

    @Test
    fun administradorNoPuedeValidarUnaTareaAsignadaASiMismo() {
        val tareaDelEstudiante = Tarea(
            "propia", "Riego", "Regar", "sector-1", "estudiante-1", "Por validar"
        )
        assertFalse(
            PermisosHuerto.puedeValidarTarea(
                PermisosHuerto.ESTUDIANTE,
                "estudiante-1",
                tareaDelEstudiante
            )
        )
        assertTrue(
            PermisosHuerto.puedeValidarTarea(
                PermisosHuerto.ADMINISTRADOR,
                "admin-2",
                tareaDelEstudiante
            )
        )
        assertFalse(
            PermisosHuerto.puedeValidarTarea(
                PermisosHuerto.ADMINISTRADOR,
                "estudiante-1",
                tareaDelEstudiante
            )
        )
        val asignadaAlAdministrador = tareaDelEstudiante.copy(asignadoA = "admin-1")
        assertFalse(
            PermisosHuerto.puedeValidarTarea(
                PermisosHuerto.ADMINISTRADOR,
                "admin-1",
                asignadaAlAdministrador
            )
        )
    }

    @Test
    fun fechaCosechaDebeSerValidaYNoAnteriorALaSiembra() {
        assertTrue(ReglasHuerto.fechaCosechaValida("2026-10-17", "2026-12-01"))
        assertTrue(ReglasHuerto.fechaCosechaValida("2026-10-17", "2026-10-17"))
        assertFalse(ReglasHuerto.fechaCosechaValida("2026-10-17", "2026-10-16"))
        assertFalse(ReglasHuerto.fechaCosechaValida("2026-02-30", "2026-03-10"))
    }

    @Test
    fun tiposDeTareaSeLimitaALasCategoriasDelHuerto() {
        assertTrue(TipoTarea.esValido(TipoTarea.RIEGO))
        assertTrue(TipoTarea.esValido(TipoTarea.SIEMBRA))
        assertTrue(TipoTarea.esValido(TipoTarea.MANTENIMIENTO))
        assertTrue(TipoTarea.esValido(TipoTarea.OBSERVACION))
        assertFalse(TipoTarea.esValido("Administrar presupuesto"))
    }

    @Test
    fun soloAdministradorPuedeActualizarEstadosDeProblemasPermitidos() {
        assertTrue(
            PermisosHuerto.puedeActualizarEstadoProblema(
                PermisosHuerto.ADMINISTRADOR,
                "En revisión"
            )
        )
        assertFalse(
            PermisosHuerto.puedeActualizarEstadoProblema(
                PermisosHuerto.ESTUDIANTE,
                "Atendido"
            )
        )
        assertTrue(
            PermisosHuerto.puedeActualizarEstadoProblema(
                PermisosHuerto.ADMINISTRADOR,
                "Cerrado"
            )
        )
        assertFalse(
            PermisosHuerto.puedeActualizarEstadoProblema(
                PermisosHuerto.ADMINISTRADOR,
                "Estado inválido"
            )
        )
    }

    @Test
    fun usuarioNoAutenticadoNoPuedeConsultarTareas() {
        assertTrue(PermisosHuerto.tareasVisibles(null, null, tareas).isEmpty())
    }

    @Test
    fun tareasAtrasadasExcluyeCompletadasYFechasFuturas() {
        val tareasConFechas = listOf(
            Tarea("vencida", "Riego", "Regar", "sector-1", "estudiante-1", "En desarrollo", "2026-10-15"),
            Tarea("hoy", "Poda", "Podar", "sector-1", "estudiante-1", "Pendiente", "2026-10-16"),
            Tarea("futura", "Siembra", "Sembrar", "sector-1", "estudiante-1", "Pendiente", "2026-10-17"),
            Tarea("hecha", "Limpieza", "Limpiar", "sector-1", "estudiante-1", "Completada", "2026-10-10"),
            Tarea("validada", "Riego", "Regar", "sector-1", "estudiante-1", "Validada", "2026-10-09"),
            Tarea("sin-fecha", "Observación", "Observar", "sector-1", "estudiante-1", "Pendiente"),
            Tarea("fecha-invalida", "Poda", "Podar", "sector-1", "estudiante-1", "Pendiente", "2026-99-99")
        )

        assertEquals(
            listOf(tareasConFechas.first()),
            PermisosHuerto.tareasAtrasadas(tareasConFechas, "2026-10-16")
        )
    }

    @Test
    fun tareasAtrasadasExcluyeTareasValidadas() {
        val validada = Tarea(
            "validada", "Riego", "Regar", "sector-1", "estudiante-1", "Validada", "2026-10-09"
        )

        assertTrue(PermisosHuerto.tareasAtrasadas(listOf(validada), "2026-10-16").isEmpty())
    }

    @Test
    fun tareaAtrasadaSoloEsVisibleParaSuResponsable() {
        val atrasada = Tarea(
            "vencida", "Riego", "Regar", "sector-1", "estudiante-1", "Pendiente", "2026-10-15"
        )

        assertEquals(
            listOf(atrasada),
            PermisosHuerto.tareasVisibles(
                PermisosHuerto.ESTUDIANTE,
                "estudiante-1",
                listOf(atrasada)
            ).let { PermisosHuerto.tareasAtrasadas(it, "2026-10-16") }
        )
        assertTrue(
            PermisosHuerto.tareasVisibles(
                PermisosHuerto.ESTUDIANTE,
                "estudiante-2",
                listOf(atrasada)
            ).let { PermisosHuerto.tareasAtrasadas(it, "2026-10-16") }.isEmpty()
        )
    }
}
