package com.example.notasawit.Admin.AuditInternal

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.example.notasawit.Admin.AuditInternal.data.Section2QuestionData
import com.example.notasawit.Admin.AuditInternal.data.Section3QuestionData
import com.example.notasawit.Admin.AuditInternal.model.AuditItem
import com.example.notasawit.Room.AuditEntity.AuditHeader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN_BOTTOM = 50f
    private const val MARGIN_LEFT = 50f

    fun generatePdf(context: Context, auditHeader: AuditHeader, auditAnswers: Map<String, Boolean?>): String? {
        val pdfDocument = PdfDocument()
        
        // Define colors
        val primaryColor = Color.parseColor("#2E7D32") // Dark Green
        val secondaryColor = Color.parseColor("#4CAF50")
        val textColor = Color.DKGRAY
        
        val titlePaint = Paint().apply {
            textAlign = Paint.Align.CENTER
            textSize = 24f
            isFakeBoldText = true
            color = Color.WHITE
        }
        val headerPaint = Paint().apply {
            color = primaryColor
            style = Paint.Style.FILL
        }
        val textPaint = Paint().apply {
            textSize = 12f
            color = textColor
        }
        val boldPaint = Paint().apply {
            textSize = 14f
            color = primaryColor
            isFakeBoldText = true
        }
        val borderPaint = Paint().apply {
            color = primaryColor
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        val lineSpacing = 22f

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        var yPosition = 90f

        fun checkNewPage() {
            if (yPosition > PAGE_HEIGHT - MARGIN_BOTTOM - 20) {
                // Draw border for the current page before finishing
                canvas.drawRect(MARGIN_LEFT - 10, 20f, PAGE_WIDTH - MARGIN_LEFT + 10, PAGE_HEIGHT - 20f, borderPaint)
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPosition = 50f 
            }
        }

        fun drawTextWrapped(text: String, x: Float, paint: Paint, maxWidth: Float) {
            val lines = wrapText(text, paint, maxWidth)
            for (line in lines) {
                canvas.drawText(line, x, yPosition, paint)
                yPosition += lineSpacing
                checkNewPage()
            }
        }

        // Draw Top Header Background
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 80f, headerPaint)
        // Draw Title
        canvas.drawText("Laporan Audit Internal", PAGE_WIDTH / 2f, 50f, titlePaint)
        
        yPosition = 100f

        // Section 1: Data Awal
        canvas.drawText("BAGIAN 1: DATA AWAL", MARGIN_LEFT, yPosition, boldPaint)
        yPosition += lineSpacing + 5
        canvas.drawText("Tanggal Audit: ${auditHeader.tanggal}", MARGIN_LEFT + 20, yPosition, textPaint)
        yPosition += lineSpacing
        canvas.drawText("Nama Auditor: ${auditHeader.namaAuditor}", MARGIN_LEFT + 20, yPosition, textPaint)
        yPosition += lineSpacing
        canvas.drawText("Nama Petani: ${auditHeader.namaPetani}", MARGIN_LEFT + 20, yPosition, textPaint)
        yPosition += lineSpacing
        canvas.drawText("Desa: ${auditHeader.desa}", MARGIN_LEFT + 20, yPosition, textPaint)
        yPosition += lineSpacing * 2
        checkNewPage()

        fun drawQuestions(items: List<AuditItem>) {
            for (item in items) {
                when (item) {
                    is AuditItem.Header -> {
                        yPosition += lineSpacing // Extra space before header
                        checkNewPage()
                        canvas.drawText(item.title, MARGIN_LEFT, yPosition, boldPaint)
                        yPosition += lineSpacing
                        checkNewPage()
                    }
                    is AuditItem.Question -> {
                        val answerStr = getAnswerValue(auditAnswers, item.key)
                        val questionText = "${item.question} Jawaban: $answerStr"
                        drawTextWrapped(questionText, MARGIN_LEFT + 10, textPaint, PAGE_WIDTH - MARGIN_LEFT - 20)
                    }
                }
            }
        }

        // Section 2 Questions
        canvas.drawText("BAGIAN 2: PENGETAHUAN", MARGIN_LEFT, yPosition, boldPaint)
        yPosition += lineSpacing
        checkNewPage()
        
        drawQuestions(Section2QuestionData.getQuestions())
        
        yPosition += lineSpacing
        checkNewPage()

        // Section 3 Questions
        canvas.drawText("BAGIAN 3: IMPLEMENTASI RSPO", MARGIN_LEFT, yPosition, boldPaint)
        yPosition += lineSpacing
        checkNewPage()

        drawQuestions(Section3QuestionData.getQuestions())

        yPosition += lineSpacing
        checkNewPage()

        // Section 4: Kesimpulan
        canvas.drawText("BAGIAN 4: KESIMPULAN", MARGIN_LEFT, yPosition, boldPaint)
        yPosition += lineSpacing
        checkNewPage()

        canvas.drawText("Ringkasan Temuan:", MARGIN_LEFT + 10, yPosition, textPaint)
        yPosition += lineSpacing
        checkNewPage()
        drawTextWrapped(auditHeader.ringkasanTemuan.ifEmpty { "-" }, MARGIN_LEFT + 20, textPaint, PAGE_WIDTH - MARGIN_LEFT - 40)
        
        yPosition += lineSpacing
        checkNewPage()

        canvas.drawText("Rencana Perbaikan:", MARGIN_LEFT + 10, yPosition, textPaint)
        yPosition += lineSpacing
        checkNewPage()
        drawTextWrapped(auditHeader.rencanaPerbaikan.ifEmpty { "-" }, MARGIN_LEFT + 20, textPaint, PAGE_WIDTH - MARGIN_LEFT - 40)
        
        yPosition += lineSpacing
        checkNewPage()

        canvas.drawText("Rencana Pemeriksaan: ${auditHeader.rencanaPemeriksaan}", MARGIN_LEFT + 10, yPosition, textPaint)
        yPosition += lineSpacing * 2
        checkNewPage()
        
        // Bukti Pelaksanaan Audit (Image)
        if (auditHeader.fotoPath.isNotEmpty()) {
            val imgFile = File(auditHeader.fotoPath)
            if (imgFile.exists()) {
                canvas.drawText("Bukti Pelaksanaan Audit:", MARGIN_LEFT + 10, yPosition, boldPaint)
                yPosition += lineSpacing
                checkNewPage()
                
                try {
                    val bitmap = android.graphics.BitmapFactory.decodeFile(imgFile.absolutePath)
                    if (bitmap != null) {
                        // Calculate scale to fit page width with margins
                        val maxImgWidth = PAGE_WIDTH - (MARGIN_LEFT * 2) - 20
                        val scale = maxImgWidth / bitmap.width.toFloat()
                        val scaledHeight = bitmap.height * scale
                        
                        // Check if it fits on current page
                        if (yPosition + scaledHeight > PAGE_HEIGHT - MARGIN_BOTTOM - 20) {
                            yPosition = PAGE_HEIGHT - MARGIN_BOTTOM - 10f // force new page
                            checkNewPage()
                        }
                        
                        val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(bitmap, maxImgWidth.toInt(), scaledHeight.toInt(), true)
                        canvas.drawBitmap(scaledBitmap, MARGIN_LEFT + 10, yPosition, null)
                        yPosition += scaledHeight + lineSpacing
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    canvas.drawText("(Gagal memuat foto)", MARGIN_LEFT + 20, yPosition, textPaint)
                    yPosition += lineSpacing
                }
            }
        }

        // Final page border
        canvas.drawRect(MARGIN_LEFT - 10, 20f, PAGE_WIDTH - MARGIN_LEFT + 10, PAGE_HEIGHT - 20f, borderPaint)
        pdfDocument.finishPage(page)

        // Save
        val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        if (directory != null && !directory.exists()) {
            directory.mkdirs()
        }

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Audit_${auditHeader.namaPetani.replace(" ", "_")}_$timeStamp.pdf"
        val file = File(directory, fileName)

        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            file.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            null
        } finally {
            pdfDocument.close()
        }
    }

    private fun getAnswerValue(auditAnswers: Map<String, Boolean?>, key: String): String {
        val boolVal = auditAnswers[key]
        return when (boolVal) {
            true -> "Ya"
            false -> "Tidak"
            else -> "Belum Diisi"
        }
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val lines = mutableListOf<String>()
        val words = text.split(" ")
        var currentLine = ""

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val testWidth = paint.measureText(testLine)
            if (testWidth > maxWidth) {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine)
                }
                currentLine = word
            } else {
                currentLine = testLine
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }
        return lines
    }
}
