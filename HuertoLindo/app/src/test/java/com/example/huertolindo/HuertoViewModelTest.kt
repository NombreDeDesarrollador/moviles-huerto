package com.example.huertolindo

import android.app.Application
import com.example.huertolindo.domain.model.*
import com.example.huertolindo.data.local.SessionManager
import com.example.huertolindo.domain.repository.HuertoRepository
import com.example.huertolindo.domain.repository.HuertoSnapshot
import com.example.huertolindo.domain.security.PermisosHuerto
import com.example.huertolindo.presentation.viewmodel.HuertoViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HuertoViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val application = mockk<Application>(relaxed = true)
    private val repository = mockk<HuertoRepository>(relaxed = true)
    private val sessionManager = mockk<SessionManager>(relaxed = true)

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

    private val demoCultivo = Cultivo(
        id = "cul-1",
        nombreComun = "Lechuga",
        sectorId = "sec-1",
        fechaSiembra = "2026-10-01",
        estadoDesarrollo = "Sembrado",
        frecuenciaRiego = "2",
        fechaCosechaEstimada = "2026-12-01"
    )

    private val demoTarea = Tarea(
        id = "tar-1",
        titulo = "Regar lechugas",
        descripcion = "Riego matutino",
        sectorId = "sec-1",
        asignadoA = "estudiante-1",
        estado = "Pendiente",
        fechaProgramada = "2026-10-15",
        tipo = TipoTarea.RIEGO,
        cultivoId = "cul-1"
    )

    private val demoTareaPorValidar = Tarea(
        id = "tar-validar",
        titulo = "Revisar sustrato",
        descripcion = "Verificar acidez",
        sectorId = "sec-1",
        asignadoA = "estudiante-1",
        estado = "Por validar",
        fechaProgramada = "2026-10-16",
        tipo = TipoTarea.OBSERVACION,
        cultivoId = "cul-1"
    )

    private val demoReporte = ReporteProblema(
        id = "rep-1",
        sectorId = "sec-1",
        cultivoId = "cul-1",
        usuarioId = "estudiante-1",
        descripcion = "Plaga detectada",
        prioridad = "Media",
        estado = "Informado",
        fechaReporte = "2026-10-14",
        tipoProblema = "Plagas"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { repository.inicializar() } returns Unit
        val snapshot = HuertoSnapshot(
            sectores = listOf(demoSector),
            cultivos = listOf(demoCultivo),
            tareas = listOf(demoTarea, demoTareaPorValidar),
            evidencias = emptyList(),
            validaciones = emptyList(),
            reportesProblemas = listOf(demoReporte),
            actividades = emptyList()
        )
        coEvery { repository.cargarSnapshot() } returns snapshot
        coEvery { repository.cargarSnapshot(any()) } returns snapshot
        coEvery { repository.obtenerUsuarios() } returns listOf(demoAdmin, demoEstudiante)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.crearViewModel(sesion: Usuario? = demoAdmin): HuertoViewModel {
        every { sessionManager.obtenerUsuarioSesion() } returns sesion
        val vm = HuertoViewModel(
            application,
            repository,
            testDispatcher,
            sessionManager = sessionManager
        )
        advanceUntilIdle()
        return vm
    }

    @Test
    fun inicializacionCargaSnapshotYUsuariosEnUiState() = runTest {
        val viewModel = crearViewModel()

        assertEquals(1, viewModel.listaSectores.value.size)
        assertEquals("Sector Hortalizas", viewModel.listaSectores.value.first().nombre)
        assertEquals(1, viewModel.listaCultivos.value.size)
        assertEquals(2, viewModel.usuariosDisponibles.value.size)
        assertEquals(2, viewModel.uiState.value.tareas.size)
        assertEquals(1, viewModel.uiState.value.reportesProblemas.size)
        assertFalse(viewModel.uiState.value.cargandoDatos)
    }

    @Test
    fun falloDelBackendNoRellenaDatosDesdeRoom() = runTest {
        coEvery { repository.cargarSnapshot(any()) } throws java.io.IOException("Backend no disponible")

        val viewModel = crearViewModel()

        assertTrue(viewModel.listaSectores.value.isEmpty())
        assertTrue(viewModel.listaCultivos.value.isEmpty())
        assertEquals("Backend no disponible", viewModel.errorPersistencia)
    }

    @Test
    fun inicioDeSesionExitosoActualizaUsuarioActual() = runTest {
        coEvery { repository.autenticar("docente@huertolindo.cl", "HuertoDocente2026!") } returns demoAdmin

        val viewModel = crearViewModel(null)
        var resultado: HuertoViewModel.ResultadoInicioSesion? = null

        viewModel.iniciarSesion("docente@huertolindo.cl", "HuertoDocente2026!") {
            resultado = it
        }
        advanceUntilIdle()

        assertTrue(resultado is HuertoViewModel.ResultadoInicioSesion.Exito)
        assertEquals(demoAdmin, viewModel.usuarioActual)
        assertEquals(demoAdmin, viewModel.uiState.value.usuarioActual)
        assertTrue(viewModel.esAdministrador)
    }

    @Test
    fun inicioDeSesionFallidoRetornaCredencialesInvalidas() = runTest {
        coEvery { repository.autenticar("invalido@huertolindo.cl", "wrong") } returns null

        val viewModel = crearViewModel(null)
        var resultado: HuertoViewModel.ResultadoInicioSesion? = null

        viewModel.iniciarSesion("invalido@huertolindo.cl", "wrong") {
            resultado = it
        }
        advanceUntilIdle()

        assertTrue(resultado is HuertoViewModel.ResultadoInicioSesion.CredencialesInvalidas)
        assertNull(viewModel.usuarioActual)
    }

    @Test
    fun cerrarSesionLimpiaUsuarioActualYDatos() = runTest {
        coEvery { repository.autenticar(any(), any()) } returns demoAdmin
        val viewModel = crearViewModel()
        viewModel.iniciarSesion("docente@huertolindo.cl", "pass") {}
        advanceUntilIdle()

        assertEquals(demoAdmin, viewModel.usuarioActual)
        viewModel.cerrarSesion()

        assertNull(viewModel.usuarioActual)
        assertTrue(viewModel.usuariosDisponibles.value.isEmpty())
    }

    @Test
    fun registrarUsuarioInvocaRepositorio() = runTest {
        coEvery { repository.registrarUsuario(any(), any()) } returns Unit
        val viewModel = crearViewModel()

        var exito = false
        viewModel.registrarUsuario(
            nombre = "Carlos",
            apellido = "Mora",
            correo = "carlos@huertolindo.cl",
            password = "Password123!"
        ) { resultado ->
            if (resultado is HuertoViewModel.ResultadoRegistro.Exito) exito = true
        }
        advanceUntilIdle()

        assertTrue(exito)
        coVerify(exactly = 1) { repository.registrarUsuario(any(), any()) }
    }

    @Test
    fun administradoresPuedenCrearSectores() = runTest {
        coEvery { repository.autenticar(any(), any()) } returns demoAdmin
        coEvery { repository.guardarSector(any()) } answers { firstArg() }

        val viewModel = crearViewModel()
        viewModel.iniciarSesion("admin@huertolindo.cl", "pass") {}
        advanceUntilIdle()

        val creado = viewModel.agregarSector(
            nombre = "Sector Sur",
            ubicacion = "Patio Trasero",
            descripcion = "Área de compost",
            estado = "Disponible"
        )
        advanceUntilIdle()

        assertTrue(creado)
        assertTrue(viewModel.listaSectores.value.any { it.nombre == "Sector Sur" })
        coVerify(exactly = 1) { repository.guardarSector(match { it.nombre == "Sector Sur" }) }
    }

    @Test
    fun falloAlGuardarSectorEnBackendRevierteElOptimismoYExponeElError() = runTest {
        coEvery { repository.autenticar(any(), any()) } returns demoAdmin
        coEvery { repository.guardarSector(any()) } throws IllegalStateException("Backend HTTP 403")

        val viewModel = crearViewModel()
        viewModel.iniciarSesion("admin@huertolindo.cl", "pass") {}
        advanceUntilIdle()

        viewModel.agregarSector(
            nombre = "No persistido",
            ubicacion = "Patio",
            descripcion = "Prueba",
            estado = "Disponible"
        )
        advanceUntilIdle()

        assertTrue(viewModel.listaSectores.value.none { it.nombre == "No persistido" })
        assertEquals("Backend HTTP 403", viewModel.errorPersistencia)
    }

    @Test
    fun administradoresPuedenEditarYEliminarSectores() = runTest {
        coEvery { repository.autenticar(any(), any()) } returns demoAdmin
        coEvery { repository.guardarSector(any()) } answers { firstArg() }
        coEvery { repository.actualizarSector(any()) } returns Unit
        coEvery { repository.eliminarSector(any()) } returns Unit

        val viewModel = crearViewModel()
        viewModel.iniciarSesion("admin@huertolindo.cl", "pass") {}
        advanceUntilIdle()

        // Create disposable sector with no assigned tasks or crops
        viewModel.agregarSector("Sector Vacio", "Este", "Sin cultivos", "Disponible")
        advanceUntilIdle()
        val sectorVacio = viewModel.listaSectores.value.first { it.nombre == "Sector Vacio" }

        val editado = viewModel.editarSector(
            sectorId = sectorVacio.id,
            nombre = "Sector Vacio Modificado",
            ubicacion = "Oeste",
            descripcion = "Actualizado",
            estado = "En uso"
        )
        advanceUntilIdle()
        assertTrue(editado)

        val eliminado = viewModel.eliminarSector(sectorVacio.id)
        advanceUntilIdle()
        assertTrue(eliminado)
    }

    @Test
    fun administradoresPuedenGestionarCultivos() = runTest {
        coEvery { repository.autenticar(any(), any()) } returns demoAdmin
        coEvery { repository.guardarCultivo(any(), any()) } answers { firstArg() }
        coEvery { repository.actualizarCultivo(any()) } returns Unit

        val viewModel = crearViewModel()
        viewModel.iniciarSesion("admin@huertolindo.cl", "pass") {}
        advanceUntilIdle()

        val creado = viewModel.agregarCultivo(
            nombre = "Zanahoria",
            sectorId = "sec-1",
            fechaSiembra = "2026-10-01",
            fechaCosechaEstimada = "2026-12-15",
            estadoDesarrollo = "Sembrado",
            frecuenciaRiego = "3"
        )
        advanceUntilIdle()
        assertTrue(creado)

        val cultivoCreado = viewModel.listaCultivos.value.first { it.nombreComun == "Zanahoria" }
        val editado = viewModel.editarCultivo(
            cultivoId = cultivoCreado.id,
            nombre = "Zanahoria Baby",
            fechaSiembra = "2026-10-01",
            fechaCosechaEstimada = "2026-12-15",
            estadoDesarrollo = "En crecimiento",
            frecuenciaRiego = "2"
        )
        advanceUntilIdle()
        assertTrue(editado)
    }

    @Test
    fun administradoresPuedenCrearYEditarTareas() = runTest {
        coEvery { repository.autenticar(any(), any()) } returns demoAdmin
        coEvery { repository.crearTarea(any(), any(), any(), any(), any()) } answers {
            firstArg<Tarea>() to arg<ActividadTarea>(4)
        }
        coEvery { repository.actualizarTarea(any(), any(), any(), any(), any()) } returns Unit

        val viewModel = crearViewModel()
        viewModel.iniciarSesion("admin@huertolindo.cl", "pass") {}
        advanceUntilIdle()

        val creada = viewModel.crearTarea(
            titulo = "Poda de mantenimiento",
            descripcion = "Podar ramas secas",
            sectorId = "sec-1",
            usuarioId = "estudiante-1",
            fechaProgramada = "2026-10-20",
            tipo = TipoTarea.MANTENIMIENTO,
            cultivoId = "cul-1"
        )
        advanceUntilIdle()
        assertTrue(creada)

        val tareaCreada = viewModel.uiState.value.tareas.first { it.titulo == "Poda de mantenimiento" }
        val editada = viewModel.editarTarea(
            tareaId = tareaCreada.id,
            titulo = "Poda profunda",
            descripcion = "Podar ramas secas y desinfectar",
            sectorId = "sec-1",
            usuarioId = "estudiante-1",
            fechaProgramada = "2026-10-22",
            tipo = TipoTarea.MANTENIMIENTO,
            cultivoId = "cul-1"
        )
        advanceUntilIdle()
        assertTrue(editada)
    }

    @Test
    fun noPermiteCrearTareaSinCultivoAsociado() = runTest {
        coEvery { repository.autenticar(any(), any()) } returns demoAdmin
        val viewModel = crearViewModel()
        viewModel.iniciarSesion("admin@huertolindo.cl", "pass") {}
        advanceUntilIdle()

        val creada = viewModel.crearTarea(
            titulo = "Tarea sin cultivo",
            descripcion = "No debe guardarse",
            sectorId = "sec-1",
            usuarioId = "estudiante-1",
            fechaProgramada = "2026-10-20",
            tipo = TipoTarea.MANTENIMIENTO
        )

        assertFalse(creada)
    }

    @Test
    fun estudiantePuedeIniciarTareaAsignada() = runTest {
        coEvery { repository.autenticar(any(), any()) } returns demoEstudiante
        val viewModel = crearViewModel()
        viewModel.iniciarSesion("estudiante1@huertolindo.cl", "pass") {}
        advanceUntilIdle()

        val iniciada = viewModel.iniciarTarea("tar-1")
        advanceUntilIdle()
        assertTrue(iniciada)
        val tareaEnCurso = viewModel.uiState.value.tareas.first { it.id == "tar-1" }
        assertEquals("En desarrollo", tareaEnCurso.estado)
    }

    @Test
    fun administradorPuedeValidarYRechazarTareasEnRevision() = runTest {
        coEvery { repository.autenticar(any(), any()) } returns demoAdmin
        coEvery { repository.validarTarea(any(), any(), any(), any()) } returns Unit

        val viewModel = crearViewModel()
        viewModel.iniciarSesion("admin@huertolindo.cl", "pass") {}
        advanceUntilIdle()

        // Test validating demoTareaPorValidar which is in "Por validar"
        val validada = viewModel.validarTarea("tar-validar")
        advanceUntilIdle()
        assertTrue(validada)
        val tareaValidada = viewModel.uiState.value.tareas.first { it.id == "tar-validar" }
        assertEquals("Validada", tareaValidada.estado)

        // Test rejection with motivo
        var rechazoResultado: String? = "inicial"
        viewModel.rechazarTarea("tar-1", "Falta documentación") {
            rechazoResultado = it
        }
        advanceUntilIdle()
        assertNotNull(rechazoResultado) // Not in "Por validar", should fail
    }

    @Test
    fun uiStateReflejaLoadingYMensajesDeErrorYExito() = runTest {
        val viewModel = crearViewModel()

        viewModel.setLoading(true)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.isLoading)

        viewModel.setError("Error de prueba en servidor")
        advanceUntilIdle()
        assertEquals("Error de prueba en servidor", viewModel.uiState.value.errorMessage)
        assertEquals("Error de prueba en servidor", viewModel.errorMessage)

        viewModel.setSuccess("Operación completada")
        advanceUntilIdle()
        assertEquals("Operación completada", viewModel.uiState.value.successMessage)
        assertEquals("Operación completada", viewModel.successMessage)

        viewModel.limpiarError()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.errorMessage)
        assertNull(viewModel.errorMessage)

        viewModel.limpiarExito()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.successMessage)
        assertNull(viewModel.successMessage)
    }

    @Test
    fun tareasVisiblesFiltraPorPerfilEstudiante() = runTest {
        coEvery { repository.autenticar(any(), any()) } returns demoEstudiante
        val viewModel = crearViewModel()
        viewModel.iniciarSesion("estudiante1@huertolindo.cl", "pass") {}
        advanceUntilIdle()

        val tareas = viewModel.tareasVisibles
        assertEquals(2, tareas.size)
        assertTrue(tareas.all { it.asignadoA == "estudiante-1" })
    }

    @Test
    fun reportesProblemasSePuedenCargarYActualizar() = runTest {
        coEvery { repository.autenticar(any(), any()) } returns demoAdmin
        coEvery { repository.obtenerReportesProblema(any()) } returns listOf(demoReporte)
        coEvery { repository.actualizarReporte(any()) } returns Unit

        val viewModel = crearViewModel()
        viewModel.iniciarSesion("admin@huertolindo.cl", "pass") {}
        advanceUntilIdle()

        viewModel.cargarReportesProblema()
        advanceUntilIdle()

        assertEquals(1, viewModel.reportesProblemas.value.size)
        assertEquals("Plagas", viewModel.reportesProblemas.value.first().tipoProblema)

        var resultado: String? = "inicial"
        viewModel.actualizarEstadoProblema("rep-1", "En revisión", "Revisando en terreno") {
            resultado = it
        }
        advanceUntilIdle()
        assertNull(resultado)
    }
}
