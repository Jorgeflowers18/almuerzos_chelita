package com.chelita.almuerzos.datos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

// Todos los datos de la app en un momento dado: lo que va dentro de un archivo de respaldo.
data class ContenidoRespaldo(
    val creadoEn: Long,
    val pedidos: List<PedidoConLineas>,
    val configuracion: ConfiguracionEntity?,
    val menus: List<MenuDiaEntity>,
)

// Consultas para sacar y restaurar respaldos.
@Dao
abstract class RespaldoDao {

    // Lee todo dentro de una transacción, para que el respaldo sea una "foto" consistente
    // aunque alguien esté guardando un pedido en ese momento.
    @Transaction
    open suspend fun leerTodo(creadoEn: Long) = ContenidoRespaldo(
        creadoEn = creadoEn,
        pedidos = pedidos(),
        configuracion = configuracion(),
        menus = menus(),
    )

    @Query("SELECT COUNT(*) FROM pedidos")
    abstract suspend fun contarPedidos(): Int

    // Borra todo y pone en su lugar los datos del respaldo. Si algo falla a la mitad,
    // la transacción deshace todo y los datos anteriores quedan intactos.
    @Transaction
    open suspend fun reemplazarTodo(contenido: ContenidoRespaldo) {
        borrarPedidos() // las líneas se borran solas (CASCADE)
        borrarConfiguracion()
        borrarMenus()
        insertarPedidos(contenido.pedidos.map { it.pedido })
        insertarLineas(contenido.pedidos.flatMap { it.lineas })
        contenido.configuracion?.let { insertarConfiguracion(it) }
        insertarMenus(contenido.menus)
    }

    @Transaction
    @Query("SELECT * FROM pedidos ORDER BY id")
    protected abstract suspend fun pedidos(): List<PedidoConLineas>

    @Query("SELECT * FROM configuracion WHERE id = ${ConfiguracionEntity.UNICA}")
    protected abstract suspend fun configuracion(): ConfiguracionEntity?

    @Query("SELECT * FROM menu_dia ORDER BY fecha")
    protected abstract suspend fun menus(): List<MenuDiaEntity>

    @Query("DELETE FROM pedidos")
    protected abstract suspend fun borrarPedidos()

    @Query("DELETE FROM configuracion")
    protected abstract suspend fun borrarConfiguracion()

    @Query("DELETE FROM menu_dia")
    protected abstract suspend fun borrarMenus()

    @Insert
    protected abstract suspend fun insertarPedidos(pedidos: List<PedidoEntity>)

    @Insert
    protected abstract suspend fun insertarLineas(lineas: List<LineaPedidoEntity>)

    @Insert
    protected abstract suspend fun insertarConfiguracion(configuracion: ConfiguracionEntity)

    @Insert
    protected abstract suspend fun insertarMenus(menus: List<MenuDiaEntity>)
}
