package com.example.notasawit.Admin.KunjunganLahan

import androidx.lifecycle.ViewModel
import com.example.notasawit.Admin.KunjunganLahan.model.KunjunganItem
import com.example.notasawit.Room.KunjunganLahanEntity.KunjunganLahanForm
import java.util.UUID

class KunjunganLahanViewModel : ViewModel() {

    var kunjunganLahanForm = KunjunganLahanForm(
        idKunjungan = UUID.randomUUID().toString()
    )
    
    // Jawaban Section 2
    var section2Answers = mutableListOf<KunjunganItem>()

    // Jawaban sebelumnya untuk pre-fill kunjungan ulang (follow-up)
    val previousAnswers = mutableMapOf<String, Boolean>()
    val kunjunganAnswers = mutableMapOf<String, Boolean>()

    fun resetForm() {
        kunjunganLahanForm = KunjunganLahanForm(
            idKunjungan = UUID.randomUUID().toString()
        )
        section2Answers.clear()
        previousAnswers.clear()
        kunjunganAnswers.clear()
    }

    fun updatePetaniAndAuditor(idPetani: Int, namaPetani: String, namaAuditor: String, userId: Int? = null) {
        kunjunganLahanForm = kunjunganLahanForm.copy(
            idPetani = idPetani,
            namaPetani = namaPetani,
            namaAuditor = namaAuditor,
            userId = userId
        )
    }

    fun updatePeriodeAndAttempt(periode: String, attempt: Int) {
        kunjunganLahanForm = kunjunganLahanForm.copy(
            periode = periode,
            visitAttempt = attempt
        )
    }
}
