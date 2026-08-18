package com.example.notasawit.Admin.AuditInternal.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.notasawit.Admin.AuditInternal.model.AuditItem
import com.example.notasawit.databinding.ItemHeaderBinding
import com.example.notasawit.databinding.ItemQuestionBinding

class AuditQuestionAdapter(
    private val items: MutableList<AuditItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_QUESTION = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is AuditItem.Header -> TYPE_HEADER
            is AuditItem.Question -> TYPE_QUESTION
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {

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
                val binding = ItemQuestionBinding.inflate(
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

            is HeaderViewHolder ->
                holder.bind(items[position] as AuditItem.Header)

            is QuestionViewHolder ->
                holder.bind(items[position] as AuditItem.Question)

        }

    }

    //================ HEADER =================

    inner class HeaderViewHolder(
        private val binding: ItemHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AuditItem.Header) {
            binding.tvHeader.text = item.title
        }

    }

    //================ QUESTION =================

    inner class QuestionViewHolder(
        private val binding: ItemQuestionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AuditItem.Question) {

            binding.tvQuestion.text = item.question

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

    fun getItems(): MutableList<AuditItem> {
        return items
    }

    fun setItems(newItems: List<AuditItem>) {

        items.clear()
        items.addAll(newItems)

        notifyDataSetChanged()

    }

    fun isAllAnswered(): Boolean {

        return items
            .filterIsInstance<AuditItem.Question>()
            .all { it.answer != null }

    }

    fun getFirstUnansweredPosition(): Int {

        return items.indexOfFirst { it is AuditItem.Question && it.answer == null }

    }

}