package com.chelita.almuerzos.datos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.chelita.almuerzos.dominio.EstadoPedido
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

// DAO = "Data Access Object": las consultas a la base de datos.
// Room escribe el código de cada función a partir de su anotación (@Query, @Insert...).
//
// - Las funciones que devuelven Flow se "suscriben": cada vez que cambian los datos,
//   entregan la lista nueva y la pantalla se actualiza sola.
// - Las funciones "suspend" se ejecutan en segundo plano para no congelar la pantalla.
@Dao
abstract class PedidoDao {

    @Transaction
    @Query("SELECT * FROM pedidos WHERE fecha = :fecha ORDER BY creadoEn")
    abstract fun observarDelDia(fecha: LocalDate): Flow<List<PedidoConLineas>>

    // Los pedidos entre dos fechas (ambas incluidas), para el resumen.
    @Transaction
    @Query("SELECT * FROM pedidos WHERE fecha BETWEEN :desde AND :hasta ORDER BY fecha, creadoEn")
    abstract fun observarEntre(desde: LocalDate, hasta: LocalDate): Flow<List<PedidoConLineas>>

    @Transaction
    @Query("SELECT * FROM pedidos WHERE id = :id")
    abstract suspend fun obtener(id: Long): PedidoConLineas?

    // Igual que obtener, pero avisa cada vez que el pedido cambia (o null si se borró).
    @Transaction
    @Query("SELECT * FROM pedidos WHERE id = :id")
    abstract fun observar(id: Long): Flow<PedidoConLineas?>

    @Query("UPDATE pedidos SET estado = :estado WHERE id = :id")
    abstract suspend fun cambiarEstado(id: Long, estado: EstadoPedido)

    @Query("DELETE FROM pedidos WHERE id = :id")
    abstract suspend fun eliminar(id: Long)

    @Query("SELECT COUNT(*) FROM pedidos WHERE fecha < :fecha")
    abstract fun contarAntesDe(fecha: LocalDate): Flow<Int>

    // Borra los pedidos viejos (sus líneas se borran solas) y devuelve cuántos borró.
    @Query("DELETE FROM pedidos WHERE fecha < :fecha")
    abstract suspend fun borrarAntesDe(fecha: LocalDate): Int

    // Guarda el pedido con sus líneas dentro de una transacción: o se guarda todo o nada,
    // así nunca queda un pedido a medias si algo falla.
    @Transaction
    open suspend fun guardar(pedido: PedidoEntity, lineas: List<LineaPedidoEntity>): Long {
        val id = if (pedido.id == 0L) {
            insertar(pedido)
        } else {
            actualizar(pedido)
            borrarLineas(pedido.id)
            pedido.id
        }
        insertarLineas(lineas.map { it.copy(pedidoId = id) })
        return id
    }

    @Insert
    protected abstract suspend fun insertar(pedido: PedidoEntity): Long

    @Update
    protected abstract suspend fun actualizar(pedido: PedidoEntity)

    @Insert
    protected abstract suspend fun insertarLineas(lineas: List<LineaPedidoEntity>)

    @Query("DELETE FROM lineas_pedido WHERE pedidoId = :pedidoId")
    protected abstract suspend fun borrarLineas(pedidoId: Long)
}
