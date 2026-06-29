package com.example.notasawit.Autentikasi.Daftar.DataDiri

import android.R
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import com.example.notasawit.Autentikasi.Daftar.DataDiri.Desa.Desa
import com.example.notasawit.Autentikasi.Daftar.DataDiri.Desa.DesaApiResponse
import com.example.notasawit.Network.PetaniApi
import com.example.notasawit.databinding.FragmentDataDiri2Binding
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import com.google.gson.Gson
import java.io.IOException



/**
 * A simple [Fragment] subclass.
 * Use the [DataDiri2Fragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class DataDiri2Fragment : Fragment() {

    // GUNAKAN 'by lazy' agar requireActivity() tidak langsung dipanggil saat Fragment dibuat
    private val sharedPref by lazy {
        requireActivity().getSharedPreferences("NOTASAWIT_PREF", Context.MODE_PRIVATE)
    }

    // Pindahkan pengambilan data string ini ke dalam fungsi atau gunakan lazy juga


    private val sp_noHpPetani by lazy { sharedPref.getString("noHpPetani", "") }
    private val sp_emailPetani by lazy { sharedPref.getString("emailPetani", "") }
    private val sp_alamatPetani by lazy { sharedPref.getString("alamatPetani", "") }
    private val sp_desaPetani by lazy { sharedPref.getInt("desaPetani", 0) }



    private var _binding: FragmentDataDiri2Binding? = null
    private val binding get() = _binding!!
    private val desaList = mutableListOf<Desa>()
    private var selectedDesaId: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDataDiri2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadDesa()
        binding.btnNext.isEnabled = false
        binding.btnNext.alpha = 0.5f

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                periksaKelengkapanData()
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        binding.etEmail.setText(sp_emailPetani)

        binding.etEmail.addTextChangedListener(textWatcher)
        binding.etNomorHandphone.addTextChangedListener(textWatcher)
        binding.etAlamat.addTextChangedListener(textWatcher)
        binding.etDesa.addTextChangedListener(textWatcher)
//Ambil ID Desa
        binding.etDesa.setOnItemClickListener { _, _, position, _ ->

            selectedDesaId = desaList[position].desa_id
        }

        binding.btnNext.setOnClickListener {
            val email_petani = binding.etEmail.text.toString().trim()
            val noHpPetani = binding.etNomorHandphone.text.toString().trim()
            val alamatPetani = binding.etAlamat.text.toString().trim()
            val desaPetani = binding.etDesa.text.toString().trim()

            // 1. Tambahkan validasi apakah user sudah memilih desa dari dropdown (selectedDesaId tidak null)
            if (selectedDesaId == null) {
                Toast.makeText(requireContext(), "Silakan pilih desa dari daftar yang tersedia", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (email_petani.isNotEmpty() && noHpPetani.isNotEmpty()
                && alamatPetani.isNotEmpty() && desaPetani.isNotEmpty()) {

                // Simpan ke SharedPreferences
                sharedPref.edit().apply {
                    putString("emailPetani", email_petani)
                    putString("noHpPetani", noHpPetani)
                    putString("alamatPetani", alamatPetani)
                    putInt("desaPetani", selectedDesaId ?: 0) // Tetap simpan nama desa jika diperlukan untuk tampilan

                    // 2. PERBAIKAN UTAMA: Simpan ID Desanya ke SharedPreferences sebagai Integer
                    putInt("desaIdPetani", selectedDesaId ?: 0)
                    apply()
                }

                val bapakActivity = activity as? DataDiriActivity
                bapakActivity?.nextStep()
            }
        }

    }
    private fun periksaKelengkapanData() {
        val email_petani = binding.etEmail.text.toString().trim()
        val noHpPetani = binding.etNomorHandphone.text.toString().trim()
        val alamatPetani = binding.etAlamat.text.toString().trim()
        val desaPetani = binding.etDesa.text.toString().trim()

        val semuaSudahIsi = email_petani.isNotEmpty() &&
                noHpPetani.isNotEmpty() &&
                alamatPetani.isNotEmpty() &&
                desaPetani.isNotEmpty()

        if (semuaSudahIsi) {
            binding.btnNext.isEnabled = true
            binding.btnNext.alpha = 1.0f
        } else {
            binding.btnNext.isEnabled = false
            binding.btnNext.alpha = 0.5f
        }
    }
    private fun loadDesa() {

        PetaniApi.getDesa(object : Callback {

            override fun onFailure(
                call: Call,
                e: IOException
            ) {

                requireActivity().runOnUiThread {

                    Toast.makeText(
                        requireContext(),
                        "Gagal mengambil desa",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onResponse(
                call: Call,
                response: Response
            ) {

                val json = response.body?.string()

                val desaResponse =
                    Gson().fromJson(
                        json,
                        DesaApiResponse::class.java
                    )

                desaList.clear()
                desaList.addAll(desaResponse.data)

                val namaDesa =
                    desaList.map { it.desa_nama }

                requireActivity().runOnUiThread {

                    val adapter = ArrayAdapter(
                        requireContext(),
                        R.layout.simple_dropdown_item_1line,
                        namaDesa
                    )

                    binding.etDesa.setAdapter(adapter)
                }
            }
        })
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }






}