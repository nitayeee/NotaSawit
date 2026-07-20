package com.example.notasawit.Room.AuditEntity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_table") // <-- Tambahkan ini
data class AuditForm(
    @PrimaryKey val idAudit: String, // <-- Jadikan ID Unik sebagai Primary Key
    // === SECTION 1: DATA AWAL ===
    val tanggal: String = "",
    val desa: String = "",
    val namaAuditor: String = "",
    val namaPetani: String = "",

    // === SECTION 2: ASPEK PENGETAHUAN ===
    // 1) Asosiasi (2 pertanyaan)
    val asosiasiQ1: Boolean? = null, // null artinya belum diisi, true = Ya, false = Tidak
    val asosiasiQ2: Boolean? = null,

    // 2) SOP (18 pertanyaan)
    val sopQ1: Boolean? = null,
    val sopQ2: Boolean? = null,
    val sopQ3: Boolean? = null,
    val sopQ4: Boolean? = null,
    val sopQ5: Boolean? = null,
    val sopQ6: Boolean? = null,
    val sopQ7: Boolean? = null,
    val sopQ8: Boolean? = null,
    val sopQ9: Boolean? = null,
    val sopQ10: Boolean? = null,
    val sopQ11: Boolean? = null,
    val sopQ12: Boolean? = null,
    val sopQ13: Boolean? = null,
    val sopQ14: Boolean? = null,
    val sopQ15: Boolean? = null,
    val sopQ16: Boolean? = null,
    val sopQ17: Boolean? = null,
    val sopQ18: Boolean? = null,


    // 3) Pelatihan (14 pertanyaan)
    val pelatihanQ1: Boolean? = null,
    val pelatihanQ2: Boolean? = null,
    val pelatihanQ3: Boolean? = null,
    val pelatihanQ4: Boolean? = null,
    val pelatihanQ5: Boolean? = null,
    val pelatihanQ6: Boolean? = null,
    val pelatihanQ7: Boolean? = null,
    val pelatihanQ8: Boolean? = null,
    val pelatihanQ9: Boolean? = null,
    val pelatihanQ10: Boolean? = null,
    val pelatihanQ11: Boolean? = null,
    val pelatihanQ12: Boolean? = null,
    val pelatihanQ13: Boolean? = null,
    val pelatihanQ14: Boolean? = null,

    // 4) TENTANG BAHAN KIMIA DAN LIMBAH BAHAN BERBAHAYA & BERACUN (LB3) (7 pertanyaan)

    val lb3Q1: Boolean? = null,
    val lb3Q2: Boolean? = null,
    val lb3Q3: Boolean? = null,
    val lb3Q4: Boolean? = null,
    val lb3Q5: Boolean? = null,
    val lb3Q6: Boolean? = null,
    val lb3Q7: Boolean? = null,

// 5) TENTANG NILAI KONSERVASI TINGGI (NKT) (7 pertanyaan)

    val nktQ1: Boolean? = null,
    val nktQ2: Boolean? = null,
    val nktQ3: Boolean? = null,
    val nktQ4: Boolean? = null,
    val nktQ5: Boolean? = null,
    val nktQ6: Boolean? = null,
    val nktQ7: Boolean? = null,

// 6) TENTANG NILAI KONSERVASI TINGGI (NKT) (6 pertanyaan)

    val sosialTenagaKerjaQ1: Boolean? = null,
    val sosialTenagaKerjaQ2: Boolean? = null,
    val sosialTenagaKerjaQ3: Boolean? = null,
    val sosialTenagaKerjaQ4: Boolean? = null,
    val sosialTenagaKerjaQ5: Boolean? = null,
    val sosialTenagaKerjaQ6: Boolean? = null,

// 7) TENTANG KESELAMATAN DAN KESEHATAN KERJA (K3) (9 pertanyaan)

    val k3Q1: Boolean? = null,
    val k3Q2: Boolean? = null,
    val k3Q3: Boolean? = null,
    val k3Q4: Boolean? = null,
    val k3Q5: Boolean? = null,
    val k3Q6: Boolean? = null,
    val k3Q7: Boolean? = null,
    val k3Q8: Boolean? = null,
    val k3Q9: Boolean? = null,


    // === SECTION 3: IMPLEMENTASI RSPO ===
    // 13 pertanyaan
    val dokumenQ1: Boolean? = null,
    val dokumenQ2: Boolean? = null,
    val dokumenQ3: Boolean? = null,
    val dokumenQ4: Boolean? = null,
    val dokumenQ5: Boolean? = null,
    val dokumenQ6: Boolean? = null,
    val dokumenQ7: Boolean? = null,
    val dokumenQ8: Boolean? = null,
    val dokumenQ9: Boolean? = null,
    val dokumenQ10: Boolean? = null,
    val dokumenQ11: Boolean? = null,
    val dokumenQ12: Boolean? = null,
    val dokumenQ13: Boolean? = null,

    // 11 pertanyaan
    val kebunQ1: Boolean? = null,
    val kebunQ2: Boolean? = null,
    val kebunQ3: Boolean? = null,
    val kebunQ4: Boolean? = null,
    val kebunQ5: Boolean? = null,
    val kebunQ6: Boolean? = null,
    val kebunQ7: Boolean? = null,
    val kebunQ8: Boolean? = null,
    val kebunQ9: Boolean? = null,
    val kebunQ10: Boolean? = null,
    val kebunQ11: Boolean? = null,


    // === SECTION 4: KESIMPULAN ===
    val ringkasanTemuan: String = "",
    val rencanaPerbaikan: String = "",
    val rencanaPemeriksaan: String = "",
    val fotoPath: String = "",
    val pdfPath: String = "",

        // ID Unik (UUID) untuk laporan ini
    val isSynced: Boolean = false    // Penanda apakah sudah terkirim ke Laravel atau belum
)