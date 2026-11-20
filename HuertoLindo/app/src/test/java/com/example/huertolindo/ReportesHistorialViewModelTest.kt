package com.example.huertolindo

import android.app.Application
import android.net.Uri
import com.example.huertolindo.domain.model.*
import com.example.huertolindo.domain.repository.HuertoRepository
import com.example.huertolindo.domain.repository.HuertoSnapshot
import com.example.huertolindo.domain.security.PermisosHuerto
import com.example.huertolindo.domain.usecase.GestionCultivosUseCase
import com.example.huertolindo.domain.usecase.GestionReportesUseCase
import com.example.huertolindo.domain.usecase.GestionTareasUseCase
import com.example.huertolindo.presentation.viewmodel.ReportesHistorialViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReportesHistorialViewModelTest {

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

    private val demoReporte = ReporteProblema(
        id = "rep-1",
        sectorId = "sec-1",
        cultivoId = null,
        usuarioId = "estudiante-1",
        descripcion = "Gotera en tubería",
        prioridad = "Media",
        estado = "Informado",
        fechaReporte = "2026-10-14",
        tipoProblema = "Falta de agua"
    )

    private val demoActividad = ActividadTarea(
        id = "act-1",
        tareaId = "tar-1",
        tituloTarea = "Regar tomates",
        usuarioRelacionadoId = "estudiante-1",
        usuarioId = "admin-1",
        nombreUsuario = "Docente Guía",
        accion = "Creó tarea",
        estado = "Pendiente",
        fecha = "2026-10-14 10:00:00"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { repository.inicializar() } returns Unit
        coEvery { repository.cargarSnapshot() } returns HuertoSnapshot(
            sectores = listOf(demoSector),
            cultivos = emptyList(),
            tareas = emptyList(),
            evidencias = emptyList(),
            validaciones = emptyList(),
            reportesProblemas = listOf(demoReporte),
            actividades = listOf(demoActividad)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.crearViewModel(): ReportesHistorialViewModel {
        val vm = ReportesHistorialViewModel(
            application = application,
            huertoRepository = repository,
            ioDispatcher = testDispatcher,
            reportesUseCase = GestionReportesUseCase(repository),
            cultivosUseCase = GestionCultivosUseCase(repository),
            tareasUseCase = GestionTareasUseCase(repository)
        )
        advanceUntilIdle()
        return vm
    }

    @Test
    fun inicializacionCargaReportesEHistorial() = runTest {
        val viewModel = crearViewModel()

        assertEquals(1, viewModel.reportesProblemas.size)
        assertEquals(1, viewModel.historial.size)
        assertEquals("Gotera en tubería", viewModel.reportesProblemas.first().descripcion)
    }

    @Test
    fun crearReporteProblemaExitoso() = runTest {
        coEvery { repository.insertarReporte(any(), any(), any(), any(), any()) } returns Unit

        val viewModel = crearViewModel()
        var error: String? = "inicial"

        viewModel.crearReporteProblema(
            descripcion = "Manguera rota",
            sector = demoSector,
            cultivo = null,
            tipoProblema = "Falta de agua",
            usuario = demoEstudiante,
            fotografia = mockk<Uri>(relaxed = true)
        ) { error = it }
        advanceUntilIdle()

        assertNull(error)
        assertEquals(2, viewModel.reportesProblemas.size)
        coVerify { repository.insertarReporte(match { it.descripcion == "Manguera rota" }, any(), any(), any(), any()) }
    }

    @Test
    fun actualizarEstadoProblemaExitoso() = runTest {
        coEvery { repository.actualizarReporte(any()) } returns Unit

        val viewModel = crearViewModel()
        var error: String? = "inicial"

        viewModel.actualizarEstadoProblema(
            reporte = demoReporte,
            nuevoEstado = "Atendido",
            comentario = "Se reparó la tubería",
            administrador = demoAdmin
        ) { error = it }
        advanceUntilIdle()

        assertNull(error)
        val actualizado = viewModel.reportesProblemas.first { it.id == demoReporte.id }
        assertEquals("Atendido", actualizado.estado)
        assertTrue(actualizado.comentarioSeguimiento.contains("Se reparó la tubería"))
    }
}
