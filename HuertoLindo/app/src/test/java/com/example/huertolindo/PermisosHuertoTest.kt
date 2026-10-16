package com.example.huertolindo

import com.example.huertolindo.data.model.Tarea
import com.example.huertolindo.data.security.PermisosHuerto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermisosHuertoTest {
    private val tareas = listOf(
        Tarea("1", "Riego", "Regar", "sector-1", "estudiante-1", "Pendiente"),
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
    fun estudiantePuedeEnviarSoloSuTareaPendiente() {
        assertTrue(PermisosHuerto.puedeEnviarTarea("estudiante", "estudiante-1", tareas[0]))
        assertFalse(PermisosHuerto.puedeEnviarTarea("estudiante", "estudiante-2", tareas[0]))
        assertFalse(PermisosHuerto.puedeEnviarTarea("estudiante", "estudiante-1", tareas[2]))
        assertFalse(PermisosHuerto.puedeEnviarTarea("administrador", "admin-1", tareas[0]))
    }

    @Test
    fun usuarioNoAutenticadoNoPuedeConsultarTareas() {
        assertTrue(PermisosHuerto.tareasVisibles(null, null, tareas).isEmpty())
    }
}
