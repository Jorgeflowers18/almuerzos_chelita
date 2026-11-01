package com.chelita.almuerzos.datos.respaldo

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

// Una carpeta del celular donde se guardan los archivos de respaldo.
interface CarpetaDeRespaldos {
    val nombre: String

    // El texto del archivo, o null si no existe.
    fun leer(archivo: String): String?

    // Crea el archivo o reemplaza el que ya había con ese nombre.
    fun escribir(archivo: String, contenido: String)
}

// Cómo se llega a la carpeta que eligió la dueña. Es una "interface" para que las pruebas
// puedan usar una carpeta de mentira en memoria.
interface AccesoCarpetas {
    // Pide a Android que recuerde el permiso para siempre (si no, se pierde al cerrar la app).
    fun conservarPermiso(uri: Uri)

    // null si la carpeta ya no existe o se perdió el permiso (por ejemplo, tras reinstalar la app).
    fun abrir(uri: Uri): CarpetaDeRespaldos?
}

// La carpeta real, elegida con el selector de archivos de Android. Como queda fuera de la app
// (por ejemplo en Documentos), los respaldos no se borran aunque se desinstale la app.
class CarpetasDelCelular(private val contexto: Context) : AccesoCarpetas {

    override fun conservarPermiso(uri: Uri) {
        contexto.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
    }

    override fun abrir(uri: Uri): CarpetaDeRespaldos? {
        val conPermiso = contexto.contentResolver.persistedUriPermissions.any { it.uri == uri && it.isWritePermission }
        if (!conPermiso) return null
        val carpeta = DocumentFile.fromTreeUri(contexto, uri) ?: return null
        if (!carpeta.exists()) return null
        return CarpetaDelCelular(contexto, carpeta)
    }
}

private class CarpetaDelCelular(contexto: Context, private val carpeta: DocumentFile) : CarpetaDeRespaldos {

    private val resolver = contexto.contentResolver

    override val nombre: String = carpeta.name ?: "Carpeta de respaldos"

    override fun leer(archivo: String): String? {
        val documento = carpeta.findFile(archivo) ?: return null
        return resolver.openInputStream(documento.uri)?.use { it.readBytes().decodeToString() }
    }

    // Primero se escribe un archivo temporal completo y solo después se cambia por el anterior.
    // Así, si el celular se apaga a mitad de camino, el respaldo anterior sigue sano.
    override fun escribir(archivo: String, contenido: String) {
        val temporal = "$archivo.tmp"
        carpeta.findFile(temporal)?.delete()
        val nuevo = carpeta.createFile("application/octet-stream", temporal)
            ?: throw ErrorDeRespaldo("No se pudo crear el archivo en la carpeta de respaldos.")
        val salida = resolver.openOutputStream(nuevo.uri, "wt")
            ?: throw ErrorDeRespaldo("No se pudo escribir en la carpeta de respaldos.")
        salida.use { it.write(contenido.encodeToByteArray()) }

        carpeta.findFile(archivo)?.delete()
        if (!nuevo.renameTo(archivo)) throw ErrorDeRespaldo("No se pudo guardar el respaldo.")
    }
}
