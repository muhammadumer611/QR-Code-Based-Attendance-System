package com.university.attendance

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.university.attendance.databinding.ItemAttendanceStudentBinding

/**
 * Screen 3 adapter: shows each student in the selected class as a tappable
 * row. Tapping opens that student's subject-wise attendance detail.
 */
class AttendanceStudentAdapter(
    private val students: List<Student>,
    private val onClick: (Student) -> Unit
) : RecyclerView.Adapter<AttendanceStudentAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemAttendanceStudentBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(student: Student) {
            binding.tvStudentName.text = student.fullName
            binding.tvStudentRegNo.text = student.regNo
            binding.root.setOnClickListener { onClick(student) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAttendanceStudentBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(students[position])
    }

    override fun getItemCount(): Int = students.size
}