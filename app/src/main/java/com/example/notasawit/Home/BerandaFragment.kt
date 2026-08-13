package com.example.notasawit.Home

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.notasawit.InputKegiatan.InputKegiatanActivity
import com.example.notasawit.Pemasukan.InputPemasukanActivity
import com.example.notasawit.databinding.FragmentBerandaBinding
import com.example.notasawit.Model.QuoteResponse
import com.example.notasawit.Network.RetrofitClient
import com.example.notasawit.Pengeluaran.InputPengeluaranActivity

import com.example.notasawit.Room.AppDatabase
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BerandaFragment : Fragment() {

    private var _binding: FragmentBerandaBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler(Looper.getMainLooper())
    private val sharedPref by lazy {
        requireActivity().getSharedPreferences("NOTASAWIT_PREF", Context.MODE_PRIVATE)
    }
    private lateinit var database: AppDatabase
    private val quoteRunnable = object : Runnable {
        override fun run() {

            if (_binding != null) {

                binding.tvQuote.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction {
                        loadQuote()
                        binding.tvQuote.animate()
                            .alpha(1f)
                            .setDuration(300)
                            .start()
                    }
                    .start()

                // ganti quote setiap 10 detik
                handler.postDelayed(this, 10000)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentBerandaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        val username = sharedPref.getString("username", "") ?: ""
        binding.username.setText("$username!")
        binding.username.setTextColor(Color.parseColor("#1B4D2E"))
        // quote pertama
        loadQuote()
        database = AppDatabase.getDatabase(requireContext())

        // mulai auto refresh quote
        handler.postDelayed(quoteRunnable, 10000)

        binding.cardInputKegiatan.setOnClickListener {
            startActivity(
                Intent(
                    requireContext(),
                    InputKegiatanActivity::class.java
                )
            )
        }

        binding.cardPemasukan.setOnClickListener {
            startActivity(
                Intent(
                    requireContext(),
                    InputPemasukanActivity::class.java
                )
            )
        }
        binding.cardPengeluaran.setOnClickListener {
            startActivity(
                Intent(
                    requireContext(),
                    InputPengeluaranActivity::class.java
                )
            )
        }
        
        binding.btnProfileContainer.setOnClickListener {
            startActivity(
                Intent(
                    requireContext(),
                    com.example.notasawit.ProfilPetani.ProfilPetaniActivity::class.java
                )
            )
        }
        
        binding.cardEdukasi.setOnClickListener {
            startActivity(
                Intent(
                    requireContext(),
                    EdukasiActivity::class.java
                )
            )
        }
        updateProfileUI()

        val petaniId = sharedPref.getInt("petani_id", -1)
        if (petaniId != -1) {
            loadPetaniSummary(petaniId)
            checkTahunTanamLahan(petaniId)
        }
        
        binding.btnIsiTahunTanam.setOnClickListener {
            startActivity(Intent(requireContext(), com.example.notasawit.ProfilPetani.EditLahanActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        updateProfileUI()
        val petaniId = sharedPref.getInt("petani_id", -1)
        if (petaniId != -1) {
            checkTahunTanamLahan(petaniId)
        }
    }

    private fun updateProfileUI() {
        val fotoProfil = sharedPref.getString("profilPetani", null)
        val namaPetani = sharedPref.getString("namaPetani", "") ?: ""
        
        if (fotoProfil.isNullOrBlank() || fotoProfil == "null") {
            binding.imgProfile.visibility = View.GONE
            binding.tvInitial.visibility = View.VISIBLE
            binding.tvInitial.text = getInitials(namaPetani)
        } else {
            binding.tvInitial.visibility = View.GONE
            binding.imgProfile.visibility = View.VISIBLE
            val fullUrl = if (!fotoProfil.startsWith("http")) {
                "http://160.187.144.157/storage/$fotoProfil"
            } else {
                fotoProfil
            }
            Glide.with(this)
                .load(fullUrl)
                .into(binding.imgProfile)
        }
    }


    fun getInitials(name: String): String {
        val words = name.trim().split("\\s+".toRegex())

        return when {
            words.size >= 2 ->
                "${words.first()[0]}${words.last()[0]}".uppercase()

            words.isNotEmpty() ->
                words[0][0].toString().uppercase()

            else -> "?"
        }
    }

    private fun loadQuote() {

        RetrofitClient.api.getRandomQuote()
            .enqueue(object : Callback<List<QuoteResponse>> {

                override fun onResponse(
                    call: Call<List<QuoteResponse>>,
                    response: Response<List<QuoteResponse>>
                ) {

                    if (!isAdded || _binding == null) return

                    if (response.isSuccessful &&
                        response.body() != null &&
                        response.body()!!.isNotEmpty()
                    ) {

                        val quote = response.body()!![0]

                        binding.tvQuote.text = quote.q
                        binding.createdBy.text = "— ${quote.a}"
                    } else {
                        binding.tvQuote.text =
                            "Tetap semangat dalam mengelola kebun hari ini 🌱"
                    }
                }
                override fun onFailure(
                    call: Call<List<QuoteResponse>>,
                    t: Throwable
                ) {
                    if (!isAdded || _binding == null) return
                    Log.e("QUOTE_ERROR", t.message ?: "Unknown Error")
                    binding.tvQuote.text =
                        "Tetap semangat dalam mengelola kebun hari ini 🌱"
                }
            })
    }

    private fun loadPetaniSummary(petaniId: Int) {
        com.example.notasawit.Network.PetaniApi.getPetaniSummary(petaniId, object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                if (!isAdded || _binding == null) return
                requireActivity().runOnUiThread {
                    binding.tvPemasukan.text = "Rp 0"
                    binding.tvPengeluaran.text = "Rp 0"
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (!isAdded || _binding == null) return
                val responseData = response.body?.string()
                if (response.isSuccessful && responseData != null) {
                    try {
                        val jsonObject = org.json.JSONObject(responseData)
                        if (jsonObject.getBoolean("success")) {
                            val dataObj = jsonObject.getJSONObject("data")
                            val pemasukanIni = dataObj.getDouble("pemasukan_bulan_ini")
                            val pemasukanLalu = dataObj.getDouble("pemasukan_bulan_lalu")
                            val pengeluaranIni = dataObj.getDouble("pengeluaran_bulan_ini")
                            val pengeluaranLalu = dataObj.getDouble("pengeluaran_bulan_lalu")

                            requireActivity().runOnUiThread {
                                binding.tvPemasukan.text = formatCurrency(pemasukanIni)
                                binding.tvPengeluaran.text = formatCurrency(pengeluaranIni)

                                // Logic Pemasukan
                                if (pemasukanIni > pemasukanLalu) {
                                    binding.tvBadgePemasukan.text = "↑ Meningkat"
                                    binding.tvBadgePemasukan.setBackgroundResource(com.example.notasawit.R.drawable.bg_badge_green)
                                    binding.tvBadgePemasukan.setTextColor(Color.parseColor("#1B4D2E"))
                                } else if (pemasukanIni < pemasukanLalu) {
                                    binding.tvBadgePemasukan.text = "↓ Menurun"
                                    binding.tvBadgePemasukan.setBackgroundResource(com.example.notasawit.R.drawable.bg_badge_red)
                                    binding.tvBadgePemasukan.setTextColor(Color.parseColor("#C62828"))
                                } else {
                                    binding.tvBadgePemasukan.text = "Stabil"
                                    binding.tvBadgePemasukan.setBackgroundResource(com.example.notasawit.R.drawable.bg_badge_green)
                                    binding.tvBadgePemasukan.setTextColor(Color.parseColor("#1B4D2E"))
                                }

                                // Logic Pengeluaran
                                if (pengeluaranIni < pengeluaranLalu) {
                                    binding.tvBadgePengeluaran.text = "↓ Lebih hemat"
                                    binding.tvBadgePengeluaran.setBackgroundResource(com.example.notasawit.R.drawable.bg_badge_green)
                                    binding.tvBadgePengeluaran.setTextColor(Color.parseColor("#1B4D2E"))
                                } else if (pengeluaranIni > pengeluaranLalu) {
                                    binding.tvBadgePengeluaran.text = "↑ Lebih boros"
                                    binding.tvBadgePengeluaran.setBackgroundResource(com.example.notasawit.R.drawable.bg_badge_red)
                                    binding.tvBadgePengeluaran.setTextColor(Color.parseColor("#C62828"))
                                } else {
                                    binding.tvBadgePengeluaran.text = "Stabil"
                                    binding.tvBadgePengeluaran.setBackgroundResource(com.example.notasawit.R.drawable.bg_badge_green)
                                    binding.tvBadgePengeluaran.setTextColor(Color.parseColor("#1B4D2E"))
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        })
    }

    private fun formatCurrency(amount: Double): String {
        val format = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("id", "ID"))
        format.maximumFractionDigits = 0
        return format.format(amount).replace("Rp", "Rp ")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(quoteRunnable)
        _binding = null
    }
//    override fun onResume() {
//        super.onResume()
//
//        lifecycleScope.launch {
//
//            SyncProduksiRepository(
//                requireContext(),
//                database
//            ).syncProduksi()
//
//    }

    private fun checkTahunTanamLahan(petaniId: Int) {
        com.example.notasawit.Network.PetaniApi.getLahanByPetani(petaniId, object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                // Ignore failure
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (!isAdded || _binding == null) return
                val responseData = response.body?.string()
                
                var hasEmptyTahunTanam = false
                
                if (response.isSuccessful && responseData != null) {
                    try {
                        val jsonObject = org.json.JSONObject(responseData)
                        if (jsonObject.optBoolean("success", false)) {
                            val dataArray = jsonObject.optJSONArray("data")
                            if (dataArray != null && dataArray.length() > 0) {
                                val lahanList = mutableListOf<com.example.notasawit.Room.Lahan.LahanEntity>()
                                for (i in 0 until dataArray.length()) {
                                    val lahanObj = dataArray.getJSONObject(i)
                                    
                                    val lahanId = lahanObj.optInt("lahan_id", 0)
                                    val lahanNama = lahanObj.optString("lahan_nama", "")
                                    lahanList.add(
                                        com.example.notasawit.Room.Lahan.LahanEntity(
                                            lahan_id = lahanId,
                                            petani_id = petaniId,
                                            lahan_nama = lahanNama
                                        )
                                    )
                                    
                                    val tahunTanam = lahanObj.optString("tahun_tanam", "")
                                    if (tahunTanam.isNullOrEmpty() || tahunTanam == "null" || tahunTanam == "0") {
                                        hasEmptyTahunTanam = true
                                        // Jangan break agar semua lahan tersimpan di lokal database
                                    }
                                }
                                
                                if (lahanList.isNotEmpty()) {
                                    lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                        database.LahanDao().insertLahan(lahanList)
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                requireActivity().runOnUiThread {
                    if (hasEmptyTahunTanam) {
                        binding.cardAlertTahunTanam.visibility = android.view.View.VISIBLE
                    } else {
                        binding.cardAlertTahunTanam.visibility = android.view.View.GONE
                    }
                }
            }
        })
    }
}