package com.example.notasawit.Admin.KunjunganLahan

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.notasawit.Admin.KunjunganLahan.adapter.KunjunganQuestionAdapter
import com.example.notasawit.Admin.KunjunganLahan.data.KunjunganQuestionData
import com.example.notasawit.Admin.KunjunganLahan.model.KunjunganItem
import com.example.notasawit.Room.AppDatabase
import com.example.notasawit.databinding.FragmentKlSection2Binding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class KLSection2Fragment : Fragment() {

    private var _binding: FragmentKlSection2Binding? = null
    private val binding get() = _binding!!
    private val viewModel: KunjunganLahanViewModel by activityViewModels()

    private lateinit var adapter: KunjunganQuestionAdapter
    private lateinit var database: AppDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKlSection2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity() as KunjunganLahanActivity
        database = activity.database

        val data = if (viewModel.section2Answers.isEmpty()) {
            KunjunganQuestionData.getQuestions()
        } else {
            viewModel.section2Answers
        }

        adapter = KunjunganQuestionAdapter(data.toMutableList())

        binding.rvQuestion.layoutManager = LinearLayoutManager(requireContext())
        binding.rvQuestion.adapter = adapter

        // SIMPAN
        binding.btnSimpan.setOnClickListener {
            if (!adapter.isAllAnswered()) {
                Toast.makeText(requireContext(), "Semua pertanyaan wajib dijawab!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Simpan jawaban ke view model
            viewModel.section2Answers = adapter.getItems().toMutableList()
            simpanSection2()

            // Simpan ke database Room
            lifecycleScope.launch(Dispatchers.IO) {
                // Generate PDF
                val generatedPdfPath = PdfGeneratorKunjungan.generatePdf(requireContext(), viewModel.kunjunganLahanForm)
                if (generatedPdfPath != null) {
                    viewModel.kunjunganLahanForm = viewModel.kunjunganLahanForm.copy(pdfPath = generatedPdfPath)
                }

                database.KunjunganLahanDao().insertKunjunganLahan(viewModel.kunjunganLahanForm)
                withContext(Dispatchers.Main) {
                    triggerDataSync()
                    com.example.notasawit.utils.CustomAlert.showSuccess(
                        requireActivity(),
                        "Berhasil",
                        "Data & PDF disimpan & siap disinkron!"
                    )
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        requireActivity().finish() // Tutup activity setelah berhasil simpan
                    }, 1500)
                }
            }
        }

        // BACK
        binding.btnBack.setOnClickListener {
            viewModel.section2Answers = adapter.getItems().toMutableList()
            (requireActivity() as KunjunganLahanActivity).navigateBack(1)
        }
    }

    private fun simpanSection2() {
        var form = viewModel.kunjunganLahanForm
        
        adapter.getItems()
            .filterIsInstance<KunjunganItem.Question>()
            .forEach { item ->
                form = when (item.key) {
                    "q1_patokBatas" -> form.copy(q1_patokBatas = item.answer)
                    "q2_idKebun" -> form.copy(q2_idKebun = item.answer)
                    "q3_piringanPasarPikul" -> form.copy(q3_piringanPasarPikul = item.answer)
                    "q4_pelepahDitunas" -> form.copy(q4_pelepahDitunas = item.answer)
                    "q5_susunanPelepah" -> form.copy(q5_susunanPelepah = item.answer)
                    "q6_turnera" -> form.copy(q6_turnera = item.answer)
                    "q7_bekasPembakaran" -> form.copy(q7_bekasPembakaran = item.answer)
                    "q8_botolRacunPlastik" -> form.copy(q8_botolRacunPlastik = item.answer)
                    "q9_sampahPlastik" -> form.copy(q9_sampahPlastik = item.answer)
                    "q10_plangSungai" -> form.copy(q10_plangSungai = item.answer)
                    "q11_semprotSungai" -> form.copy(q11_semprotSungai = item.answer)
                    "q12_sampahSungai" -> form.copy(q12_sampahSungai = item.answer)
                    "q13_semprotTotal" -> form.copy(q13_semprotTotal = item.answer)
                    "q14_racunKontak" -> form.copy(q14_racunKontak = item.answer)
                    "q15_hamaPenyakit" -> form.copy(q15_hamaPenyakit = item.answer)
                    else -> form
                }
            }
        
        viewModel.kunjunganLahanForm = form
    }

    private fun triggerDataSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<com.example.notasawit.Sync.SyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(requireContext()).enqueueUniqueWork(
            "SyncKunjunganWork",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
