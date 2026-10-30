package com.example.notasawit.Notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.notasawit.DetailRiwayatKeuangan.RiwayatPemasukanActivity
import com.example.notasawit.DetailRiwayatKeuangan.RiwayatPengeluaranActivity
import com.example.notasawit.R
import java.text.NumberFormat
import java.util.Locale

object NotificationHelper {

    private const val CHANNEL_ID = "keuangan_notification_channel"
    private const val CHANNEL_NAME = "Notifikasi Transaksi Keuangan"
    private const val CHANNEL_DESC = "Pemberitahuan pencatatan produksi dan pengeluaran sawit"

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val existingChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (existingChannel == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_DESC
                    enableVibration(true)
                    enableLights(true)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    fun showProduksiNotification(
        context: Context,
        produksiId: Int,
        jumlahTbs: Int,
        totalPendapatan: Double
    ) {
        createNotificationChannel(context)

        val intent = Intent(context, RiwayatPemasukanActivity::class.java).apply {
            putExtra("produksi_id", produksiId)
            putExtra("from_notification", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val requestCode = if (produksiId != 0) produksiId else (System.currentTimeMillis() % 10000).toInt()

        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val formattedPendapatan = formatRupiah(totalPendapatan)
        val message = "Produksi TBS sebesar $jumlahTbs Kg (Rp $formattedPendapatan) berhasil disimpan. Ketuk untuk melihat detail."

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Pemasukan / Produksi Baru")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(defaultSound)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationId = 100000 + requestCode
        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun showPengeluaranNotification(
        context: Context,
        biayaId: Int,
        biayaNama: String,
        biayaTotal: Double
    ) {
        createNotificationChannel(context)

        val intent = Intent(context, RiwayatPengeluaranActivity::class.java).apply {
            putExtra("biaya_id", biayaId)
            putExtra("from_notification", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val requestCode = if (biayaId != 0) biayaId else (System.currentTimeMillis() % 10000).toInt()

        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val formattedTotal = formatRupiah(biayaTotal)
        val namaText = if (biayaNama.isNotBlank()) biayaNama else "Biaya Operasional"
        val message = "Pengeluaran $namaText sebesar Rp $formattedTotal berhasil disimpan. Ketuk untuk melihat detail."

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Pengeluaran Baru")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(defaultSound)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationId = 200000 + requestCode
        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun formatRupiah(nominal: Double): String {
        return try {
            val formatter = NumberFormat.getNumberInstance(Locale("id", "ID"))
            formatter.format(nominal.toLong())
        } catch (e: Exception) {
            nominal.toLong().toString()
        }
    }
}
