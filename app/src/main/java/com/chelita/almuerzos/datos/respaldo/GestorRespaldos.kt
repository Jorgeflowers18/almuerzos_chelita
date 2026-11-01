package com.chelita.almuerzos.datos.respaldo

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.content.edit
import com.chelita.almuerzos.datos.AlmuerzosDatabase
import com.chelita.almuerzos.datos.ContenidoRespaldo
import com.chelita.almuerzos.datos.PedidosRepositorio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

// Los dos únicos archivos de respaldo. Cada respaldo reemplaza al anterior del mismo tipo,
// así nunca se acumulan archivos en el celular.
enum class TipoRespaldo(val archivo: String) {
    AUTOMATICO("almuerzos-chelita-respaldo.json"),
    ANTES_DE_LIMPIAR("almuerzos-chelita-antes-de-limpiar.json"),
}

data class EstadoRespaldos(
    val carpeta: String? = null, // nombre de la carpeta elegida, null si todavía no hay
    val ultimoAutomatico: Long? = null,
    val ultimoAntesDeLimpiar: Long? = null,
)

// Se encarga de todo lo relacionado con respaldos: guardarlos en la carpeta elegida,
// prepararlos para enviar a otro celular y restaurarlos.
class GestorRespaldos(
    private val contexto: Context,
    db: AlmuerzosDatabase,
    private val repositorio: PedidosRepositorio,
    private val carpetas: AccesoCarpetas = CarpetasDelCelular(contexto),
    private val ahora: () -> Long = { System.currentTimeMillis() },
) {
    private val respaldoDao = db.respaldoDao()
    private val preferencias = contexto.getSharedPreferences("respaldos", Context.MODE_PRIVATE)

    // Evita que dos respaldos se escriban al mismo tiempo sobre el mismo archivo.
    private val candado = Mutex()

    private val _estado = MutableStateFlow(leerEstado())
    val estado: StateFlow<EstadoRespaldos> = _estado.asStateFlow()

    /**
     * Guarda la carpeta elegida y saca el primer respaldo. Si este celular está vacío
     * (por ejemplo, recién instalada la app) y la carpeta ya tiene un respaldo, no lo pisa:
     * lo devuelve para ofrecer restaurarlo.
     */
    suspend fun elegirCarpeta(uri: Uri): ContenidoRespaldo? = withContext(Dispatchers.IO) {
        carpetas.conservarPermiso(uri)
        val carpeta = carpetas.abrir(uri) ?: throw ErrorDeRespaldo("No se pudo abrir esa carpeta. Elige otra.")
        preferencias.edit {
            putString(CLAVE_CARPETA, uri.toString())
            putString(CLAVE_NOMBRE_CARPETA, carpeta.nombre)
        }
        _estado.value = leerEstado()

        val existente = carpeta.leer(TipoRespaldo.AUTOMATICO.archivo)?.let {
            runCatching { FormatoRespaldo.desdeJson(it) }.getOrNull()
        }
        if (existente != null && respaldoDao.contarPedidos() == 0) return@withContext existente

        if (respaldoDao.contarPedidos() > 0) respaldar(TipoRespaldo.AUTOMATICO)
        null
    }

    // Escribe un respaldo con todos los datos. Lanza ErrorDeRespaldo si no se pudo.
    suspend fun respaldar(tipo: TipoRespaldo) = withContext(Dispatchers.IO) {
        candado.withLock {
            val carpeta = carpetaActual() ?: throw ErrorDeRespaldo("Primero elige la carpeta de respaldos en Ajustes.")
            val contenido = respaldoDao.leerTodo(ahora())
            // Un celular sin pedidos nunca reemplaza un respaldo: protege el respaldo
            // si la app se reinstaló y todavía está vacía.
            if (contenido.pedidos.isEmpty()) throw ErrorDeRespaldo("Todavía no hay pedidos para respaldar.")
            carpeta.escribir(tipo.archivo, FormatoRespaldo.aJson(contenido))
            preferencias.edit { putLong(tipo.claveUltimo(), contenido.creadoEn) }
            _estado.value = leerEstado()
        }
    }

    // Se llama cada vez que la app pasa a segundo plano. Para no escribir de más,
    // respalda como mucho una vez cada 15 minutos. Nunca lanza errores.
    suspend fun respaldoAutomatico() {
        val ultimo = _estado.value.ultimoAutomatico ?: 0L
        if (_estado.value.carpeta == null || ahora() - ultimo < INTERVALO_AUTOMATICO) return
        runCatching { respaldar(TipoRespaldo.AUTOMATICO) }
    }

    // Borra los pedidos de más de 2 meses, pero solo después de guardarlos en el respaldo
    // "antes de limpiar". Si el respaldo falla, no se borra nada.
    suspend fun limpiarConRespaldo(): Int {
        respaldar(TipoRespaldo.ANTES_DE_LIMPIAR)
        return repositorio.borrarAntiguos()
    }

    suspend fun pedidosEnEsteCelular(): Int = respaldoDao.contarPedidos()

    // Lee un respaldo que la mesera eligió (por ejemplo, uno recibido por WhatsApp).
    suspend fun leerArchivo(uri: Uri): ContenidoRespaldo = withContext(Dispatchers.IO) {
        val texto = try {
            contexto.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
        } catch (e: Exception) {
            throw ErrorDeRespaldo("No se pudo abrir el archivo.", e)
        } ?: throw ErrorDeRespaldo("No se pudo abrir el archivo.")
        FormatoRespaldo.desdeJson(texto)
    }

    // Reemplaza todos los datos de este celular por los del respaldo.
    suspend fun restaurar(contenido: ContenidoRespaldo) = withContext(Dispatchers.IO) {
        candado.withLock { respaldoDao.reemplazarTodo(contenido) }
    }

    // Prepara un archivo con todos los datos para enviarlo (WhatsApp, Drive...).
    // Se guarda siempre con el mismo nombre en la memoria temporal de la app, así que tampoco se acumula.
    suspend fun archivoParaEnviar(): Uri = withContext(Dispatchers.IO) {
        val contenido = respaldoDao.leerTodo(ahora())
        val carpeta = File(contexto.cacheDir, "respaldos").apply { mkdirs() }
        val archivo = File(carpeta, TipoRespaldo.AUTOMATICO.archivo)
        archivo.writeText(FormatoRespaldo.aJson(contenido))
        FileProvider.getUriForFile(contexto, "${contexto.packageName}.archivos", archivo)
    }

    private fun carpetaActual(): CarpetaDeRespaldos? =
        preferencias.getString(CLAVE_CARPETA, null)?.let { carpetas.abrir(Uri.parse(it)) }

    private fun leerEstado() = EstadoRespaldos(
        carpeta = preferencias.getString(CLAVE_NOMBRE_CARPETA, null)
            ?.takeIf { carpetaActual() != null },
        ultimoAutomatico = preferencias.getLong(TipoRespaldo.AUTOMATICO.claveUltimo(), 0L).takeIf { it > 0 },
        ultimoAntesDeLimpiar = preferencias.getLong(TipoRespaldo.ANTES_DE_LIMPIAR.claveUltimo(), 0L).takeIf { it > 0 },
    )

    private fun TipoRespaldo.claveUltimo() = "ultimo_$name"

    companion object {
        private const val CLAVE_CARPETA = "carpeta"
        private const val CLAVE_NOMBRE_CARPETA = "nombre_carpeta"
        const val INTERVALO_AUTOMATICO = 15 * 60 * 1000L
    }
}
