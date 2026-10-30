package com.example.notasawit.Admin.AuditInternal.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.notasawit.Admin.AuditInternal.model.BuktiTemuanItem
import com.example.notasawit.R
import com.google.android.material.textfield.TextInputEditText
import java.io.File

class BuktiTemuanAdapter(
    private val items: MutableList<BuktiTemuanItem>,
    private val onDeleteClicked: (Int) -> Unit
) : RecyclerView.Adapter<BuktiTemuanAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTemuanNomor: TextView = itemView.findViewById(R.id.tvTemuanNomor)
        val btnHapusFoto: ImageView = itemView.findViewById(R.id.btnHapusFoto)
        val ivFotoTemuan: ImageView = itemView.findViewById(R.id.ivFotoTemuan)
        val etKeteranganFoto: TextInputEditText = itemView.findViewById(R.id.etKeteranganFoto)
        var textWatcher: TextWatcher? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bukti_temuan_audit, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.tvTemuanNomor.text = "Foto Temuan #${position + 1}"

        // Load foto
        if (item.fotoPath.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(File(item.fotoPath))
                .placeholder(R.drawable.logo)
                .error(android.R.drawable.ic_menu_gallery)
                .into(holder.ivFotoTemuan)
        }

        // Hapus TextWatcher lama sebelum set text agar tidak looping / bug
        holder.textWatcher?.let { holder.etKeteranganFoto.removeTextChangedListener(it) }

        holder.etKeteranganFoto.setText(item.keterangan)

        // Pasang TextWatcher baru untuk update keterangan foto secara real-time
        holder.textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val currentPos = holder.bindingAdapterPosition
                if (currentPos != RecyclerView.NO_POSITION && currentPos < items.size) {
                    items[currentPos].keterangan = s?.toString() ?: ""
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        holder.etKeteranganFoto.addTextChangedListener(holder.textWatcher)

        holder.btnHapusFoto.setOnClickListener {
            val currentPos = holder.bindingAdapterPosition
            if (currentPos != RecyclerView.NO_POSITION && currentPos < items.size) {
                onDeleteClicked(currentPos)
            }
        }
    }

    override fun getItemCount(): Int = items.size
}
