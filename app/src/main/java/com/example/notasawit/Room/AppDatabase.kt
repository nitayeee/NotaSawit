package com.example.notasawit.Room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.notasawit.Admin.AuditInternal.AuditDao.AuditDao
import com.example.notasawit.Room.AuditEntity.AuditForm
import com.example.notasawit.Room.Auditor.AuditorEntity
import com.example.notasawit.Room.DetailKegiatan.DetailKegiatanDao
import com.example.notasawit.Room.DetailKegiatan.DetailKegiatanEntity
import com.example.notasawit.Room.JenisKegiatan.JenisKegiatanDao
import com.example.notasawit.Room.JenisKegiatan.JenisKegiatanEntity
import com.example.notasawit.Room.KegiatanPetani.KegiatanDao
import com.example.notasawit.Room.KegiatanPetani.KegiatanEntity
import com.example.notasawit.Room.Lahan.LahanDao
import com.example.notasawit.Room.Lahan.LahanEntity
import com.example.notasawit.Room.Pengeluaran.PengeluaranDao
import com.example.notasawit.Room.Pengeluaran.PengeluaranEntity
import com.example.notasawit.Room.Petani.MasterDao
import com.example.notasawit.Room.Petani.PetaniEntity
import com.example.notasawit.Room.Produksi.ProduksiDao
import com.example.notasawit.Room.Produksi.ProduksiEntity

@Database(
    entities = [
        AuditForm::class,
        DesaEntity::class,
        AuditorEntity::class,
        PetaniEntity::class,
        LahanEntity::class,
        ProduksiEntity::class,
        PengeluaranEntity::class,
        JenisKegiatanEntity::class,
        KegiatanEntity::class,
        DetailKegiatanEntity::class,
    ],
    version = 12
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun auditDao(): AuditDao

    abstract fun masterDao(): MasterDao
    abstract fun DesaDao(): DesaDao
    abstract fun LahanDao(): LahanDao
    abstract fun ProduksiDao(): ProduksiDao
    abstract fun PengeluaranDao(): PengeluaranDao
    abstract fun JenisKegiatanDao(): JenisKegiatanDao
    abstract fun KegiatanDao(): KegiatanDao
    abstract fun DetailKegiatanDao(): DetailKegiatanDao

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