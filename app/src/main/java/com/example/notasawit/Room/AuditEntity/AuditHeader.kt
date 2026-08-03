package com.example.notasawit.Room.AuditEntity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_header")
data class AuditHeader(
    @PrimaryKey val idAudit: String,
    
    // Data Awal
    val tanggal: String = "",
    val desa: String = "",
    val namaAuditor: String = "",
    val namaPetani: String = "",
    val idPetani: Int? = null,

    // Kesimpulan
    val ringkasanTemuan: String = "",
    val rencanaPerbaikan: String = "",
    val rencanaPemeriksaan: String = "",
    val fotoPath: String = "",
    val pdfPath: String = "",

    val isSynced: Boolean = false,
    val statusAudit: String = "",

    // Konfigurasi Pengulangan
    val periode: String = "", // cth: "2026-S1"
    val auditAttempt: Int = 1,
    val parentAuditId: String? = null
)
