package com.chelita.almuerzos.datos.respaldo

import com.chelita.almuerzos.datos.ConfiguracionEntity
import com.chelita.almuerzos.datos.ContenidoRespaldo
import com.chelita.almuerzos.datos.LineaPedidoEntity
import com.chelita.almuerzos.datos.MenuDiaEntity
import com.chelita.almuerzos.datos.PedidoConLineas
import com.chelita.almuerzos.datos.PedidoEntity
import com.chelita.almuerzos.dominio.EstadoPedido
import com.chelita.almuerzos.dominio.Precios
import com.chelita.almuerzos.dominio.Producto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

// org.json viene con Android, por eso estas pruebas usan Robolectric.
@RunWith(RobolectricTestRunner::class)
class FormatoRespaldoTest {

    private val contenido = ContenidoRespaldo(
        creadoEn = 1_793_000_000_000,
        pedidos = listOf(
            PedidoConLineas(
                PedidoEntity(
                    id = 7,
                    nombre = "María \"la vecina\"",
                    paraLlevar = true,
                    observaciones = "sin ensalada\ny poco arroz",
                    estado = EstadoPedido.PAGADO,
                    fecha = LocalDate.of(2026, 11, 1),
                    creadoEn = 1_792_999_000_000,
                    tarrinasSegundo = 1,
                    tarrinasSopa = 2,
                    envasesProteina = 1,
                    costoTarrinas = 70,
                    total = 520,
                ),
                listOf(
                    LineaPedidoEntity(7, Producto.ALMUERZO_MIXTO, 1, 325),
                    LineaPedidoEntity(7, Producto.SOPA, 1, 50),
                    LineaPedidoEntity(7, Producto.PORCION_PROTEINA, 1, 75),
                ),
            ),
        ),
        configuracion = ConfiguracionEntity.desde(Precios(almuerzo = 275)),
        menus = listOf(MenuDiaEntity(LocalDate.of(2026, 11, 1), "Seco de pollo", "Locro de papa")),
    )

    @Test
    fun `lo que se guarda se lee igual`() {
        assertEquals(contenido, FormatoRespaldo.desdeJson(FormatoRespaldo.aJson(contenido)))
    }

    @Test
    fun `un respaldo sin precios editados ni pedidos tambien se lee`() {
        val vacio = ContenidoRespaldo(creadoEn = 1, pedidos = emptyList(), configuracion = null, menus = emptyList())
        assertEquals(vacio, FormatoRespaldo.desdeJson(FormatoRespaldo.aJson(vacio)))
    }

    @Test
    fun `rechaza archivos que no son respaldos`() {
        val error = assertThrows(ErrorDeRespaldo::class.java) { FormatoRespaldo.desdeJson("hola, esto es una foto") }
        assertTrue(error.message!!.contains("no es un respaldo"))
        assertThrows(ErrorDeRespaldo::class.java) { FormatoRespaldo.desdeJson("""{"app":"otra-app","version":1}""") }
    }

    @Test
    fun `rechaza respaldos de una version mas nueva de la app`() {
        val nuevo = FormatoRespaldo.aJson(contenido).replace("\"version\": 1", "\"version\": 99")
        val error = assertThrows(ErrorDeRespaldo::class.java) { FormatoRespaldo.desdeJson(nuevo) }
        assertTrue(error.message!!.contains("más nueva"))
    }

    @Test
    fun `rechaza respaldos danados`() {
        val productoRaro = FormatoRespaldo.aJson(contenido).replace("ALMUERZO_MIXTO", "PIZZA")
        assertThrows(ErrorDeRespaldo::class.java) { FormatoRespaldo.desdeJson(productoRaro) }

        val fechaRara = FormatoRespaldo.aJson(contenido).replace("2026-11-01", "ayer")
        assertThrows(ErrorDeRespaldo::class.java) { FormatoRespaldo.desdeJson(fechaRara) }

        val cortado = FormatoRespaldo.aJson(contenido).dropLast(40)
        assertThrows(ErrorDeRespaldo::class.java) { FormatoRespaldo.desdeJson(cortado) }
    }
}
