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
}
