package com.example.notasawit.Network

import android.content.Context
import android.net.Uri
import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Callback
import okhttp3.MultipartBody
import java.io.File
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.ByteArrayOutputStream

object PetaniApi {

    private const val BASE_URL = "http://160.187.144.157/api"

    fun registerPetani(
        nama: String,
        username: String,
        tglLahir: String,
        jk: String,
        email: String,
        noHp: String,
        alamat: String,
        desa: Int,
        pin: String,
        callback: Callback
    ) {

        val json = """
        {
          "petani_nama": "$nama",
          "petani_username": "$username",
          "petani_tanggal_lahir": "$tglLahir",
          "petani_jenis_kelamin": "$jk",
          "petani_email": "$email",
          "petani_no_hp": "$noHp",
          "petani_status": 0,
          "petani_alamat": "$alamat",
          "desa_id": "$desa",
          "petani_pin": "$pin"
        }
        """.trimIndent()

        val body = json.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$BASE_URL/register")
            .post(body)
            .build()

        ApiClient.client.newCall(request).enqueue(callback)
    }
    fun getDesa(callback: Callback) {
        val request = Request.Builder()
            .url("$BASE_URL/desa")
            .get()
            .build()
        ApiClient.client.newCall(request).enqueue(callback)
    }
    fun getJenisKegiatan(callback: Callback) {
        val request = Request.Builder()
            .url("$BASE_URL/jenis-kegiatan")
            .get()
            .build()
        ApiClient.client.newCall(request).enqueue(callback)
    }
    
    fun getAdmins(callback: Callback) {
        val request = Request.Builder()
            .url("$BASE_URL/users/admins")
            .get()
            .build()
        ApiClient.client.newCall(request).enqueue(callback)
    }

    fun getAllPetani(callback: Callback) {
        val request = Request.Builder()
            .url("$BASE_URL/petani")
            .get()
            .build()
        ApiClient.client.newCall(request).enqueue(callback)
    }

    fun getDetailPetani(petaniId: Int, callback: Callback) {
        val request = Request.Builder()
            .url("$BASE_URL/petani/$petaniId")
            .get()
            .build()
        ApiClient.client.newCall(request).enqueue(callback)
    }
    fun login(
        username: String,
        password: String,
        callback: Callback
    ) {

        val json = """
    {
        "username": "$username",
        "password": "$password"
    }
    """.trimIndent()

        val body = json.toRequestBody(
            "application/json".toMediaType()
        )

        val request = Request.Builder()
            .url("$BASE_URL/login")
            .post(body)
            .build()

        ApiClient.client.newCall(request)
            .enqueue(callback)
    }
    fun getLahanByPetani(
        petaniId: Int,
        callback: Callback
    ) {

        val request = Request.Builder()
            .url("$BASE_URL/lahan/petani/$petaniId")
            .get()
            .build()

        ApiClient.client.newCall(request)
            .enqueue(callback)
    }
    fun getRiwayatKeuangan(
        petaniId: Int,
        bulan: Int?,
        tahun: Int?,
        lahanId: Int?,
        tipe: String,
        callback: Callback
    ) {

        val url = buildString {

            append("$BASE_URL/riwayat-keuangan?")
            append("petani_id=$petaniId")

            bulan?.let {
                append("&bulan=$it")
            }

            tahun?.let {
                append("&tahun=$it")
            }

            lahanId?.let {
                append("&lahan_id=$it")
            }

            append("&tipe=$tipe")
        }

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        ApiClient.client
            .newCall(request)
            .enqueue(callback)
    }

    fun postProduksi(
        context: Context,
        produksiTanggal: String,
        jumlahTbs: Int,
        hargaTbs: Double,
        petaniId: Int,
        lahanIds: List<Int>,
        produksiKet: String,
        totalPendapatan: Double,
        imageUri: Uri?
    ): okhttp3.Call { // <-- 1. Tambahkan tipe kembalian Call di sini

        val builder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("produksi_tanggal", produksiTanggal)
            .addFormDataPart("jumlah_tbs", jumlahTbs.toString())
            .addFormDataPart("harga_tbs", hargaTbs.toString())
            .addFormDataPart("petani_id", petaniId.toString())
            .addFormDataPart("produksi_ket", produksiKet)
            .addFormDataPart("total_pendapatan", totalPendapatan.toString())

        lahanIds.forEach { id ->
            builder.addFormDataPart(
                "lahan_id[]",
                id.toString()
            )
        }

        imageUri?.let { uri ->
            Log.d("UPLOAD_IMAGE", "Image Uri: $uri")

            val bytes = if (uri.scheme == "file") {
                File(uri.path!!).readBytes()
            } else {
                context.contentResolver.openInputStream(uri)?.use {
                    it.readBytes()
                }
            }

            Log.d("UPLOAD_IMAGE", "Image size: ${bytes?.size}")

            Log.d("UPLOAD_IMAGE", "Image size before compression: ${bytes?.size}")

            if (bytes != null) {
                var finalBytes = bytes
                try {
                    // Kompres gambar agar tidak melebihi batas (maksimum upload PHP biasanya 2MB atau 5MB)
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bmp != null) {
                        val stream = ByteArrayOutputStream()
                        // Mulai dengan quality 80
                        var quality = 80
                        bmp.compress(Bitmap.CompressFormat.JPEG, quality, stream)
                        
                        // Jika masih lebih besar dari 1.5MB, turunkan kualitas
                        while (stream.toByteArray().size > 1.5 * 1024 * 1024 && quality > 10) {
                            stream.reset()
                            quality -= 15
                            bmp.compress(Bitmap.CompressFormat.JPEG, quality, stream)
                        }
                        finalBytes = stream.toByteArray()
                        Log.d("UPLOAD_IMAGE", "Image size after compression: ${finalBytes.size}")
                    }
                } catch (e: Exception) {
                    Log.e("UPLOAD_IMAGE", "Gagal kompres gambar", e)
                }

                builder.addFormDataPart(
                    "produksi_bukti",
                    "bukti.jpg",
                    finalBytes.toRequestBody("image/jpeg".toMediaType())
                )
            }
        }

