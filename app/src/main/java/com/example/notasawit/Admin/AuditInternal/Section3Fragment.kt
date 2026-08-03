package com.example.notasawit.Admin.AuditInternal

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.notasawit.Admin.AuditInternal.AuditViewModel.AuditViewModel
import com.example.notasawit.Admin.AuditInternal.adapter.AuditQuestionAdapter
import com.example.notasawit.Admin.AuditInternal.data.Section2QuestionData
import com.example.notasawit.Admin.AuditInternal.data.Section3QuestionData
import com.example.notasawit.Admin.AuditInternal.model.AuditItem
import com.example.notasawit.databinding.FragmentSection3Binding
import kotlin.getValue


class Section3Fragment : Fragment() {
    private var _binding: FragmentSection3Binding? = null
    private val binding get() = _binding!!
    private val viewModel: AuditViewModel by activityViewModels()

    private lateinit var adapter: AuditQuestionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSection3Binding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val allQuestions = Section3QuestionData.getQuestions()

        val data = if (viewModel.auditHeader.auditAttempt > 1) {
            // Filter hanya pertanyaan yang dijawab "Tidak Sesuai" (false) pada audit sebelumnya
            allQuestions.filter { item ->
                if (item is AuditItem.Question) {
                    viewModel.auditAnswers[item.key] == false
                } else {
                    true // Biarkan header (AuditItem.Header) tetap muncul
                }
            }
        } else if (viewModel.section3Answers.isEmpty()) {
            allQuestions
        } else {
            viewModel.section3Answers
        }

        adapter = AuditQuestionAdapter(data.toMutableList())

        binding.rvQuestion.layoutManager = LinearLayoutManager(requireContext())
        binding.rvQuestion.adapter = adapter
        binding.btnNext.setOnClickListener {

            if (!adapter.isAllAnswered()) {

                Toast.makeText(
                    requireContext(),
                    "Semua pertanyaan wajib dijawab!",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }
            simpanSection3()

            viewModel.section3Answers =
                adapter.getItems().toMutableList()

            (requireActivity() as AuditInternalActivity)
                .navigateTo(Section4Fragment(), 4)

        }
        binding.btnBack.setOnClickListener {

            // Simpan dulu jawaban
            viewModel.section3Answers =
                adapter.getItems().toMutableList()

            // Kembali ke Section1
            (requireActivity() as AuditInternalActivity)
                .navigateBack(1)

        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    private fun simpanSection3() {
        adapter.getItems()
            .filterIsInstance<AuditItem.Question>()
            .forEach { item ->
                viewModel.auditAnswers[item.key] = item.answer
            }
    }
}