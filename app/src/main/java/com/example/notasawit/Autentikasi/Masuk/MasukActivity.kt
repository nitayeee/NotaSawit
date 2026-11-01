package com.example.notasawit.Autentikasi.Masuk

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.notasawit.Autentikasi.Daftar.DaftarActivity
import com.example.notasawit.R
import com.example.notasawit.databinding.ActivityMasukBinding
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.lifecycle.lifecycleScope
import com.example.notasawit.Admin.BaseAdminActivity
import com.example.notasawit.Autentikasi.Daftar.DataDiri.DataDiriActivity
import com.example.notasawit.Autentikasi.Daftar.DataDiri.Desa.DesaApiResponse
import com.example.notasawit.BaseActivity
import com.example.notasawit.InputKegiatan.JenisKegiatan.JenisKegiatanApiResponse
import com.example.notasawit.Model.LahanResponse
import com.example.notasawit.Model.LoginResponse
import com.example.notasawit.Network.PetaniApi
import com.example.notasawit.Room.AppDatabase
import com.example.notasawit.Room.DesaEntity
import com.example.notasawit.Room.JenisKegiatan.JenisKegiatanEntity
import com.example.notasawit.Room.Lahan.LahanEntity
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import java.security.MessageDigest


class MasukActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMasukBinding
    private lateinit var credentialManager: CredentialManager
    private lateinit var database: AppDatabase

    private val sharedPref by lazy { getSharedPreferences("NOTASAWIT_PREF", MODE_PRIVATE) }
    private val sp_petaniId by lazy { sharedPref.getInt("petani_id", 0) }
    private val sp_namaPetani by lazy { sharedPref.getString("namaPetani", "") }
    private val username by lazy { sharedPref.getString("username", "") }
    private val sp_tanggalLahir by lazy { sharedPref.getString("tanggalLahir", "") }
    private val sp_jenisKelamin by lazy { sharedPref.getString("jenisKelamin", "") }
    private val sp_desaPetani by lazy { sharedPref.getInt("desa_petani", 0) }
    private val sp_role by lazy { sharedPref.getString("role", "") }
    private val sp_noHpPetani by lazy { sharedPref.getString("noHpPetani", "") }
    private val sp_emailPetani by lazy { sharedPref.getString("emailPetani", "") }
    private val sp_alamatPetani by lazy { sharedPref.getString("alamatPetani", "") }
    private val sp_profilPetani by lazy { sharedPref.getString("profilPetani", "") }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMasukBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        database = AppDatabase.getDatabase(this)
