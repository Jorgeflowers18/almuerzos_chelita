package com.chelita.almuerzos

import android.app.Application
import com.chelita.almuerzos.datos.AlmuerzosDatabase
import com.chelita.almuerzos.datos.PedidosRepositorio

// Se crea una sola vez cuando arranca la app, antes que cualquier pantalla.
// Guarda la base de datos para que todas las pantallas compartan la misma.
class AlmuerzosApp : Application() {
    // "by lazy" = se crea la primera vez que alguien la usa, no antes.
    val repositorio: PedidosRepositorio by lazy {
        PedidosRepositorio(AlmuerzosDatabase.crear(this))
    }
}
