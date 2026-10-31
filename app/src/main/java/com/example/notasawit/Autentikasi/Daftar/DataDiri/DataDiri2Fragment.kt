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

class DataDiri2Fragment : Fragment() {

    private val sharedPref by lazy {
        requireActivity().getSharedPreferences("NOTASAWIT_PREF", Context.MODE_PRIVATE)
    }

    private var _binding: FragmentDataDiri2Binding? = null
    private val binding get() = _binding!!
    private val desaList = mutableListOf<Desa>()
    private var selectedDesaId: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDataDiri2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.btnNext.isEnabled = false
        binding.btnNext.alpha = 0.5f

        // Pulihkan data yang tersimpan sebelumnya
        val spEmail = sharedPref.getString("emailPetani", "") ?: ""
        val spNoHp = sharedPref.getString("noHpPetani", "") ?: ""
        val spAlamat = sharedPref.getString("alamatPetani", "") ?: ""
        val spDesaNama = sharedPref.getString("desaNamaPetani", "") ?: ""
        val spDesaId = sharedPref.getInt("desaIdPetani", sharedPref.getInt("desaPetani", 0))

        if (binding.etEmail.text.isNullOrEmpty() && spEmail.isNotEmpty()) {
            binding.etEmail.setText(spEmail)
        }
        if (binding.etNomorHandphone.text.isNullOrEmpty() && spNoHp.isNotEmpty()) {
            binding.etNomorHandphone.setText(spNoHp)
        }
        if (binding.etAlamat.text.isNullOrEmpty() && spAlamat.isNotEmpty()) {
            binding.etAlamat.setText(spAlamat)
        }
        if (spDesaId != 0) {
            selectedDesaId = spDesaId
        }
        if (binding.etDesa.text.isNullOrEmpty() && spDesaNama.isNotEmpty()) {
            binding.etDesa.setText(spDesaNama, false)
        }

        loadDesa()

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                periksaKelengkapanData()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        binding.etEmail.addTextChangedListener(textWatcher)
        binding.etNomorHandphone.addTextChangedListener(textWatcher)
        binding.etAlamat.addTextChangedListener(textWatcher)
        binding.etDesa.addTextChangedListener(textWatcher)

        // Ambil ID Desa dari pemilihan dropdown
        binding.etDesa.setOnItemClickListener { parent, _, position, _ ->
            val selectedName = parent.getItemAtPosition(position).toString()
            val found = desaList.find { it.desa_nama == selectedName }
            selectedDesaId = found?.desa_id ?: desaList.getOrNull(position)?.desa_id
            periksaKelengkapanData()
        }

        // Tombol Kembali
        binding.btnBack.setOnClickListener {
            simpanKeSharedPref()
            val bapakActivity = activity as? DataDiriActivity
            bapakActivity?.previousStep()
        }

        // Tombol Selanjutnya
        binding.btnNext.setOnClickListener {
            val emailPetani = binding.etEmail.text.toString().trim()
            val noHpPetani = binding.etNomorHandphone.text.toString().trim()
            val alamatPetani = binding.etAlamat.text.toString().trim()
            val desaPetani = binding.etDesa.text.toString().trim()

            // Jika selectedDesaId masih null, coba cari dari text dropdown
            if (selectedDesaId == null || selectedDesaId == 0) {
                val found = desaList.find { it.desa_nama.equals(desaPetani, ignoreCase = true) }
                if (found != null) {
                    selectedDesaId = found.desa_id
                } else {
                    Toast.makeText(requireContext(), "Silakan pilih desa dari daftar yang tersedia", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            if (emailPetani.isNotEmpty() && noHpPetani.isNotEmpty()
                && alamatPetani.isNotEmpty() && desaPetani.isNotEmpty()) {
                simpanKeSharedPref()
                val bapakActivity = activity as? DataDiriActivity
                bapakActivity?.nextStep()
            }
        }

        periksaKelengkapanData()
    }

    private fun simpanKeSharedPref() {
        if (_binding == null) return
        val emailPetani = binding.etEmail.text.toString().trim()
        val noHpPetani = binding.etNomorHandphone.text.toString().trim()
        val alamatPetani = binding.etAlamat.text.toString().trim()
        val desaPetani = binding.etDesa.text.toString().trim()

        sharedPref.edit().apply {
            putString("emailPetani", emailPetani)
            putString("noHpPetani", noHpPetani)
            putString("alamatPetani", alamatPetani)
            if (selectedDesaId != null && selectedDesaId != 0) {
                putInt("desaPetani", selectedDesaId!!)
                putInt("desaIdPetani", selectedDesaId!!)
            }
            if (desaPetani.isNotEmpty()) {
                putString("desaNamaPetani", desaPetani)
            }
            apply()
        }
    }

    override fun onPause() {
        super.onPause()
        simpanKeSharedPref()
    }

    private fun periksaKelengkapanData() {
        if (_binding == null) return
        val emailPetani = binding.etEmail.text?.toString()?.trim() ?: ""
        val noHpPetani = binding.etNomorHandphone.text?.toString()?.trim() ?: ""
        val alamatPetani = binding.etAlamat.text?.toString()?.trim() ?: ""
        val desaPetani = binding.etDesa.text?.toString()?.trim() ?: ""

        val desaValid = (selectedDesaId != null && selectedDesaId != 0) ||
                desaList.any { it.desa_nama.equals(desaPetani, ignoreCase = true) }

        val semuaSudahIsi = emailPetani.isNotEmpty() &&
                noHpPetani.isNotEmpty() &&
                alamatPetani.isNotEmpty() &&
                desaPetani.isNotEmpty() &&
                desaValid

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
            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    if (isAdded) {
                        Toast.makeText(
                            requireContext(),
                            "Gagal mengambil desa: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val json = response.body?.string() ?: return
                try {
                    val desaResponse = Gson().fromJson(json, DesaApiResponse::class.java)

                    desaList.clear()
                    if (desaResponse.data != null) {
                        desaList.addAll(desaResponse.data)
                    }

                    val namaDesa = desaList.map { it.desa_nama }

                    activity?.runOnUiThread {
                        if (_binding == null || !isAdded) return@runOnUiThread

                        val adapter = ArrayAdapter(
                            requireContext(),
                            R.layout.simple_dropdown_item_1line,
                            namaDesa
                        )
                        binding.etDesa.setAdapter(adapter)

                        // Pulihkan pilihan desa jika sebelumnya sudah pernah dipilih
                        val curSavedDesaId = selectedDesaId ?: sharedPref.getInt("desaIdPetani", sharedPref.getInt("desaPetani", 0))
                        val curSavedDesaNama = sharedPref.getString("desaNamaPetani", "") ?: ""

                        if (curSavedDesaId != 0) {
                            val found = desaList.find { it.desa_id == curSavedDesaId }
                            if (found != null) {
                                selectedDesaId = found.desa_id
                                binding.etDesa.setText(found.desa_nama, false)
                            } else if (curSavedDesaNama.isNotEmpty()) {
                                binding.etDesa.setText(curSavedDesaNama, false)
                            }
                        } else if (curSavedDesaNama.isNotEmpty()) {
                            binding.etDesa.setText(curSavedDesaNama, false)
                            val found = desaList.find { it.desa_nama.equals(curSavedDesaNama, ignoreCase = true) }
                            if (found != null) {
                                selectedDesaId = found.desa_id
                            }
                        }

                        periksaKelengkapanData()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        })
    }

    override fun onDestroyView() {
        simpanKeSharedPref()
        super.onDestroyView()
        _binding = null
    }
}