getJenisKegiatan()
        credentialManager = CredentialManager.create(this)

        // Auto-fill Username akun terakhir agar petani tidak perlu mengetik ulang
        val lastUsername = sharedPref.getString("last_username", "")?.ifEmpty { null }
            ?: sharedPref.getString("username", "") ?: ""
        if (lastUsername.isNotEmpty()) {
            binding.etUsername.setText(lastUsername)
            binding.etPin.requestFocus()
        }

        val isRemembered = sharedPref.getBoolean("remember_me", true)
        binding.cbRememberMe.isChecked = isRemembered

        // Cek dukungan biometrik dan ketersediaan akun untuk login fingerprint
        val biometricManager = BiometricManager.from(this)
        val canUseBiometric = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
        val hasSavedAccount = (sharedPref.getInt("petani_id", 0) > 0 || sharedPref.getInt("user_id", 0) > 0 || lastUsername.isNotEmpty())

        if (canUseBiometric && hasSavedAccount) {
            binding.btnFingerprint.visibility = View.VISIBLE
            binding.btnFingerprint.setOnClickListener {
                showBiometricPrompt()
            }
        } else {
            binding.btnFingerprint.visibility = View.GONE
        }

        binding.btnBantuanWa.setOnClickListener {
            showBantuanLoginDialog()
        }
        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString()
            val pin = binding.etPin.text.toString()
            PetaniApi.login(
                username,
                pin,
                object : Callback {

                    override fun onFailure(
                        call: Call,
                        e: IOException
                    ) {

                    }
                    override fun onResponse(
                        call: Call,
                        response: Response
                    ) {

                        val json = response.body?.string()

                        Log.d("LOGIN_RESPONSE", json ?: "NULL")

                        if (json.isNullOrEmpty()) {
                            return
                        }

                        try {

                            val loginResponse =
                                Gson().fromJson(
                                    json,
                                    LoginResponse::class.java
                                )

                            val data = loginResponse.data

                            val sharedPref = getSharedPreferences(
                                "NOTASAWIT_PREF",
                                MODE_PRIVATE
                            )

                            if (loginResponse.success) {

                                val rememberMe = binding.cbRememberMe.isChecked
                                if (loginResponse.role == "petani") {
                                    sharedPref.edit().apply {
                                        putBoolean("is_logged_in", true)
                                        putBoolean("remember_me", rememberMe)
                                        if (rememberMe) {
                                            putString("last_username", username)
                                        } else {
                                            remove("last_username")
                                        }
                                        putString("role", loginResponse.role)
                                        putInt("petani_id", data?.petani_id ?: 0)
                                        putString("namaPetani", data?.petani_nama ?: "")
                                        putString("username", data?.petani_username ?: "")
                                        putString("profilPetani", data?.petani_profil ?: "")
                                        putInt("desaPetani", data?.desa_id ?: 0)
                                        getLahan(data?.petani_id ?: 0)
                                        apply()
                                    }
                                } else {
                                    sharedPref.edit().apply {
                                        putBoolean("is_logged_in", true)
                                        putBoolean("remember_me", rememberMe)
                                        if (rememberMe) {
                                            putString("last_username", username)
                                        } else {
                                            remove("last_username")
                                        }
                                        putString("role", "admin")
                                        putInt("user_id", data?.user_id ?: 0)
                                        putString("username", data?.user_username ?: "")
                                        putString("user_role", data?.user_role ?: "")
                                        putInt("admin_desa_id", data?.desa_id ?: 0)
                                        apply()
                                    }
                                }

                                getDesa()
                                getAdminsAndPetani()

                                runOnUiThread {
                                    val nama = if (loginResponse.role == "petani") data?.petani_nama else data?.user_username
                                    val targetIntent = if (loginResponse.role == "petani") {
                                        Intent(this@MasukActivity, BaseActivity::class.java)
                                    } else {
                                        Intent(this@MasukActivity, BaseAdminActivity::class.java)
                                    }
                                    targetIntent.putExtra("SHOW_WELCOME_MASCOT", true)
                                    targetIntent.putExtra("EXTRA_NAME", nama ?: "")
                                    startActivity(targetIntent)
                                    finish()
                                }
                            } else {
                                runOnUiThread {
                                    com.example.notasawit.Utils.CustomAlert.showError(
                                        this@MasukActivity,
                                        "Gagal",
                                        loginResponse.message ?: "Email atau password salah"
                                    )
                                }
                            }

                        } catch (e: Exception) {
                            Log.e("LOGIN_ERROR", "Response: $json")
                            Log.e("LOGIN_ERROR", e.toString())

                            runOnUiThread {
                                com.example.notasawit.Utils.CustomAlert.showError(
                                    this@MasukActivity,
                                    "Error",
                                    "Format response tidak sesuai"
                                )
                            }
                        }
                    }
                }
            )
