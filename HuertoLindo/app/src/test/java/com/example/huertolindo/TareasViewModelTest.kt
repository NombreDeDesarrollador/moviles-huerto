package com.example.huertolindo

import android.app.Application
import android.net.Uri
import com.example.huertolindo.domain.model.*
import com.example.huertolindo.domain.repository.HuertoRepository
import com.example.huertolindo.domain.repository.HuertoSnapshot
import com.example.huertolindo.domain.security.PermisosHuerto
import com.example.huertolindo.domain.usecase.GestionTareasUseCase
import com.example.huertolindo.presentation.viewmodel.TareasViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TareasViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val application = mockk<Application>(relaxed = true)
    private val repository = mockk<HuertoRepository>(relaxed = true)

    private val demoAdmin = Usuario(
        id = "admin-1",
        nombre = "Docente",
        apellido = "Guía",
        correo = "docente@huertolindo.cl",
        perfil = PermisosHuerto.ADMINISTRADOR,
        tallerAsignado = "General"
    )

    private val demoEstudiante = Usuario(
        id = "estudiante-1",
        nombre = "Estudiante",
        apellido = "Uno",
        correo = "estudiante1@huertolindo.cl",
        perfil = PermisosHuerto.ESTUDIANTE,
        tallerAsignado = "Taller A"
    )

    private val demoSector = Sector(
        id = "sec-1",
        nombre = "Sector Hortalizas",
        descripcion = "Zona de hortalizas",
        estado = "En uso",
        ubicacion = "Norte"
    )

    private val demoTarea = Tarea(
        id = "tar-1",
        titulo = "Regar tomates",
        descripcion = "Riego matutino",
        sectorId = "sec-1",
        asignadoA = "estudiante-1",
        estado = "Pendiente",
        fechaProgramada = "2026-10-15",
        tipo = TipoTarea.RIEGO
    )

    private val demoTareaPorValidar = Tarea(
        id = "tar-validar",
        titulo = "Revisar sustrato",
        descripcion = "Verificar acidez",
        sectorId = "sec-1",
        asignadoA = "estudiante-1",
        estado = "Por validar",
        fechaProgramada = "2026-10-16",
        tipo = TipoTarea.OBSERVACION
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { repository.inicializar() } returns Unit
        coEvery { repository.cargarSnapshot() } returns HuertoSnapshot(
            sectores = listOf(demoSector),
            cultivos = emptyList(),
            tareas = listOf(demoTarea, demoTareaPorValidar),
            evidencias = emptyList(),
            validaciones = emptyList(),
            reportesProblemas = emptyList(),
            actividades = emptyList()
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.crearViewModel(): TareasViewModel {
        val vm = TareasViewModel(
            application = application,
            huertoRepository = repository,
            ioDispatcher = testDispatcher,
            tareasUseCase = GestionTareasUseCase(repository)
        )
        advanceUntilIdle()
        return vm
    }

    @Test
    fun inicializacionCargaTareasCorrectamente() = runTest {
        val viewModel = crearViewModel()

        assertEquals(2, viewModel.tareas.size)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun iniciarTareaCambiaEstadoAEnDesarrollo() = runTest {
        val viewModel = crearViewModel()
        var error: String? = null

        viewModel.iniciarTarea(demoTarea.id, demoEstudiante) { error = it }
        advanceUntilIdle()

        assertNull(error)
        val tareaModificada = viewModel.tareas.first { it.id == demoTarea.id }
        assertEquals("En desarrollo", tareaModificada.estado)
    }

    @Test
    fun entregarTareaConFotoYComentarioCambiaAPorValidar() = runTest {
        val tareaEnDesarrollo = demoTarea.copy(estado = "En desarrollo")
        coEvery { repository.cargarSnapshot() } returns HuertoSnapshot(
            sectores = listOf(demoSector),
            cultivos = emptyList(),
            tareas = listOf(tareaEnDesarrollo),
            evidencias = emptyList(),
            validaciones = emptyList(),
            reportesProblemas = emptyList(),
            actividades = emptyList()
        )
        val mockEvidencia = Evidencia(
            id = "ev-1",
            tareaId = tareaEnDesarrollo.id,
            tipo = "FOTOGRAFIA",
            rutaArchivo = "/tmp/foto.jpg",
            descripcion = "Foto de avance",
            fechaRegistro = "2026-10-15"
        )
        coEvery { repository.guardarEvidenciaLocal(any(), any(), any()) } returns mockEvidencia
        coEvery { repository.entregarTarea(any(), any(), any(), any()) } returns Unit

        val viewModel = crearViewModel()
        var errorResultado: String? = "inicial"

        viewModel.entregarTarea(
            tareaId = tareaEnDesarrollo.id,
            comentario = "Se regó según indicaciones",
            imagenUri = mockk<Uri>(relaxed = true),
            estudiante = demoEstudiante
        ) { errorResultado = it }
        advanceUntilIdle()

        assertNull(errorResultado)
        val tareaEntregada = viewModel.tareas.first { it.id == tareaEnDesarrollo.id }
        assertEquals("Por validar", tareaEntregada.estado)
        assertEquals(1, viewModel.evidencias.size)
    }

    @Test
    fun validarTareaCambiaEstadoAValidada() = runTest {
        coEvery { repository.validarTarea(any(), any(), any(), any()) } returns Unit

        val viewModel = crearViewModel()
        var error: String? = "inicial"

        viewModel.validarTarea(demoTareaPorValidar.id, demoAdmin) { error = it }
        advanceUntilIdle()

        assertNull(error)
        val tareaValidada = viewModel.tareas.first { it.id == demoTareaPorValidar.id }
        assertEquals("Validada", tareaValidada.estado)
        assertEquals(1, viewModel.validaciones.size)
    }

    @Test
    fun rechazarTareaConMotivoActualizaEstadoYGuardaFeedback() = runTest {
        coEvery { repository.validarTarea(any(), any(), any(), any()) } returns Unit

        val viewModel = crearViewModel()
        var error: String? = "inicial"

        viewModel.rechazarTarea(
            tareaId = demoTareaPorValidar.id,
            motivo = "Falta enfocar mejor el sustrato",
            administrador = demoAdmin
        ) { error = it }
        advanceUntilIdle()

        assertNull(error)
        val tareaRechazada = viewModel.tareas.first { it.id == demoTareaPorValidar.id }
        assertEquals("Rechazada", tareaRechazada.estado)
        assertEquals("Falta enfocar mejor el sustrato", tareaRechazada.motivoRechazo)
    }
}
