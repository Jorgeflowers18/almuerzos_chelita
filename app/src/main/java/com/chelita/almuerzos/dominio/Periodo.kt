package com.chelita.almuerzos.dominio

import java.time.LocalDate
import java.time.YearMonth

// Un rango de fechas, incluidos el primer y el último día.
data class Rango(val desde: LocalDate, val hasta: LocalDate)

// Los periodos que se pueden ver en el resumen.
enum class Periodo(val etiqueta: String) {
    HOY("Hoy"),
    ESTE_MES("Este mes"),
    MES_ANTERIOR("Mes anterior");

    // Las fechas que abarca el periodo, contando desde "hoy".
    fun rango(hoy: LocalDate): Rango = when (this) {
        HOY -> Rango(hoy, hoy)
        ESTE_MES -> Rango(hoy.withDayOfMonth(1), hoy)
        MES_ANTERIOR -> {
            val mes = YearMonth.from(hoy).minusMonths(1)
            Rango(mes.atDay(1), mes.atEndOfMonth())
        }
    }
}
