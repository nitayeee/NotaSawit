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
import com.example.notasawit.Autentikasi.Daftar.DataDiri.DataDiriActivity
import com.example.notasawit.BaseActivity
import com.example.notasawit.Model.LoginResponse
import com.example.notasawit.Network.PetaniApi
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.gson.Gson
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import java.security.MessageDigest


class MasukActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMasukBinding
    private lateinit var credentialManager: CredentialManager

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

                                sharedPref.edit().apply {

                                    putString("role", loginResponse.role)

                                    putInt("petani_id", data?.petani_id ?: 0)
                                    putString("namaPetani", data?.petani_nama ?: "")
                                    putString("username", data?.petani_username ?: "")
                                    putString("profilPetani", data?.petani_profil ?: "")
                                    putInt("desaPetani", data?.desa_id ?: 0)

                                    apply()
                                }

                                runOnUiThread {

                                    Toast.makeText(
                                        this@MasukActivity,
                                        "Login Berhasil",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    startActivity(
                                        Intent(
                                            this@MasukActivity,
                                            BaseActivity::class.java
                                        )
                                    )

                                    finish()
                                }

                            } else {

                                runOnUiThread {
                                    Toast.makeText(
                                        this@MasukActivity,
                                        loginResponse.message,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }

                        } catch (e: Exception) {

                            Log.e("LOGIN_ERROR", "Response: $json")
                            Log.e("LOGIN_ERROR", e.toString())

                            runOnUiThread {
                                Toast.makeText(
                                    this@MasukActivity,
                                    "Format response tidak sesuai",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }
            )
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


}