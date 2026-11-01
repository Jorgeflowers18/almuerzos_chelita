package com.chelita.almuerzos.datos.respaldo

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chelita.almuerzos.datos.AlmuerzosDatabase
import com.chelita.almuerzos.datos.PedidosRepositorio
import com.chelita.almuerzos.dominio.Precios
import com.chelita.almuerzos.dominio.Producto.ALMUERZO
import com.chelita.almuerzos.dominio.Producto.SOPA
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

// Una carpeta de mentira: guarda los archivos en un mapa en memoria.
private class CarpetaEnMemoria : CarpetaDeRespaldos {
    override val nombre = "Respaldos Chelita"
    val archivos = mutableMapOf<String, String>()
    var escrituras = 0

    override fun leer(archivo: String) = archivos[archivo]

    override fun escribir(archivo: String, contenido: String) {
        archivos[archivo] = contenido
        escrituras++
    }
}

private class AccesoEnMemoria(private val carpeta: CarpetaEnMemoria) : AccesoCarpetas {
    override fun conservarPermiso(uri: Uri) = Unit
    override fun abrir(uri: Uri): CarpetaDeRespaldos = carpeta
}

@RunWith(RobolectricTestRunner::class)
class GestorRespaldosTest {

    private val contexto: Context = ApplicationProvider.getApplicationContext()
    private val carpetaElegida = Uri.parse("content://prueba/respaldos")
    private lateinit var db: AlmuerzosDatabase
    private lateinit var repo: PedidosRepositorio
    private lateinit var carpeta: CarpetaEnMemoria
    private lateinit var gestor: GestorRespaldos
    private var hoy = LocalDate.of(2026, 11, 1)
    private var ahora = 1_000_000_000L

    private fun nuevaBaseDeDatos() =
        Room.inMemoryDatabaseBuilder(contexto, AlmuerzosDatabase::class.java).allowMainThreadQueries().build()

    @Before
    fun preparar() {
        db = nuevaBaseDeDatos()
        repo = PedidosRepositorio(db, hoy = { hoy })
        carpeta = CarpetaEnMemoria()
        gestor = GestorRespaldos(contexto, db, repo, AccesoEnMemoria(carpeta), ahora = { ahora })
    }

    @After
    fun cerrar() {
        db.close()
    }

    @Test
    fun `sin carpeta elegida no se respalda nada`() = runTest {
        repo.guardarPedido("Ana", paraLlevar = false, mapOf(ALMUERZO to 1))

        gestor.respaldoAutomatico()

        assertNull(gestor.estado.value.carpeta)
        assertTrue(carpeta.archivos.isEmpty())
        assertThrows(ErrorDeRespaldo::class.java) { runBlocking { gestor.respaldar(TipoRespaldo.AUTOMATICO) } }
    }

    @Test
    fun `al elegir la carpeta se saca el primer respaldo`() = runTest {
        repo.guardarPedido("Ana", paraLlevar = false, mapOf(ALMUERZO to 1))

        assertNull(gestor.elegirCarpeta(carpetaElegida))

        assertEquals("Respaldos Chelita", gestor.estado.value.carpeta)
        assertEquals(ahora, gestor.estado.value.ultimoAutomatico)
        val respaldo = FormatoRespaldo.desdeJson(carpeta.archivos.getValue(TipoRespaldo.AUTOMATICO.archivo))
        assertEquals(listOf("Ana"), respaldo.pedidos.map { it.pedido.nombre })
    }

    @Test
    fun `el respaldo automatico reemplaza el archivo y espera 15 minutos entre uno y otro`() = runTest {
        repo.guardarPedido("Ana", paraLlevar = false, mapOf(ALMUERZO to 1))
        gestor.elegirCarpeta(carpetaElegida)
        repo.guardarPedido("Beto", paraLlevar = false, mapOf(SOPA to 1))

        ahora += 10 * 60 * 1000 // 10 minutos después: todavía no
        gestor.respaldoAutomatico()
        assertEquals(1, carpeta.escrituras)

        ahora += 6 * 60 * 1000 // 16 minutos después del primero: sí
        gestor.respaldoAutomatico()
        assertEquals(2, carpeta.escrituras)

        // Sigue habiendo un solo archivo, ahora con los dos pedidos.
        assertEquals(setOf(TipoRespaldo.AUTOMATICO.archivo), carpeta.archivos.keys)
        val respaldo = FormatoRespaldo.desdeJson(carpeta.archivos.getValue(TipoRespaldo.AUTOMATICO.archivo))
        assertEquals(listOf("Ana", "Beto"), respaldo.pedidos.map { it.pedido.nombre })
    }

