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

        val title = remoteMessage.notification?.title 
            ?: remoteMessage.data["title"] 
            ?: "Tugas Baru"

        val message = remoteMessage.notification?.body 
            ?: remoteMessage.data["body"] 
            ?: remoteMessage.data["pesan"] 
            ?: "Anda menerima tugas baru dari Superadmin."

        sendNotification(title, message)
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

    private fun sendNotification(title: String, messageBody: String) {
        val intent = Intent(applicationContext, DaftarTugasActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
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