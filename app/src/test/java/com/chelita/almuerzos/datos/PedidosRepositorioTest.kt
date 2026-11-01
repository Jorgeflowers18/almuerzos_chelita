package com.chelita.almuerzos.datos

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.chelita.almuerzos.dominio.EstadoPedido
import com.chelita.almuerzos.dominio.Precios
import com.chelita.almuerzos.dominio.Producto.ALMUERZO
import com.chelita.almuerzos.dominio.Producto.PORCION_PROTEINA
import com.chelita.almuerzos.dominio.Producto.SEGUNDO
import com.chelita.almuerzos.dominio.Producto.SOPA
import com.chelita.almuerzos.dominio.Rango
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

// Pruebas de la base de datos. Robolectric simula Android en la PC y la base de datos
// se crea en memoria, así que cada prueba empieza vacía y no deja archivos.
@RunWith(RobolectricTestRunner::class)
class PedidosRepositorioTest {

    private lateinit var db: AlmuerzosDatabase
    private lateinit var repo: PedidosRepositorio
    private var hoy = LocalDate.of(2026, 11, 2)

    @Before
    fun crearBaseDeDatos() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AlmuerzosDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = PedidosRepositorio(db, hoy = { hoy })
    }

    @After
    fun cerrarBaseDeDatos() {
        db.close()
    }

    @Test
    fun `guarda un pedido nuevo con su cobro calculado`() = runTest {
        val id = repo.guardarPedido("  María ", paraLlevar = true, mapOf(ALMUERZO to 2, SOPA to 1), "sin ensalada")

        val guardado = repo.obtener(id)!!
        assertEquals("María", guardado.pedido.nombre)
        assertEquals("sin ensalada", guardado.pedido.observaciones)
        assertEquals(EstadoPedido.PENDIENTE, guardado.pedido.estado)
        assertEquals(hoy, guardado.pedido.fecha)
        assertEquals(mapOf(ALMUERZO to 2, SOPA to 1), guardado.cantidades)
        assertEquals(2 * 25 + 3 * 15, guardado.pedido.costoTarrinas)
        assertEquals(550 + 95, guardado.pedido.total)
    }

    @Test
    fun `usa precios por defecto hasta que se guarden otros`() = runTest {
        assertEquals(Precios(), repo.precios.first())

        repo.guardarPrecios(Precios(almuerzo = 300))
        assertEquals(300, repo.precios.first().almuerzo)

        val id = repo.guardarPedido("Ana", paraLlevar = false, mapOf(ALMUERZO to 1))
        assertEquals(300, repo.obtener(id)!!.pedido.total)
    }

    @Test
    fun `un pedido viejo conserva su precio aunque cambien los precios`() = runTest {
        val id = repo.guardarPedido("Luis", paraLlevar = false, mapOf(ALMUERZO to 1))
        repo.guardarPrecios(Precios(almuerzo = 300))

        val guardado = repo.obtener(id)!!
        assertEquals(250, guardado.pedido.total)
        assertEquals(250, guardado.lineas.single().precioUnitario)
    }

    @Test
    fun `editar un pedido reemplaza sus productos y conserva estado y fecha`() = runTest {
        val id = repo.guardarPedido("Rosa", paraLlevar = false, mapOf(ALMUERZO to 1, SOPA to 1))
        repo.cambiarEstado(id, EstadoPedido.SERVIDO)

        hoy = hoy.plusDays(1)
        repo.guardarPedido("Rosa", paraLlevar = true, mapOf(SEGUNDO to 1, PORCION_PROTEINA to 2), id = id)

        val editado = repo.obtener(id)!!
        assertEquals(mapOf(SEGUNDO to 1, PORCION_PROTEINA to 2), editado.cantidades)
        assertEquals(EstadoPedido.SERVIDO, editado.pedido.estado)
        assertEquals(LocalDate.of(2026, 11, 2), editado.pedido.fecha)
        assertEquals(200 + 150 + 25 + 15, editado.pedido.total)
    }

    @Test
    fun `al editar se puede corregir el estado`() = runTest {
        val id = repo.guardarPedido("Rosa", paraLlevar = false, mapOf(ALMUERZO to 1))
        repo.cambiarEstado(id, EstadoPedido.PAGADO)

        repo.guardarPedido("Rosa", paraLlevar = false, mapOf(ALMUERZO to 1), id = id, estado = EstadoPedido.PENDIENTE)

        assertEquals(EstadoPedido.PENDIENTE, repo.obtener(id)!!.pedido.estado)
    }

    @Test
    fun `pedidos de hoy trae solo los del dia en orden de llegada`() = runTest {
        repo.guardarPedido("Ayer", paraLlevar = false, mapOf(SOPA to 1))
        hoy = hoy.plusDays(1)
        repo.guardarPedido("Primero", paraLlevar = false, mapOf(SOPA to 1))
        repo.guardarPedido("Segundo", paraLlevar = false, mapOf(SOPA to 1))

        val nombres = repo.pedidosDeHoy().first().map { it.pedido.nombre }
        assertEquals(listOf("Primero", "Segundo"), nombres)
    }

    @Test
    fun `pedidos entre dos fechas incluye el primer y el ultimo dia`() = runTest {
        hoy = LocalDate.of(2026, 10, 31)
        repo.guardarPedido("Octubre", paraLlevar = false, mapOf(SOPA to 1))
        hoy = LocalDate.of(2026, 11, 1)
        repo.guardarPedido("Primero", paraLlevar = false, mapOf(SOPA to 1))
        hoy = LocalDate.of(2026, 11, 30)
        repo.guardarPedido("Ultimo", paraLlevar = false, mapOf(SOPA to 1))
        hoy = LocalDate.of(2026, 12, 1)
        repo.guardarPedido("Diciembre", paraLlevar = false, mapOf(SOPA to 1))

        val noviembre = Rango(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30))
        val nombres = repo.pedidosEntre(noviembre).first().map { it.pedido.nombre }
        assertEquals(listOf("Primero", "Ultimo"), nombres)
    }

    @Test
    fun `observar un pedido entrega sus cambios y null si se elimina`() = runTest {
        val id = repo.guardarPedido("Lucía", paraLlevar = false, mapOf(ALMUERZO to 1))
        assertEquals(EstadoPedido.PENDIENTE, repo.observar(id).first()!!.pedido.estado)

        repo.cambiarEstado(id, EstadoPedido.PAGADO)
        assertEquals(EstadoPedido.PAGADO, repo.observar(id).first()!!.pedido.estado)

        repo.eliminar(id)
        assertNull(repo.observar(id).first())
    }

    @Test
    fun `cambia el estado de un pedido`() = runTest {
        val id = repo.guardarPedido("Pedro", paraLlevar = false, mapOf(ALMUERZO to 1))
        repo.cambiarEstado(id, EstadoPedido.PAGADO)
        assertEquals(EstadoPedido.PAGADO, repo.obtener(id)!!.pedido.estado)
    }

    @Test
    fun `eliminar un pedido borra tambien sus lineas`() = runTest {
        val id = repo.guardarPedido("Borrar", paraLlevar = false, mapOf(ALMUERZO to 1, SOPA to 2))
        repo.eliminar(id)

        assertNull(repo.obtener(id))
        val cursor = db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM lineas_pedido")
        cursor.moveToFirst()
        assertEquals(0, cursor.getInt(0))
        cursor.close()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `no se guarda un pedido sin nombre`() = runTest {
        repo.guardarPedido("   ", paraLlevar = false, mapOf(ALMUERZO to 1))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `no se guarda un pedido sin productos`() = runTest {
        repo.guardarPedido("Vacío", paraLlevar = false, mapOf(ALMUERZO to 0))
    }
}