    @Test
    fun `un celular vacio no pisa el respaldo que ya habia y ofrece restaurarlo`() = runTest {
        // El celular viejo dejó un respaldo en la carpeta.
        repo.guardarPedido("Ana", paraLlevar = false, mapOf(ALMUERZO to 1))
        gestor.elegirCarpeta(carpetaElegida)
        val respaldoViejo = carpeta.archivos.getValue(TipoRespaldo.AUTOMATICO.archivo)

        // App recién instalada: base de datos vacía, misma carpeta.
        val dbNueva = nuevaBaseDeDatos()
        val gestorNuevo = GestorRespaldos(contexto, dbNueva, PedidosRepositorio(dbNueva), AccesoEnMemoria(carpeta), ahora = { ahora })

        val encontrado = gestorNuevo.elegirCarpeta(carpetaElegida)
        ahora += 60 * 60 * 1000
        gestorNuevo.respaldoAutomatico()

        assertNotNull(encontrado)
        assertEquals(listOf("Ana"), encontrado!!.pedidos.map { it.pedido.nombre })
        assertEquals(respaldoViejo, carpeta.archivos.getValue(TipoRespaldo.AUTOMATICO.archivo))
        dbNueva.close()
    }

    @Test
    fun `antes de limpiar se respaldan los pedidos que se van a borrar`() = runTest {
        hoy = LocalDate.of(2026, 8, 15)
        repo.guardarPedido("Viejo", paraLlevar = false, mapOf(ALMUERZO to 1))
        hoy = LocalDate.of(2026, 11, 1)
        repo.guardarPedido("Nuevo", paraLlevar = false, mapOf(SOPA to 1))
        gestor.elegirCarpeta(carpetaElegida)

        assertEquals(1, gestor.limpiarConRespaldo())

        val antes = FormatoRespaldo.desdeJson(carpeta.archivos.getValue(TipoRespaldo.ANTES_DE_LIMPIAR.archivo))
        assertEquals(listOf("Viejo", "Nuevo"), antes.pedidos.map { it.pedido.nombre })
        assertEquals(ahora, gestor.estado.value.ultimoAntesDeLimpiar)
        assertEquals(1, gestor.pedidosEnEsteCelular())
        // Nunca más de dos archivos.
        assertEquals(TipoRespaldo.entries.map { it.archivo }.toSet(), carpeta.archivos.keys)
    }

    @Test
    fun `si no hay carpeta no se borra nada`() = runTest {
        hoy = LocalDate.of(2026, 8, 15)
        repo.guardarPedido("Viejo", paraLlevar = false, mapOf(ALMUERZO to 1))
        hoy = LocalDate.of(2026, 11, 1)

        assertThrows(ErrorDeRespaldo::class.java) { runBlocking { gestor.limpiarConRespaldo() } }
        assertEquals(1, gestor.pedidosEnEsteCelular())
    }

    @Test
    fun `restaurar reemplaza todos los datos por los del respaldo`() = runTest {
        repo.guardarPedido("Ana", paraLlevar = true, mapOf(ALMUERZO to 2, SOPA to 1), "sin ensalada")
        repo.guardarPrecios(Precios(almuerzo = 300))
        repo.guardarMenu("Seco de pollo", "Locro")
        gestor.elegirCarpeta(carpetaElegida)
        val respaldo = FormatoRespaldo.desdeJson(carpeta.archivos.getValue(TipoRespaldo.AUTOMATICO.archivo))

        // Otro celular con sus propios datos.
        val dbOtra = nuevaBaseDeDatos()
        val repoOtro = PedidosRepositorio(dbOtra, hoy = { hoy })
        repoOtro.guardarPedido("Pedido que se reemplaza", paraLlevar = false, mapOf(SOPA to 3))
        val gestorOtro = GestorRespaldos(contexto, dbOtra, repoOtro, AccesoEnMemoria(CarpetaEnMemoria()), ahora = { ahora })

        gestorOtro.restaurar(respaldo)

        val pedidos = repoOtro.pedidosDeHoy().first()
        assertEquals(listOf("Ana"), pedidos.map { it.pedido.nombre })
        assertEquals(repo.pedidosDeHoy().first(), pedidos)
        assertEquals(300, repoOtro.precios.first().almuerzo)
        assertEquals("Seco de pollo", repoOtro.menuDeHoy().first()!!.segundo)
        // Los pedidos nuevos siguen funcionando después de restaurar.
        repoOtro.guardarPedido("Beto", paraLlevar = false, mapOf(SOPA to 1))
        assertFalse(repoOtro.pedidosDeHoy().first().isEmpty())
        dbOtra.close()
    }
}
