package com.example.notasawit

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TutorialAdapter(private val tutorialList: List<TutorialItem>) :
    RecyclerView.Adapter<TutorialAdapter.TutorialViewHolder>() {

    inner class TutorialViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivTutorial: ImageView = view.findViewById(R.id.ivTutorial)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvDescription: TextView = view.findViewById(R.id.tvDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TutorialViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tutorial, parent, false)
        return TutorialViewHolder(view)
    }

    override fun onBindViewHolder(holder: TutorialViewHolder, position: Int) {
        val currentItem = tutorialList[position]
        holder.ivTutorial.setImageResource(currentItem.imageRes)
        holder.tvTitle.text = currentItem.title
        holder.tvDescription.text = currentItem.description
    }

    override fun getItemCount(): Int {
        return tutorialList.size
    }
}
