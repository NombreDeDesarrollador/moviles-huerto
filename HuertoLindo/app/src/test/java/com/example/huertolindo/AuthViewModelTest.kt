package com.example.huertolindo

import android.app.Application
import com.example.huertolindo.data.local.SessionManager
import com.example.huertolindo.domain.model.Usuario
import com.example.huertolindo.domain.repository.HuertoRepository
import com.example.huertolindo.domain.security.PermisosHuerto
import com.example.huertolindo.domain.usecase.AuthUseCase
import com.example.huertolindo.presentation.viewmodel.AuthViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val application = mockk<Application>(relaxed = true)
    private val repository = mockk<HuertoRepository>(relaxed = true)
    private val sessionManager = mockk<SessionManager>(relaxed = true)
    private lateinit var authUseCase: AuthUseCase

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

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { sessionManager.obtenerUsuarioSesion() } returns null
        authUseCase = AuthUseCase(repository, sessionManager)
        coEvery { repository.obtenerUsuarios() } returns listOf(demoAdmin, demoEstudiante)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.crearViewModel(): AuthViewModel {
        val vm = AuthViewModel(
            application = application,
            huertoRepository = repository,
            ioDispatcher = testDispatcher,
            sessionManager = sessionManager,
            authUseCase = authUseCase
        )
        advanceUntilIdle()
        return vm
    }

    @Test
    fun inicializacionRestaurarSesionDesdeSessionManager() = runTest {
        every { sessionManager.obtenerUsuarioSesion() } returns demoEstudiante

        val viewModel = crearViewModel()

        assertEquals(demoEstudiante, viewModel.usuarioActual)
        assertFalse(viewModel.esAdministrador)
        assertEquals(2, viewModel.uiState.value.usuariosDisponibles.size)
    }

    @Test
    fun inicioDeSesionExitosoActualizaUsuarioYPersiste() = runTest {
        coEvery { repository.autenticar("docente@huertolindo.cl", "HuertoDocente2026!") } returns demoAdmin

        val viewModel = crearViewModel()
        var resultado: AuthViewModel.ResultadoInicioSesion? = null

        viewModel.iniciarSesion("docente@huertolindo.cl", "HuertoDocente2026!") {
            resultado = it
        }
        advanceUntilIdle()

        assertTrue(resultado is AuthViewModel.ResultadoInicioSesion.Exito)
        assertEquals(demoAdmin, viewModel.usuarioActual)
        assertTrue(viewModel.esAdministrador)
        verify { sessionManager.guardarSesion(demoAdmin) }
        assertNotNull(viewModel.uiState.value.successMessage)
    }

    @Test
    fun inicioDeSesionFallidoMantieneUsuarioNull() = runTest {
        coEvery { repository.autenticar(any(), any()) } returns null

        val viewModel = crearViewModel()
        var resultado: AuthViewModel.ResultadoInicioSesion? = null

        viewModel.iniciarSesion("invalido@huertolindo.cl", "badpass") {
            resultado = it
        }
        advanceUntilIdle()

        assertTrue(resultado is AuthViewModel.ResultadoInicioSesion.CredencialesInvalidas)
        assertNull(viewModel.usuarioActual)
        assertEquals("Credenciales inválidas", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun registroDeUsuarioExitoso() = runTest {
        coEvery { repository.registrarUsuario(any()) } returns Unit

        val viewModel = crearViewModel()
        var resultado: AuthViewModel.ResultadoRegistro? = null

        viewModel.registrarUsuario(
            nombre = "Carlos",
            apellido = "Pérez",
            correo = "carlos@huertolindo.cl",
            password = "Password123!",
            tallerAsignado = "Taller Verde"
        ) {
            resultado = it
        }
        advanceUntilIdle()

        assertTrue(resultado is AuthViewModel.ResultadoRegistro.Exito)
        coVerify { repository.registrarUsuario(match { it.correo == "carlos@huertolindo.cl" }) }
    }

    @Test
    fun registroAdministradorExitosoCuandoActorEsAdmin() = runTest {
        coEvery { repository.autenticar("docente@huertolindo.cl", "HuertoDocente2026!") } returns demoAdmin
        coEvery { repository.registrarAdministrador(any(), "admin-1") } returns true

        val viewModel = crearViewModel()
        viewModel.iniciarSesion("docente@huertolindo.cl", "HuertoDocente2026!") {}
        advanceUntilIdle()

        var resultado: AuthViewModel.ResultadoRegistroAdministrador? = null
        viewModel.registrarAdministrador(
            nombre = "Laura",
            apellido = "Docente",
            correo = "laura@huertolindo.cl",
            password = "Docente2026!"
        ) {
            resultado = it
        }
        advanceUntilIdle()

        assertTrue(resultado is AuthViewModel.ResultadoRegistroAdministrador.Exito)
        coVerify { repository.registrarAdministrador(match { it.correo == "laura@huertolindo.cl" }, "admin-1") }
    }

    @Test
    fun cerrarSesionLimpiaEstadoYSessionManager() = runTest {
        coEvery { repository.autenticar("docente@huertolindo.cl", "HuertoDocente2026!") } returns demoAdmin

        val viewModel = crearViewModel()
        viewModel.iniciarSesion("docente@huertolindo.cl", "HuertoDocente2026!") {}
        advanceUntilIdle()

        assertNotNull(viewModel.usuarioActual)

        viewModel.cerrarSesion()
        advanceUntilIdle()

        assertNull(viewModel.usuarioActual)
        assertNull(viewModel.fotoPerfilUri)
        verify { sessionManager.cerrarSesion() }
    }
}
