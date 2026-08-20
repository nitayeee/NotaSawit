package com.example.notasawit.Room.KunjunganLahanEntity

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface KunjunganLahanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKunjunganLahan(kunjunganLahan: KunjunganLahanForm)

    @Update
    suspend fun updateKunjunganLahan(kunjunganLahan: KunjunganLahanForm)

    @Query("SELECT * FROM kunjungan_lahan_table")
    suspend fun getAllKunjunganLahan(): List<KunjunganLahanForm>

    @Query("SELECT * FROM kunjungan_lahan_table WHERE idKunjungan = :id")
    suspend fun getKunjunganLahanById(id: String): KunjunganLahanForm?
    
    @Query("SELECT * FROM kunjungan_lahan_table WHERE isSynced = 0")
    suspend fun getUnsyncedKunjunganLahan(): List<KunjunganLahanForm>
    
    @Query("UPDATE kunjungan_lahan_table SET isSynced = 1 WHERE idKunjungan = :id")
    suspend fun markAsSynced(id: String)

    // Cek kunjungan terakhir petani pada periode tertentu
    @Query("SELECT * FROM kunjungan_lahan_table WHERE namaPetani = :namaPetani AND periode = :periode ORDER BY visitAttempt DESC LIMIT 1")
    suspend fun getLastKunjunganForPetani(namaPetani: String, periode: String): KunjunganLahanForm?

    @Query("SELECT * FROM kunjungan_lahan_table WHERE (idPetani = :idPetani OR LOWER(TRIM(namaPetani)) = LOWER(TRIM(:namaPetani))) AND periode = :periode ORDER BY visitAttempt DESC LIMIT 1")
    suspend fun getLastKunjunganForPetaniWithId(idPetani: Int, namaPetani: String, periode: String): KunjunganLahanForm?

    // Ambil SEMUA kunjungan petani pada periode tertentu
    @Query("SELECT * FROM kunjungan_lahan_table WHERE (idPetani = :idPetani OR LOWER(TRIM(namaPetani)) = LOWER(TRIM(:namaPetani))) AND periode = :periode ORDER BY visitAttempt ASC")
    suspend fun getAllKunjunganForPetani(idPetani: Int, namaPetani: String, periode: String): List<KunjunganLahanForm>

    // Ambil SEMUA kunjungan petani di seluruh periode (fallback)
    @Query("SELECT * FROM kunjungan_lahan_table WHERE (idPetani = :idPetani OR LOWER(TRIM(namaPetani)) = LOWER(TRIM(:namaPetani))) ORDER BY visitAttempt ASC")
    suspend fun getAllKunjunganForPetaniAllPeriods(idPetani: Int, namaPetani: String): List<KunjunganLahanForm>

    @Query("UPDATE kunjungan_lahan_table SET statusKunjungan = :status, ringkasanTemuan = :keterangan WHERE idKunjungan = :idKunjungan")
    suspend fun updateKunjunganStatus(idKunjungan: String, status: String, keterangan: String?)
}
