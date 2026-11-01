package com.example.notasawit.Utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.net.URLEncoder

object AdminDesaHelper {

    data class AdminContact(
        val namaAdmin: String,
        val namaDesa: String,
        val desaId: Int,
        val noHpWa: String // Nomor WhatsApp format internasional (e.g. 6282285335295)
    )

    // Nomor default Layanan Asosiasi Pusat jika nomor admin desa belum terdaftar
    const val DEFAULT_ADMIN_PHONE = "6282285335295"

    // Pemetaan nomor WhatsApp per desa_id (nomor dummy dapat diedit sewaktu-waktu)
    val ADMIN_MAP = mapOf(
        1 to AdminContact("Nita Salsabilla", "Maredan", 1, "6282285335295"),
        2 to AdminContact("Admin Simpang Beringin", "Simpang Beringin", 2, "6282285335295"),
        3 to AdminContact("Zeez", "Sekijang", 3, "6282285335295"),
        4 to AdminContact("Dani Raditya", "Lubuk Ogung", 4, "6282285335295"),
        5 to AdminContact("Siti Solik", "Kerinci Kiri", 5, "6282285335295"),
        6 to AdminContact("User 1", "Kiab Jaya 1", 6, "6282285335295"),
        7 to AdminContact("Budi Santosos", "Kiab Jaya 2", 7, "6282285335295"),
        8 to AdminContact("M Ikhsan", "Langgam", 8, "6282285335295"),
        9 to AdminContact("Admin Muda Setia", "Muda Setia", 9, "6282285335295"),
        10 to AdminContact("Romusa", "Beringin Indah", 10, "6282285335295"),
        11 to AdminContact("Seventeen", "Dundangan", 11, "6282285335295"),
        12 to AdminContact("Aulia Aprilianti", "Sialang Indah", 12, "6282285335295"),
        19 to AdminContact("Dhini Handayani", "Kiab Jaya", 19, "6282285335295")
    )

    fun getAdminByDesaId(desaId: Int): AdminContact {
        return ADMIN_MAP[desaId] ?: AdminContact("Admin SILAUSA", "Desa Terkait", desaId, DEFAULT_ADMIN_PHONE)
    }

    /**
     * Membuka room chat WhatsApp langsung tanpa perlu menyimpan nomor ke kontak HP
     */
    fun openWhatsApp(context: Context, rawPhone: String, message: String) {
        var cleanPhone = rawPhone.replace("+", "").replace("-", "").replace(" ", "").trim()
        if (cleanPhone.startsWith("0")) {
            cleanPhone = "62" + cleanPhone.substring(1)
        }
        if (cleanPhone.isEmpty()) {
            cleanPhone = DEFAULT_ADMIN_PHONE
        }

        try {
            val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=${URLEncoder.encode(message, "UTF-8")}"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            try {
                // Prioritaskan membuka langsung aplikasi WhatsApp utama
                val waIntent = Intent(intent).setPackage("com.whatsapp")
                context.startActivity(waIntent)
            } catch (e1: Exception) {
                try {
                    // Coba WhatsApp Business
                    val w4bIntent = Intent(intent).setPackage("com.whatsapp.w4b")
                    context.startActivity(w4bIntent)
                } catch (e2: Exception) {
                    // Fallback ke browser jika aplikasi WhatsApp belum terpasang
                    context.startActivity(intent)
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Tidak dapat membuka WhatsApp", Toast.LENGTH_SHORT).show()
        }
    }
}

