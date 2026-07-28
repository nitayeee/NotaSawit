package com.example.notasawit.Autentikasi.Daftar.DataDiri

import android.content.Context
import android.content.Context.MODE_PRIVATE
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

class DataDiri1Fragment : Fragment() {
    // GUNAKAN 'by lazy' agar requireActivity() tidak langsung dipanggil saat Fragment dibuat
    private val sharedPref by lazy {
        requireActivity().getSharedPreferences("NOTASAWIT_PREF", Context.MODE_PRIVATE)
    }

    // Pindahkan pengambilan data string ini ke dalam fungsi atau gunakan lazy juga

    private val sp_namaPetani by lazy { sharedPref.getString("namaPetani", "") }
    private val username by lazy { sharedPref.getString("username", "") }
    private val sp_tanggalLahir by lazy { sharedPref.getString("tanggalLahir", "") }
    private val sp_jenisKelamin by lazy { sharedPref.getString("jenisKelamin", "") }
    private val sp_usia by lazy { sharedPref.getString("usia", "") }


    private var _binding: FragmentDataDiri1Binding? = null
    private val binding get() = _binding!!

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

        binding.etUsia.isEnabled = false
        binding.etUsia.setTextColor(resources.getColor(android.R.color.black, null))

        val jenisKelaminAdapter = android.widget.ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            arrayOf("Laki-laki", "Perempuan")
        )
        binding.etJenisKelamin.setAdapter(jenisKelaminAdapter)

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
        binding.etJenisKelamin.addTextChangedListener(textWatcher)
        binding.etUsia.addTextChangedListener(textWatcher)

        binding.btnNext.setOnClickListener {
            val namaPetani = binding.etNamaDepan.text.toString().trim()
            val username = binding.etUsername.text.toString().trim()
            val tanggalLahir = binding.etTanggalLahir.text.toString().trim()
            val jenisKelamin = binding.etJenisKelamin.text.toString().trim()
            val usia = binding.etUsia.text.toString().trim()

            if (namaPetani.isNotEmpty() && tanggalLahir.isNotEmpty()
                && jenisKelamin.isNotEmpty() && usia.isNotEmpty()&& username.isNotEmpty()) {
                // Simpan ke SharedPreferences
                sharedPref.edit().apply {
                    putString("namaPetani", namaPetani)
                    putString("tanggalLahir", tanggalLahir)
                    putString("jenisKelamin", jenisKelamin)
                    putString("username", username)
                    putString("usia", usia)
                    apply()
                }

                val bapakActivity = activity as? DataDiriActivity
                bapakActivity?.nextStep()
            }
        }
    }

    private fun periksaKelengkapanData() {
        val namaDepan = binding.etNamaDepan.text.toString().trim()
        val tanggalLahir = binding.etTanggalLahir.text.toString().trim()
        val jenisKelamin = binding.etJenisKelamin.text.toString().trim()
        val usia = binding.etUsia.text.toString().trim()

        val semuaSudahIsi = namaDepan.isNotEmpty() &&
                tanggalLahir.isNotEmpty() &&
                jenisKelamin.isNotEmpty() &&
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