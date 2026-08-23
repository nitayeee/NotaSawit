package com.example.notasawit.Admin.KunjunganLahan.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.notasawit.Admin.KunjunganLahan.model.KunjunganItem
import com.example.notasawit.Admin.KunjunganLahan.model.KunjunganQuestionType
import com.example.notasawit.databinding.ItemHeaderBinding
import com.example.notasawit.databinding.ItemKunjunganQuestionBinding

class KunjunganQuestionAdapter(
    private val items: MutableList<KunjunganItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_QUESTION = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is KunjunganItem.Header -> TYPE_HEADER
            is KunjunganItem.Question -> TYPE_QUESTION
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val binding = ItemHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                HeaderViewHolder(binding)
            }
            else -> {
                val binding = ItemKunjunganQuestionBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                QuestionViewHolder(binding)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> holder.bind(items[position] as KunjunganItem.Header)
            is QuestionViewHolder -> holder.bind(items[position] as KunjunganItem.Question)
        }
    }

    //================ HEADER =================
    inner class HeaderViewHolder(
        private val binding: ItemHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: KunjunganItem.Header) {
            binding.tvHeader.text = item.title
        }
    }

    //================ QUESTION =================
    inner class QuestionViewHolder(
        private val binding: ItemKunjunganQuestionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var textWatcherOther: TextWatcher? = null
        private var textWatcherEssay: TextWatcher? = null

        fun bind(item: KunjunganItem.Question) {
            binding.tvQuestion.text = item.question
            binding.tvStandard.text = "Standar: ${item.standard}"

            // Reset listener dulu
            binding.rgAnswer.setOnCheckedChangeListener(null)
            if (textWatcherOther != null) {
                binding.etOtherText.removeTextChangedListener(textWatcherOther)
            }
            if (textWatcherEssay != null) {
                binding.etEssayText.removeTextChangedListener(textWatcherEssay)
            }

            if (item.type == KunjunganQuestionType.RADIO_ADA_TIDAK) {
                binding.layoutRadioAnswer.visibility = View.VISIBLE
                binding.layoutEssayAnswer.visibility = View.GONE

                when (item.textAnswer) {
                    "Ada" -> {
                        binding.rbAda.isChecked = true
                        binding.etOtherText.visibility = View.GONE
                    }
                    "Tidak Ada" -> {
                        binding.rbTidakAda.isChecked = true
                        binding.etOtherText.visibility = View.GONE
                    }
                    null, "" -> {
                        if (item.answer == true) {
                            binding.rbAda.isChecked = true
                            item.textAnswer = "Ada"
                            binding.etOtherText.visibility = View.GONE
                        } else if (item.answer == false) {
                            binding.rbTidakAda.isChecked = true
                            item.textAnswer = "Tidak Ada"
                            binding.etOtherText.visibility = View.GONE
                        } else {
                            binding.rgAnswer.clearCheck()
                            binding.etOtherText.visibility = View.GONE
                        }
                    }
                    else -> {
                        binding.rbOther.isChecked = true
                        binding.etOtherText.visibility = View.VISIBLE
                        binding.etOtherText.setText(item.textAnswer)
                    }
                }

                binding.rgAnswer.setOnCheckedChangeListener { _, checkedId ->
                    when (checkedId) {
                        binding.rbAda.id -> {
                            binding.etOtherText.visibility = View.GONE
                            item.textAnswer = "Ada"
                            item.answer = true
                        }
                        binding.rbTidakAda.id -> {
                            binding.etOtherText.visibility = View.GONE
                            item.textAnswer = "Tidak Ada"
                            item.answer = false
                        }
                        binding.rbOther.id -> {
                            binding.etOtherText.visibility = View.VISIBLE
                            val txt = binding.etOtherText.text.toString().trim()
                            item.textAnswer = txt
                            item.answer = txt.isNotEmpty()
                        }
                    }
                }

                textWatcherOther = object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        if (binding.rbOther.isChecked) {
                            val txt = s?.toString()?.trim() ?: ""
                            item.textAnswer = txt
                            item.answer = txt.isNotEmpty()
                        }
                    }
                    override fun afterTextChanged(s: Editable?) {}
                }
                binding.etOtherText.addTextChangedListener(textWatcherOther)

            } else {
                // ESSAY TYPE
                binding.layoutRadioAnswer.visibility = View.GONE
                binding.layoutEssayAnswer.visibility = View.VISIBLE

                binding.etEssayText.setText(item.textAnswer ?: "")

                textWatcherEssay = object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        val txt = s?.toString()?.trim() ?: ""
                        item.textAnswer = txt
                        item.answer = txt.isNotEmpty()
                    }
                    override fun afterTextChanged(s: Editable?) {}
                }
                binding.etEssayText.addTextChangedListener(textWatcherEssay)
            }
        }
    }

    //================ PUBLIC FUNCTION =================
    fun getItems(): MutableList<KunjunganItem> {
        return items
    }

    fun isAllAnswered(): Boolean {
        return items
            .filterIsInstance<KunjunganItem.Question>()
            .all { q ->
                if (q.type == KunjunganQuestionType.RADIO_ADA_TIDAK) {
                    !q.textAnswer.isNullOrBlank() || q.answer != null
                } else {
                    !q.textAnswer.isNullOrBlank()
                }
            }
    }

    fun getFirstUnansweredPosition(): Int {
        return items.indexOfFirst { item ->
            if (item is KunjunganItem.Question) {
                if (item.type == KunjunganQuestionType.RADIO_ADA_TIDAK) {
                    item.textAnswer.isNullOrBlank() && item.answer == null
                } else {
                    item.textAnswer.isNullOrBlank()
                }
            } else false
        }
    }
}
