package com.example.notasawit.Autentikasi.Masuk

import android.content.Intent
import android.os.Bundle
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

        // 1. Cek apakah user ini sudah pernah login sebelumnya
        val sharedPref = getSharedPreferences("NOTASAWIT_PREF", Context.MODE_PRIVATE)
        val sudahPernahLogin = sharedPref.getBoolean("sudah_pernah_login", false)
        val sp_namaPetani = sharedPref.getString("namaPetani", "")
        val sp_alamatPetani = sharedPref.getString("alamatPetani", "")

        binding.btnFingerprint.setOnClickListener {
            if (sudahPernahLogin) {
                // Jika sudah pernah login, cek apakah HP mendukung sidik jari
                checkAndShowFingerprint()
            }
        }
        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString()
            val pin = binding.etPin.text.toString()
            val pinHash = hashPin(pin)
            PetaniApi.login(
                username,
                pinHash,
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

                                if(loginResponse.role == "petani"){
                                    sharedPref.edit().apply {
                                        putString("role", loginResponse.role)
                                        putInt("petani_id", data?.petani_id ?: 0)
                                        putString("namaPetani", data?.petani_nama ?: "")
                                        putString("username", data?.petani_username ?: "")
                                        putString("profilPetani", data?.petani_profil ?: "")
                                        putInt("desaPetani", data?.desa_id ?: 0)
                                        getLahan()
                                        apply()
                                    }
                                }else {
                                    sharedPref.edit().apply {
                                        putString("role", "admin")
                                        putInt("user_id", data?.user_id ?: 0)
                                        putString("username", data?.user_username ?: "")
                                        putString("user_role", data?.user_role ?: "")
                                        apply()
                                    }
                                }

                                getDesa()
                                getAdminsAndPetani()

                                runOnUiThread {
                                    val nama = if (loginResponse.role == "petani") data?.petani_nama else data?.user_username
                                    com.example.notasawit.utils.CustomAlert.showSuccess(
                                        this@MasukActivity,
                                        "Berhasil",
                                        "Selamat datang, $nama"
                                    )

                                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                        if(loginResponse.role == "petani"){
                                            startActivity(Intent(this@MasukActivity, BaseActivity::class.java))
                                        } else {
                                            startActivity(Intent(this@MasukActivity, BaseAdminActivity::class.java))
                                        }
                                        finish()
                                    }, 1500)
                                }
                            } else {
                                runOnUiThread {
                                    com.example.notasawit.utils.CustomAlert.showError(
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
                                com.example.notasawit.utils.CustomAlert.showError(
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
        binding.btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }
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
    private fun hashPin(pin: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(pin.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)

        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    // LOGIN SUKSES! Langsung pindah ke Beranda
                    startActivity(Intent(this@MasukActivity, BaseActivity::class.java))
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
            .setSubtitle("Gunakan sidik jari untuk masuk ke NOTASAWIT")
            .setNegativeButtonText("Gunakan PIN")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
    private fun signInWithGoogle() {

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
        }
    }
    private fun sendTokenToServer(
        idToken: String,
        nama: String,
        email: String
    ) {

        sharedPref.edit()
            .putBoolean("is_login", true)
            .putBoolean("sudah_pernah_login", true)
            .putString("namaPetani", nama)
            .putString("emailPetani", email)
            .apply()

        Toast.makeText(
            this,
            "Login Google Berhasil",
            Toast.LENGTH_SHORT
        ).show()

        startActivity(
            Intent(
                this,
                DataDiriActivity::class.java
            )
        )
        finish()
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
    private fun getLahan(){
        PetaniApi.getLahanByPetani(sp_petaniId, object : Callback {

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
                            petani_id = sp_petaniId,
                            lahan_nama = it.lahan_nama
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
                                listPetani.add(com.example.notasawit.Room.Petani.PetaniEntity(
                                    idPetani = item.getInt("petani_id"),
                                    namaPetani = item.getString("petani_nama"),
                                    namaDesa = item.getString("petani_username") 
                                ))
                            }
                            if (listPetani.isNotEmpty()) {
                                lifecycleScope.launch(Dispatchers.IO) {
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