package com.chelita.almuerzos.datos.respaldo

import com.chelita.almuerzos.datos.ConfiguracionEntity
import com.chelita.almuerzos.datos.ContenidoRespaldo
import com.chelita.almuerzos.datos.LineaPedidoEntity
import com.chelita.almuerzos.datos.MenuDiaEntity
import com.chelita.almuerzos.datos.PedidoConLineas
import com.chelita.almuerzos.datos.PedidoEntity
import com.chelita.almuerzos.dominio.EstadoPedido
import com.chelita.almuerzos.dominio.Producto
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.DateTimeException
import java.time.LocalDate

// Un problema con un respaldo, con un mensaje que se le puede mostrar a la mesera.
class ErrorDeRespaldo(mensaje: String, causa: Throwable? = null) : Exception(mensaje, causa)

// Convierte los datos a texto JSON y de vuelta. JSON es un formato de texto que no depende
// de la versión de la base de datos, así un respaldo viejo se puede abrir en una app nueva.
object FormatoRespaldo {

    private const val APP = "almuerzos-chelita"

    // Subir este número si cambia el formato, y seguir sabiendo leer los anteriores.
    const val VERSION = 1

    fun aJson(contenido: ContenidoRespaldo): String = JSONObject()
        .put("app", APP)
        .put("version", VERSION)
        .put("creadoEn", contenido.creadoEn)
        .put("configuracion", contenido.configuracion?.let(::configuracionAJson) ?: JSONObject.NULL)
        .put("menus", JSONArray(contenido.menus.map(::menuAJson)))
        .put("pedidos", JSONArray(contenido.pedidos.map(::pedidoAJson)))
        .toString(1)

    fun desdeJson(texto: String): ContenidoRespaldo {
        val raiz = try {
            JSONObject(texto)
        } catch (e: JSONException) {
            throw ErrorDeRespaldo("El archivo no es un respaldo de Almuerzos Chelita.", e)
        }
        if (raiz.optString("app") != APP) throw ErrorDeRespaldo("El archivo no es un respaldo de Almuerzos Chelita.")
        if (raiz.optInt("version") > VERSION) {
            throw ErrorDeRespaldo("El respaldo es de una versión más nueva de la app. Actualiza la app primero.")
        }
        return try {
            ContenidoRespaldo(
                creadoEn = raiz.getLong("creadoEn"),
                configuracion = if (raiz.isNull("configuracion")) null else configuracionDesde(raiz.getJSONObject("configuracion")),
                menus = raiz.getJSONArray("menus").objetos().map(::menuDesde),
                pedidos = raiz.getJSONArray("pedidos").objetos().map(::pedidoDesde),
            )
        } catch (e: JSONException) {
            throw ErrorDeRespaldo("El respaldo está dañado y no se puede leer.", e)
        } catch (e: IllegalArgumentException) { // un producto o estado que no existe
            throw ErrorDeRespaldo("El respaldo está dañado y no se puede leer.", e)
        } catch (e: DateTimeException) { // una fecha mal escrita
            throw ErrorDeRespaldo("El respaldo está dañado y no se puede leer.", e)
        }
    }

    private fun pedidoAJson(p: PedidoConLineas) = JSONObject()
        .put("id", p.pedido.id)
        .put("nombre", p.pedido.nombre)
        .put("paraLlevar", p.pedido.paraLlevar)
        .put("observaciones", p.pedido.observaciones)
        .put("estado", p.pedido.estado.name)
        .put("fecha", p.pedido.fecha.toString())
        .put("creadoEn", p.pedido.creadoEn)
        .put("tarrinasSegundo", p.pedido.tarrinasSegundo)
        .put("tarrinasSopa", p.pedido.tarrinasSopa)
        .put("envasesProteina", p.pedido.envasesProteina)
        .put("costoTarrinas", p.pedido.costoTarrinas)
        .put("total", p.pedido.total)
        .put(
            "lineas",
            JSONArray(
                p.lineas.map {
                    JSONObject()
                        .put("producto", it.producto.name)
                        .put("cantidad", it.cantidad)
                        .put("precioUnitario", it.precioUnitario)
                },
            ),
        )

    private fun pedidoDesde(o: JSONObject): PedidoConLineas {
        val id = o.getLong("id")
        return PedidoConLineas(
            pedido = PedidoEntity(
                id = id,
                nombre = o.getString("nombre"),
                paraLlevar = o.getBoolean("paraLlevar"),
                observaciones = o.getString("observaciones"),
                estado = EstadoPedido.valueOf(o.getString("estado")),
                fecha = LocalDate.parse(o.getString("fecha")),
                creadoEn = o.getLong("creadoEn"),
                tarrinasSegundo = o.getInt("tarrinasSegundo"),
                tarrinasSopa = o.getInt("tarrinasSopa"),
                envasesProteina = o.getInt("envasesProteina"),
                costoTarrinas = o.getInt("costoTarrinas"),
                total = o.getInt("total"),
            ),
            lineas = o.getJSONArray("lineas").objetos().map {
                LineaPedidoEntity(
                    pedidoId = id,
                    producto = Producto.valueOf(it.getString("producto")),
                    cantidad = it.getInt("cantidad"),
                    precioUnitario = it.getInt("precioUnitario"),
                )
            },
        )
    }

    private fun configuracionAJson(c: ConfiguracionEntity) = JSONObject()
        .put("almuerzo", c.almuerzo)
        .put("segundo", c.segundo)
        .put("sopa", c.sopa)
        .put("proteinaExtra", c.proteinaExtra)
        .put("tarrinaSegundo", c.tarrinaSegundo)
        .put("tarrinaSopa", c.tarrinaSopa)
        .put("envaseProteina", c.envaseProteina)
        .put("porcionesPorEnvase", c.porcionesPorEnvase)

    private fun configuracionDesde(o: JSONObject) = ConfiguracionEntity(
        almuerzo = o.getInt("almuerzo"),
        segundo = o.getInt("segundo"),
        sopa = o.getInt("sopa"),
        proteinaExtra = o.getInt("proteinaExtra"),
        tarrinaSegundo = o.getInt("tarrinaSegundo"),
        tarrinaSopa = o.getInt("tarrinaSopa"),
        envaseProteina = o.getInt("envaseProteina"),
        porcionesPorEnvase = o.getInt("porcionesPorEnvase"),
    )

    private fun menuAJson(m: MenuDiaEntity) = JSONObject()
        .put("fecha", m.fecha.toString())
        .put("segundo", m.segundo)
        .put("sopa", m.sopa)

    private fun menuDesde(o: JSONObject) = MenuDiaEntity(
        fecha = LocalDate.parse(o.getString("fecha")),
        segundo = o.getString("segundo"),
        sopa = o.getString("sopa"),
    )

    private fun JSONArray.objetos(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
}