        val requestBody = builder.build()

        val request = Request.Builder()
            .url("$BASE_URL/produksi")
            .post(requestBody)
            .build()

        // 2. KUNCI UTAMA: Kembalikan objek Call-nya, hapus .enqueue()
        return ApiClient.client.newCall(request)
    }
    fun postPengeluaran(
        context: Context,
        biayaTanggal: String,
        biayaJenis: String,
        biayaJumlah: Int,
        biayaNama: String,
        biayaKet: String,
        petaniId: Int,
        biayaTotal: Double,
        lahanIds: List<Int>, // 🔄 Diubah menjadi List<Int> agar sama dengan format pemasukan/kegiatan
        imageUri: Uri?
    ): okhttp3.Call { // 🔄 Diubah agar mengembalikan objek Call (tanpa callback & enqueue langsung)
        val builder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("biaya_tanggal", biayaTanggal)
            .addFormDataPart("biaya_jenis", biayaJenis)
            .addFormDataPart("biaya_jumlah", biayaJumlah.toString())
            .addFormDataPart("biaya_ket", biayaKet)
            .addFormDataPart("biaya_nama", biayaNama)
            .addFormDataPart("biaya_total", biayaTotal.toString())
            .addFormDataPart("petani_id", petaniId.toString())

        // 🔄 Mengirim semua lahan menggunakan array parameter format Laravel `lahan_id[]`
        lahanIds.forEach { id ->
            builder.addFormDataPart(
                "lahan_id[]",
                id.toString()
            )
        }

        imageUri?.let { uri ->
            Log.d("UPLOAD_IMAGE", "Image Uri: $uri")

            val bytes = if (uri.scheme == "file") {
                File(uri.path!!).readBytes()
            } else {
                context.contentResolver.openInputStream(uri)?.use {
                    it.readBytes()
                }
            }

            if (bytes != null) {
                var finalBytes = bytes
                try {
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bmp != null) {
                        val stream = ByteArrayOutputStream()
                        var quality = 80
                        bmp.compress(Bitmap.CompressFormat.JPEG, quality, stream)

                        while (stream.toByteArray().size > 1.5 * 1024 * 1024 && quality > 10) {
                            stream.reset()
                            quality -= 15
                            bmp.compress(Bitmap.CompressFormat.JPEG, quality, stream)
                        }
                        finalBytes = stream.toByteArray()
                        Log.d("UPLOAD_IMAGE", "Image size after compression: ${finalBytes.size}")
                    }
                } catch (e: Exception) {
                    Log.e("UPLOAD_IMAGE", "Gagal kompres gambar", e)
                }

                builder.addFormDataPart(
                    "biaya_bukti",
                    "bukti.jpg",
                    finalBytes.toRequestBody("image/jpeg".toMediaType())
                )
            }
        }

        val requestBody = builder.build()

        val request = Request.Builder()
            .url("$BASE_URL/biaya-operasional")
            .post(requestBody)
            .header("Accept", "application/json") // <-- TAMBAHKAN INI
            .build()

        // 🔄 KUNCI UTAMA: Mengembalikan objek Call agar bisa dieksekusi via Repository/Worker secara sinkronus
        return ApiClient.client.newCall(request)
    }
    fun getDetailProduksi(
        produksiId: Int,
        callback: Callback
    ) {

        val request = Request.Builder()
            .url("$BASE_URL/produksi/$produksiId")
            .get()
            .build()


        ApiClient.client
            .newCall(request)
            .enqueue(callback)
    }
    fun getDetailBiayaOperasional(
        biayaOperasionalId: Int,
        callback: Callback
    ) {

        val request = Request.Builder()
            .url("$BASE_URL/biaya-operasional/$biayaOperasionalId")
            .get()
            .build()


        ApiClient.client
            .newCall(request)
            .enqueue(callback)
    }

    fun postKegiatan(
        kegiatanTanggal: String,
        kegiatanJumlah: Int,
        kegiatanSatuan: String,
        jenisKegiatanId: Int,
        petaniId: Int,
        kegiatanKet: String,
        lahanIds: List<Int>
    ): okhttp3.Call { // <-- 1. Tambahkan tipe kembalian Call di sini

        val builder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("kegiatan_tanggal", kegiatanTanggal)
            .addFormDataPart("kegiatan_jumlah", kegiatanJumlah.toString())
            .addFormDataPart("kegiatan_satuan", kegiatanSatuan)
            .addFormDataPart("jenis_kegiatan_id", jenisKegiatanId.toString())
            .addFormDataPart("petani_id", petaniId.toString())
            .addFormDataPart("kegiatan_ket", kegiatanKet)

        // kirim semua lahan
        lahanIds.forEach { id ->
            builder.addFormDataPart(
                "lahan_id[]",
                id.toString()
            )
        }

        val requestBody = builder.build()

        val request = Request.Builder()
            .url("$BASE_URL/kegiatan")
            .post(requestBody)
            .build()

        // 2. KUNCI UTAMA: Kembalikan objek Call-nya, hapus .enqueue()
        return ApiClient.client.newCall(request)
    }

    fun getRiwayatKegiatan(
        petaniId: Int,
        callback: Callback
    ) {

        val url = "$BASE_URL/api/kegiatan?petani_id=$petaniId"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        ApiClient.client
            .newCall(request)
            .enqueue(callback)
    }
    fun getDetailKegiatan(
        petaniId: Int,
        callback: Callback
    ) {

        val url = "$BASE_URL/api/detail-kegiatan?petani_id=$petaniId"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        ApiClient.client
            .newCall(request)
            .enqueue(callback)
    }

    fun postKunjunganLapangan(
        tanggalKunjungan: String,
        desaKebun: String,
        desaKepengurusan: String,
        namaAuditor: String,
        pdfPath: String
    ): okhttp3.Call {

        val builder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("tanggal_kunjungan", tanggalKunjungan)
            .addFormDataPart("desa_kebun", desaKebun)
            .addFormDataPart("desa_kepengurusan", desaKepengurusan)
            .addFormDataPart("nama_auditor", namaAuditor)

        if (pdfPath.isNotEmpty()) {
            val file = File(pdfPath)
            if (file.exists()) {
                val bytes = file.readBytes()
                val mediaType = if (file.extension.lowercase() == "pdf") {
                    "application/pdf".toMediaType()
                } else {
                    "image/*".toMediaType()
                }
                builder.addFormDataPart(
                    "file_kunjungan",
                    file.name,
                    bytes.toRequestBody(mediaType)
                )
            }
        }

        val requestBody = builder.build()

        val request = Request.Builder()
            .url("$BASE_URL/kunjungan-lapangan") // Sesuaikan URL dengan route di api.php
            .post(requestBody)
            .header("Accept", "application/json")
            .build()

        return ApiClient.client.newCall(request)
    }

    fun postAuditInternal(
        idAudit: String,
        userId: Int,
        tanggal: String,
        desa: String,
        namaAuditor: String,
        namaPetani: String,
        pdfPath: String
    ): okhttp3.Call {

        val builder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("id_audit", idAudit)
            .addFormDataPart("user_id", userId.toString())
            .addFormDataPart("tanggal", tanggal)
            .addFormDataPart("desa", desa)
            .addFormDataPart("nama_auditor", namaAuditor)
            .addFormDataPart("nama_petani", namaPetani)

        val file = java.io.File(pdfPath)
        if (file.exists()) {
            val mediaType = "application/pdf".toMediaTypeOrNull()
            builder.addFormDataPart(
                "file_kunjungan", file.name,
                file.readBytes().toRequestBody(mediaType)
            )
        }

        val requestBody = builder.build()

        val request = Request.Builder()
            .url("$BASE_URL/audit-internal")
            .post(requestBody)
            .build()

        return ApiClient.client.newCall(request)
    }

    fun getNotifications(
        petaniId: Int,
        callback: Callback
    ) {
        val request = Request.Builder()
            .url("$BASE_URL/audit-internal/petani/$petaniId")
            .get()
            .header("Accept", "application/json")
            .build()
        ApiClient.client.newCall(request).enqueue(callback)
    }

    fun markNotificationAsRead(type: String, id: Int, callback: Callback) {
        val json = """
            {
                "type": "$type",
                "id": $id
            }
        """.trimIndent()
        
        val requestBody = json.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        
        val request = Request.Builder()
            .url("$BASE_URL/notifications/read")
            .put(requestBody)
            .build()
        ApiClient.client.newCall(request).enqueue(callback)
    }
}