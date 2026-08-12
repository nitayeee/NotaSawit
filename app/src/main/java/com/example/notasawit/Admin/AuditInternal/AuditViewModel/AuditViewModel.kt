package com.example.notasawit.Admin.AuditInternal.AuditViewModel

import androidx.lifecycle.ViewModel
import com.example.notasawit.Admin.AuditInternal.model.AuditItem
import com.example.notasawit.Room.AuditEntity.AuditHeader
import java.util.UUID

class AuditViewModel : ViewModel() {

    var auditHeader = AuditHeader(
        idAudit = UUID.randomUUID().toString()
    )
    
    // Menyimpan semua jawaban dengan format Map<questionKey, Boolean?>
    val auditAnswers = mutableMapOf<String, Boolean?>()

    // Menyimpan jawaban dari audit sebelumnya (untuk Audit Ulang)
    val previousAnswers = mutableMapOf<String, Boolean?>()

    // Jawaban Section 2 yang sedang di-load di UI
    var section2Answers = mutableListOf<AuditItem>()

    // Jawaban Section 3 yang sedang di-load di UI
    var section3Answers = mutableListOf<AuditItem>()

    fun updatePetaniAndAuditor(idPetani: Int, namaPetani: String, namaAuditor: String) {
        auditHeader = auditHeader.copy(
            idPetani = idPetani,
            namaPetani = namaPetani,
            namaAuditor = namaAuditor
        )
    }

    fun updatePeriodeAndAttempt(periode: String, attempt: Int) {
        auditHeader = auditHeader.copy(
            periode = periode,
            auditAttempt = attempt
        )
    }

}