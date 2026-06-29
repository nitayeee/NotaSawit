package com.example.notasawit.Admin.AuditInternal.AuditViewModel


import androidx.lifecycle.ViewModel
import com.example.notasawit.Admin.AuditInternal.model.AuditItem
import com.example.notasawit.Room.AuditEntity.AuditForm
import java.util.UUID

class AuditViewModel : ViewModel() {

    var auditForm = AuditForm(
        idAudit = UUID.randomUUID().toString()
    )
    // Jawaban Section 2
    var section2Answers = mutableListOf<AuditItem>()

    // Jawaban Section 3
    var section3Answers = mutableListOf<AuditItem>()

}