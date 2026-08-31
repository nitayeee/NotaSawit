package com.example.notasawit.ProfilPetani

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.notasawit.Network.PetaniApi
import com.example.notasawit.R
import com.google.android.material.textfield.TextInputEditText
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class UbahPinActivity : AppCompatActivity() {

    private lateinit var etPinBaru: TextInputEditText
    private lateinit var etKonfirmasiPin: TextInputEditText
    private lateinit var btnSimpan: Button
    private lateinit var btnBack: ImageView

    private val sharedPref by lazy { getSharedPreferences("NOTASAWIT_PREF", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ubah_pin)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        etPinBaru = findViewById(R.id.etPinBaru)
        etKonfirmasiPin = findViewById(R.id.etKonfirmasiPin)
        btnSimpan = findViewById(R.id.btnSimpan)
        btnBack = findViewById(R.id.btnBack)

        btnBack.setOnClickListener { finish() }

        btnSimpan.setOnClickListener {
            val pinBaru = etPinBaru.text.toString().trim()
            val konfirmasiPin = etKonfirmasiPin.text.toString().trim()

            if (pinBaru.isEmpty()) {
                etPinBaru.error = "PIN tidak boleh kosong"
                etPinBaru.requestFocus()
                return@setOnClickListener
            }
            if (pinBaru.length != 6) {
                etPinBaru.error = "PIN harus 6 digit"
                etPinBaru.requestFocus()
                return@setOnClickListener
            }
            if (konfirmasiPin.isEmpty()) {
                etKonfirmasiPin.error = "Konfirmasi PIN tidak boleh kosong"
                etKonfirmasiPin.requestFocus()
                return@setOnClickListener
            }
            if (pinBaru != konfirmasiPin) {
                etKonfirmasiPin.error = "PIN dan Konfirmasi PIN tidak sama"
                etKonfirmasiPin.requestFocus()
                return@setOnClickListener
            }

            // Semua validasi berhasil, kirim ke server
            btnSimpan.isEnabled = false
            btnSimpan.text = "Menyimpan..."
            
            ubahPin(pinBaru)
        }
    }

    private fun ubahPin(pinBaru: String) {
        val petaniId = sharedPref.getInt("petani_id", 0)
        if (petaniId == 0) {
            Toast.makeText(this, "Petani ID tidak ditemukan", Toast.LENGTH_SHORT).show()
            btnSimpan.isEnabled = true
            btnSimpan.text = "Simpan PIN"
            return
        }

        PetaniApi.ubahPin(petaniId, pinBaru, object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    btnSimpan.isEnabled = true
                    btnSimpan.text = "Simpan PIN"
                    Toast.makeText(this@UbahPinActivity, "Terjadi kesalahan jaringan: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                var message = "Gagal mengubah PIN"
                
                if (body != null) {
                    try {
                        val json = JSONObject(body)
                        message = json.optString("message", message)
                    } catch (e: Exception) {}
                }

                runOnUiThread {
                    btnSimpan.isEnabled = true
                    btnSimpan.text = "Simpan PIN"
                    
                    if (response.isSuccessful) {
                        Toast.makeText(this@UbahPinActivity, message, Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        Toast.makeText(this@UbahPinActivity, message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }
}
