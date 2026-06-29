package com.example.notasawit.Admin.AuditInternal.data

import com.example.notasawit.Admin.AuditInternal.model.AuditItem

object Section3QuestionData {

    fun getQuestions(): List<AuditItem> {

        return listOf(

            AuditItem.Header("1. VERIFIKASI DOKUMEN"),

            AuditItem.Question(
                key = "dokumenQ1",
                question = "1. Sudah memiliki dan tersedia Salinan Kartu Tanda Penduduk (KTP)?"
            ),
            AuditItem.Question(
                key = "dokumenQ2",
                question = "2. Sudah memiliki dan tersedia Salinan Kartu Keluarga (KK)?"
            ),
            AuditItem.Question(
                key = "dokumenQ3",
                question = "3. Sudah memiliki dan tersedia Pas Foto?"
            ),
            AuditItem.Question(
                key = "dokumenQ4",
                question = "4. Sudah memiliki dan tersedia Surat Bukti Kepemilikan Tanah?"
            ),
            AuditItem.Question(
                key = "dokumenQ5",
                question = "5. Sudah memiliki dan tersedia Surat Tanda Daftar Budidaya (STDB)?"
            ),
            AuditItem.Question(
                key = "dokumenQ6",
                question = "6. Sudah memiliki dan tersedia Surat Pernyataan Kesanggupan Pengelolan Lingkungan (SPPL)?"
            ),
            AuditItem.Question(
                key = "dokumenQ7",
                question = "7. Sudah memiliki dan tersedia Surat/Form Padiatapa (Annex III)?"
            ),
            AuditItem.Question(
                key = "dokumenQ8",
                question = "8. Sudah memiliki dan tersedia Bukti Pembayaran Upah (khusus bagi yang memiliki pekerja)?\nNB : Bagi Petani yang tidak memiliki pekerja silahkan memilih pilihan \"Ya\"."
            ),
            AuditItem.Question(
                key = "dokumenQ9",
                question = "9. Sudah memiliki dan tersedia Peta Lahan?"
            ),
            AuditItem.Question(
                key = "dokumenQ10",
                question = "10. Sudah memiliki dan tersedia Surat Pernyataan Bergabung Asosiasi (Formulir Pendaftaran)?"
            ),
            AuditItem.Question(
                key = "dokumenQ11",
                question = "11. Sudah memiliki dan tersedia Kontrak Kerja (khusus untuk yang memiliki pekerja)?\nNB : Bagi Petani yang tidak memiliki pekerja silahkan memilih pilihan \"Ya\"."
            ),
            AuditItem.Question(
                key = "dokumenQ12",
                question = "12. Sudah memiliki dan tersedia Surat Pernyataan RSPO (Annex II)?"
            ),
            AuditItem.Question(
                key = "dokumenQ13",
                question = "13. Sudah memiliki, mengisi dan tersedia Logbook Atau Catatan Petani?"
            ),

            AuditItem.Header("2. VERIFIKASI KEBUN"),

            AuditItem.Question(
                key = "kebunQ1",
                question = "1. Sudah memiliki dan melakukan perawatan patok batas lahan?"
            ),
            AuditItem.Question(
                key = "kebunQ2",
                question = "2. Sudah melakukan Penyusunan pelepah Letter \"U\"?"
            ),
            AuditItem.Question(
                key = "kebunQ3",
                question = "3. Sudah melakukan Pembersihan piringan?"
            ),
            AuditItem.Question(
                key = "kebunQ4",
                question = "4. Sudah melakukan Penunasan?"
            ),
            AuditItem.Question(
                key = "kebunQ5",
                question = "5. Sudah melakukan pembersihan kebun dari gulma?"
            ),
            AuditItem.Question(
                key = "kebunQ6",
                question = "6. Sudah melakukan penanaman Tanaman Inang (Beneficial Plant)?"
            ),AuditItem.Question(
                key = "kebunQ7",
                question = "7. Tidak ada praktek bakar atau bekas bakaran?"
            ),AuditItem.Question(
                key = "kebunQ8",
                question = "8. Tidak ada Penggunaan Paraquat (Racun Bakar)?"
            ),AuditItem.Question(
                key = "kebunQ9",
                question = "9. Tidak ada Praktek Kerja Paksa?"
            ),AuditItem.Question(
                key = "kebunQ10",
                question = "10. Tidak ada Limbah Bahan Berbahaya dan Beracun (B3) di kebun?"
            ),AuditItem.Question(
                key = "kebunQ11",
                question = "11. Tidak ada praktik semprot total di areal kebun?"
            )






        )
    }
}