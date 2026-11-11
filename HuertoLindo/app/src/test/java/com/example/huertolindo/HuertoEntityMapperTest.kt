package com.example.huertolindo

import com.example.huertolindo.data.local.mapper.toDomain
import com.example.huertolindo.data.local.mapper.toEntity
import com.example.huertolindo.domain.model.ActividadTarea
import com.example.huertolindo.domain.model.Cultivo
import com.example.huertolindo.domain.model.Evidencia
import com.example.huertolindo.domain.model.ReporteProblema
import com.example.huertolindo.domain.model.Sector
import com.example.huertolindo.domain.model.Tarea
import com.example.huertolindo.domain.model.Usuario
import com.example.huertolindo.domain.model.Validacion
import org.junit.Assert.assertEquals
import org.junit.Test

class HuertoEntityMapperTest {
    @Test
    fun domainModelsRoundTripThroughRoomEntities() {
        val actividad = ActividadTarea("a", "t", "Tarea", "u1", "u2", "Usuario", "Creó", "Pendiente", "fecha")
        val cultivo = Cultivo("c", "Lechuga", "s", "siembra", "Desarrollo", "Diario", "cosecha")
        val evidencia = Evidencia("e", "t", "c", "r", "foto", "/foto.jpg", "nota", "fecha")
        val reporte = ReporteProblema("r", "s", "c", "u", "Descripción", "Alta", "Pendiente", "fecha")
        val sector = Sector("s", "Sector", "Descripción", "En uso", "Patio")
        val tarea = Tarea("t", "Tarea", "Descripción", "s", "u", "Pendiente", "fecha", "motivo", "Riego")
        val usuario = Usuario("u", "Nombre", "correo@example.com", "estudiante", "General", "Apellido", "salt", "hash")
        val validacion = Validacion("v", "t", "u", "Aprobada", "Observación", "fecha")

        assertEquals(actividad, actividad.toEntity().toDomain())
        assertEquals(cultivo, cultivo.toEntity().toDomain())
        assertEquals(evidencia, evidencia.toEntity().toDomain())
        assertEquals(reporte, reporte.toEntity().toDomain())
        assertEquals(sector, sector.toEntity().toDomain())
        assertEquals(tarea, tarea.toEntity().toDomain())
        assertEquals(usuario, usuario.toEntity().toDomain())
        assertEquals(validacion, validacion.toEntity().toDomain())
    }
}
