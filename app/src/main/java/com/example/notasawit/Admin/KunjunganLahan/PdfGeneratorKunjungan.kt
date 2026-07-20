package com.example.notasawit.Admin.KunjunganLahan

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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

object PdfGeneratorKunjungan {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN_BOTTOM = 50f
    private const val MARGIN_LEFT = 50f

    fun generatePdf(context: Context, form: KunjunganLahanForm): String? {
        val pdfDocument = PdfDocument()
        
        // Define colors
        val primaryColor = Color.parseColor("#2E7D32") // Dark Green
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
        canvas.drawText("Laporan Kunjungan Lahan", PAGE_WIDTH / 2f, 50f, titlePaint)
        
        yPosition = 100f

        // Section 1: Data Awal
        canvas.drawText("BAGIAN 1: DATA AWAL", MARGIN_LEFT, yPosition, boldPaint)
        yPosition += lineSpacing + 5
        canvas.drawText("Tanggal: ${form.tanggal}", MARGIN_LEFT + 20, yPosition, textPaint)
        yPosition += lineSpacing
        canvas.drawText("Nama Auditor: ${form.namaAuditor}", MARGIN_LEFT + 20, yPosition, textPaint)
        yPosition += lineSpacing
        canvas.drawText("Nama Petani: ${form.namaPetani}", MARGIN_LEFT + 20, yPosition, textPaint)
        yPosition += lineSpacing
        canvas.drawText("Desa Kebun: ${form.desaKebun}", MARGIN_LEFT + 20, yPosition, textPaint)
        yPosition += lineSpacing
        canvas.drawText("Desa Kepengurusan: ${form.desaKepengurusan}", MARGIN_LEFT + 20, yPosition, textPaint)
        yPosition += lineSpacing * 2
        checkNewPage()

        fun drawQuestions(items: List<KunjunganItem>) {
            for (item in items) {
                when (item) {
                    is KunjunganItem.Header -> {
                        yPosition += lineSpacing // Extra space before header
                        checkNewPage()
                        canvas.drawText(item.title, MARGIN_LEFT, yPosition, boldPaint)
                        yPosition += lineSpacing
                        checkNewPage()
                    }
                    is KunjunganItem.Question -> {
                        val answerStr = getAnswerValue(form, item.key)
                        val questionText = "${item.question} Jawaban: $answerStr"
                        drawTextWrapped(questionText, MARGIN_LEFT + 10, textPaint, PAGE_WIDTH - MARGIN_LEFT - 20)
                    }
                }
            }
        }

        drawQuestions(KunjunganQuestionData.getQuestions())
        
        yPosition += lineSpacing
        checkNewPage()

        // Final page border
        canvas.drawRect(MARGIN_LEFT - 10, 20f, PAGE_WIDTH - MARGIN_LEFT + 10, PAGE_HEIGHT - 20f, borderPaint)
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
