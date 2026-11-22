package com.example.huertolindo

import android.app.Application
import com.example.huertolindo.data.local.ActividadTareaDao
import com.example.huertolindo.data.local.CultivoDao
import com.example.huertolindo.data.local.EvidenciaDao
import com.example.huertolindo.data.local.HuertoDatabase
import com.example.huertolindo.data.local.HuertoDatabaseProvider
import com.example.huertolindo.data.local.ReporteProblemaDao
import com.example.huertolindo.data.local.SectorDao
import com.example.huertolindo.data.local.TareaDao
import com.example.huertolindo.data.local.UsuarioDao
import com.example.huertolindo.data.local.ValidacionDao
import com.example.huertolindo.data.local.entity.UsuarioEntity
import com.example.huertolindo.data.remote.HuertoApiService
import com.example.huertolindo.data.remote.BackendAuthResponseDto
import com.example.huertolindo.data.remote.BackendSectorDto
import com.example.huertolindo.data.remote.BackendUsuarioDto
import com.example.huertolindo.data.remote.LoginRequestDto
import com.example.huertolindo.data.repository.RoomHuertoRepository
import com.example.huertolindo.domain.model.Sector
import com.example.huertolindo.domain.security.PasswordHasher
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
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class RoomHuertoRepositoryTest {

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

    private lateinit var repository: RoomHuertoRepository

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

        repository = RoomHuertoRepository(application, api)
    }

    @After
    fun tearDown() {
        unmockkObject(HuertoDatabaseProvider)
    }

    @Test
    fun autenticarLlamaAServidorYActualizaBaseLocal() = runTest {
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
        coEvery { usuarioDao.obtenerPorCorreo("docente@huertolindo.cl") } returns null
        coEvery { usuarioDao.asegurar(any()) } returns Unit

        val usuario = repository.autenticar("docente@huertolindo.cl", "HuertoDocente2026!")

        assertNotNull(usuario)
        assertEquals("1", usuario?.id)
        assertEquals(PermisosHuerto.ADMINISTRADOR, usuario?.perfil)
        coVerify { api.login(match { it.correo == "docente@huertolindo.cl" }) }
        coVerify { usuarioDao.asegurar(match { it.id == "1" }) }
    }

    @Test
    fun autenticarHaceFallbackALocalCuandoFallaServidor() = runTest {
        coEvery { api.login(any()) } throws IOException("Error de red simulado")

        val salt = PasswordHasher.createSalt()
        val saltB64 = PasswordHasher.encodeBase64(salt)
        val hash = PasswordHasher.hash("Pass123!", salt)
        val hashB64 = PasswordHasher.encodeBase64(hash)

        val localUserValid = UsuarioEntity(
            id = "usr-offline",
            nombre = "Estudiante",
            apellido = "Local",
            correo = "estudiante1@huertolindo.cl",
            perfil = "estudiante",
            tallerAsignado = "Taller A",
            passwordSalt = saltB64,
            passwordHash = hashB64
        )
        coEvery { usuarioDao.obtenerPorCorreo("estudiante1@huertolindo.cl") } returns localUserValid

        val usuario = repository.autenticar("estudiante1@huertolindo.cl", "Pass123!")

        assertNotNull(usuario)
        assertEquals("usr-offline", usuario?.id)
    }

    @Test
    fun guardarSectorPersisteEnServidorYRoom() = runTest {
        val sector = Sector(
            id = "sec-1",
            nombre = "Sector Hortalizas",
            descripcion = "Zona de prueba",
            estado = "Disponible",
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
        coEvery { sectorDao.insertar(any()) } returns Unit

        val guardado = repository.guardarSector(sector)

        assertEquals("15", guardado.id)
        coVerify { api.guardarSectorBackend(match { it.nombre == "Sector Hortalizas" }) }
        coVerify { sectorDao.insertar(match { it.id == "15" }) }
    }

    @Test
    fun guardarSectorNoAparentaExitoSiFallaServidor() = runTest {
        val sector = Sector(
            id = "sec-offline",
            nombre = "Sector Offline",
            descripcion = "Sin red",
            estado = "Disponible",
            ubicacion = "Sur"
        )
        coEvery { api.guardarSectorBackend(any()) } throws IOException("Sin conexion")

        val error = runCatching { repository.guardarSector(sector) }.exceptionOrNull()

        assertNotNull(error)
        coVerify(exactly = 0) { sectorDao.insertar(any()) }
    }

    @Test
    fun eliminarSectorEliminaEnServidorYLocal() = runTest {
        val sector = Sector("sec-del", "Borrar", "Desc", "Disponible", "Sur")
        val voidResponse = mockk<Response<Void>>(relaxed = true) {
            every { isSuccessful } returns true
        }
        coEvery { api.eliminarSector("sec-del") } returns voidResponse
        coEvery { sectorDao.eliminar(any()) } returns Unit

        repository.eliminarSector(sector)

        coVerify { api.eliminarSector("sec-del") }
        coVerify { sectorDao.eliminar(match { it.id == "sec-del" }) }
    }

}
