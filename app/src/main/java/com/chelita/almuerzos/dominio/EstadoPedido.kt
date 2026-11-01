package com.chelita.almuerzos.dominio

// Estados por los que pasa un pedido, en orden.
// En la base de datos se guarda el nombre en mayúsculas ("PENDIENTE"), así que
// NO renombrar estas opciones una vez que la app esté en uso.
enum class EstadoPedido(val nombre: String) {
    PENDIENTE("Pendiente"),
    SERVIDO("Servido"),
    PAGADO("Pagado"),
}
