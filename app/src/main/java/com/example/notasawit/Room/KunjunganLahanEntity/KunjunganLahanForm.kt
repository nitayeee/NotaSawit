package com.example.notasawit.Room.KunjunganLahanEntity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "kunjungan_lahan_table")
data class KunjunganLahanForm(
    @PrimaryKey val idKunjungan: String,
    
    // === SECTION 1: DATA AWAL ===
    val tanggal: String = "",
    val desaKebun: String = "",
    val desaKepengurusan: String = "",
    val namaAuditor: String = "",
    val namaPetani: String = "",

    // === SECTION 2: PERTANYAAN ===
    val q1_patokBatas: Boolean? = null,
    val q2_idKebun: Boolean? = null,
    val q3_piringanPasarPikul: Boolean? = null,
    val q4_pelepahDitunas: Boolean? = null,
    val q5_susunanPelepah: Boolean? = null,
    val q6_turnera: Boolean? = null,
    val q7_bekasPembakaran: Boolean? = null,
    val q8_botolRacunPlastik: Boolean? = null,
    val q9_sampahPlastik: Boolean? = null,
    val q10_plangSungai: Boolean? = null,
    val q11_semprotSungai: Boolean? = null,
    val q12_sampahSungai: Boolean? = null,
    val q13_semprotTotal: Boolean? = null,
    val q14_racunKontak: Boolean? = null,
    val q15_hamaPenyakit: Boolean? = null,

    val isSynced: Boolean = false,
    val pdfPath: String = ""
)
