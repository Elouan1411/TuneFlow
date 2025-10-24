package com.example.tuneflow

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SwipeAdapter(val items: MutableList<String>) :
    RecyclerView.Adapter<SwipeAdapter.SwipeViewHolder>() {

    inner class SwipeViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.textView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SwipeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_page_home, parent, false)
        return SwipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: SwipeViewHolder, position: Int) {
        holder.textView.text = items[position]
    }

    override fun getItemCount(): Int = items.size

    fun addPage(item: String) {
        items.add(item)
        notifyItemInserted(items.size - 1)
    }

    fun removeFirstPage() {
        if (items.isNotEmpty()) {
            items.removeAt(0)
            notifyItemRemoved(0)
        }
    }
}
