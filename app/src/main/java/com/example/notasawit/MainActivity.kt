package com.example.notasawit

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.notasawit.Autentikasi.Masuk.MasukActivity
import com.example.notasawit.databinding.ActivityMainBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        lifecycleScope.launch {
            delay(2000) //simulasi pengambilan data selama 2 detik

            val sharedPref = getSharedPreferences("NOTASAWIT_PREF", Context.MODE_PRIVATE)
            val isFirstTime = sharedPref.getBoolean("is_first_time", true)
            val role = sharedPref.getString("role", "")
            
            if (isFirstTime) {
                startActivity(Intent(this@MainActivity, TutorialActivity::class.java))
            } else {
                val intent = Intent(this@MainActivity, MasukActivity::class.java)
                if (role == "petani" || role == "admin") {
                    intent.putExtra("SHOW_FINGERPRINT", true)
                }
                startActivity(intent)
            }
            finish()
        }
    }
}