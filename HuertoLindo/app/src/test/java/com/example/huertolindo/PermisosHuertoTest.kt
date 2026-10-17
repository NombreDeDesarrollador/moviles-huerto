package com.example.huertolindo

import com.example.huertolindo.data.model.Tarea
import com.example.huertolindo.data.security.PermisosHuerto
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
            Tarea("sin-fecha", "Observación", "Observar", "sector-1", "estudiante-1", "Pendiente")
        )

        assertEquals(
            listOf(tareasConFechas.first()),
            PermisosHuerto.tareasAtrasadas(tareasConFechas, "2026-10-16")
        )
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
