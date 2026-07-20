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
import com.example.notasawit.Admin.AuditInternal.model.AuditItem
import com.example.notasawit.databinding.FragmentSection2Binding
import kotlin.getValue


class Section2Fragment : Fragment() {

    private var _binding: FragmentSection2Binding? = null
    private val binding get() = _binding!!
    private val viewModel: AuditViewModel by activityViewModels()

    private lateinit var adapter: AuditQuestionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSection2Binding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val data = if (viewModel.section2Answers.isEmpty()) {
            Section2QuestionData.getQuestions()
        } else {
            viewModel.section2Answers
        }

        adapter = AuditQuestionAdapter(data.toMutableList())

        binding.rvQuestion.layoutManager = LinearLayoutManager(requireContext())
        binding.rvQuestion.adapter = adapter

        // NEXT
        binding.btnNext.setOnClickListener {

            if (!adapter.isAllAnswered()) {

                Toast.makeText(
                    requireContext(),
                    "Semua pertanyaan wajib dijawab!",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            // Simpan jawaban
            viewModel.section2Answers =
                adapter.getItems().toMutableList()
            simpanSection2()

            (requireActivity() as AuditInternalActivity)
                .navigateTo(Section3Fragment(), 3)
        }

        // BACK
        binding.btnBack.setOnClickListener {

            // Simpan dulu jawaban
            viewModel.section2Answers =
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
    private fun simpanSection2() {

        var form = viewModel.auditForm

        adapter.getItems()
            .filterIsInstance<AuditItem.Question>()
            .forEach { item ->

                form = when (item.key) {

                    "asosiasiQ1" -> form.copy(asosiasiQ1 = item.answer)
                    "asosiasiQ2" -> form.copy(asosiasiQ2 = item.answer)

                    "sopQ1" -> form.copy(sopQ1 = item.answer)
                    "sopQ2" -> form.copy(sopQ2 = item.answer)
                    "sopQ3" -> form.copy(sopQ3 = item.answer)
                    "sopQ4" -> form.copy(sopQ4 = item.answer)
                    "sopQ5" -> form.copy(sopQ5 = item.answer)
                    "sopQ6" -> form.copy(sopQ6 = item.answer)
                    "sopQ7" -> form.copy(sopQ7 = item.answer)
                    "sopQ8" -> form.copy(sopQ8 = item.answer)
                    "sopQ9" -> form.copy(sopQ9 = item.answer)
                    "sopQ10" -> form.copy(sopQ10 = item.answer)
                    "sopQ11" -> form.copy(sopQ11 = item.answer)
                    "sopQ12" -> form.copy(sopQ12 = item.answer)
                    "sopQ13" -> form.copy(sopQ13 = item.answer)
                    "sopQ14" -> form.copy(sopQ14 = item.answer)
                    "sopQ15" -> form.copy(sopQ15 = item.answer)
                    "sopQ16" -> form.copy(sopQ16 = item.answer)
                    "sopQ17" -> form.copy(sopQ17 = item.answer)
                    "sopQ18" -> form.copy(sopQ18 = item.answer)

                    "pelatihanQ1" -> form.copy(pelatihanQ1 = item.answer)
                    "pelatihanQ2" -> form.copy(pelatihanQ2 = item.answer)
                    "pelatihanQ3" -> form.copy(pelatihanQ3 = item.answer)
                    "pelatihanQ4" -> form.copy(pelatihanQ4 = item.answer)
                    "pelatihanQ5" -> form.copy(pelatihanQ5 = item.answer)
                    "pelatihanQ6" -> form.copy(pelatihanQ6 = item.answer)
                    "pelatihanQ7" -> form.copy(pelatihanQ7 = item.answer)
                    "pelatihanQ8" -> form.copy(pelatihanQ8 = item.answer)
                    "pelatihanQ9" -> form.copy(pelatihanQ9 = item.answer)
                    "pelatihanQ10" -> form.copy(pelatihanQ10 = item.answer)
                    "pelatihanQ11" -> form.copy(pelatihanQ11 = item.answer)
                    "pelatihanQ12" -> form.copy(pelatihanQ12 = item.answer)
                    "pelatihanQ13" -> form.copy(pelatihanQ13 = item.answer)
                    "pelatihanQ14" -> form.copy(pelatihanQ14 = item.answer)

                    "lb3Q1" -> form.copy(lb3Q1 = item.answer)
                    "lb3Q2" -> form.copy(lb3Q2 = item.answer)
                    "lb3Q3" -> form.copy(lb3Q3 = item.answer)
                    "lb3Q4" -> form.copy(lb3Q4 = item.answer)
                    "lb3Q5" -> form.copy(lb3Q5 = item.answer)
                    "lb3Q6" -> form.copy(lb3Q6 = item.answer)
                    "lb3Q7" -> form.copy(lb3Q7 = item.answer)

                    "nktQ1" -> form.copy(nktQ1 = item.answer)
                    "nktQ2" -> form.copy(nktQ2 = item.answer)
                    "nktQ3" -> form.copy(nktQ3 = item.answer)
                    "nktQ4" -> form.copy(nktQ4 = item.answer)
                    "nktQ5" -> form.copy(nktQ5 = item.answer)
                    "nktQ6" -> form.copy(nktQ6 = item.answer)
                    "nktQ7" -> form.copy(nktQ7 = item.answer)

                    "sosialTenagaKerjaQ1" -> form.copy(sosialTenagaKerjaQ1 = item.answer)
                    "sosialTenagaKerjaQ2" -> form.copy(sosialTenagaKerjaQ2 = item.answer)
                    "sosialTenagaKerjaQ3" -> form.copy(sosialTenagaKerjaQ3 = item.answer)
                    "sosialTenagaKerjaQ4" -> form.copy(sosialTenagaKerjaQ4 = item.answer)
                    "sosialTenagaKerjaQ5" -> form.copy(sosialTenagaKerjaQ5 = item.answer)
                    "sosialTenagaKerjaQ6" -> form.copy(sosialTenagaKerjaQ6 = item.answer)

                    "k3Q1" -> form.copy(k3Q1 = item.answer)
                    "k3Q2" -> form.copy(k3Q2 = item.answer)
                    "k3Q3" -> form.copy(k3Q3 = item.answer)
                    "k3Q4" -> form.copy(k3Q4 = item.answer)
                    "k3Q5" -> form.copy(k3Q5 = item.answer)
                    "k3Q6" -> form.copy(k3Q6 = item.answer)
                    "k3Q7" -> form.copy(k3Q7 = item.answer)
                    "k3Q8" -> form.copy(k3Q8 = item.answer)
                    "k3Q9" -> form.copy(k3Q9 = item.answer)



                    else -> form
                }
            }

        viewModel.auditForm = form
    }

}
