package com.chelita.almuerzos.datos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface ConfiguracionDao {

    // Devuelve null mientras no se hayan guardado precios (se usan los de por defecto).
    @Query("SELECT * FROM configuracion WHERE id = ${ConfiguracionEntity.UNICA}")
    fun observar(): Flow<ConfiguracionEntity?>

    // Upsert = inserta si no existe, actualiza si ya existe.
    @Upsert
    suspend fun guardar(configuracion: ConfiguracionEntity)

    @Query("SELECT * FROM menu_dia WHERE fecha = :fecha")
    fun observarMenu(fecha: LocalDate): Flow<MenuDiaEntity?>

    @Upsert
    suspend fun guardarMenu(menu: MenuDiaEntity)

    @Query("DELETE FROM menu_dia WHERE fecha < :fecha")
    suspend fun borrarMenusAntesDe(fecha: LocalDate)
}
