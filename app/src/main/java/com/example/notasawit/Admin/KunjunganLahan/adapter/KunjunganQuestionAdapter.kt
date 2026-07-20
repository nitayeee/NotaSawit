package com.example.notasawit.Admin.KunjunganLahan.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.notasawit.Admin.KunjunganLahan.model.KunjunganItem
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

        fun bind(item: KunjunganItem.Question) {
            binding.tvQuestion.text = item.question
            binding.tvStandard.text = "Standar: ${item.standard}"

            // Reset listener dulu
            binding.rgAnswer.setOnCheckedChangeListener(null)

            when (item.answer) {
                true -> binding.rbYa.isChecked = true
                false -> binding.rbTidak.isChecked = true
                null -> binding.rgAnswer.clearCheck()
            }

            binding.rgAnswer.setOnCheckedChangeListener { _, checkedId ->
                item.answer = when (checkedId) {
                    binding.rbYa.id -> true
                    binding.rbTidak.id -> false
                    else -> null
                }
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
            .all { it.answer != null }
    }
}
