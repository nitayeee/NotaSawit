package com.example.notasawit.Autentikasi.Daftar.DataDiri

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.notasawit.databinding.FragmentDataDiri1Binding
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.graphics.Color
import android.content.res.ColorStateList

class DataDiri1Fragment : Fragment() {
    private val sharedPref by lazy {
        requireActivity().getSharedPreferences("NOTASAWIT_PREF", Context.MODE_PRIVATE)
    }

    private val sp_namaPetani by lazy { sharedPref.getString("namaPetani", "") }
    private val sp_username by lazy { sharedPref.getString("username", "") }
    private val sp_tanggalLahir by lazy { sharedPref.getString("tanggalLahir", "") }
    private val sp_jenisKelamin by lazy { sharedPref.getString("jenisKelamin", "") }
    private val sp_usia by lazy { sharedPref.getString("usia", "") }

    private var _binding: FragmentDataDiri1Binding? = null
    private val binding get() = _binding!!
    
    private var selectedJenisKelamin: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDataDiri1Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnNext.isEnabled = false
        binding.btnNext.alpha = 0.5f

        binding.etTanggalLahir.setOnClickListener {
            tampilkanDatePicker()
        }
        
        binding.etNamaDepan.setText(sp_namaPetani)
        binding.etUsername.setText(sp_username)
        binding.etTanggalLahir.setText(sp_tanggalLahir)
        binding.etUsia.setText(sp_usia)
        
        if (sp_jenisKelamin?.isNotEmpty() == true) {
            setJenisKelamin(sp_jenisKelamin!!)
        }

        binding.etUsia.isEnabled = false
        binding.etUsia.setTextColor(resources.getColor(android.R.color.black, null))

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                periksaKelengkapanData()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        binding.etNamaDepan.addTextChangedListener(textWatcher)
        binding.etUsername.addTextChangedListener(textWatcher)
        binding.etTanggalLahir.addTextChangedListener(textWatcher)
        binding.etUsia.addTextChangedListener(textWatcher)
        
        binding.btnPria.setOnClickListener {
            setJenisKelamin("Laki-laki")
        }
        binding.btnWanita.setOnClickListener {
            setJenisKelamin("Perempuan")
        }

        binding.btnNext.setOnClickListener {
            val namaPetani = binding.etNamaDepan.text.toString().trim()
            val username = binding.etUsername.text.toString().trim()
            val tanggalLahir = binding.etTanggalLahir.text.toString().trim()
            val usia = binding.etUsia.text.toString().trim()

            if (namaPetani.isNotEmpty() && tanggalLahir.isNotEmpty()
                && selectedJenisKelamin.isNotEmpty() && usia.isNotEmpty() && username.isNotEmpty()) {
                // Simpan ke SharedPreferences
                sharedPref.edit().apply {
                    putString("namaPetani", namaPetani)
                    putString("tanggalLahir", tanggalLahir)
                    putString("jenisKelamin", selectedJenisKelamin)
                    putString("username", username)
                    putString("usia", usia)
                    apply()
                }

                val bapakActivity = activity as? DataDiriActivity
                bapakActivity?.nextStep()
            }
        }
        
        periksaKelengkapanData()
    }
    
    private fun setJenisKelamin(jk: String) {
        selectedJenisKelamin = jk
        val activeBgColor = Color.parseColor("#E8ECE9")
        val activeTextColor = Color.parseColor("#1B4D2E")
        val activeStrokeColor = Color.parseColor("#1B4D2E")

        val inactiveBgColor = Color.TRANSPARENT
        val inactiveTextColor = Color.parseColor("#555555")
        val inactiveStrokeColor = Color.parseColor("#DDDDDD")

        if (jk == "Laki-laki") {
            binding.btnPria.setBackgroundColor(activeBgColor)
            binding.btnPria.setTextColor(activeTextColor)
            binding.btnPria.iconTint = ColorStateList.valueOf(activeTextColor)
            binding.btnPria.strokeColor = ColorStateList.valueOf(activeStrokeColor)

            binding.btnWanita.setBackgroundColor(inactiveBgColor)
            binding.btnWanita.setTextColor(inactiveTextColor)
            binding.btnWanita.iconTint = ColorStateList.valueOf(inactiveTextColor)
            binding.btnWanita.strokeColor = ColorStateList.valueOf(inactiveStrokeColor)
        } else if (jk == "Perempuan") {
            binding.btnWanita.setBackgroundColor(activeBgColor)
            binding.btnWanita.setTextColor(activeTextColor)
            binding.btnWanita.iconTint = ColorStateList.valueOf(activeTextColor)
            binding.btnWanita.strokeColor = ColorStateList.valueOf(activeStrokeColor)

            binding.btnPria.setBackgroundColor(inactiveBgColor)
            binding.btnPria.setTextColor(inactiveTextColor)
            binding.btnPria.iconTint = ColorStateList.valueOf(inactiveTextColor)
            binding.btnPria.strokeColor = ColorStateList.valueOf(inactiveStrokeColor)
        }
        periksaKelengkapanData()
    }

    private fun periksaKelengkapanData() {
        val namaDepan = binding.etNamaDepan.text.toString().trim()
        val username = binding.etUsername.text.toString().trim()
        val tanggalLahir = binding.etTanggalLahir.text.toString().trim()
        val usia = binding.etUsia.text.toString().trim()

        val semuaSudahIsi = namaDepan.isNotEmpty() &&
                username.isNotEmpty() &&
                tanggalLahir.isNotEmpty() &&
                selectedJenisKelamin.isNotEmpty() &&
                usia.isNotEmpty()

        if (semuaSudahIsi) {
            binding.btnNext.isEnabled = true
            binding.btnNext.alpha = 1.0f
        } else {
            binding.btnNext.isEnabled = false
            binding.btnNext.alpha = 0.5f
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun tampilkanDatePicker() {
        // 1. Inisialisasi MaterialDatePicker
        val builder = MaterialDatePicker.Builder.datePicker()
        builder.setTitleText("Pilih Tanggal Lahir")
        val picker = builder.build()

        picker.addOnPositiveButtonClickListener { selection ->
            // 2. Format tanggal untuk ditampilkan di EditText
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val tanggalTerpilih = sdf.format(Date(selection))
            binding.etTanggalLahir.setText(tanggalTerpilih)

            // 3. Hitung Usia Otomatis
            val kalenderLahir = Calendar.getInstance().apply { timeInMillis = selection }
            val kalenderSekarang = Calendar.getInstance()

            var usia = kalenderSekarang.get(Calendar.YEAR) - kalenderLahir.get(Calendar.YEAR)

            // Cek jika belum ulang tahun di tahun ini, maka kurangi 1
            if (kalenderSekarang.get(Calendar.DAY_OF_YEAR) < kalenderLahir.get(Calendar.DAY_OF_YEAR)) {
                usia--
            }

            // Set ke EditText Usia (pastikan minimal 0)
            val hasilUsia = if (usia < 0) 0 else usia
            binding.etUsia.setText(hasilUsia.toString())
        }

        picker.show(parentFragmentManager, "DATE_PICKER_TAG")
    }
}