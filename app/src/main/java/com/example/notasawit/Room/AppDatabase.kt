package com.example.notasawit.Room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.notasawit.Admin.AuditInternal.AuditDao.AuditDao
import com.example.notasawit.Room.AuditEntity.AuditForm
import com.example.notasawit.Room.Auditor.AuditorEntity
import com.example.notasawit.Room.Desa.DesaEntity
import com.example.notasawit.Room.Petani.MasterDao
import com.example.notasawit.Room.Petani.PetaniEntity

@Database(
    entities = [
        AuditForm::class,
        DesaEntity::class,
        AuditorEntity::class,
        PetaniEntity::class
    ],
    version = 3
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun auditDao(): AuditDao

    abstract fun masterDao(): MasterDao
    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {

            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nota_sawit_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance

                instance
            }
        }
    }
}