package com.example.notasawit.Admin.KunjunganLahan

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.example.notasawit.Admin.KunjunganLahan.data.KunjunganQuestionData
import com.example.notasawit.Admin.KunjunganLahan.model.KunjunganItem
import com.example.notasawit.Room.KunjunganLahanEntity.KunjunganLahanForm
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

object PdfGeneratorKunjungan {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN_LEFT = 40f
    private const val MARGIN_RIGHT = 40f
    private const val MARGIN_TOP = 50f
    private const val MARGIN_BOTTOM = 50f

    fun generatePdf(context: Context, form: KunjunganLahanForm, answers: List<KunjunganItem> = emptyList()): String? {
        val pdfDocument = PdfDocument()

        val titlePaint = Paint().apply {
            textAlign = Paint.Align.CENTER
            textSize = 14f
            isFakeBoldText = true
            isUnderlineText = true
            color = Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val textPaint = Paint().apply {
            textSize = 10.5f
            color = Color.BLACK
        }
        val boldTextPaint = Paint().apply {
            textSize = 10.5f
            isFakeBoldText = true
            color = Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val answerPaint = Paint().apply {
            textSize = 10.5f
            color = Color.BLACK
        }
        val borderPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f
            color = Color.BLACK
        }

        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        var yPosition = MARGIN_TOP

        fun checkNewPage(requiredSpace: Float = 0f) {
            if (yPosition + requiredSpace > PAGE_HEIGHT - MARGIN_BOTTOM) {
                pdfDocument.finishPage(page)
                val newPageNumber = pdfDocument.pages.size + 1
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, newPageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPosition = MARGIN_TOP
            }
        }

        // TITLE
        canvas.drawText("FORMULIR KUNJUNGAN LAPANGAN", (PAGE_WIDTH / 2).toFloat(), yPosition, titlePaint)
        yPosition += 30f

        val lineSpacing = 16f
        val labelCol1 = MARGIN_LEFT
        val colonCol1 = MARGIN_LEFT + 110f
        val valCol1 = MARGIN_LEFT + 120f

        val labelCol2 = MARGIN_LEFT + 280f
        val colonCol2 = MARGIN_LEFT + 370f
        val valCol2 = MARGIN_LEFT + 380f

        // HEADER INFO
        canvas.drawText("Tanggal", labelCol1, yPosition, textPaint)
        canvas.drawText(":", colonCol1, yPosition, textPaint)
        canvas.drawText(form.tanggal, valCol1, yPosition, textPaint)

        canvas.drawText("Desa Kebun", labelCol2, yPosition, textPaint)
        canvas.drawText(":", colonCol2, yPosition, textPaint)
        canvas.drawText(form.desaKebun, valCol2, yPosition, textPaint)

        yPosition += lineSpacing + 5f

        canvas.drawText("Nama Petani", labelCol1, yPosition, textPaint)
        canvas.drawText(":", colonCol1, yPosition, textPaint)
        canvas.drawText(form.namaPetani, valCol1, yPosition, textPaint)

        canvas.drawText("Desa Kepengurusan", labelCol2, yPosition, textPaint)
        canvas.drawText(":", colonCol2, yPosition, textPaint)
        canvas.drawText(form.desaKepengurusan, valCol2, yPosition, textPaint)

        yPosition += lineSpacing + 5f

        canvas.drawText("Auditor", labelCol1, yPosition, textPaint)
        canvas.drawText(":", colonCol1, yPosition, textPaint)
        canvas.drawText(form.namaAuditor, valCol1, yPosition, textPaint)

        canvas.drawText("Status / Periode", labelCol2, yPosition, textPaint)
        canvas.drawText(":", colonCol2, yPosition, textPaint)
        canvas.drawText("${form.statusKunjungan} (${form.periode} ke-${form.visitAttempt})", valCol2, yPosition, boldTextPaint)

        yPosition += 30f

        // QUESTIONS
        val questionsList = if (answers.isNotEmpty()) answers else KunjunganQuestionData.getQuestions()
        
        val leftBoxWidth = 340f
        val rightBoxWidth = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT - leftBoxWidth
        
        for (item in questionsList) {
            if (item is KunjunganItem.Question) {
                val answerText = item.textAnswer ?: getAnswerValue(form, item.key)
                
                // Calculate required height
                val standardLines = wrapText(item.standard, boldTextPaint, rightBoxWidth - 10f)
                val answerLines = wrapText(answerText, answerPaint, leftBoxWidth - 20f)
                
                val qLines = wrapText(item.question, textPaint, leftBoxWidth - 10f)
                
                val maxContentLines = max(standardLines.size, max(answerLines.size, 2))
                val boxHeight = maxContentLines * lineSpacing + 20f // 10f padding top and bottom
                
                val totalRequiredSpace = (qLines.size * lineSpacing) + boxHeight + 15f
                
                checkNewPage(requiredSpace = totalRequiredSpace)
                
                // Draw Question
                for (line in qLines) {
                    canvas.drawText(line, MARGIN_LEFT, yPosition, textPaint)
                    yPosition += lineSpacing
                }
                
                // Draw "Standar" word slightly above the box on the right
                canvas.drawText("Standar", MARGIN_LEFT + leftBoxWidth + 2f, yPosition - 4f, textPaint)
                
                // Draw Rectangles
                canvas.drawRect(MARGIN_LEFT, yPosition, MARGIN_LEFT + leftBoxWidth, yPosition + boxHeight, borderPaint)
                canvas.drawRect(MARGIN_LEFT + leftBoxWidth, yPosition, PAGE_WIDTH - MARGIN_RIGHT, yPosition + boxHeight, borderPaint)
                
                // Draw Standard text (centered vertically and horizontally)
                val standardTotalHeight = standardLines.size * lineSpacing
                var standardY = yPosition + (boxHeight - standardTotalHeight) / 2f + textPaint.textSize - 2f
                for (line in standardLines) {
                    val textWidth = boldTextPaint.measureText(line)
                    val startX = MARGIN_LEFT + leftBoxWidth + (rightBoxWidth - textWidth) / 2f
                    canvas.drawText(line, startX, standardY, boldTextPaint)
                    standardY += lineSpacing
                }

                // Draw Answer text (centered vertically)
                val answerTotalHeight = answerLines.size * lineSpacing
                var answerY = yPosition + (boxHeight - answerTotalHeight) / 2f + answerPaint.textSize - 2f
                for (line in answerLines) {
                    canvas.drawText(line, MARGIN_LEFT + 10f, answerY, answerPaint)
                    answerY += lineSpacing
                }
                
                yPosition += boxHeight + 8f
            }
        }
        
        // Signatures
        checkNewPage(requiredSpace = 80f)
        yPosition += 20f
        canvas.drawText("(Auditor)", MARGIN_LEFT + 40f, yPosition + 50f, boldTextPaint)
        canvas.drawText("(Petani)", PAGE_WIDTH - MARGIN_RIGHT - 80f, yPosition + 50f, boldTextPaint)

        pdfDocument.finishPage(page)

        // --- HALAMAN BARU: BUKTI PELAKSANAAN ---
        val newPageNumber = pdfDocument.pages.size + 1
        pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, newPageNumber).create()
        page = pdfDocument.startPage(pageInfo)
        canvas = page.canvas
        yPosition = MARGIN_TOP

        // Judul Bukti Pelaksanaan (posisi & font sama seperti "FORMULIR KUNJUNGAN LAPANGAN")
        canvas.drawText("BUKTI PELAKSANAAN", (PAGE_WIDTH / 2).toFloat(), yPosition, titlePaint)
        yPosition += 30f

        // Waktu Pelaksanaan & Titik Koordinat
        canvas.drawText("Waktu Pelaksanaan", labelCol1, yPosition, textPaint)
        canvas.drawText(":", colonCol1, yPosition, textPaint)
        canvas.drawText(form.waktuBukti ?: "-", valCol1, yPosition, textPaint)

        yPosition += lineSpacing + 4f

        canvas.drawText("Titik Koordinat", labelCol1, yPosition, textPaint)
        canvas.drawText(":", colonCol1, yPosition, textPaint)
        val lat = form.latitude ?: 0.0
        val lng = form.longitude ?: 0.0
        val coordStr = if (lat != 0.0 || lng != 0.0) "$lat, $lng" else "-"
        canvas.drawText(coordStr, valCol1, yPosition, textPaint)

        yPosition += 25f

        // Ringkasan Temuan (jika ada)
        if (form.ringkasanTemuan.isNotBlank()) {
            canvas.drawText("Ringkasan Temuan / Catatan", labelCol1, yPosition, boldTextPaint)
            yPosition += lineSpacing
            val temuanLines = wrapText(form.ringkasanTemuan, textPaint, PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT)
            for (tLine in temuanLines) {
                canvas.drawText(tLine, labelCol1, yPosition, textPaint)
                yPosition += lineSpacing
            }
            yPosition += 15f
        }

        // Foto Bukti Pelaksanaan
        if (!form.fotoBuktiPath.isNullOrBlank()) {
            val imgFile = File(form.fotoBuktiPath)
            if (imgFile.exists()) {
                try {
                    val bitmap = android.graphics.BitmapFactory.decodeFile(imgFile.absolutePath)
                    if (bitmap != null) {
                        val maxImgWidth = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT
                        val scale = maxImgWidth / bitmap.width.toFloat()
                        var scaledHeight = bitmap.height * scale

                        val maxAllowedHeight = PAGE_HEIGHT - yPosition - MARGIN_BOTTOM - 20f
                        if (scaledHeight > maxAllowedHeight && maxAllowedHeight > 100f) {
                            scaledHeight = maxAllowedHeight
                        }

                        val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(
                            bitmap,
                            maxImgWidth.toInt(),
                            scaledHeight.toInt(),
                            true
                        )
                        canvas.drawBitmap(scaledBitmap, MARGIN_LEFT, yPosition, null)
                        yPosition += scaledHeight + 15f
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    canvas.drawText("(Gagal memuat foto bukti)", MARGIN_LEFT, yPosition, textPaint)
                }
            }
        }

        pdfDocument.finishPage(page)

        // Save
        val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        if (directory != null && !directory.exists()) {
            directory.mkdirs()
        }

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "KunjunganLahan_${form.namaPetani.replace(" ", "_")}_$timeStamp.pdf"
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

    private fun getAnswerValue(form: KunjunganLahanForm, key: String): String {
        val boolVal = when(key) {
            "q1_patokBatas" -> form.q1_patokBatas
            "q2_idKebun" -> form.q2_idKebun
            "q3_piringanPasarPikul" -> form.q3_piringanPasarPikul
            "q4_pelepahDitunas" -> form.q4_pelepahDitunas
            "q5_susunanPelepah" -> form.q5_susunanPelepah
            "q6_turnera" -> form.q6_turnera
            "q7_bekasPembakaran" -> form.q7_bekasPembakaran
            "q8_botolRacunPlastik" -> form.q8_botolRacunPlastik
            "q9_sampahPlastik" -> form.q9_sampahPlastik
            "q10_plangSungai" -> form.q10_plangSungai
            "q11_semprotSungai" -> form.q11_semprotSungai
            "q12_sampahSungai" -> form.q12_sampahSungai
            "q13_semprotTotal" -> form.q13_semprotTotal
            "q14_racunKontak" -> form.q14_racunKontak
            "q15_hamaPenyakit" -> form.q15_hamaPenyakit
            else -> null
        }
        return when (boolVal) {
            true -> "Ya"
            false -> "Tidak"
            else -> ""
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
