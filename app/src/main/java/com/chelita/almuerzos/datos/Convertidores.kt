package com.chelita.almuerzos.datos

import androidx.room.TypeConverter
import java.time.LocalDate

// SQLite no sabe guardar fechas. Estas funciones le enseñan a Room a convertirlas
// en un número (días desde 1970) y de vuelta. Los enums Room los guarda solo, como texto.
class Convertidores {
    @TypeConverter
    fun fechaANumero(fecha: LocalDate): Long = fecha.toEpochDay()

    @TypeConverter
    fun numeroAFecha(dias: Long): LocalDate = LocalDate.ofEpochDay(dias)
}
