package com.example.notasawit.Admin.AuditInternal.data

import com.example.notasawit.Admin.AuditInternal.model.AuditItem

object Section2QuestionData {

    fun getQuestions(): List<AuditItem> {

        return listOf(

            AuditItem.Header("1. TENTANG ASOSIASI"),

            AuditItem.Question(
                key = "asosiasiQ1",
                question = "1. Apakah Bapak/Ibu mengetahui siapa ketua asosiasi?"
            ),

            AuditItem.Question(
                key = "asosiasiQ2",
                question = "2. Apakah Bapak/Ibu mengetahui sekretariat asosiasi?"
            ),

            AuditItem.Header("2. TENTANG STANDARD OPERATING PROCEDURE (SOP)"),

            AuditItem.Question(
                key = "sopQ1",
                question = "1. Apakah Bapak/Ibu tahu Prosedur Keanggotaan?"
            ),

            AuditItem.Question(
                key = "sopQ3",
                question = "2. Apakah Bapak/Ibu tahu Prosedur Komunikasi dan Konsultasi dengan Para Pihak?"
            ),

            AuditItem.Question(
                key = "sopQ3",
                question = "3. Apakah Bapak/Ibu tahu Prosedur Tugas dan Tanggung Jawab Pengurus Asosiasi?"
            ),

            AuditItem.Question(
                key = "sopQ4",
                question = "4. Apakah Bapak/Ibu tahu Prosedur Pelaksanaan Audit Internal?"
            ),

            AuditItem.Question(
                key = "sopQ5",
                question = "5. Apakah Bapak/Ibu tahu Prosedur Keselamatan dan Kesehatan Kerja (K3)?"
            ),

            AuditItem.Question(
                key = "sopQ6",
                question = "6. Apakah Bapak/Ibu tahu Prosedur Kepemilikan Lahan Anggota?"
            ),

            AuditItem.Question(
                key = "sopQ7",
                question = "7. Apakah Bapak/Ibu tahu Prosedur Free, Prior and Informed Consent (FPIC) atau Padiatapa?"
            ),

            AuditItem.Question(
                key = "sopQ8",
                question = "8. Apakah Bapak/Ibu tahu Prosedur Nilai Konservasi Tinggi (NKT)?"
            ),

            AuditItem.Question(
                key = "sopQ9",
                question = "9. Apakah Bapak/Ibu tahu Prosedur Penanaman Baru dan Replanting?"
            ),

            AuditItem.Question(
                key = "sopQ10",
                question = "10. Apakah Bapak/Ibu tahu Prosedur Pemupukan ?"
            ),

            AuditItem.Question(
                key = "sopQ11",
                question = "11. Apakah Bapak/Ibu tahu Prosedur Pengendalian Hama dan Penyakit Tanaman (HPT)?"
            ),

            AuditItem.Question(
                key = "sopQ12",
                question = "12. Apakah Bapak/Ibu tahu Prosedur Pemeliharaan Tanaman Kelapa Sawit?"
            ),

            AuditItem.Question(
                key = "sopQ13",
                question = "13. Apakah Bapak/Ibu tahu Prosedur Panen?"
            ),

            AuditItem.Question(
                key = "sopQ14",
                question = "14. Apakah Bapak/Ibu tahu Prosedur Penjualan Tandan Buah Segar (TBS)?"
            ),

            AuditItem.Question(
                key = "sopQ15",
                question = "15. Apakah Bapak/Ibu tahu Prosedur terkait Bahan Berbahaya dan Beracun (B3) dan Limbah Bahan Berbahaya dan Beracun (LB3)?"
            ),

            AuditItem.Question(
                key = "sopQ16",
                question = "16. Apakah Bapak/Ibu tahu Prosedur Pengelolaan Dokumen di Asosiasi?"
            ),

            AuditItem.Question(
                key = "sopQ17",
                question = "17. Apakah Bapak/Ibu tahu Prosedur Pengambilan Uang asosiasi?"
            ),

            AuditItem.Question(
                key = "sopQ18",
                question = "18. Apakah Bapak/Ibu tahu Prosedur Tenaga Kerja?"
            ),

            AuditItem.Header("3. TENTANG PELATIHAN-PELATIHAN"),

            AuditItem.Question(
                key = "pelatihanQ1",
                question = "1. Apakah Bapak/Ibu sudah mendapat pelatihan Prinsip dan Kriteria RSPO dan ISPO?"
            ),

            AuditItem.Question(
                key = "pelatihanQ2",
                question = "2. Apakah Bapak/Ibu sudah mendapat pelatihan Pemupukan?"
            ),
            AuditItem.Question(
                key = "pelatihanQ3",
                question = "3. Apakah Bapak/Ibu sudah mendapat pelatihan Pengendalian Hama Terpadu (PHT)?"
            ),
            AuditItem.Question(
                key = "pelatihanQ4",
                question = "4. Apakah Bapak/Ibu sudah mendapat pelatihan Panen yang Baik?"
            ),
            AuditItem.Question(
                key = "pelatihanQ5",
                question = "5. Apakah Bapak/Ibu sudah mendapat pelatihan Perawatan Kebun?"
            ),
            AuditItem.Question(
                key = "pelatihanQ6",
                question = "6. Apakah Bapak/Ibu sudah mendapat pelatihan penggunaan bahan kimia/pestisida yang aman?"
            ),
            AuditItem.Question(
                key = "pelatihanQ7",
                question = "7. Apakah Bapak/Ibu sudah mendapat pelatihan tentang Lingkungan, Nilai Konservasi Tinggi (NKT) dan hutan Stok Karbon Tinggi (SKT)?"
            ),
            AuditItem.Question(
                key = "pelatihanQ8",
                question = "8. Apakah Bapak/Ibu sudah mendapat pelatihan  Free, Prior and Informed Consent (FPIC)?"
            ),
            AuditItem.Question(
                key = "pelatihanQ9",
                question = "9. Apakah Bapak/Ibu sudah mendapat pelatihan  Keselamatan dan Kesehatan Kerja (K3)?"
            ),
            AuditItem.Question(
                key = "pelatihanQ10",
                question = "10. Apakah Bapak/Ibu sudah mendapat pelatihan terkait dengan aspek-aspek Sosial?"
            ),
            AuditItem.Question(
                key = "pelatihanQ11",
                question = "11. Apakah Bapak/Ibu sudah mendapat pelatihan tentang Financial Literacy atau pengelolaan keuangan?"
            ),
            AuditItem.Question(
                key = "pelatihanQ12",
                question = "12. Apakah Bapak/Ibu sudah mendapat pelatihan tentang ketenagakerjaan?"
            ),
            AuditItem.Question(
                key = "pelatihanQ13",
                question = "13. Apakah Bapak/Ibu sudah mendapat pelatihan tentang Bahan Berbahaya dan Beracun (B3) dan pengelolaan B3?"
            ),
            AuditItem.Question(
                key = "pelatihanQ14",
                question = "14. Apakah Bapak/Ibu sudah mendapat pelatihan tentang pencatatan Logbook?"
            ),

            AuditItem.Header("4. TENTANG BAHAN KIMIA DAN LIMBAH BAHAN BERBAHAYA & BERACUN (LB3)"),

            AuditItem.Question(
                key = "lb3Q1",
                question = "1. Apakah Bapak/Ibu tahu larangan semprot total?"
            ),
            AuditItem.Question(
                key = "lb3Q2",
                question = "2. Apakah Bapak/Ibu tahu bahan aktif Paraquat (racun bakar) dilarang penggunaannya?"
            ),
            AuditItem.Question(
                key = "lb3Q3",
                question = "3. Apakah Bapak/Ibu tahu kebun yang di pinggir sungai dilarang disemprot minimal 3 pokok (20 meter) dari pinggir sungai?"
            ),
            AuditItem.Question(
                key = "lb3Q4",
                question = "4. Apakah Bapak/Ibu tahu alat alat pelindung diri yang digunakan saat menyemprot ?"
            ),
            AuditItem.Question(
                key = "lb3Q5",
                question = "5. Apakah Bapak/Ibu tahu tata cara penyimpanan bahan pestisida yang tepat?"
            ),
            AuditItem.Question(
                key = "lb3Q6",
                question = "6. Apakah Bapak/Ibu tahu cara penanganan terhadap keracunan pestisida?"
            ),
            AuditItem.Question(
                key = "lb3Q7",
                question = "7. Apakah Bapak/Ibu sudah mengumpulkan wadah bekas pestisida di tempat yang aman?"
            ),

            AuditItem.Header("5. TENTANG NILAI KONSERVASI TINGGI (NKT)"),

            AuditItem.Question(
                key = "nktQ1",
                question = "1.  Apakah Bapak/ibu tahu bahwa asosiasi sudah melakukan identifikasi dan memiliki laporan Nilai Konservasi Tinggi (NKT)?"
            ),
            AuditItem.Question(
                key = "nktQ2",
                question = "2. Apakah Bapak/ibu tahu daftar hewan dan tumbuhan yang dilindungi?"
            ),
            AuditItem.Question(
                key = "nktQ3",
                question = "3. Apakah Bapak/ibu tahu penanganan jika menjumpai hewan dan tumbuhan yang dilindungi?"
            ),
            AuditItem.Question(
                key = "nktQ4",
                question = "4. Apakah Bapak/ibu tahu terdapat larangan membuka lahan dengan cara bakar?"
            ),
            AuditItem.Question(
                key = "nktQ5",
                question = "5. Apakah Bapak/ibu tahu  terdapat larangan membuang sampah ke sungai?"
            ),
            AuditItem.Question(
                key = "nktQ6",
                question = "6. Apakah Bapak/ibu tahu bahwa menangkap ikan dengan cara meracuni sungai itu dilarang?"
            ),
            AuditItem.Question(
                key = "nktQ7",
                question = "7. Apakah Bapak/ibu tahu pengeloaan lahan jika ditemukan areal lahan miring?"
            ),

            AuditItem.Header("6. SOSIAL DAN TENAGA KERJA"),

            AuditItem.Question(
                key = "sosialTenagaKerjaQ1",
                question = "1. Apakah Bapak/ibu tahu berapa Upah Minimum Kabupaten (UMK) atau Upah Minimum Provinsi (UMP) sekarang?"
            ),
            AuditItem.Question(
                key = "sosialTenagaKerjaQ2",
                question = "2. Apakah Bapak/ibu tahu bahwa ada larangan mempekerjakan anak di bawah umur?"
            ),
            AuditItem.Question(
                key = "sosialTenagaKerjaQ3",
                question = "3. Apakah Bapak/ibu tahu bahwa ada larangan diskriminasi atau ketidakadilan terhadap tenaga kerja?"
            ),
            AuditItem.Question(
                key = "sosialTenagaKerjaQ4",
                question = "4. Apakah Bapak/ibu tahu bahwa tidak boleh terjadi perbudakan atau tenaga kerja paksa dan pelecehan seksual?"
            ),
            AuditItem.Question(
                key = "sosialTenagaKerjaQ5",
                question = "5. Apakah Bapak/ibu tahu jika memiliki pekerja wajib dan sudah membuat kontrak kerja dengan pekerja (khusus bagi yang memiliki pekerja)? \nNB : Bagi Petani yang tidak memiliki pekerja silahkan memilih pilihan \"Ya\""
            ),
            AuditItem.Question(
                key = "sosialTenagaKerjaQ6",
                question = "Apakah Bapak/ibu tahu bahwa pekerja berhak berserikat/berorganisasi?"
            ),

            AuditItem.Header("7. KESELAMATAN DAN KESEHATAN KERJA (K3)"),

            AuditItem.Question(
                key = "k3Q1",
                question = "1. Apakah Bapak/Ibu sudah mengetahui terkait Hasil Identifikasi dan Mitigasi Resiko Kerja (HIRA) yang ada di Asosiasi?"
            ),
            AuditItem.Question(
                key = "k3Q2",
                question = "2. Apakah Bapak/ibu (Petani terdaftar) sudah ikut program BPJS ketenagakerjaan ?"
            ),
            AuditItem.Question(
                key = "k3Q3",
                question = "3. Apakah Bapak/Ibu (Petani Terdaftar) sudah memiliki dan menggunakan Alat Pelindung Diri (APD) untuk seluruh jenis pekerjaan?"
            ),
            AuditItem.Question(
                key = "k3Q4",
                question = "4. Apakah pekerja Bapak/Ibu sudah memiliki dan menggunakan Alat Pelindung Diri (APD) untuk seluruh jenis pekerjaan?"
            ),
            AuditItem.Question(
                key = "k3Q5",
                question = "5. Apakah Bapak/ibu (petani terdaftar) memastikan tidak pernah mengalami kecelakaan kerja sewaktu melakukan aktivitas di lahan?"
            ),
            AuditItem.Question(
                key = "k3Q6",
                question = "6. Apakah pekerja Bapak/ibu memastikan tidak pernah mengalami kecelakaan kerja sewaktu melakukan aktivitas di lahan (jika memiliki pekerja)?"
            ),
            AuditItem.Question(
                key = "k3Q7",
                question = "7. Apakah Bapak/Ibu tahu cara penggunaan isi kotak Pertolongan Pertama Pada Kecelakaan Kerja (P3K)?"
            ),
            AuditItem.Question(
                key = "k3Q8",
                question = "8. Apakah Bapak/Ibu tahu apa yang harus dilakukan jika terjadi keadaan Darurat (Kebakaran, Gempa Bumi, Banjir)?"
            ),
            AuditItem.Question(
                key = "k3Q9",
                question = "9. Apakah Bapak/Ibu sudah mengetahui tindakan jika terjadi kecelakaan kerja?\n"
            ),



            // dst...
        )
    }
}