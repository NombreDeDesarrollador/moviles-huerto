package com.example.huertolindo

import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.huertolindo.data.repository.RoomHuertoRepository
import com.example.huertolindo.presentation.media.crearUriFotografia
import com.example.huertolindo.presentation.media.eliminarFotografiaTemporal
import kotlinx.coroutines.runBlocking
import java.io.File

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.huertolindo", appContext.packageName)
    }

    @Test
    fun guardaUnaImagenValidaDesdeFileProvider() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val uri = crearUriFotografia(context)
        val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        var evidenciaPath: String? = null

        try {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
            } ?: fail("No se pudo abrir el archivo de prueba.")

            val evidencia = RoomHuertoRepository(context).guardarEvidenciaLocal(
                tareaId = "tarea-prueba",
                comentario = "Prueba de imagen",
                imagenUri = uri.toString()
            )
            evidenciaPath = evidencia.rutaArchivo

            assertTrue(File(evidencia.rutaArchivo).isFile)
        } finally {
            bitmap.recycle()
            evidenciaPath?.let { File(it).delete() }
            eliminarFotografiaTemporal(context, uri)
        }
    }
}