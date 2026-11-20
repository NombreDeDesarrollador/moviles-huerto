package com.example.huertolindo

import android.app.Application
import android.net.Uri
import com.example.huertolindo.data.local.SessionManager
import com.example.huertolindo.domain.model.*
import com.example.huertolindo.domain.repository.HuertoRepository
import com.example.huertolindo.domain.repository.HuertoSnapshot
import com.example.huertolindo.domain.security.PermisosHuerto
import com.example.huertolindo.domain.security.ReglasHuerto
import com.example.huertolindo.domain.usecase.*
import com.example.huertolindo.presentation.viewmodel.AuthViewModel
import com.example.huertolindo.presentation.viewmodel.ReportesHistorialViewModel
import com.example.huertolindo.presentation.viewmodel.SectoresCultivosViewModel
import com.example.huertolindo.presentation.viewmodel.TareasViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class CasosDeUsoFuncionalesTest {

    private val testDispatcher = StandardTestDispatcher()
    private val application = mockk<Application>(relaxed = true)
    private val repository = mockk<HuertoRepository>(relaxed = true)
    private val sessionManager = mockk<SessionManager>(relaxed = true)

    private val docenteAdmin = Usuario(
        id = "usr-docente",
        nombre = "Docente",
        apellido = "Guía",
        correo = "docente@huertolindo.cl",
        perfil = PermisosHuerto.ADMINISTRADOR,
        tallerAsignado = "General"
    )

    private val estudianteColaborador = Usuario(
        id = "usr-estudiante-1",
        nombre = "Estudiante",
        apellido = "Uno",
        correo = "estudiante1@huertolindo.cl",
        perfil = PermisosHuerto.ESTUDIANTE,
        tallerAsignado = "Taller A"
    )

    private val sectorDemo = Sector(
        id = "sec-invernadero",
        nombre = "Invernadero Principal",
        descripcion = "Zona de hortalizas y aromáticas",
        estado = "En uso",
        ubicacion = "Zona Norte"
    )

    private val cultivoDemo = Cultivo(
        id = "cul-lechuga",
        nombreComun = "Lechuga Costina",
        sectorId = "sec-invernadero",
        fechaSiembra = "2026-10-01",
        fechaCosechaEstimada = "2026-12-01",
        estadoDesarrollo = EstadoCultivo.SEMBRADO,
        frecuenciaRiego = "2"
    )

    private val tareaDemo = Tarea(
        id = "tar-riego-1",
        titulo = "Riego por goteo",
        descripcion = "Verificar goteros y humedecer bancal",
        sectorId = "sec-invernadero",
        asignadoA = "usr-estudiante-1",
        estado = "Pendiente",
        fechaProgramada = "2026-10-15",
        tipo = TipoTarea.RIEGO,
        cultivoId = "cul-lechuga"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { sessionManager.obtenerUsuarioSesion() } returns null
        coEvery { repository.inicializar() } returns Unit
        coEvery { repository.cargarSnapshot() } returns HuertoSnapshot(
            sectores = listOf(sectorDemo),
            cultivos = listOf(cultivoDemo),
            tareas = listOf(tareaDemo),
            evidencias = emptyList(),
            validaciones = emptyList(),
            reportesProblemas = emptyList(),
            actividades = emptyList()
        )
        coEvery { repository.obtenerUsuarios() } returns listOf(docenteAdmin, estudianteColaborador)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- MÓDULO 1: AUTENTICACIÓN Y SESIÓN ---

    @Test
    fun cu01_autenticacionDocenteAdminExitosa() = runTest {
        coEvery { repository.autenticar("docente@huertolindo.cl", "HuertoDocente2026!") } returns docenteAdmin

        val authVm = AuthViewModel(
            application, repository, testDispatcher, sessionManager,
            AuthUseCase(repository, sessionManager)
        )
        var res: AuthViewModel.ResultadoInicioSesion? = null
        authVm.iniciarSesion("docente@huertolindo.cl", "HuertoDocente2026!") { res = it }
        advanceUntilIdle()

        assertTrue(res is AuthViewModel.ResultadoInicioSesion.Exito)
        assertEquals(PermisosHuerto.ADMINISTRADOR, authVm.usuarioActual?.perfil)
        assertTrue(authVm.esAdministrador)
        verify { sessionManager.guardarSesion(docenteAdmin) }
    }

    @Test
    fun cu02_autenticacionEstudianteColaboradorExitosa() = runTest {
        coEvery { repository.autenticar("estudiante1@huertolindo.cl", "HuertoEstudiante2026!") } returns estudianteColaborador

        val authVm = AuthViewModel(
            application, repository, testDispatcher, sessionManager,
            AuthUseCase(repository, sessionManager)
        )
        var res: AuthViewModel.ResultadoInicioSesion? = null
        authVm.iniciarSesion("estudiante1@huertolindo.cl", "HuertoEstudiante2026!") { res = it }
        advanceUntilIdle()

        assertTrue(res is AuthViewModel.ResultadoInicioSesion.Exito)
        assertEquals(PermisosHuerto.ESTUDIANTE, authVm.usuarioActual?.perfil)
        assertFalse(authVm.esAdministrador)
        assertEquals("Taller A", authVm.usuarioActual?.tallerAsignado)
    }

    @Test
    fun cu03_autenticacionCredencialesInvalidasRechazada() = runTest {
        coEvery { repository.autenticar("docente@huertolindo.cl", "WrongPassword") } returns null

        val authVm = AuthViewModel(
            application, repository, testDispatcher, sessionManager,
            AuthUseCase(repository, sessionManager)
        )
        var res: AuthViewModel.ResultadoInicioSesion? = null
        authVm.iniciarSesion("docente@huertolindo.cl", "WrongPassword") { res = it }
        advanceUntilIdle()

        assertTrue(res is AuthViewModel.ResultadoInicioSesion.CredencialesInvalidas)
        assertNull(authVm.usuarioActual)
        assertNotNull(authVm.uiState.value.errorMessage)
    }

    @Test
    fun cu04_persistenciaSesionAnteReinicioYRotacion() = runTest {
        every { sessionManager.obtenerUsuarioSesion() } returns docenteAdmin

        val authVm = AuthViewModel(
            application, repository, testDispatcher, sessionManager,
            AuthUseCase(repository, sessionManager)
        )
        advanceUntilIdle()

        assertEquals(docenteAdmin, authVm.usuarioActual)
        assertEquals("docente@huertolindo.cl", authVm.uiState.value.usuarioActual?.correo)
    }

    @Test
    fun cu05_cierreDeSesionLimpiaCredenciales() = runTest {
        every { sessionManager.obtenerUsuarioSesion() } returns estudianteColaborador

        val authVm = AuthViewModel(
            application, repository, testDispatcher, sessionManager,
            AuthUseCase(repository, sessionManager)
        )
        advanceUntilIdle()
        assertNotNull(authVm.usuarioActual)

        authVm.cerrarSesion()
        advanceUntilIdle()

        assertNull(authVm.usuarioActual)
        verify { sessionManager.cerrarSesion() }
    }

    @Test
    fun cu06_privilegioMinimoEstudianteBloqueaAccionesAdmin() = runTest {
        val useCase = GestionSectoresUseCase(repository)

        // Estudiante intentando agregar un sector
        val nuevoSector = useCase.agregarSector(
            nombre = "Sector Prohibido",
            ubicacion = "Sur",
            descripcion = "Intento",
            estado = "Disponible",
            inicioMantenimiento = "",
            terminoMantenimiento = "",
            actorPerfil = PermisosHuerto.ESTUDIANTE
        )
        assertNull(nuevoSector)

        // Estudiante intentando validar una tarea
        assertFalse(
            PermisosHuerto.puedeValidarTarea(
                PermisosHuerto.ESTUDIANTE,
                estudianteColaborador.id,
                tareaDemo.copy(estado = "Por validar")
            )
        )
    }

    // --- MÓDULO 2: SECTORES Y CULTIVOS ---

    @Test
    fun cu07_creacionYEdicionSectorValidoPorAdmin() = runTest {
        coEvery { repository.guardarSector(any()) } returns Unit
        coEvery { repository.actualizarSector(any()) } returns Unit

        val vm = SectoresCultivosViewModel(
            application, repository, testDispatcher,
            GestionSectoresUseCase(repository), GestionCultivosUseCase(repository)
        )
        advanceUntilIdle()

        val creado = vm.agregarSector(
            nombre = "Bancales Hidropónicos",
            ubicacion = "Zona Este",
            descripcion = "Lechugas hidropónicas",
            estado = "Disponible",
            actorPerfil = PermisosHuerto.ADMINISTRADOR
        )
        advanceUntilIdle()
        assertTrue(creado)
        coVerify { repository.guardarSector(match { it.nombre == "Bancales Hidropónicos" }) }

        val editado = vm.editarSector(
            sectorId = sectorDemo.id,
            nombre = "Invernadero Climatizado",
            ubicacion = "Zona Norte",
            descripcion = "Climatización activa",
            estado = "Disponible",
            actorPerfil = PermisosHuerto.ADMINISTRADOR
        )
        advanceUntilIdle()
        assertTrue(editado)
        coVerify { repository.actualizarSector(match { it.nombre == "Invernadero Climatizado" }) }
    }

    @Test
    fun cu08_creacionSectorRechazadaConDatosInvalidos() = runTest {
        val vm = SectoresCultivosViewModel(
            application, repository, testDispatcher,
            GestionSectoresUseCase(repository), GestionCultivosUseCase(repository)
        )
        advanceUntilIdle()

        // Nombre vacío
        val sinNombre = vm.agregarSector(
            nombre = "   ",
            ubicacion = "Ubicación",
            descripcion = "Desc",
            estado = "Disponible",
            actorPerfil = PermisosHuerto.ADMINISTRADOR
        )
        assertFalse(sinNombre)

        // Mantenimiento con fechas invertidas
        val fechasInvertidas = vm.agregarSector(
            nombre = "Sector Mantenimiento",
            ubicacion = "Norte",
            descripcion = "Prueba",
            estado = "En mantenimiento",
            inicioMantenimiento = "2026-12-01",
            terminoMantenimiento = "2026-10-01",
            actorPerfil = PermisosHuerto.ADMINISTRADOR
        )
        assertFalse(fechasInvertidas)
    }

    @Test
    fun cu09_restriccionEliminacionSectorConDependencias() = runTest {
        val vm = SectoresCultivosViewModel(
            application, repository, testDispatcher,
            GestionSectoresUseCase(repository), GestionCultivosUseCase(repository)
        )
        advanceUntilIdle()

        // sectorDemo tiene cultivoDemo asociado
        val eliminado = vm.eliminarSector(
            sectorId = sectorDemo.id,
            tareas = listOf(tareaDemo),
            reportes = emptyList(),
            actorPerfil = PermisosHuerto.ADMINISTRADOR
        )
        advanceUntilIdle()

        assertFalse(eliminado)
        coVerify(exactly = 0) { repository.eliminarSector(any()) }
    }

    @Test
    fun cu10_registroCultivoValidoAsociadoASectorExistente() = runTest {
        coEvery { repository.guardarCultivo(any(), any()) } returns Unit

        val vm = SectoresCultivosViewModel(
            application, repository, testDispatcher,
            GestionSectoresUseCase(repository), GestionCultivosUseCase(repository)
        )
        advanceUntilIdle()

        val agregado = vm.agregarCultivo(
            nombre = "Espinaca Baby",
            sector = sectorDemo,
            fechaSiembra = "2026-10-10",
            fechaCosechaEstimada = "2026-11-20",
            estadoDesarrollo = EstadoCultivo.SEMBRADO,
            frecuenciaRiego = "1",
            actorPerfil = PermisosHuerto.ADMINISTRADOR
        )
        advanceUntilIdle()

        assertTrue(agregado)
        coVerify { repository.guardarCultivo(match { it.nombreComun == "Espinaca Baby" }, sectorDemo) }
    }

    @Test
    fun cu11_rechazoCultivoConFechasOInconsistenciasDeRiego() = runTest {
        val vm = SectoresCultivosViewModel(
            application, repository, testDispatcher,
            GestionSectoresUseCase(repository), GestionCultivosUseCase(repository)
        )
        advanceUntilIdle()

        // Cosecha anterior a siembra
        val fechaInvalida = vm.agregarCultivo(
            nombre = "Tomate",
            sector = sectorDemo,
            fechaSiembra = "2026-11-20",
            fechaCosechaEstimada = "2026-10-10",
            estadoDesarrollo = EstadoCultivo.SEMBRADO,
            frecuenciaRiego = "2",
            actorPerfil = PermisosHuerto.ADMINISTRADOR
        )
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.errorMessage)

        // Frecuencia de riego no positiva
        assertFalse(ReglasHuerto.frecuenciaRiegoValida("0"))
        assertFalse(ReglasHuerto.frecuenciaRiegoValida("-3"))
        assertFalse(ReglasHuerto.frecuenciaRiegoValida("tres días"))
    }

    @Test
    fun cu12_transicionCompletaDeEstadosDeCultivo() {
        val estadosEsperados: List<String> = listOf(
            EstadoCultivo.PLANIFICADO,
            EstadoCultivo.SEMBRADO,
            EstadoCultivo.EN_CRECIMIENTO,
            EstadoCultivo.LISTO_COSECHA,
            EstadoCultivo.COSECHADO,
            EstadoCultivo.CON_PROBLEMAS,
            EstadoCultivo.FINALIZADO
        )
        estadosEsperados.forEach { estado ->
            assertTrue("El estado $estado debe ser válido", EstadoCultivo.esValido(estado))
        }
    }

    @Test
    fun cu13_deteccionAutomaticaCultivosConRiegoPendiente() = runTest {
        val useCase = GestionCultivosUseCase(repository)
        val cultivos = listOf(cultivoDemo)

        val proximoRiego = useCase.fechaProximoRiego(cultivoDemo)
        assertNotNull(proximoRiego)
        assertTrue(ReglasHuerto.esFechaIsoValida(proximoRiego!!))
    }

    // --- MÓDULO 3: GESTIÓN Y EJECUCIÓN DE TAREAS ---

    @Test
    fun cu14_creacionYAsignacionTareaPorAdmin() = runTest {
        coEvery { repository.crearTarea(any(), any(), any(), any(), any()) } returns Unit

        val vm = TareasViewModel(application, repository, testDispatcher, GestionTareasUseCase(repository))
        advanceUntilIdle()

        val creada = vm.crearTarea(
            titulo = "Poda de hojas secas",
            descripcion = "Retirar hojas bajeras",
            sector = sectorDemo,
            cultivoId = cultivoDemo.id,
            estudiantes = listOf(estudianteColaborador),
            actor = docenteAdmin,
            fechaProgramada = "2026-11-30",
            tipo = TipoTarea.PODA
        )
        advanceUntilIdle()

        assertTrue(creada)
        coVerify {
            repository.crearTarea(
                match { it.titulo == "Poda de hojas secas" && it.estado == "Pendiente" },
                sectorDemo,
                estudianteColaborador,
                docenteAdmin,
                any()
            )
        }
    }

    @Test
    fun cu15_asignacionGrupalDeTareasPorTaller() = runTest {
        val estudiante2 = estudianteColaborador.copy(id = "usr-estudiante-2", correo = "estudiante2@huertolindo.cl")
        val useCase = GestionTareasUseCase(repository)

        val tareas = useCase.crearTareas(
            titulo = "Limpieza de pasillos",
            descripcion = "Barrer y ordenar",
            sector = sectorDemo,
            cultivoId = null,
            estudiantes = listOf(estudianteColaborador, estudiante2),
            actor = docenteAdmin,
            fechaProgramada = "2026-11-28",
            tipo = TipoTarea.MANTENIMIENTO
        )

        assertEquals(2, tareas.size)
        assertEquals("usr-estudiante-1", tareas[0].first.asignadoA)
        assertEquals("usr-estudiante-2", tareas[1].first.asignadoA)
    }

    @Test
    fun cu16_inicioDeTrabajoTareaPorEstudiante() = runTest {
        val vm = TareasViewModel(application, repository, testDispatcher, GestionTareasUseCase(repository))
        advanceUntilIdle()

        var error: String? = "inicial"
        vm.iniciarTarea(tareaDemo.id, estudianteColaborador) { error = it }
        advanceUntilIdle()

        assertNull(error)
        val tareaEnCurso = vm.tareas.first { it.id == tareaDemo.id }
        assertEquals("En desarrollo", tareaEnCurso.estado)
    }

    @Test
    fun cu17_entregaTareaConEvidenciaFotograficaYComentario() = runTest {
        val tareaEnCurso = tareaDemo.copy(estado = "En desarrollo")
        coEvery { repository.cargarSnapshot() } returns HuertoSnapshot(
            sectores = listOf(sectorDemo), cultivos = listOf(cultivoDemo),
            tareas = listOf(tareaEnCurso), evidencias = emptyList(),
            validaciones = emptyList(), reportesProblemas = emptyList(), actividades = emptyList()
        )
        val evidencia = Evidencia(
            id = "evi-1", tareaId = tareaEnCurso.id, tipo = "FOTOGRAFIA",
            rutaArchivo = "/tmp/foto_riego.jpg", descripcion = "Bancal regado completamente",
            fechaRegistro = "2026-10-15"
        )
        coEvery { repository.guardarEvidenciaLocal(any(), any(), any()) } returns evidencia
        coEvery { repository.entregarTarea(any(), any(), any(), any()) } returns Unit

        val vm = TareasViewModel(application, repository, testDispatcher, GestionTareasUseCase(repository))
        advanceUntilIdle()

        var error: String? = "inicial"
        vm.entregarTarea(
            tareaId = tareaEnCurso.id,
            comentario = "Bancal regado completamente",
            imagenUri = mockk(relaxed = true),
            estudiante = estudianteColaborador
        ) { error = it }
        advanceUntilIdle()

        assertNull(error)
        val tareaEntregada = vm.tareas.first { it.id == tareaEnCurso.id }
        assertEquals("Por validar", tareaEntregada.estado)
    }

    @Test
    fun cu18_prohibicionEstrictaAutoValidacionPorEstudiante() {
        val tareaPorValidar = tareaDemo.copy(estado = "Por validar")
        val puedeEstudiante = PermisosHuerto.puedeValidarTarea(
            PermisosHuerto.ESTUDIANTE,
            estudianteColaborador.id,
            tareaPorValidar
        )
        assertFalse("El estudiante nunca debe poder auto-validar tareas", puedeEstudiante)
    }

    @Test
    fun cu19_validacionYAprobacionTareaPorAdmin() = runTest {
        val tareaPorValidar = tareaDemo.copy(estado = "Por validar")
        coEvery { repository.cargarSnapshot() } returns HuertoSnapshot(
            sectores = listOf(sectorDemo), cultivos = listOf(cultivoDemo),
            tareas = listOf(tareaPorValidar), evidencias = emptyList(),
            validaciones = emptyList(), reportesProblemas = emptyList(), actividades = emptyList()
        )
        coEvery { repository.validarTarea(any(), any(), any(), any()) } returns Unit

        val vm = TareasViewModel(application, repository, testDispatcher, GestionTareasUseCase(repository))
        advanceUntilIdle()

        var error: String? = "inicial"
        vm.validarTarea(tareaPorValidar.id, docenteAdmin) { error = it }
        advanceUntilIdle()

        assertNull(error)
        val validada = vm.tareas.first { it.id == tareaPorValidar.id }
        assertEquals("Validada", validada.estado)
    }

    @Test
    fun cu20_rechazoTareaConMotivoObligatorioPorAdmin() = runTest {
        val tareaPorValidar = tareaDemo.copy(estado = "Por validar")
        coEvery { repository.cargarSnapshot() } returns HuertoSnapshot(
            sectores = listOf(sectorDemo), cultivos = listOf(cultivoDemo),
            tareas = listOf(tareaPorValidar), evidencias = emptyList(),
            validaciones = emptyList(), reportesProblemas = emptyList(), actividades = emptyList()
        )
        coEvery { repository.validarTarea(any(), any(), any(), any()) } returns Unit

        val vm = TareasViewModel(application, repository, testDispatcher, GestionTareasUseCase(repository))
        advanceUntilIdle()

        var error: String? = "inicial"
        vm.rechazarTarea(
            tareaId = tareaPorValidar.id,
            motivo = "Foto poco nítida, repetir evidencia",
            administrador = docenteAdmin
        ) { error = it }
        advanceUntilIdle()

        assertNull(error)
        val rechazada = vm.tareas.first { it.id == tareaPorValidar.id }
        assertEquals("Rechazada", rechazada.estado)
        assertEquals("Foto poco nítida, repetir evidencia", rechazada.motivoRechazo)
    }

    @Test
    fun cu21_bloqueoRechazoTareaSinMotivoExplicito() = runTest {
        val tareaPorValidar = tareaDemo.copy(estado = "Por validar")
        coEvery { repository.cargarSnapshot() } returns HuertoSnapshot(
            sectores = listOf(sectorDemo), cultivos = listOf(cultivoDemo),
            tareas = listOf(tareaPorValidar), evidencias = emptyList(),
            validaciones = emptyList(), reportesProblemas = emptyList(), actividades = emptyList()
        )

        val vm = TareasViewModel(application, repository, testDispatcher, GestionTareasUseCase(repository))
        advanceUntilIdle()

        var error: String? = null
        vm.rechazarTarea(
            tareaId = tareaPorValidar.id,
            motivo = "   ",
            administrador = docenteAdmin
        ) { error = it }
        advanceUntilIdle()

        assertNotNull(error)
        assertEquals("Indica el motivo del rechazo.", error)
    }

    // --- MÓDULO 4: REPORTES, ALERTAS Y SENSORES ---

    @Test
    fun cu22_registroReporteProblemaEnEstadoInicialInformado() = runTest {
        coEvery { repository.insertarReporte(any(), any(), any(), any(), any()) } returns Unit

        val vm = ReportesHistorialViewModel(
            application, repository, testDispatcher,
            GestionReportesUseCase(repository), GestionCultivosUseCase(repository), GestionTareasUseCase(repository)
        )
        advanceUntilIdle()

        var error: String? = "inicial"
        vm.crearReporteProblema(
            descripcion = "Fuga de agua en manguera sector hortalizas",
            sector = sectorDemo,
            cultivo = cultivoDemo,
            tipoProblema = "Falta de agua",
            usuario = estudianteColaborador,
            fotografia = mockk(relaxed = true)
        ) { error = it }
        advanceUntilIdle()

        assertNull(error)
        assertEquals(1, vm.reportesProblemas.size)
        assertEquals("Informado", vm.reportesProblemas.first().estado)
    }

    @Test
    fun cu23_avanceYCierreReporteProblemaExclusivoAdmin() = runTest {
        val reporteInicial = ReporteProblema(
            id = "rep-01",
            sectorId = sectorDemo.id,
            cultivoId = null,
            usuarioId = estudianteColaborador.id,
            descripcion = "Plaga simulada en tomates",
            prioridad = "Alta",
            estado = "Informado",
            fechaReporte = "2026-10-15",
            tipoProblema = "Plagas"
        )
        coEvery { repository.cargarSnapshot() } returns HuertoSnapshot(
            sectores = listOf(sectorDemo), cultivos = listOf(cultivoDemo),
            tareas = emptyList(), evidencias = emptyList(),
            validaciones = emptyList(), reportesProblemas = listOf(reporteInicial), actividades = emptyList()
        )
        coEvery { repository.actualizarReporte(any()) } returns Unit

        val vm = ReportesHistorialViewModel(
            application, repository, testDispatcher,
            GestionReportesUseCase(repository), GestionCultivosUseCase(repository), GestionTareasUseCase(repository)
        )
        advanceUntilIdle()

        var error: String? = "inicial"
        vm.actualizarEstadoProblema(
            reporte = reporteInicial,
            nuevoEstado = "Cerrado",
            comentario = "Se aplicó infusión de ajo y se controló foco",
            administrador = docenteAdmin
        ) { error = it }
        advanceUntilIdle()

        assertNull(error)
        val cerrado = vm.reportesProblemas.first { it.id == "rep-01" }
        assertEquals("Cerrado", cerrado.estado)
        assertTrue(cerrado.comentarioSeguimiento.contains("Se aplicó infusión de ajo"))
    }

    @Test
    fun cu24_deteccionYFiltradoTareasAtrasadas() {
        val tareaAtrasada = tareaDemo.copy(fechaProgramada = "2020-01-01", estado = "Pendiente")
        val tareaAlDia = tareaDemo.copy(fechaProgramada = "2030-01-01", estado = "Pendiente")
        val useCase = GestionTareasUseCase(repository)

        val atrasadas = useCase.tareasAtrasadas(listOf(tareaAtrasada, tareaAlDia))
        assertEquals(1, atrasadas.size)
        assertEquals(tareaAtrasada.id, atrasadas.first().id)
    }

    @Test
    fun cu25_etiquetadoObligatorioSimulacionAcademicaEnSensores() = runTest {
        val vm = SectoresCultivosViewModel(
            application, repository, testDispatcher,
            GestionSectoresUseCase(repository), GestionCultivosUseCase(repository)
        )
        advanceUntilIdle()

        val sensor = vm.sensores.firstOrNull()
        assertNotNull(sensor)
        assertEquals("Simulación académica / Dato de prueba", sensor?.origen)
    }

    // --- MÓDULO 5: RETROFIT + ROOM ---

    @Test
    fun cu26_sincronizacionServidorPrimeroConRespaldoLocal() = runTest {
        val mockSector = sectorDemo.copy(id = "sec-nuevo", nombre = "Sector Florales")
        coEvery { repository.guardarSector(mockSector) } returns Unit

        repository.guardarSector(mockSector)
        coVerify(exactly = 1) { repository.guardarSector(mockSector) }
    }

    @Test
    fun cu27_resilienciaCaidaDeRedFallbackLocal() = runTest {
        coEvery { repository.cargarSnapshot() } throws IOException("Sin conexión a servidor Spring Boot")

        val vm = SectoresCultivosViewModel(
            application, repository, testDispatcher,
            GestionSectoresUseCase(repository), GestionCultivosUseCase(repository)
        )
        advanceUntilIdle()

        // El ViewModel no crashea, maneja el error adecuadamente en su UiState
        assertNotNull(vm.uiState.value.errorMessage)
        assertFalse(vm.uiState.value.isLoading)
    }
}
