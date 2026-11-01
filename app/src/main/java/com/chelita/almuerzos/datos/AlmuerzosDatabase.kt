package com.chelita.almuerzos.datos

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

// La base de datos de la app (un archivo SQLite llamado almuerzos.db dentro del celular).
//
// IMPORTANTE: cuando la app ya esté en uso, cualquier cambio en las tablas exige
// subir "version" y escribir una migración. Nunca usar fallbackToDestructiveMigration():
// borraría todos los pedidos al actualizar la app.
@Database(
    entities = [
        PedidoEntity::class,
        LineaPedidoEntity::class,
        ConfiguracionEntity::class,
        MenuDiaEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Convertidores::class)
abstract class AlmuerzosDatabase : RoomDatabase() {
    abstract fun pedidoDao(): PedidoDao
    abstract fun configuracionDao(): ConfiguracionDao

    companion object {
        const val NOMBRE_ARCHIVO = "almuerzos.db"

        fun crear(context: Context): AlmuerzosDatabase =
            Room.databaseBuilder(context, AlmuerzosDatabase::class.java, NOMBRE_ARCHIVO).build()
    }
}
