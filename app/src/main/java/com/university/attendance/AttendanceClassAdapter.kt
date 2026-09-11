package com.university.attendance

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.university.attendance.databinding.ItemAttendanceClassBinding

/**
 * Screen 2 adapter: shows each class (Program + Session + Section) as a
 * tappable row. Tapping drills down into that class's student list.
 */
class AttendanceClassAdapter(
    private val classes: List<StudentClass>,
    private val onClick: (StudentClass) -> Unit
) : RecyclerView.Adapter<AttendanceClassAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemAttendanceClassBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(studentClass: StudentClass) {
            binding.tvClassTitle.text = "${studentClass.programName} - Section ${studentClass.section}"
            binding.tvClassSubtitle.text = "Session ${studentClass.session} • ${studentClass.studentCount} Students"
            binding.root.setOnClickListener { onClick(studentClass) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAttendanceClassBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(classes[position])
    }

    override fun getItemCount(): Int = classes.size
}