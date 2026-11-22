package com.example.huertolindo

import android.app.Application
import com.example.huertolindo.data.local.ActividadTareaDao
import com.example.huertolindo.data.local.CultivoDao
import com.example.huertolindo.data.local.EvidenciaDao
import com.example.huertolindo.data.local.HuertoDatabase
import com.example.huertolindo.data.local.HuertoDatabaseProvider
import com.example.huertolindo.data.local.MigracionDao
import com.example.huertolindo.data.local.ReporteProblemaDao
import com.example.huertolindo.data.local.SectorDao
import com.example.huertolindo.data.local.TareaDao
import com.example.huertolindo.data.local.UsuarioDao
import com.example.huertolindo.data.local.ValidacionDao
import com.example.huertolindo.data.remote.HuertoApiService
import com.example.huertolindo.data.remote.BackendAuthResponseDto
import com.example.huertolindo.data.remote.BackendSectorDto
import com.example.huertolindo.data.remote.BackendUsuarioDto
import com.example.huertolindo.data.remote.LoginRequestDto
import com.example.huertolindo.data.repository.ApiHuertoRepository
import com.example.huertolindo.domain.model.Sector
import com.example.huertolindo.domain.model.Usuario
import com.example.huertolindo.domain.security.PermisosHuerto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ApiHuertoRepositoryTest {

    private val application = mockk<Application>(relaxed = true)
    private val api = mockk<HuertoApiService>(relaxed = true)
    private val database = mockk<HuertoDatabase>(relaxed = true)

    private val usuarioDao = mockk<UsuarioDao>(relaxed = true)
    private val sectorDao = mockk<SectorDao>(relaxed = true)
    private val cultivoDao = mockk<CultivoDao>(relaxed = true)
    private val tareaDao = mockk<TareaDao>(relaxed = true)
    private val evidenciaDao = mockk<EvidenciaDao>(relaxed = true)
    private val validacionDao = mockk<ValidacionDao>(relaxed = true)
    private val reporteDao = mockk<ReporteProblemaDao>(relaxed = true)
    private val actividadDao = mockk<ActividadTareaDao>(relaxed = true)
    private val migracionDao = mockk<MigracionDao>(relaxed = true)

    private lateinit var repository: ApiHuertoRepository

    @Before
    fun setUp() {
        mockkObject(HuertoDatabaseProvider)
        every { HuertoDatabaseProvider.getDatabase(any()) } returns database

        every { database.usuarioDao() } returns usuarioDao
        every { database.sectorDao() } returns sectorDao
        every { database.cultivoDao() } returns cultivoDao
        every { database.tareaDao() } returns tareaDao
        every { database.evidenciaDao() } returns evidenciaDao
        every { database.validacionDao() } returns validacionDao
        every { database.reporteProblemaDao() } returns reporteDao
        every { database.actividadTareaDao() } returns actividadDao
        every { database.migracionDao() } returns migracionDao
        coEvery { migracionDao.estaCompletada() } returns true

        repository = ApiHuertoRepository(application, api)
    }

    @After
    fun tearDown() {
        unmockkObject(HuertoDatabaseProvider)
    }

    @Test
    fun autenticarLlamaAlServidorSinReplicarEnRoom() = runTest {
        val usuarioDto = BackendAuthResponseDto(
            usuario = BackendUsuarioDto(
                id = 1L,
                nombre = "Docente Huerto",
                correo = "docente@huertolindo.cl",
                rol = "ADMIN",
                activo = true
            )
        )
        coEvery { api.login(any()) } returns Response.success(usuarioDto)
        val usuario = repository.autenticar("docente@huertolindo.cl", "HuertoDocente2026!")

        assertNotNull(usuario)
        assertEquals("1", usuario?.id)
        assertEquals(PermisosHuerto.ADMINISTRADOR, usuario?.perfil)
        coVerify { api.login(match { it.correo == "docente@huertolindo.cl" }) }
        coVerify(exactly = 0) { usuarioDao.asegurar(any()) }
    }

    @Test
    fun autenticarFallaSinUsarUsuariosLocalesCuandoElServidorNoEstaDisponible() = runTest {
        coEvery { api.login(any()) } throws IOException("Error de red simulado")

        val error = runCatching {
            repository.autenticar("estudiante1@huertolindo.cl", "Pass123!")
        }.exceptionOrNull()

        assertTrue(error is IOException)
        coVerify(exactly = 0) { usuarioDao.obtenerPorCorreo(any()) }
    }

    @Test
    fun obtenerUsuariosFallaSinFallbackLocal() = runTest {
        coEvery { api.obtenerUsuariosBackend() } throws IOException("Sin conexión")

        val error = runCatching { repository.obtenerUsuarios() }.exceptionOrNull()

        assertTrue(error is IOException)
        coVerify(exactly = 0) { usuarioDao.obtenerTodos() }
    }

    @Test
    fun migracionFallidaNoBorraNiMarcaDatosLocales() = runTest {
        coEvery { api.login(any()) } returns Response.success(
            BackendAuthResponseDto(
                BackendUsuarioDto(1, "Docente", "docente@huertolindo.cl", "ADMIN", true)
            )
        )
        coEvery { migracionDao.estaCompletada() } returns false
        coEvery { usuarioDao.obtenerTodos() } returns emptyList()
        coEvery { sectorDao.obtenerTodos() } returns listOf(
            com.example.huertolindo.data.local.entity.SectorEntity(
                id = "local-sector",
                nombre = "Sector real",
                descripcion = "Existente",
                estado = "En uso",
                ubicacion = "Patio"
            )
        )
        coEvery { cultivoDao.obtenerTodos() } returns emptyList()
        coEvery { tareaDao.obtenerTodos() } returns emptyList()
        coEvery { reporteDao.obtenerTodos() } returns emptyList()
        coEvery { evidenciaDao.obtenerTodos() } returns emptyList()
        coEvery { validacionDao.obtenerTodos() } returns emptyList()
        coEvery { actividadDao.obtenerTodos() } returns emptyList()
        coEvery { api.obtenerUsuariosBackend() } throws IOException("Backend fuera de línea")

        val usuario = repository.autenticar("docente@huertolindo.cl", "password")

        assertNotNull(usuario)
        assertEquals("1", usuario?.id)
        coVerify(exactly = 0) { migracionDao.marcarCompletada() }
        coVerify(exactly = 0) { sectorDao.eliminarTodos() }
        coVerify(exactly = 0) { usuarioDao.eliminarTodos() }
    }

    @Test
    fun guardarSectorUsaLaRespuestaRemotaSinGuardarEnRoom() = runTest {
        val sector = Sector(
            id = "sec-1",
            nombre = "Sector Hortalizas",
            descripcion = "Zona de prueba",
            estado = "En uso",
            ubicacion = "Norte"
        )
        coEvery { api.guardarSectorBackend(any()) } returns Response.success(
            BackendSectorDto(
                id = 15L,
                nombre = "Sector Hortalizas",
                descripcion = "Zona de prueba",
                ubicacion = "Norte"
            )
        )
        val guardado = repository.guardarSector(sector)

        assertEquals("15", guardado.id)
        coVerify { api.guardarSectorBackend(match { it.nombre == "Sector Hortalizas" }) }
        coVerify(exactly = 0) { sectorDao.insertar(any()) }
    }

    @Test
    fun guardarSectorNoAparentaExitoSiFallaServidor() = runTest {
        val sector = Sector(
            id = "sec-offline",
            nombre = "Sector Offline",
            descripcion = "Sin red",
            estado = "En uso",
            ubicacion = "Sur"
        )
        coEvery { api.guardarSectorBackend(any()) } throws IOException("Sin conexion")

        val error = runCatching { repository.guardarSector(sector) }.exceptionOrNull()

        assertNotNull(error)
        coVerify(exactly = 0) { sectorDao.insertar(any()) }
    }

    @Test
    fun guardarSectorRechazaCamposQueLaApiNoPuedePersistir() = runTest {
        val sector = Sector(
            id = "sec-1",
            nombre = "Sector Disponible",
            descripcion = "Falta soporte del estado",
            estado = "Disponible",
            ubicacion = "Sur"
        )

        val error = runCatching { repository.guardarSector(sector) }.exceptionOrNull()

        assertTrue(error is IOException)
        coVerify(exactly = 0) { api.guardarSectorBackend(any()) }
        coVerify(exactly = 0) { sectorDao.insertar(any()) }
    }

    @Test
    fun registrarUsuarioNoAparentaExitoSinBackendConfigurado() = runTest {
        val repositorySinBackend = ApiHuertoRepository(application, null)
        val usuario = Usuario(
            id = "usr-1",
            nombre = "Estudiante",
            correo = "estudiante@huertolindo.cl",
            perfil = PermisosHuerto.ESTUDIANTE,
            tallerAsignado = "Taller Verde"
        )

        val error = runCatching {
            repositorySinBackend.registrarUsuario(usuario, "Password123!")
        }.exceptionOrNull()

        assertTrue(error is IOException)
        coVerify(exactly = 0) { usuarioDao.insertar(any()) }
    }

    @Test
    fun eliminarSectorSoloSolicitaEliminacionRemota() = runTest {
        val sector = Sector("sec-del", "Borrar", "Desc", "Disponible", "Sur")
        val voidResponse = mockk<Response<Void>>(relaxed = true) {
            every { isSuccessful } returns true
        }
        coEvery { api.eliminarSector("sec-del") } returns voidResponse
        repository.eliminarSector(sector)

        coVerify { api.eliminarSector("sec-del") }
        coVerify(exactly = 0) { sectorDao.eliminar(any()) }
    }

}
