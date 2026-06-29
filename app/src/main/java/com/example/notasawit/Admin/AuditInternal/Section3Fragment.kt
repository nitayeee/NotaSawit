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

        val data = if (viewModel.section3Answers.isEmpty()) {
            Section3QuestionData.getQuestions()
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

        var form = viewModel.auditForm

        adapter.getItems()
            .filterIsInstance<AuditItem.Question>()
            .forEach { item ->

                form = when (item.key) {

                    "dokumenQ1" -> form.copy(dokumenQ1 = item.answer)
                    "dokumenQ2" -> form.copy(dokumenQ2 = item.answer)
                    "dokumenQ3" -> form.copy(dokumenQ3 = item.answer)
                    "dokumenQ4" -> form.copy(dokumenQ4 = item.answer)
                    "dokumenQ5" -> form.copy(dokumenQ5 = item.answer)
                    "dokumenQ6" -> form.copy(dokumenQ6 = item.answer)
                    "dokumenQ7" -> form.copy(dokumenQ7 = item.answer)
                    "dokumenQ8" -> form.copy(dokumenQ8 = item.answer)
                    "dokumenQ9" -> form.copy(dokumenQ9 = item.answer)
                    "dokumenQ10" -> form.copy(dokumenQ10 = item.answer)
                    "dokumenQ11" -> form.copy(dokumenQ11 = item.answer)
                    "dokumenQ12" -> form.copy(dokumenQ12 = item.answer)
                    "dokumenQ13" -> form.copy(dokumenQ13 = item.answer)

                    "kebunQ1" -> form.copy(kebunQ1 = item.answer)
                    "kebunQ2" -> form.copy(kebunQ2 = item.answer)
                    "kebunQ3" -> form.copy(kebunQ3 = item.answer)
                    "kebunQ4" -> form.copy(kebunQ4 = item.answer)
                    "kebunQ5" -> form.copy(kebunQ5 = item.answer)
                    "kebunQ6" -> form.copy(kebunQ6 = item.answer)
                    "kebunQ7" -> form.copy(kebunQ7 = item.answer)
                    "kebunQ8" -> form.copy(kebunQ8 = item.answer)
                    "kebunQ9" -> form.copy(kebunQ9 = item.answer)
                    "kebunQ10" -> form.copy(kebunQ10 = item.answer)
                    "kebunQ11" -> form.copy(kebunQ11 = item.answer)

                    else -> form
                }
            }

        viewModel.auditForm = form
    }
}