//            getDesa()
//            getLahan()
//            startActivity(
//                                        Intent(
//                                           this@MasukActivity,
//                                            BaseActivity::class.java
//                                       ))

        }
        
        binding.tvLupaPin.setOnClickListener {
            startActivity(Intent(this@MasukActivity, LupaPinActivity::class.java))
        }

        binding.btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }
    }

    private fun showBantuanLoginDialog() {
        val currentUsername = binding.etUsername.text.toString().trim()
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Bantuan Masuk Akun")
            .setMessage("Jika Anda lupa PIN atau mengalami kendala masuk ke akun SILAUSA, Anda dapat menghubungi Admin Desa/KUD atau melakukan reset PIN mandiri.")
            .setPositiveButton("WhatsApp Admin") { _, _ ->
                try {
                    val pesan = if (currentUsername.isNotEmpty()) {
                        "Halo Admin SILAUSA, saya petani dengan username $currentUsername mengalami kendala masuk akun. Mohon bantuannya."
                    } else {
                        "Halo Admin SILAUSA, saya petani mengalami kendala masuk akun SILAUSA. Mohon bantuannya."
                    }
                    val url = "https://api.whatsapp.com/send?text=${java.net.URLEncoder.encode(pesan, "UTF-8")}"
                    val waIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
                    startActivity(waIntent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Tidak dapat membuka WhatsApp", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Reset PIN Mandiri") { _, _ ->
                startActivity(Intent(this, LupaPinActivity::class.java))
            }
            .setNeutralButton("Tutup", null)
            .show()
    }

    private fun checkAndShowFingerprint() {
        val biometricManager = BiometricManager.from(this)

        // Cek apakah hardware ada dan sidik jari sudah didaftarkan di pengaturan HP
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                showBiometricPrompt()
            }
            else -> {
                // Jika HP tidak mendukung atau sidik jari belum diatur,
                // biarkan user pakai Email & PIN saja (tidak muncul apa-apa)
            }
        }
    }


    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)

        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    
                    val sharedPref = getSharedPreferences("NOTASAWIT_PREF", Context.MODE_PRIVATE)
                    sharedPref.edit().putBoolean("is_logged_in", true).apply()
                    val role = sharedPref.getString("role", "petani")
                    val nama = sharedPref.getString("namaPetani", sharedPref.getString("username", "Petani Sawit"))
                    
                    val targetIntent = if (role == "admin") {
                        Intent(this@MasukActivity, BaseAdminActivity::class.java)
                    } else {
                        Intent(this@MasukActivity, BaseActivity::class.java)
                    }
                    targetIntent.putExtra("SHOW_WELCOME_MASCOT", true)
                    targetIntent.putExtra("EXTRA_NAME", nama ?: "")
                    startActivity(targetIntent)
                    finish()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // Jika user menekan tombol "Batal", biarkan mereka pakai PIN
                    Toast.makeText(this@MasukActivity, "Silakan masukkan PIN Anda", Toast.LENGTH_SHORT).show()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Login Cepat")
            .setSubtitle("Gunakan sidik jari untuk masuk ke SILAUSA")
            .setNegativeButtonText("Gunakan PIN")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
    private fun setGoogleLoadingState(isLoading: Boolean) {
        runOnUiThread {
            if (isLoading) {
                binding.pbGoogleLoading.visibility = View.VISIBLE
                binding.ivGoogleIcon.visibility = View.GONE
                binding.tvGoogleText.visibility = View.GONE
                binding.btnGoogleSignIn.isEnabled = false
            } else {
                binding.pbGoogleLoading.visibility = View.GONE
                binding.ivGoogleIcon.visibility = View.VISIBLE
                binding.tvGoogleText.visibility = View.VISIBLE
                binding.btnGoogleSignIn.isEnabled = true
            }
        }
    }

    private fun signInWithGoogle() {
        setGoogleLoadingState(true)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(
                getString(R.string.web_client_id)
            )
            .setFilterByAuthorizedAccounts(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {

            try {

                val result = credentialManager.getCredential(
                    context = this@MasukActivity,
                    request = request
                )

                handleSignIn(result)

            } catch (e: Exception) {
                e.printStackTrace()
                setGoogleLoadingState(false)

                Toast.makeText(
                    this@MasukActivity,
                    "Login Google gagal",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    private fun handleSignIn(
        result: GetCredentialResponse
    ) {

        val credential = result.credential

        if (
            credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {

            val googleCredential =
                GoogleIdTokenCredential.createFrom(
                    credential.data
                )

            val idToken = googleCredential.idToken

            val nama = googleCredential.displayName
            val email = googleCredential.id

            Log.d("GOOGLE", "Nama: $nama")
            Log.d("GOOGLE", "Email: $email")

            sendTokenToServer(
                idToken,
                nama ?: "",
                email
            )
        } else {
            setGoogleLoadingState(false)
        }
    }
    private fun sendTokenToServer(
        idToken: String,
        nama: String,
        email: String
    ) {
        PetaniApi.getAllPetani(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                setGoogleLoadingState(false)
                runOnUiThread {
                    Toast.makeText(this@MasukActivity, "Gagal mengecek email: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                var emailExists = false
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        try {
                            val jsonObject = org.json.JSONObject(body)
                            val dataArray = jsonObject.getJSONArray("data")
                            for (i in 0 until dataArray.length()) {
                                val item = dataArray.getJSONObject(i)
                                if (item.has("petani_email") && item.getString("petani_email") == email) {
                                    emailExists = true
                                    break
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                setGoogleLoadingState(false)

                runOnUiThread {
                    if (emailExists) {
                        // Jika sudah terdaftar, muncul alert
                        android.app.AlertDialog.Builder(this@MasukActivity)
                            .setTitle("Akun Sudah Terdaftar")
                            .setMessage("Email dari akun Google ini sudah terdaftar. Silakan login menggunakan Username dan PIN Anda.")
                            .setPositiveButton("OK", null)
                            .show()
                    } else {
                        // Jika belum terdaftar, lanjut ke DataDiriActivity
                        sharedPref.edit()
                            .putBoolean("is_login", true)
                            .putBoolean("sudah_pernah_login", true)
                            .putString("namaPetani", nama)
                            .putString("emailPetani", email)
                            .apply()

                        Toast.makeText(
                            this@MasukActivity,
                            "Melanjutkan pendaftaran...",
                            Toast.LENGTH_SHORT
                        ).show()

                        startActivity(
                            Intent(
                                this@MasukActivity,
                                DataDiriActivity::class.java
                            )
                        )
                        finish()
                    }
                }
            }
        })
    }
    private fun getDesa(){
        PetaniApi.getDesa(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
            }

            override fun onResponse(call: Call, response: Response) {

                if (response.isSuccessful) {

                    val json = response.body!!.string()

                    val desaResponse = Gson().fromJson(
                        json,
                        DesaApiResponse::class.java
                    )

                    val desaEntity = desaResponse.data.map {

                        DesaEntity(
                            idDesa = it.desa_id,
                            namaDesa = it.desa_nama
                        )

                    }

                    lifecycleScope.launch(Dispatchers.IO) {

                        database.masterDao().insertDesa(desaEntity)

                    }

                }

            }

        })
    }

    private fun getJenisKegiatan(){
        PetaniApi.getJenisKegiatan(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
            }

            override fun onResponse(call: Call, response: Response) {

                if (response.isSuccessful) {

                    val json = response.body!!.string()

                    val jenisKegaitanResponse = Gson().fromJson(
                        json,
                        JenisKegiatanApiResponse::class.java
                    )

                    val jenisKegiatanEntity = jenisKegaitanResponse.data.map {

                        JenisKegiatanEntity(
                            id_jenis = it.id_jenis,
                            nama_jenis = it.nama_jenis,
                            ikon = it.ikon
                        )

                    }

                    lifecycleScope.launch(Dispatchers.IO) {

                        database.JenisKegiatanDao().insertJenisKegiatan(jenisKegiatanEntity)

                    }

                }

            }

        })
    }
    private fun getLahan(petaniId: Int){
        PetaniApi.getLahanByPetani(petaniId, object : Callback {

            override fun onFailure(call: Call, e: IOException) {
            }

            override fun onResponse(call: Call, response: Response) {

                if (response.isSuccessful) {

                    val json = response.body!!.string()

                    val lahanResponse = Gson().fromJson(
                        json,
                        LahanResponse::class.java
                    )

                    val lahanEntity = lahanResponse.data.map {

                        LahanEntity(
                            lahan_id = it.lahan_id,
                            petani_id = petaniId,
                            lahan_nama = it.lahan_nama,
                            lahan_luas = it.lahan_luas
                        )

                    }

                    lifecycleScope.launch(Dispatchers.IO) {

                        database.LahanDao().insertLahan(lahanEntity)

                    }

                }

            }

        })
    }

    private fun getAdminsAndPetani() {
        PetaniApi.getAdmins(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        try {
                            val jsonObject = org.json.JSONObject(body)
                            val dataArray = jsonObject.getJSONArray("data")
                            val listAuditor = mutableListOf<com.example.notasawit.Room.Auditor.AuditorEntity>()
                            for (i in 0 until dataArray.length()) {
                                val item = dataArray.getJSONObject(i)
                                listAuditor.add(com.example.notasawit.Room.Auditor.AuditorEntity(
                                    idAuditor = item.getInt("user_id"),
                                    namaAuditor = item.getString("user_nama"),
                                    username = item.getString("user_username") 
                                ))
                            }
                            if (listAuditor.isNotEmpty()) {
                                lifecycleScope.launch(Dispatchers.IO) {
                                    database.masterDao().insertAuditor(listAuditor)
                                }
                            }
                        } catch (e: Exception) {}
                    }
                }
            }
        })

        PetaniApi.getAllPetani(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        try {
                            val jsonObject = org.json.JSONObject(body)
                            val dataArray = jsonObject.getJSONArray("data")
                            
                            val listPetani = mutableListOf<com.example.notasawit.Room.Petani.PetaniEntity>()
                            for (i in 0 until dataArray.length()) {
                                val item = dataArray.getJSONObject(i)
                                val desaId = item.optInt("desa_id", 0)
                                val foto = item.optString("petani_foto", item.optString("user_profil", item.optString("foto", item.optString("profil_petani", ""))))
                                
                                val namaDesa = if (item.has("desa") && !item.isNull("desa")) {
                                    item.getJSONObject("desa").optString("nama_desa", "-")
                                } else {
                                    item.optString("petani_username", "-") 
                                }

                                listPetani.add(com.example.notasawit.Room.Petani.PetaniEntity(
                                    idPetani = item.optInt("petani_id", 0),
                                    namaPetani = item.optString("petani_nama", "-"),
                                    namaDesa = namaDesa,
                                    desaId = desaId,
                                    fotoProfil = if (foto.isNullOrEmpty() || foto == "null") null else foto
                                ))
                            }
                            
                            lifecycleScope.launch(Dispatchers.IO) {
                                // Bersihkan data room lama sebelum memasukkan data baru
                                database.masterDao().deleteAllPetani()
                                
                                if (listPetani.isNotEmpty()) {
                                    database.masterDao().insertPetani(listPetani)
                                }
                            }
                        } catch (e: Exception) {}
                    }
                }
            }
        })
    }
}