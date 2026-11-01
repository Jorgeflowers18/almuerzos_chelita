package com.chelita.almuerzos.ui.administracion

import com.chelita.almuerzos.dominio.Precios
import com.chelita.almuerzos.dominio.aCentavos
import com.chelita.almuerzos.dominio.comoMonto

// Un precio que se puede editar: qué es y en qué campo de Precios se guarda.
enum class CampoPrecio(val etiqueta: String) {
    ALMUERZO("Almuerzo completo"),
    SEGUNDO("Segundo"),
    SOPA("Sopa"),
    PROTEINA_EXTRA("Proteína extra"),
    TARRINA_SEGUNDO("Tarrina de segundo"),
    TARRINA_SOPA("Tarrina de sopa"),
    ENVASE_PROTEINA("Envase de proteína"),
}

// Los precios tal como se escriben en la pantalla (texto). Solo se pueden guardar
// cuando todos son montos válidos.
data class FormularioPrecios(
    val montos: Map<CampoPrecio, String> = CampoPrecio.entries.associateWith { "" },
    val porcionesPorEnvase: String = "",
) {
    fun monto(campo: CampoPrecio): String = montos[campo] ?: ""

    fun cambiar(campo: CampoPrecio, texto: String) = copy(montos = montos + (campo to texto))

    fun montoValido(campo: CampoPrecio): Boolean = monto(campo).aCentavos() != null

    // Entre 1 y 10 porciones por envase.
    val porcionesValidas: Boolean get() = porcionesPorEnvase.trim().toIntOrNull() in 1..10

    // Los precios listos para guardar, o null si algún campo está mal escrito.
    fun aPrecios(): Precios? {
        val c = CampoPrecio.entries.associateWith { monto(it).aCentavos() ?: return null }
        if (!porcionesValidas) return null
        return Precios(
            almuerzo = c.getValue(CampoPrecio.ALMUERZO),
            segundo = c.getValue(CampoPrecio.SEGUNDO),
            sopa = c.getValue(CampoPrecio.SOPA),
            proteinaExtra = c.getValue(CampoPrecio.PROTEINA_EXTRA),
            tarrinaSegundo = c.getValue(CampoPrecio.TARRINA_SEGUNDO),
            tarrinaSopa = c.getValue(CampoPrecio.TARRINA_SOPA),
            envaseProteina = c.getValue(CampoPrecio.ENVASE_PROTEINA),
            porcionesPorEnvase = porcionesPorEnvase.trim().toInt(),
        )
    }

    companion object {
        fun desde(precios: Precios) = FormularioPrecios(
            montos = mapOf(
                CampoPrecio.ALMUERZO to precios.almuerzo.comoMonto(),
                CampoPrecio.SEGUNDO to precios.segundo.comoMonto(),
                CampoPrecio.SOPA to precios.sopa.comoMonto(),
                CampoPrecio.PROTEINA_EXTRA to precios.proteinaExtra.comoMonto(),
                CampoPrecio.TARRINA_SEGUNDO to precios.tarrinaSegundo.comoMonto(),
                CampoPrecio.TARRINA_SOPA to precios.tarrinaSopa.comoMonto(),
                CampoPrecio.ENVASE_PROTEINA to precios.envaseProteina.comoMonto(),
            ),
            porcionesPorEnvase = precios.porcionesPorEnvase.toString(),
        )
    }
}
