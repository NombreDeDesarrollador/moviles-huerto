package com.example.huertolindo

import android.app.Application
import com.example.huertolindo.domain.model.Cultivo
import com.example.huertolindo.domain.model.Sector
import com.example.huertolindo.domain.repository.HuertoRepository
import com.example.huertolindo.domain.repository.HuertoSnapshot
import com.example.huertolindo.domain.usecase.GestionCultivosUseCase
import com.example.huertolindo.domain.usecase.GestionSectoresUseCase
import com.example.huertolindo.presentation.viewmodel.SectoresCultivosViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SectoresCultivosViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val application = mockk<Application>(relaxed = true)
    private val repository = mockk<HuertoRepository>(relaxed = true)

    private val demoSector = Sector(
        id = "sec-1",
        nombre = "Sector Hortalizas",
        descripcion = "Zona de hortalizas",
        estado = "En uso",
        ubicacion = "Norte"
    )

    private val demoCultivo = Cultivo(
        id = "cul-1",
        nombreComun = "Tomate",
        sectorId = "sec-1",
        fechaSiembra = "2026-10-01",
        estadoDesarrollo = "Sembrado",
        frecuenciaRiego = "2",
        fechaCosechaEstimada = "2026-12-01"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { repository.inicializar() } returns Unit
        coEvery { repository.cargarSnapshot() } returns HuertoSnapshot(
            sectores = listOf(demoSector),
            cultivos = listOf(demoCultivo),
            tareas = emptyList(),
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

    private fun TestScope.crearViewModel(): SectoresCultivosViewModel {
        val vm = SectoresCultivosViewModel(
            application = application,
            huertoRepository = repository,
            ioDispatcher = testDispatcher,
            sectoresUseCase = GestionSectoresUseCase(repository),
            cultivosUseCase = GestionCultivosUseCase(repository)
        )
        advanceUntilIdle()
        return vm
    }

    @Test
    fun inicializacionCargaSectoresCultivosYSensores() = runTest {
        val viewModel = crearViewModel()

        assertEquals(1, viewModel.sectores.size)
        assertEquals("Sector Hortalizas", viewModel.sectores.first().nombre)
        assertEquals(1, viewModel.cultivos.size)
        assertEquals(1, viewModel.sensores.size)
        assertTrue(viewModel.sensores.first().origen.contains("Simulación académica / Dato de prueba"))
    }

    @Test
    fun agregarSectorExitosoParaAdministrador() = runTest {
        coEvery { repository.guardarSector(any()) } returns Unit

        val viewModel = crearViewModel()
        val resultado = viewModel.agregarSector(
            nombre = "Invernadero Sur",
            ubicacion = "Zona Sur",
            descripcion = "Zona protegida",
            estado = "Disponible",
            actorPerfil = "ADMIN"
        )
        advanceUntilIdle()

        assertTrue(resultado)
        assertEquals(2, viewModel.sectores.size)
        coVerify { repository.guardarSector(match { it.nombre == "Invernadero Sur" }) }
    }

    @Test
    fun agregarSectorRechazadoConDatosInvalidos() = runTest {
        val viewModel = crearViewModel()
        val resultado = viewModel.agregarSector(
            nombre = "",
            ubicacion = "Zona",
            descripcion = "Desc",
            estado = "Disponible",
            actorPerfil = "ADMIN"
        )
        advanceUntilIdle()

        assertFalse(resultado)
        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun agregarCultivoExitosoParaAdministrador() = runTest {
        coEvery { repository.guardarCultivo(any(), any()) } returns Unit

        val viewModel = crearViewModel()
        val resultado = viewModel.agregarCultivo(
            nombre = "Zanahoria",
            sector = demoSector,
            fechaSiembra = "2026-10-05",
            fechaCosechaEstimada = "2026-11-20",
            estadoDesarrollo = "Sembrado",
            frecuenciaRiego = "3",
            actorPerfil = "ADMIN"
        )
        advanceUntilIdle()

        assertTrue(resultado)
        assertEquals(2, viewModel.cultivos.size)
        coVerify { repository.guardarCultivo(match { it.nombreComun == "Zanahoria" }, demoSector) }
    }

    @Test
    fun eliminarSectorFallaSiTieneCultivosAsociados() = runTest {
        val viewModel = crearViewModel()
        val resultado = viewModel.eliminarSector(
            sectorId = demoSector.id,
            actorPerfil = "ADMIN"
        )
        advanceUntilIdle()

        assertFalse(resultado)
        assertEquals(1, viewModel.sectores.size)
    }
}
