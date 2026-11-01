package com.example.notasawit.ProfilPetani

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.notasawit.R
import java.net.URLEncoder

class BantuanActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_bantuan)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val btnKontakAdmin = findViewById<View>(R.id.btnKontakAdmin)
        btnKontakAdmin?.setOnClickListener {
            try {
                val adminPhone = "6282285335295"
                val pesan = "Halo Admin SILAUSA, saya ingin bertanya seputar penggunaan aplikasi SILAUSA."
                val url = "https://api.whatsapp.com/send?phone=$adminPhone&text=${URLEncoder.encode(pesan, "UTF-8")}"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Tidak dapat membuka WhatsApp", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
