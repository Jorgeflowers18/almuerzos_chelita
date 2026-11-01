package com.chelita.almuerzos

import android.app.Application
import com.chelita.almuerzos.datos.AlmuerzosDatabase
import com.chelita.almuerzos.datos.PedidosRepositorio
import com.chelita.almuerzos.datos.respaldo.GestorRespaldos
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

// Se crea una sola vez cuando arranca la app, antes que cualquier pantalla.
// Guarda la base de datos para que todas las pantallas compartan la misma.
class AlmuerzosApp : Application() {
    // "by lazy" = se crea la primera vez que alguien la usa, no antes.
    private val db: AlmuerzosDatabase by lazy { AlmuerzosDatabase.crear(this) }

    val repositorio: PedidosRepositorio by lazy { PedidosRepositorio(db) }

    val respaldos: GestorRespaldos by lazy { GestorRespaldos(this, db, repositorio) }

    // Tareas que deben terminar aunque se cierre la pantalla (como escribir un respaldo).
    private val tareas = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun respaldarEnSegundoPlano() {
        tareas.launch { respaldos.respaldoAutomatico() }
    }
}
