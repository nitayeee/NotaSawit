package com.example.notasawit

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.notasawit.Autentikasi.Masuk.MasukActivity
import com.example.notasawit.Home.BerandaFragment
import com.example.notasawit.ProfilPetani.ProfilPetaniActivity
import com.example.notasawit.RiwayatKeuangan.RiwayatActivity
import com.example.notasawit.databinding.ActivityBaseBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class BaseActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBaseBinding
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityBaseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.setOnApplyWindowInsetsListener(null)
        bottomNav.setPadding(0, 0, 0, 0)

        replaceFragment(BerandaFragment())
        binding.bottomNavigation.setOnItemSelectedListener {

            when (it.itemId) {

                R.id.home -> {
                    replaceFragment(BerandaFragment())
                    true
                }

                R.id.notification -> {
                    replaceFragment(com.example.notasawit.Notifikasi.NotificationFragment())
                    true
                }

                R.id.riwayat -> {
                    val intent = Intent(this@BaseActivity, RiwayatActivity::class.java)
                    startActivity(intent)
                    true
                }

                R.id.pengaturan -> {
                    replaceFragment(com.example.notasawit.ProfilPetani.SetelanFragment())
                    true
                }

                else -> false
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, fragment)
            //.addToBackStack(null) -> ini kita nonaktifkan agar saat back langsung keluar aplikasi
            .commit()
    }

    override fun onResume() {
        super.onResume()
        checkUnreadNotifications()
    }

    fun checkUnreadNotifications() {
        val sharedPref = getSharedPreferences("NOTASAWIT_PREF", android.content.Context.MODE_PRIVATE)
        var petaniId = sharedPref.getInt("petani_id", -1)
        if (petaniId == -1 || petaniId == 0) {
            petaniId = sharedPref.getInt("user_id", -1)
        }

        if (petaniId == -1 || petaniId == 0) return

        com.example.notasawit.Network.PetaniApi.getNotifications(petaniId, object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {}

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (!response.isSuccessful) return
                val body = response.body?.string() ?: return
                
                try {
                    val jsonObject = org.json.JSONObject(body)
                    val success = jsonObject.getBoolean("success")
                    if (success) {
                        val dataArray = jsonObject.getJSONArray("data")
                        var hasUnread = false
                        for (i in 0 until dataArray.length()) {
                            val obj = dataArray.getJSONObject(i)
                            // Jika optInt("id", 0) == 0, artinya data error, abaikan
                            if (obj.optInt("id", 0) == 0) continue
                            
                            val isRead = if (obj.optBoolean("is_read", false) || obj.optInt("is_read", 0) == 1) 1 else 0
                            if (isRead == 0) {
                                hasUnread = true
                                break
                            }
                        }
                        
                        runOnUiThread {
                            val badge = binding.bottomNavigation.getOrCreateBadge(R.id.notification)
                            badge.isVisible = hasUnread
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        })
    }
}