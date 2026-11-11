package com.example.huertolindo

import com.example.huertolindo.domain.model.EstadoCultivo
import com.example.huertolindo.domain.security.ReglasHuerto
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReglasHuertoTest {
    @Test
    fun frecuenciaDeRiegoDebeSerUnNumeroPositivoDeDias() {
        assertTrue(ReglasHuerto.frecuenciaRiegoValida("1"))
        assertTrue(ReglasHuerto.frecuenciaRiegoValida("14"))
        assertFalse(ReglasHuerto.frecuenciaRiegoValida(""))
        assertFalse(ReglasHuerto.frecuenciaRiegoValida("0"))
        assertFalse(ReglasHuerto.frecuenciaRiegoValida("-2"))
        assertFalse(ReglasHuerto.frecuenciaRiegoValida("cada semana"))
    }

    @Test
    fun cultivosAceptanEstadosDePlanificacionYFinDeCiclo() {
        assertTrue(EstadoCultivo.esValido("Planificado"))
        assertTrue(EstadoCultivo.esValido("Sembrado"))
        assertTrue(EstadoCultivo.esValido("Cosechado"))
        assertFalse(EstadoCultivo.esValido("Pendiente"))
    }
}
