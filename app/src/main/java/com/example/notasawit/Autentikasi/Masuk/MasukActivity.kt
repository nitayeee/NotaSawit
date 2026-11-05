package com.example.notasawit.Autentikasi.Masuk

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.notasawit.Autentikasi.Daftar.DaftarActivity
import com.example.notasawit.R
import com.example.notasawit.Utils.AdminDesaHelper
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
        getAdminsAndPetani()
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
        val savedPin = sharedPref.getString("saved_pin", "") ?: ""
        val biometricManager = BiometricManager.from(this)
        val canUseBiometric = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
        val hasSavedAccount = (sharedPref.getInt("petani_id", 0) > 0 || sharedPref.getInt("user_id", 0) > 0 || (lastUsername.isNotEmpty() && savedPin.isNotEmpty()))

        if (canUseBiometric && hasSavedAccount) {
            binding.btnFingerprint.visibility = View.VISIBLE
            binding.btnFingerprint.setOnClickListener {
                showBiometricPrompt()
            }
        } else {
            binding.btnFingerprint.visibility = View.GONE
        }

        binding.btnPilihUsernamePin.setOnClickListener {
            showFormLogin()
        }

        binding.btnBackToPilihan.setOnClickListener {
            showPilihanMasuk()
        }

        var isPinVisible = false
        binding.btnTogglePin.setOnClickListener {
            isPinVisible = !isPinVisible
            val currentTypeface = binding.etPin.typeface
            if (isPinVisible) {
                binding.etPin.transformationMethod = null
                binding.btnTogglePin.setImageResource(R.drawable.ic_visibility_off)
            } else {
                binding.etPin.transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
                binding.btnTogglePin.setImageResource(R.drawable.ic_visibility)
            }
            binding.etPin.typeface = currentTypeface
            binding.etPin.text?.let { binding.etPin.setSelection(it.length) }
        }

        binding.btnBantuanWa.setOnClickListener {
            showBantuanLoginDialog()
        }

        binding.btnBantuanWaForm.setOnClickListener {
            showBantuanLoginDialog()
        }

        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString()
            val pin = binding.etPin.text.toString()
            performLogin(username, pin, isBiometric = false)
        }

        binding.btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }

        binding.btnGoogleSignInForm.setOnClickListener {
            signInWithGoogle()
        }

        binding.navToDaftar.setOnClickListener {
            startActivity(Intent(this@MasukActivity, DaftarActivity::class.java))
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.layoutFormLogin.visibility == View.VISIBLE) {
                    showPilihanMasuk()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun showFormLogin() {
        binding.layoutPilihanMasuk.visibility = View.GONE
        binding.layoutFormLogin.visibility = View.VISIBLE
        binding.btnBackToPilihan.visibility = View.VISIBLE
        binding.main.smoothScrollTo(0, 0)
        if (binding.etUsername.text.isNullOrEmpty()) {
            binding.etUsername.requestFocus()
        } else {
            binding.etPin.requestFocus()
        }
    }

    private fun showPilihanMasuk() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
        currentFocus?.let { imm?.hideSoftInputFromWindow(it.windowToken, 0) }

        binding.layoutFormLogin.visibility = View.GONE
        binding.btnBackToPilihan.visibility = View.GONE
        binding.layoutPilihanMasuk.visibility = View.VISIBLE
        binding.main.smoothScrollTo(0, 0)
    }

    private fun showBantuanLoginDialog() {
        val dialog = android.app.Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_bantuan_login)
        dialog.window?.apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        val actvNamaPetani = dialog.findViewById<android.widget.AutoCompleteTextView>(R.id.actvNamaPetani)
        val tvLabelDesa = dialog.findViewById<android.widget.TextView>(R.id.tvLabelDesa)
        val tvNamaAdmin = dialog.findViewById<android.widget.TextView>(R.id.tvNamaAdmin)
        val btnKirimWhatsApp = dialog.findViewById<android.view.View>(R.id.btnKirimWhatsApp)
        val btnResetPinMandiri = dialog.findViewById<android.view.View>(R.id.btnResetPinMandiri)
        val btnCloseDialog = dialog.findViewById<android.view.View>(R.id.btnCloseDialog)

        btnCloseDialog.setOnClickListener { dialog.dismiss() }
        btnResetPinMandiri.setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(this, LupaPinActivity::class.java))
        }

        var listPetaniData = listOf<com.example.notasawit.Room.Petani.PetaniEntity>()
        var selectedPetani: com.example.notasawit.Room.Petani.PetaniEntity? = null

        // Load data petani dari database lokal Room
        lifecycleScope.launch(Dispatchers.IO) {
            val list = database.masterDao().getAllPetani()
            listPetaniData = list
            val namaList = list.map { "${it.namaPetani} - ${it.namaDesa}" }

            runOnUiThread {
                if (!isFinishing && !isDestroyed) {
                    val adapter = android.widget.ArrayAdapter(
                        this@MasukActivity,
                        android.R.layout.simple_dropdown_item_1line,
                        namaList
                    )
                    actvNamaPetani.setAdapter(adapter)

                    // Jika di form login ada username yang mirip nama, coba pre-fill
                    val currentUsername = binding.etUsername.text.toString().trim()
                    if (currentUsername.isNotEmpty()) {
                        val match = list.firstOrNull {
                            it.namaPetani.contains(currentUsername, ignoreCase = true) ||
                            currentUsername.contains(it.namaPetani, ignoreCase = true)
                        }
                        if (match != null) {
                            actvNamaPetani.setText(match.namaPetani, false)
                            selectedPetani = match
                            val admin = AdminDesaHelper.getAdminByDesaId(match.desaId)
                            tvLabelDesa.text = "📍 Desa: ${match.namaDesa}"
                            tvNamaAdmin.text = "Admin Desa: ${admin.namaAdmin}"
                        }
                    }
                }
            }
        }

        // Listener saat petani memilih nama dari dropdown
        actvNamaPetani.setOnItemClickListener { parent, _, position, _ ->
            val selectedText = parent.getItemAtPosition(position).toString()
            val parts = selectedText.split(" - ")
            val namaSaja = parts.firstOrNull() ?: selectedText
            actvNamaPetani.setText(namaSaja, false)

            selectedPetani = listPetaniData.firstOrNull { it.namaPetani.equals(namaSaja, ignoreCase = true) }
            selectedPetani?.let {
                val admin = AdminDesaHelper.getAdminByDesaId(it.desaId)
                tvLabelDesa.text = "📍 Desa: ${it.namaDesa}"
                tvNamaAdmin.text = "Admin Desa: ${admin.namaAdmin}"
            }
        }

        // Listener saat petani mengetik manual nama mereka
        actvNamaPetani.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim() ?: ""
                val match = listPetaniData.firstOrNull { it.namaPetani.equals(query, ignoreCase = true) }
                if (match != null) {
                    selectedPetani = match
                    val admin = AdminDesaHelper.getAdminByDesaId(match.desaId)
                    tvLabelDesa.text = "📍 Desa: ${match.namaDesa}"
                    tvNamaAdmin.text = "Admin Desa: ${admin.namaAdmin}"
                } else {
                    selectedPetani = null
                    if (query.isNotEmpty()) {
                        tvLabelDesa.text = "📍 Menghubungkan Admin Terkait..."
                        tvNamaAdmin.text = "Admin: Siap membantu"
                    } else {
                        tvLabelDesa.text = "📍 Wilayah: Memilih Desa..."
                        tvNamaAdmin.text = "Admin: Siap menghubungkan..."
                    }
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        btnKirimWhatsApp.setOnClickListener {
            val inputNama = actvNamaPetani.text.toString().trim()
            if (inputNama.isEmpty()) {
                Toast.makeText(this, "Silakan masukkan nama lengkap Anda", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Cari petani berdasarkan nama jika belum terpilih
            val petani = selectedPetani ?: listPetaniData.firstOrNull {
                it.namaPetani.equals(inputNama, ignoreCase = true) ||
                inputNama.contains(it.namaPetani, ignoreCase = true) ||
                it.namaPetani.contains(inputNama, ignoreCase = true)
            }

            if (petani != null) {
                val admin = AdminDesaHelper.getAdminByDesaId(petani.desaId)
                val pesan = "Halo Admin ${admin.namaAdmin}, saya ${petani.namaPetani} dari Desa ${petani.namaDesa}, mengalami kendala masuk akun SILAUSA. Mohon bantuannya untuk akun saya."
                AdminDesaHelper.openWhatsApp(this, admin.noHpWa, pesan)
            } else {
                // Jika nama tidak terdaftar, tetap arahkan ke kontak pusat dengan nama lengkap yang diketik
                val pesan = "Halo Admin SILAUSA, saya $inputNama, mengalami kendala masuk akun SILAUSA. Mohon bantuannya untuk akun saya."
                AdminDesaHelper.openWhatsApp(this, AdminDesaHelper.DEFAULT_ADMIN_PHONE, pesan)
            }
            dialog.dismiss()
        }

        dialog.show()
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


    private fun setLoginLoadingState(isLoading: Boolean) {
        runOnUiThread {
            if (isLoading) {
                binding.btnLogin.isEnabled = false
                binding.pbLogin.visibility = View.VISIBLE
                binding.tvBtnLogin.visibility = View.GONE
            } else {
                binding.btnLogin.isEnabled = true
                binding.pbLogin.visibility = View.GONE
                binding.tvBtnLogin.visibility = View.VISIBLE
            }
        }
    }

    private fun performLogin(username: String, pin: String, isBiometric: Boolean = false) {
        val cleanUsername = username.trim()
        val cleanPin = pin.trim()

        if (cleanUsername.isEmpty()) {
            com.example.notasawit.Utils.CustomAlert.showError(this, "Peringatan", "Harap masukkan username Anda")
            return
        }
        if (cleanPin.isEmpty()) {
            com.example.notasawit.Utils.CustomAlert.showError(this, "Peringatan", "Harap masukkan PIN Anda")
            return
        }

        setLoginLoadingState(true)

        PetaniApi.login(
            cleanUsername,
            cleanPin,
            object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        setLoginLoadingState(false)
                        val sharedPref = getSharedPreferences("NOTASAWIT_PREF", MODE_PRIVATE)
                        val petaniId = sharedPref.getInt("petani_id", 0)
                        val role = sharedPref.getString("role", "petani") ?: "petani"

                        // Jika login biometrik dan offline, gunakan data cache lokal
                        if (isBiometric && (petaniId > 0 || role == "admin")) {
                            sharedPref.edit().putBoolean("is_logged_in", true).apply()
                            val nama = sharedPref.getString("namaPetani", sharedPref.getString("username", "Petani Sawit"))
                            val targetIntent = if (role == "admin") {
                                Intent(this@MasukActivity, BaseAdminActivity::class.java)
                            } else {
                                Intent(this@MasukActivity, BaseActivity::class.java)
                            }
                            targetIntent.putExtra("SHOW_WELCOME_MASCOT", true)
                            targetIntent.putExtra("SHOW_LOGIN_ALERT", true)
                            targetIntent.putExtra("EXTRA_NAME", nama ?: "")
                            startActivity(targetIntent)
                            finish()
                        } else {
                            com.example.notasawit.Utils.CustomAlert.showError(
                                this@MasukActivity,
                                "Gagal Terhubung",
                                "Tidak dapat terhubung ke server. Periksa koneksi internet Anda."
                            )
                        }
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val json = response.body?.string()
                    Log.d("LOGIN_RESPONSE", json ?: "NULL")

                    runOnUiThread {
                        setLoginLoadingState(false)
                    }

                    if (json.isNullOrEmpty()) {
                        runOnUiThread {
                            com.example.notasawit.Utils.CustomAlert.showError(
                                this@MasukActivity,
                                "Gagal",
                                "Tidak ada respon dari server"
                            )
                        }
                        return
                    }

                    try {
                        val loginResponse = Gson().fromJson(json, LoginResponse::class.java)
                        val data = loginResponse.data
                        val sharedPref = getSharedPreferences("NOTASAWIT_PREF", MODE_PRIVATE)

                        if (loginResponse.success) {
                            val rememberMe = binding.cbRememberMe.isChecked
                            with(sharedPref.edit()) {
                                putBoolean("is_logged_in", true)
                                putBoolean("remember_me", rememberMe)
                                if (rememberMe) {
                                    putString("last_username", cleanUsername)
                                    putString("saved_pin", cleanPin)
                                } else {
                                    remove("last_username")
                                    remove("saved_pin")
                                }

                                if (loginResponse.role == "petani") {
                                    putString("role", loginResponse.role)
                                    putInt("petani_id", data?.petani_id ?: 0)
                                    putString("namaPetani", data?.petani_nama ?: "")
                                    putString("username", data?.petani_username ?: "")
                                    putString("profilPetani", data?.petani_profil ?: "")
                                    putInt("desaPetani", data?.desa_id ?: 0)
                                } else {
                                    putString("role", "admin")
                                    putInt("user_id", data?.user_id ?: 0)
                                    putString("username", data?.user_username ?: "")
                                    putString("user_role", data?.user_role ?: "")
                                    putInt("admin_desa_id", data?.desa_id ?: 0)
                                }
                                apply()
                            }

                            if (loginResponse.role == "petani") {
                                getLahan(data?.petani_id ?: 0)
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
                            targetIntent.putExtra("SHOW_LOGIN_ALERT", true)
                                targetIntent.putExtra("EXTRA_NAME", nama ?: "")
                                startActivity(targetIntent)
                                finish()
                            }
                        } else {
                            val errorMessage = when {
                                json.contains("Bcrypt algorithm", ignoreCase = true) ->
                                    "Format PIN akun ini perlu diperbarui oleh Admin. Silakan hubungi Admin Desa via WhatsApp."
                                else -> loginResponse.message ?: "Username atau PIN salah"
                            }
                            runOnUiThread {
                                com.example.notasawit.Utils.CustomAlert.showError(
                                    this@MasukActivity,
                                    "Gagal Masuk",
                                    errorMessage
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("LOGIN_ERROR", "Response: $json", e)
                        val errorMessage = if (json.contains("Bcrypt algorithm", ignoreCase = true)) {
                            "Format PIN akun ini perlu diperbarui oleh Admin. Silakan hubungi Admin Desa via WhatsApp."
                        } else {
                            "Terjadi kesalahan saat memproses login. Coba lagi beberapa saat lagi."
                        }
                        runOnUiThread {
                            com.example.notasawit.Utils.CustomAlert.showError(
                                this@MasukActivity,
                                "Error",
                                errorMessage
                            )
                        }
                    }
                }
            }
        )
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)

        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)

                    val sharedPref = getSharedPreferences("NOTASAWIT_PREF", Context.MODE_PRIVATE)
                    val lastUser = sharedPref.getString("last_username", "")?.ifEmpty { null }
                        ?: sharedPref.getString("username", "") ?: ""
                    val savedPin = sharedPref.getString("saved_pin", "") ?: ""
                    val petaniId = sharedPref.getInt("petani_id", 0)
                    val userId = sharedPref.getInt("user_id", 0)
                    val role = sharedPref.getString("role", "petani") ?: "petani"

                    if (lastUser.isNotEmpty() && savedPin.isNotEmpty()) {
                        binding.etUsername.setText(lastUser)
                        binding.etPin.setText(savedPin)
                        performLogin(lastUser, savedPin, isBiometric = true)
                    } else if (petaniId > 0 || userId > 0) {
                        sharedPref.edit().putBoolean("is_logged_in", true).apply()
                        val nama = sharedPref.getString("namaPetani", sharedPref.getString("username", "Petani Sawit"))
                        val targetIntent = if (role == "admin") {
                            Intent(this@MasukActivity, BaseAdminActivity::class.java)
                        } else {
                            Intent(this@MasukActivity, BaseActivity::class.java)
                        }
                        targetIntent.putExtra("SHOW_WELCOME_MASCOT", true)
                            targetIntent.putExtra("SHOW_LOGIN_ALERT", true)
                        targetIntent.putExtra("EXTRA_NAME", nama ?: "")
                        startActivity(targetIntent)
                        finish()
                    } else {
                        Toast.makeText(this@MasukActivity, "Silakan login menggunakan PIN terlebih dahulu", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        Toast.makeText(this@MasukActivity, "Autentikasi sidik jari dibatalkan", Toast.LENGTH_SHORT).show()
                    }
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

                binding.pbGoogleLoadingForm.visibility = View.VISIBLE
                binding.ivGoogleIconForm.visibility = View.GONE
                binding.tvGoogleTextForm.visibility = View.GONE
                binding.btnGoogleSignInForm.isEnabled = false
            } else {
                binding.pbGoogleLoading.visibility = View.GONE
                binding.ivGoogleIcon.visibility = View.VISIBLE
                binding.tvGoogleText.visibility = View.VISIBLE
                binding.btnGoogleSignIn.isEnabled = true

                binding.pbGoogleLoadingForm.visibility = View.GONE
                binding.ivGoogleIconForm.visibility = View.VISIBLE
                binding.tvGoogleTextForm.visibility = View.VISIBLE
                binding.btnGoogleSignInForm.isEnabled = true
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
                checkAdminEmailOrRegister(email, nama)
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
                                if (item.has("petani_email") && item.getString("petani_email").trim().equals(email.trim(), ignoreCase = true)) {
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
                    setGoogleLoadingState(false)
                    runOnUiThread {
                        loginAsPetaniGoogle(matchedPetani, nama)
                    }
                } else {
                    // Cek apakah akun adalah Admin
                    checkAdminEmailOrRegister(email, nama)
                }
            }
        })
    }

    private fun checkAdminEmailOrRegister(email: String, nama: String) {
        PetaniApi.getAdmins(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                setGoogleLoadingState(false)
                runOnUiThread {
                    proceedToRegistration(email, nama)
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
                                if (item.has("user_email") && item.getString("user_email").trim().equals(email.trim(), ignoreCase = true)) {
                                    matchedAdmin = item
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
                    if (matchedAdmin != null) {
                        loginAsAdminGoogle(matchedAdmin, nama)
                    } else {
                        proceedToRegistration(email, nama)
                    }
                }
            }
        })
    }

    private fun loginAsPetaniGoogle(item: org.json.JSONObject, defaultNama: String) {
        val sharedPref = getSharedPreferences("NOTASAWIT_PREF", MODE_PRIVATE)
        val rememberMe = binding.cbRememberMe.isChecked
        val petaniUsername = item.optString("petani_username", "")
        val petaniNama = item.optString("petani_nama", defaultNama)
        val petaniId = item.optInt("petani_id", 0)
        val desaId = item.optInt("desa_id", 0)
        val profil = item.optString("petani_profil", "")
        val pin = item.optString("petani_pin", "")

        with(sharedPref.edit()) {
            putBoolean("is_logged_in", true)
            putBoolean("remember_me", rememberMe)
            if (rememberMe) {
                if (petaniUsername.isNotEmpty()) putString("last_username", petaniUsername)
                if (pin.isNotEmpty()) putString("saved_pin", pin)
            }

            putString("role", "petani")
            putInt("petani_id", petaniId)
            putString("namaPetani", petaniNama)
            putString("username", petaniUsername)
            putString("profilPetani", profil)
            putInt("desaPetani", desaId)
            apply()
        }

        if (petaniId > 0) {
            getLahan(petaniId)
        }
        getDesa()
        getAdminsAndPetani()

        val targetIntent = Intent(this@MasukActivity, BaseActivity::class.java).apply {
            putExtra("SHOW_WELCOME_MASCOT", true)
            putExtra("SHOW_LOGIN_ALERT", true)
            putExtra("EXTRA_NAME", petaniNama)
        }
        startActivity(targetIntent)
        finish()
    }

    private fun loginAsAdminGoogle(item: org.json.JSONObject, defaultNama: String) {
        val sharedPref = getSharedPreferences("NOTASAWIT_PREF", MODE_PRIVATE)
        val rememberMe = binding.cbRememberMe.isChecked
        val adminUsername = item.optString("user_username", "")
        val adminNama = item.optString("user_nama", defaultNama)

        with(sharedPref.edit()) {
            putBoolean("is_logged_in", true)
            putBoolean("remember_me", rememberMe)
            if (rememberMe && adminUsername.isNotEmpty()) {
                putString("last_username", adminUsername)
            }
            putString("role", "admin")
            putInt("user_id", item.optInt("user_id", 0))
            putString("username", adminUsername)
            putString("user_role", item.optString("user_role", "admin"))
            putInt("admin_desa_id", item.optInt("desa_id", 0))
            apply()
        }

        getDesa()
        getAdminsAndPetani()

        val targetIntent = Intent(this@MasukActivity, BaseAdminActivity::class.java).apply {
            putExtra("SHOW_WELCOME_MASCOT", true)
            putExtra("SHOW_LOGIN_ALERT", true)
            putExtra("EXTRA_NAME", adminNama)
        }
        startActivity(targetIntent)
        finish()
    }

    private fun proceedToRegistration(email: String, nama: String) {
        sharedPref.edit()
            .putBoolean("is_login", true)
            .putBoolean("sudah_pernah_login", true)
            .putString("namaPetani", nama)
            .putString("emailPetani", email)
            .apply()

        Toast.makeText(
            this@MasukActivity,
            "Akun belum terdaftar. Melanjutkan pendaftaran...",
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

    private fun getJenisKegiatan() {
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