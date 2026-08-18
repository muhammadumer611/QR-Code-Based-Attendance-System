package com.university.attendance

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.university.attendance.databinding.ItemSearchResultBinding

/**
 * Adapter for the live search dropdown. Tapping a row expands/collapses
 * an in-place details section (contact, CNIC, department, reg no / main
 * subject) right below that row -- no navigation to another screen.
 */
class SearchResultAdapter(
    private val results: List<SearchResult>
) : RecyclerView.Adapter<SearchResultAdapter.ViewHolder>() {

    private val expandedPositions = mutableSetOf<Int>()

    inner class ViewHolder(private val binding: ItemSearchResultBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(result: SearchResult, position: Int) {
            binding.tvName.text = result.name
            binding.tvSubtitle.text = result.subtitle
            binding.ivIcon.setImageResource(
                if (result.resultType == SearchResultType.STUDENT) R.drawable.ic_students else R.drawable.ic_teacher
            )
            binding.tvTypeTag.text = if (result.resultType == SearchResultType.STUDENT) "Student" else "Teacher"

            val isExpanded = expandedPositions.contains(position)
            binding.detailsContainer.visibility = if (isExpanded) View.VISIBLE else View.GONE
            binding.ivExpandArrow.rotation = if (isExpanded) 180f else 0f

            if (isExpanded) {
                binding.tvDetailExtraLabel.text =
                    if (result.resultType == SearchResultType.STUDENT) "Reg No" else "Main Subject"
                binding.tvDetailExtraValue.text = result.extraLine.ifBlank { "—" }
                binding.tvDetailDepartment.text = result.department.ifBlank { "—" }
                binding.tvDetailContact.text = result.contactNumber.ifBlank { "—" }
                binding.tvDetailCnic.text = result.cnicNumber.ifBlank { "—" }
            }

            binding.root.setOnClickListener {
                if (isExpanded) expandedPositions.remove(position) else expandedPositions.add(position)
                notifyItemChanged(position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSearchResultBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(results[position], position)
    }

    override fun getItemCount(): Int = results.size
}