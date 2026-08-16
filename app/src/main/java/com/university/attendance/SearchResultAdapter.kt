package com.university.attendance

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.university.attendance.databinding.ItemSearchResultBinding

class SearchResultAdapter(
    private val results: List<SearchResult>,
    private val onClick: (SearchResult) -> Unit
) : RecyclerView.Adapter<SearchResultAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemSearchResultBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(result: SearchResult) {
            binding.tvName.text = result.name
            binding.tvSubtitle.text = result.subtitle
            binding.ivIcon.setImageResource(
                if (result.resultType == SearchResultType.STUDENT) R.drawable.ic_students else R.drawable.ic_teacher
            )
            binding.tvTypeTag.text = if (result.resultType == SearchResultType.STUDENT) "Student" else "Teacher"
            binding.root.setOnClickListener { onClick(result) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSearchResultBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(results[position])
    }

    override fun getItemCount(): Int = results.size
}