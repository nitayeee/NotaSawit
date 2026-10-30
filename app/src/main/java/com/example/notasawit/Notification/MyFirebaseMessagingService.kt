package com.example.notasawit.Notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.notasawit.Admin.Tugas.DaftarTugasActivity
import com.example.notasawit.Network.PetaniApi
import com.example.notasawit.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val type = remoteMessage.data["type"] ?: ""
        val dataId = remoteMessage.data["id"]?.toIntOrNull()
            ?: remoteMessage.data["produksi_id"]?.toIntOrNull()
            ?: remoteMessage.data["biaya_id"]?.toIntOrNull()
            ?: 0

        val title = remoteMessage.notification?.title 
            ?: remoteMessage.data["title"] 
            ?: when (type) {
                "produksi" -> "Pemasukan / Produksi Baru"
                "pengeluaran" -> "Pengeluaran Baru"
                else -> "Tugas Baru"
            }

        val message = remoteMessage.notification?.body 
            ?: remoteMessage.data["body"] 
            ?: remoteMessage.data["pesan"] 
            ?: when (type) {
                "produksi" -> "Ada data produksi baru. Ketuk untuk melihat detail."
                "pengeluaran" -> "Ada data pengeluaran baru. Ketuk untuk melihat detail."
                else -> "Anda menerima tugas baru dari Superadmin."
            }

        sendNotification(title, message, type, dataId)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        val sharedPref = applicationContext.getSharedPreferences("user_pref", Context.MODE_PRIVATE)
        val userId = sharedPref.getString("user_id", null) ?: sharedPref.getString("id", null)

        if (!userId.isNullOrEmpty()) {
            PetaniApi.sendFcmToken(userId, token, object : Callback {
                override fun onFailure(call: Call, e: IOException) {}
                override fun onResponse(call: Call, response: Response) {}
            })
        }
    }

    private fun sendNotification(title: String, messageBody: String, type: String = "", id: Int = 0) {
        val intent = when (type) {
            "produksi" -> {
                Intent(applicationContext, com.example.notasawit.DetailRiwayatKeuangan.RiwayatPemasukanActivity::class.java).apply {
                    putExtra("produksi_id", id)
                    putExtra("from_notification", true)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            }
            "pengeluaran" -> {
                Intent(applicationContext, com.example.notasawit.DetailRiwayatKeuangan.RiwayatPengeluaranActivity::class.java).apply {
                    putExtra("biaya_id", id)
                    putExtra("from_notification", true)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            }
            else -> {
                Intent(applicationContext, DaftarTugasActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext, (System.currentTimeMillis() % 10000).toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "tugas_notification_channel"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Notifikasi Tugas & Pengingat",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Kanal notifikasi tugas dan pengingat dari Superadmin"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}