package com.chelita.almuerzos.datos

import com.chelita.almuerzos.dominio.CalculadoraCobro
import com.chelita.almuerzos.dominio.EstadoPedido
import com.chelita.almuerzos.dominio.Precios
import com.chelita.almuerzos.dominio.Producto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

// El repositorio es la única puerta de entrada a los datos para las pantallas.
// Junta la base de datos con la lógica de cobro: al guardar un pedido calcula su total.
//
// "hoy" y "ahora" se reciben como funciones para que las pruebas puedan simular otra fecha.
class PedidosRepositorio(
    db: AlmuerzosDatabase,
    private val hoy: () -> LocalDate = { LocalDate.now() },
    private val ahora: () -> Long = { System.currentTimeMillis() },
) {
    private val pedidoDao = db.pedidoDao()
    private val configuracionDao = db.configuracionDao()

    // Precios vigentes; si nunca se editaron, los de por defecto.
    val precios: Flow<Precios> = configuracionDao.observar().map { it?.aPrecios() ?: Precios() }

    fun pedidosDeHoy(): Flow<List<PedidoConLineas>> = pedidoDao.observarDelDia(hoy())

    suspend fun obtener(id: Long): PedidoConLineas? = pedidoDao.obtener(id)

    /**
     * Crea un pedido nuevo (id = 0) o modifica uno existente, calculando su cobro
     * con los precios vigentes. Al modificar se conservan el estado y la fecha originales.
     * Devuelve el id del pedido.
     */
    suspend fun guardarPedido(
        nombre: String,
        paraLlevar: Boolean,
        cantidades: Map<Producto, Int>,
        observaciones: String = "",
        id: Long = 0,
    ): Long {
        require(nombre.isNotBlank()) { "El pedido necesita un nombre" }
        val cobro = CalculadoraCobro.calcular(cantidades, paraLlevar, precios.first())
        require(cobro.lineas.isNotEmpty()) { "El pedido no tiene productos" }

        val existente = if (id != 0L) pedidoDao.obtener(id)?.pedido else null
        val pedido = PedidoEntity(
            id = id,
            nombre = nombre.trim(),
            paraLlevar = paraLlevar,
            observaciones = observaciones.trim(),
            estado = existente?.estado ?: EstadoPedido.PENDIENTE,
            fecha = existente?.fecha ?: hoy(),
            creadoEn = existente?.creadoEn ?: ahora(),
            tarrinasSegundo = cobro.tarrinasSegundo,
            tarrinasSopa = cobro.tarrinasSopa,
            envasesProteina = cobro.envasesProteina,
            costoTarrinas = cobro.costoTarrinas,
            total = cobro.total,
        )
        val lineas = cobro.lineas.map {
            LineaPedidoEntity(pedidoId = id, producto = it.producto, cantidad = it.cantidad, precioUnitario = it.precioUnitario)
        }
        return pedidoDao.guardar(pedido, lineas)
    }

    suspend fun cambiarEstado(id: Long, estado: EstadoPedido) = pedidoDao.cambiarEstado(id, estado)

    suspend fun eliminar(id: Long) = pedidoDao.eliminar(id)

    suspend fun guardarPrecios(precios: Precios) = configuracionDao.guardar(ConfiguracionEntity.desde(precios))
}
