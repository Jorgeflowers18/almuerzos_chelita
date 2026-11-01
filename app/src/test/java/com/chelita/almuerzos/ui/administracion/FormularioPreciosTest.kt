package com.chelita.almuerzos.ui.administracion

import com.chelita.almuerzos.dominio.Precios
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class FormularioPreciosTest {

    @Test
    fun `muestra los precios guardados y los devuelve igual`() {
        val formulario = FormularioPrecios.desde(Precios())

        assertEquals("2.50", formulario.monto(CampoPrecio.ALMUERZO))
        assertEquals("0.15", formulario.monto(CampoPrecio.TARRINA_SOPA))
        assertEquals("3", formulario.porcionesPorEnvase)
        assertEquals(Precios(), formulario.aPrecios())
    }

    @Test
    fun `toma los precios como los escribe la duena`() {
        val formulario = FormularioPrecios.desde(Precios())
            .cambiar(CampoPrecio.ALMUERZO, "3")
            .cambiar(CampoPrecio.TARRINA_SEGUNDO, "0,30")
            .copy(porcionesPorEnvase = " 4 ")

        assertEquals(
            Precios(almuerzo = 300, tarrinaSegundo = 30, porcionesPorEnvase = 4),
            formulario.aPrecios(),
        )
    }

    @Test
    fun `no se puede guardar con un precio mal escrito o vacio`() {
        val base = FormularioPrecios.desde(Precios())

        assertNull(base.cambiar(CampoPrecio.SOPA, "").aPrecios())
        assertNull(base.cambiar(CampoPrecio.SOPA, "cincuenta").aPrecios())
        assertFalse(base.cambiar(CampoPrecio.SOPA, "0.5.0").montoValido(CampoPrecio.SOPA))
    }

    @Test
    fun `las porciones por envase van de 1 a 10`() {
        val base = FormularioPrecios.desde(Precios())

        assertNull(base.copy(porcionesPorEnvase = "0").aPrecios())
        assertNull(base.copy(porcionesPorEnvase = "11").aPrecios())
        assertNull(base.copy(porcionesPorEnvase = "dos").aPrecios())
        assertEquals(1, base.copy(porcionesPorEnvase = "1").aPrecios()!!.porcionesPorEnvase)
    }
}
