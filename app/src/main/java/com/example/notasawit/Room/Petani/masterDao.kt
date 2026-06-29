package com.example.notasawit.Room.Petani


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.notasawit.Room.Auditor.AuditorEntity
import com.example.notasawit.Room.Desa.DesaEntity

@Dao
interface MasterDao {

    // ==========================================
    // 1. DATA PETANI
    // ==========================================
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPetani(listPetani: List<PetaniEntity>)

    @Query("SELECT * FROM master_petani")
    suspend fun getAllPetani(): List<PetaniEntity>

    // ==========================================
    // 2. DATA DESA
    // ==========================================
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDesa(listDesa: List<DesaEntity>)

    @Query("SELECT * FROM desa_lokal")
    suspend fun getAllDesa(): List<DesaEntity>

    // ==========================================
    // 3. DATA AUDITOR
    // ==========================================
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditor(listAuditor: List<AuditorEntity>)

    @Query("SELECT * FROM master_auditor")
    suspend fun getAllAuditor(): List<AuditorEntity>
}