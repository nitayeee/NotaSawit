package com.example.notasawit.Autentikasi.Masuk

import android.content.Context
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.lifecycle.lifecycleScope
import com.example.notasawit.R
import com.example.notasawit.databinding.ActivityLupaPinBinding
import com.example.notasawit.Network.PetaniApi
import com.example.notasawit.Utils.CustomAlert
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException

class LupaPinActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLupaPinBinding
    private lateinit var credentialManager: CredentialManager

    private var verifiedUsername: String? = null
    private var verifiedEmail: String? = null
    private var verifiedNama: String? = null
    private var verifiedRole: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLupaPinBinding.inflate(layoutInflater)
        setContentView(binding.root)

        credentialManager = CredentialManager.create(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

        // Tombol cepat pilih email dengan akun Google
        binding.btnPilihGoogleEmail.setOnClickListener {
            pickGoogleAccount()
        }

        // Tombol periksa email yang diketik manual
        binding.btnPeriksaEmail.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            checkEmailExists(email)
        }

        // Tombol ganti email (kembali ke tahap 1)
        binding.btnGantiEmail.setOnClickListener {
            resetVerificationState()
        }

        // Toggle intip PIN Baru
        var isPinBaruVisible = false
        binding.btnTogglePinBaru.setOnClickListener {
            isPinBaruVisible = !isPinBaruVisible
            val tf = binding.etPinBaru.typeface
            if (isPinBaruVisible) {
                binding.etPinBaru.transformationMethod = null
                binding.btnTogglePinBaru.setImageResource(R.drawable.ic_visibility_off)
            } else {
                binding.etPinBaru.transformationMethod = PasswordTransformationMethod.getInstance()
                binding.btnTogglePinBaru.setImageResource(R.drawable.ic_visibility)
            }
            binding.etPinBaru.typeface = tf
            binding.etPinBaru.text?.let { binding.etPinBaru.setSelection(it.length) }
        }

        // Toggle intip Konfirmasi PIN
        var isKonfirmasiVisible = false
        binding.btnToggleKonfirmasiPin.setOnClickListener {
            isKonfirmasiVisible = !isKonfirmasiVisible
            val tf = binding.etKonfirmasiPin.typeface
            if (isKonfirmasiVisible) {
                binding.etKonfirmasiPin.transformationMethod = null
                binding.btnToggleKonfirmasiPin.setImageResource(R.drawable.ic_visibility_off)
            } else {
                binding.etKonfirmasiPin.transformationMethod = PasswordTransformationMethod.getInstance()
                binding.btnToggleKonfirmasiPin.setImageResource(R.drawable.ic_visibility)
            }
            binding.etKonfirmasiPin.typeface = tf
            binding.etKonfirmasiPin.text?.let { binding.etKonfirmasiPin.setSelection(it.length) }
        }

        // Tombol simpan PIN baru
        binding.btnSimpanPinBaru.setOnClickListener {
            submitResetPin()
        }
    }

    private fun pickGoogleAccount() {
        setCheckEmailLoadingState(true)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(getString(R.string.web_client_id))
            .setFilterByAuthorizedAccounts(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    context = this@LupaPinActivity,
                    request = request
                )
                val credential = result.credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val email = googleCredential.id
                    binding.etEmail.setText(email)
                    checkEmailExists(email)
                } else {
                    setCheckEmailLoadingState(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                setCheckEmailLoadingState(false)
            }
        }
    }

    private fun checkEmailExists(email: String) {
        val cleanEmail = email.trim()
        if (cleanEmail.isEmpty()) {
            CustomAlert.showError(this, "Peringatan", "Harap masukkan atau pilih email Anda terlebih dahulu")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            CustomAlert.showError(this, "Peringatan", "Format email tidak valid")
            return
        }

        setCheckEmailLoadingState(true)

        PetaniApi.getAllPetani(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                checkAdminEmail(cleanEmail)
            }

            override fun onResponse(call: Call, response: Response) {
                var matchedPetani: org.json.JSONObject? = null
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        try {
                            val jsonObject = org.json.JSONObject(body)
                            val dataArray = jsonObject.getJSONArray("data")
                            for (i in 0 until dataArray.length()) {
                                val item = dataArray.getJSONObject(i)
                                if (item.has("petani_email") && item.getString("petani_email").trim().equals(cleanEmail, ignoreCase = true)) {
                                    matchedPetani = item
                                    break
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                if (matchedPetani != null) {
                    val uName = matchedPetani.optString("petani_username", "")
                    val nama = matchedPetani.optString("petani_nama", "Petani")
                    runOnUiThread {
                        setCheckEmailLoadingState(false)
                        onEmailVerified(cleanEmail, uName, nama, "Petani")
                    }
                } else {
                    checkAdminEmail(cleanEmail)
                }
            }
        })
    }

    private fun checkAdminEmail(email: String) {
        PetaniApi.getAdmins(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    setCheckEmailLoadingState(false)
                    CustomAlert.showError(
                        this@LupaPinActivity,
                        "Gagal Terhubung",
                        "Tidak dapat terhubung ke server. Periksa koneksi internet Anda."
                    )
                }
            }

            override fun onResponse(call: Call, response: Response) {
                var matchedAdmin: org.json.JSONObject? = null
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        try {
                            val jsonObject = org.json.JSONObject(body)
                            val dataArray = jsonObject.getJSONArray("data")
                            for (i in 0 until dataArray.length()) {
                                val item = dataArray.getJSONObject(i)
                                if (item.has("user_email") && item.getString("user_email").trim().equals(email, ignoreCase = true)) {
                                    matchedAdmin = item
                                    break
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                runOnUiThread {
                    setCheckEmailLoadingState(false)
                    if (matchedAdmin != null) {
                        val uName = matchedAdmin.optString("user_username", "")
                        val nama = matchedAdmin.optString("user_nama", "Admin")
                        onEmailVerified(email, uName, nama, "Admin")
                    } else {
                        CustomAlert.showError(
                            this@LupaPinActivity,
                            "Email Tidak Terdaftar",
                            "Email '$email' tidak terdaftar dalam sistem SILAUSA. Pastikan email Anda benar."
                        )
                    }
                }
            }
        })
    }

    private fun onEmailVerified(email: String, username: String, nama: String, role: String) {
        verifiedEmail = email
        verifiedUsername = username
        verifiedNama = nama
        verifiedRole = role

        binding.tvNamaAkun.text = "Pemilik Akun: $nama ($role)"
        binding.tvEmailTerverifikasi.text = "Email: $email"

        // Sembunyikan bagian pilih email dan tampilkan card terverifikasi & form PIN baru
        binding.layoutPilihEmail.visibility = View.GONE
        binding.cardAkunTerverifikasi.visibility = View.VISIBLE
        binding.layoutInputPinBaru.visibility = View.VISIBLE

        CustomAlert.showSuccess(
            this,
            "Email Terverifikasi",
            "Akun atas nama $nama ditemukan. Silakan buat PIN baru Anda."
        )

        binding.etPinBaru.requestFocus()
        binding.main.post {
            binding.main.smoothScrollTo(0, binding.cardAkunTerverifikasi.top)
        }
    }

    private fun resetVerificationState() {
        verifiedEmail = null
        verifiedUsername = null
        verifiedNama = null
        verifiedRole = null

        binding.cardAkunTerverifikasi.visibility = View.GONE
        binding.layoutInputPinBaru.visibility = View.GONE
        binding.layoutPilihEmail.visibility = View.VISIBLE

        binding.etPinBaru.setText("")
        binding.etKonfirmasiPin.setText("")
    }

    private fun submitResetPin() {
        val pinBaru = binding.etPinBaru.text.toString().trim()
        val konfirmasiPin = binding.etKonfirmasiPin.text.toString().trim()

        if (verifiedUsername.isNullOrEmpty() || verifiedEmail.isNullOrEmpty()) {
            CustomAlert.showError(this, "Peringatan", "Harap verifikasi email Anda terlebih dahulu")
            return
        }

        if (pinBaru.isEmpty()) {
            CustomAlert.showError(this, "Peringatan", "PIN baru harus diisi")
            return
        }

        if (pinBaru.length != 6) {
            CustomAlert.showError(this, "Peringatan", "PIN baru harus terdiri dari 6 digit angka")
            return
        }

        if (konfirmasiPin.isEmpty()) {
            CustomAlert.showError(this, "Peringatan", "Harap konfirmasi PIN baru Anda")
            return
        }

        if (pinBaru != konfirmasiPin) {
            CustomAlert.showError(this, "Peringatan", "PIN baru dan Konfirmasi PIN tidak cocok")
            return
        }

        setResetPinLoadingState(true)

        PetaniApi.lupaPin(verifiedUsername!!, verifiedEmail!!, pinBaru, object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    setResetPinLoadingState(false)
                    CustomAlert.showError(
                        this@LupaPinActivity,
                        "Gagal",
                        "Tidak dapat terhubung ke server: ${e.message}"
                    )
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val json = response.body?.string()
                runOnUiThread {
                    setResetPinLoadingState(false)
                }

                if (response.isSuccessful && json != null) {
                    try {
                        val jsonObject = org.json.JSONObject(json)
                        val success = jsonObject.optBoolean("success", false)
                        val message = jsonObject.optString("message", "PIN berhasil diperbarui")

                        runOnUiThread {
                            if (success) {
                                // Simpan username ke preferences agar otomatis terisi di form login
                                val pref = getSharedPreferences("NOTASAWIT_PREF", Context.MODE_PRIVATE)
                                pref.edit().putString("last_username", verifiedUsername).apply()

                                CustomAlert.showSuccess(
                                    this@LupaPinActivity,
                                    "Berhasil",
                                    "PIN berhasil diperbarui! Silakan masuk dengan PIN baru Anda."
                                )
                                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                    finish()
                                }, 1500)
                            } else {
                                CustomAlert.showError(
                                    this@LupaPinActivity,
                                    "Gagal",
                                    message
                                )
                            }
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            CustomAlert.showError(
                                this@LupaPinActivity,
                                "Error",
                                "Format respons server tidak valid"
                            )
                        }
                    }
                } else {
                    runOnUiThread {
                        CustomAlert.showError(
                            this@LupaPinActivity,
                            "Gagal",
                            "Data akun tidak cocok atau tidak dapat diperbarui di server."
                        )
                    }
                }
            }
        })
    }

    private fun setCheckEmailLoadingState(isLoading: Boolean) {
        runOnUiThread {
            if (isLoading) {
                binding.btnPeriksaEmail.isEnabled = false
                binding.pbPeriksaEmail.visibility = View.VISIBLE
                binding.tvPeriksaEmail.visibility = View.GONE

                binding.btnPilihGoogleEmail.isEnabled = false
                binding.pbGoogleLoading.visibility = View.VISIBLE
                binding.ivGoogleIcon.visibility = View.GONE
                binding.tvGoogleText.visibility = View.GONE
            } else {
                binding.btnPeriksaEmail.isEnabled = true
                binding.pbPeriksaEmail.visibility = View.GONE
                binding.tvPeriksaEmail.visibility = View.VISIBLE

                binding.btnPilihGoogleEmail.isEnabled = true
                binding.pbGoogleLoading.visibility = View.GONE
                binding.ivGoogleIcon.visibility = View.VISIBLE
                binding.tvGoogleText.visibility = View.VISIBLE
            }
        }
    }

    private fun setResetPinLoadingState(isLoading: Boolean) {
        runOnUiThread {
            if (isLoading) {
                binding.btnSimpanPinBaru.isEnabled = false
                binding.pbSimpanPin.visibility = View.VISIBLE
                binding.tvSimpanPin.visibility = View.GONE
            } else {
                binding.btnSimpanPinBaru.isEnabled = true
                binding.pbSimpanPin.visibility = View.GONE
                binding.tvSimpanPin.visibility = View.VISIBLE
            }
        }
    }
}
