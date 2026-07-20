package com.example.notasawit.Admin.KunjunganLahan.data

import com.example.notasawit.Admin.KunjunganLahan.model.KunjunganItem

object KunjunganQuestionData {
    fun getQuestions(): List<KunjunganItem> {
        return listOf(
            KunjunganItem.Header("HASIL KUNJUNGAN LAPANGAN"),
            
            KunjunganItem.Question(
                key = "q1_patokBatas",
                question = "1. Apakah ada dipasang patok batas ?",
                standard = "Patok permanen (paralon cor) ditanam disetiap sudut"
            ),
            KunjunganItem.Question(
                key = "q2_idKebun",
                question = "2. Apakah ada dipasang ID Kebun ?",
                standard = "ID Kebun dipasang dikebun dengan keterangan (nama, luas kebun, nomor blok, APSKSPS)"
            ),
            KunjunganItem.Question(
                key = "q3_piringanPasarPikul",
                question = "3. Bagaimana kondisi piringan dan pasar pikul ?",
                standard = "Piringan dan pasar pikul harus bersih disarankan dengan cara babat, dilarang melakukan semprot total"
            ),
            KunjunganItem.Question(
                key = "q4_pelepahDitunas",
                question = "4. Apakah pelepah ditunas dengan baik ?",
                standard = "Egrek songgoh 1 dan dodos songgoh 2"
            ),
            KunjunganItem.Question(
                key = "q5_susunanPelepah",
                question = "5. Bagaimana susunan pelepah ?",
                standard = "Pelepah sudah disusun Latter \"U\""
            ),
            KunjunganItem.Question(
                key = "q6_turnera",
                question = "6. Apakah ada ditanam turnera ?",
                standard = "Menanam tanaman turnera"
            ),
            KunjunganItem.Question(
                key = "q7_bekasPembakaran",
                question = "7. Apakah ada bekas pembakaran dikebun ?",
                standard = "Jangan ada bekas pembakaran dikebun baik itu sampah, pelepah atau penanganan hama"
            ),
            KunjunganItem.Question(
                key = "q8_botolRacunPlastik",
                question = "8. Apakah ada botol racun atau plastik pupuk dikebun ?",
                standard = "Botol racun dan plastik pupuk tidak boleh ada dikebun harus dikumpulkan ke gudang limbah"
            ),
            KunjunganItem.Question(
                key = "q9_sampahPlastik",
                question = "9. Apakah ada sampah plastik dikebun ?",
                standard = "Sampah plastik harus dibersihkan tidak boleh dibakar dikebun"
            ),
            KunjunganItem.Question(
                key = "q10_plangSungai",
                question = "10. Untuk kebun dipinggir sungai, apakah ada dipasang plang himbauan pinggir sungai ?",
                standard = "Plang pinggir sungai sudah harus terpasang tepat 3 pokok atau 20 meter dari pinggir sungai"
            ),
            KunjunganItem.Question(
                key = "q11_semprotSungai",
                question = "11. Apakah ada semprot atau pemupukan kimia di pinggir sungai untuk kebun dipinggir sungai ?",
                standard = "Tidak boleh menyemprot dan mupuk kimia 3 pokok atau 20 meter dari pinggir sungai"
            ),
            KunjunganItem.Question(
                key = "q12_sampahSungai",
                question = "12. Apakah ada membuat sampah atau pelepah dipinggir sungai ?",
                standard = "Jangan membuang sampah dan pelepah dipinggir sungai"
            ),
            KunjunganItem.Question(
                key = "q13_semprotTotal",
                question = "13. Apakah masih semprot total dikebun ?",
                standard = "Tidak boleh semprot total harus semprot piringan dan pasar pikul"
            ),
            KunjunganItem.Question(
                key = "q14_racunKontak",
                question = "14. Apakah masih memakai racun kontak (paraquat) ?",
                standard = "Dianjurkan menggunakan racun kuning (Round Up, Bablas, Prima Up, Metsulindo, Garlon, Ally)"
            ),
            KunjunganItem.Question(
                key = "q15_hamaPenyakit",
                question = "15. Apakah terdapat hama dan penyakit ?\n(Ulat pemakan daun, Kumbang Tanduk, Tikus, Rayap, Ganoderma)",
                standard = "Kondisi bebas dari hama dan penyakit (Jika ada, harap ditangani)" // I added a small standard since it was empty in the image
            )
        )
    }
}
