package com.chelita.almuerzos.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.chelita.almuerzos.AlmuerzosApp
import com.chelita.almuerzos.ui.administracion.AdministracionViewModel
import com.chelita.almuerzos.ui.cobro.CobroViewModel
import com.chelita.almuerzos.ui.despacho.DespachoViewModel
import com.chelita.almuerzos.ui.pedidos.EditarPedidoViewModel
import com.chelita.almuerzos.ui.pedidos.ListaPedidosViewModel
import com.chelita.almuerzos.ui.resumen.ResumenViewModel

// Le dice a Android cómo crear cada ViewModel, entregándole el repositorio de la app.
// Al agregar una pantalla nueva con ViewModel, se agrega aquí su "initializer".
val FabricaViewModels = viewModelFactory {
    initializer { NavegacionViewModel(app().repositorio) }
    initializer { ListaPedidosViewModel(app().repositorio, app().respaldos) }
    initializer { EditarPedidoViewModel(app().repositorio, createSavedStateHandle()) }
    initializer { DespachoViewModel(app().repositorio) }
    initializer { CobroViewModel(app().repositorio, createSavedStateHandle()) }
    initializer { ResumenViewModel(app().repositorio) }
    initializer { AdministracionViewModel(app().repositorio, app().respaldos) }
}

private fun CreationExtras.app(): AlmuerzosApp =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AlmuerzosApp
