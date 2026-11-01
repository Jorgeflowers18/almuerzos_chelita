package com.chelita.almuerzos.datos

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.chelita.almuerzos.dominio.EstadoPedido
import com.chelita.almuerzos.dominio.Precios
import com.chelita.almuerzos.dominio.Producto
import java.time.LocalDate

// Cada @Entity es una tabla de la base de datos y cada propiedad es una columna.
// Todo el dinero está en centavos.

// Un pedido. Los totales se guardan ya calculados (una "foto" del momento), para que
// los pedidos viejos no cambien si después se modifican los precios.
@Entity(tableName = "pedidos", indices = [Index("fecha")])
data class PedidoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val paraLlevar: Boolean,
    val observaciones: String,
    val estado: EstadoPedido,
    val fecha: LocalDate,
    val creadoEn: Long,
    val tarrinasSegundo: Int,
    val tarrinasSopa: Int,
    val envasesProteina: Int,
    val costoTarrinas: Int,
    val total: Int,
)

// Un producto dentro de un pedido: "2 x Almuerzo a $2.50".
// Si se borra el pedido, sus líneas se borran solas (CASCADE).
@Entity(
    tableName = "lineas_pedido",
    primaryKeys = ["pedidoId", "producto"],
    foreignKeys = [
        ForeignKey(
            entity = PedidoEntity::class,
            parentColumns = ["id"],
            childColumns = ["pedidoId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class LineaPedidoEntity(
    val pedidoId: Long,
    val producto: Producto,
    val cantidad: Int,
    val precioUnitario: Int,
)

// No es una tabla: es un pedido junto con sus líneas, que Room arma automáticamente.
data class PedidoConLineas(
    @Embedded val pedido: PedidoEntity,
    @Relation(parentColumn = "id", entityColumn = "pedidoId")
    val lineas: List<LineaPedidoEntity>,
) {
    // Las cantidades pedidas en el formato que usa CalculadoraCobro: {ALMUERZO=2, SOPA=1}
    val cantidades: Map<Producto, Int> get() = lineas.associate { it.producto to it.cantidad }
}

// Precios vigentes. La tabla tiene una sola fila (id = 1).
@Entity(tableName = "configuracion")
data class ConfiguracionEntity(
    @PrimaryKey val id: Int = UNICA,
    val almuerzo: Int,
    val segundo: Int,
    val sopa: Int,
    val proteinaExtra: Int,
    val tarrinaSegundo: Int,
    val tarrinaSopa: Int,
    val envaseProteina: Int,
    val porcionesPorEnvase: Int,
) {
    fun aPrecios() = Precios(
        almuerzo = almuerzo,
        segundo = segundo,
        sopa = sopa,
        proteinaExtra = proteinaExtra,
        tarrinaSegundo = tarrinaSegundo,
        tarrinaSopa = tarrinaSopa,
        envaseProteina = envaseProteina,
        porcionesPorEnvase = porcionesPorEnvase,
    )

    // "companion object" guarda cosas que pertenecen a la clase y no a cada fila.
    companion object {
        const val UNICA = 1

        fun desde(precios: Precios) = ConfiguracionEntity(
            almuerzo = precios.almuerzo,
            segundo = precios.segundo,
            sopa = precios.sopa,
            proteinaExtra = precios.proteinaExtra,
            tarrinaSegundo = precios.tarrinaSegundo,
            tarrinaSopa = precios.tarrinaSopa,
            envaseProteina = precios.envaseProteina,
            porcionesPorEnvase = precios.porcionesPorEnvase,
        )
    }
}

// Qué se sirve cada día. Solo informativo: no afecta los precios.
@Entity(tableName = "menu_dia")
data class MenuDiaEntity(
    @PrimaryKey val fecha: LocalDate,
    val segundo: String,
    val sopa: String,
)
