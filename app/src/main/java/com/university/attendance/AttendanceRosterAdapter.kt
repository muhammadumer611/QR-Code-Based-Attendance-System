package com.university.attendance

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.university.attendance.databinding.ItemAttendanceRosterBinding

/**
 * Adapter for the Manual Update class register: one row per student, with
 * a toggle Present/Absent pill button. Tapping either updates that
 * student's state via onToggle -- the ViewModel holds the actual state.
 */
class AttendanceRosterAdapter(
    private var rows: List<AttendanceRosterRow>,
    private val onToggle: (studentId: String) -> Unit
) : RecyclerView.Adapter<AttendanceRosterAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemAttendanceRosterBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(row: AttendanceRosterRow) {
            binding.tvStudentName.text = row.student.fullName
            binding.tvStudentRegNo.text = row.student.regNo

            updateToggleAppearance(row.isPresent)

            binding.root.setOnClickListener { onToggle(row.student.studentId) }
            binding.btnTogglePresent.setOnClickListener { onToggle(row.student.studentId) }
        }

        private fun updateToggleAppearance(isPresent: Boolean) {
            if (isPresent) {
                binding.btnTogglePresent.text = "Present"
                binding.btnTogglePresent.setBackgroundResource(R.drawable.bg_status_present)
                binding.statusIndicator.setBackgroundResource(R.drawable.bg_status_dot_present)
            } else {
                binding.btnTogglePresent.text = "Absent"
                binding.btnTogglePresent.setBackgroundResource(R.drawable.bg_status_absent)
                binding.statusIndicator.setBackgroundResource(R.drawable.bg_status_dot_absent)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAttendanceRosterBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(rows[position])
    }

    override fun getItemCount(): Int = rows.size

    /** Call when the ViewModel's roster list changes (toggle, mark-all, initial load). */
    fun updateRows(newRows: List<AttendanceRosterRow>) {
        rows = newRows
        notifyDataSetChanged()
    }
}