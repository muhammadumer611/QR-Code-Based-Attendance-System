package com.university.attendance

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.university.attendance.databinding.ItemSubjectCheckBoxBinding

/**
 * Screen 2 adapter: every subject in the system as a checkbox row.
 * Subjects already assigned to a DIFFERENT teacher show that teacher's
 * name as a hint (so Admin knows reassigning will take it from them).
 */
class SubjectChecklistAdapter(
    private var subjects: List<Subject>,
    private var selectedIds: Set<String>,
    private var currentTeacherId: String,
    private val onToggle: (subjectId: String) -> Unit
) : RecyclerView.Adapter<SubjectChecklistAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemSubjectCheckBoxBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(subject: Subject) {
            binding.tvSubjectName.text = subject.subjectName
            binding.tvSubjectMeta.text =
                "${subject.courseCode} • ${subject.departmentName} • ${subject.programName}"

            val isChecked = selectedIds.contains(subject.subjectId)
            binding.checkbox.setOnCheckedChangeListener(null) // avoid firing during rebind
            binding.checkbox.isChecked = isChecked

            // If assigned to someone else, show a small hint.
            if (subject.teacherId.isNotBlank() && subject.teacherId != currentTeacherId) {
                binding.tvCurrentlyAssigned.visibility = android.view.View.VISIBLE
                binding.tvCurrentlyAssigned.text = "Currently: ${subject.teacherName}"
            } else {
                binding.tvCurrentlyAssigned.visibility = android.view.View.GONE
            }

            binding.checkbox.setOnCheckedChangeListener { _, _ -> onToggle(subject.subjectId) }
            binding.root.setOnClickListener { binding.checkbox.toggle() }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSubjectCheckBoxBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(subjects[position])
    }

    override fun getItemCount(): Int = subjects.size

    /** Call when the ViewModel's subject list or selection changes. */
    fun updateData(newSubjects: List<Subject>, newSelectedIds: Set<String>, newCurrentTeacherId: String? = null) {
        subjects = newSubjects
        selectedIds = newSelectedIds
        if (newCurrentTeacherId != null) currentTeacherId = newCurrentTeacherId
        notifyDataSetChanged()
    }
}