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
import com.example.notasawit.InputKegiatan.InputKegiatanActivity
import com.example.notasawit.Pemasukan.InputPemasukanActivity
import com.example.notasawit.databinding.FragmentBerandaBinding
import com.example.notasawit.Model.QuoteResponse
import com.example.notasawit.Network.RetrofitClient
import com.example.notasawit.Pengeluaran.InputPengeluaranActivity
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
        binding.username.setTextColor(Color.parseColor("#1B5E20"))
        // quote pertama
        loadQuote()

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
        val fotoProfil = sharedPref.getString("profilPetani", null)

        val namaPetani = sharedPref.getString("namaPetani", "") ?: ""
        if (fotoProfil.isNullOrBlank() || fotoProfil == "null") {
            binding.imgProfile.visibility = View.GONE
            binding.tvInitial.visibility = View.VISIBLE
            binding.tvInitial.text = getInitials(namaPetani)
        } else {
            binding.tvInitial.visibility = View.GONE
            binding.imgProfile.visibility = View.VISIBLE
//            Glide.with(this)
//                .load(fotoProfil)
//                .into(binding.imgProfile)
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

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(quoteRunnable)
        _binding = null
    }
}