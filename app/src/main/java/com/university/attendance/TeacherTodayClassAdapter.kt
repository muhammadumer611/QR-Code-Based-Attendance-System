package com.university.attendance

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.university.attendance.databinding.ItemTeacherTodayClassBinding

class TeacherTodayClassAdapter(
    private val onClassClick: (ClassSchedule) -> Unit
) : RecyclerView.Adapter<TeacherTodayClassAdapter.ClassViewHolder>() {

    private val classList = mutableListOf<ClassSchedule>()

    fun submitList(newList: List<ClassSchedule>) {
        classList.clear()
        classList.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ClassViewHolder {

        val binding = ItemTeacherTodayClassBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return ClassViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ClassViewHolder,
        position: Int
    ) {
        holder.bind(classList[position])
    }

    override fun getItemCount(): Int = classList.size

    inner class ClassViewHolder(
        private val binding: ItemTeacherTodayClassBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(classSchedule: ClassSchedule) {

            binding.tvSubjectName.text =
                classSchedule.subjectName.ifBlank {
                    "Subject"
                }

            binding.tvClassName.text =
                classSchedule.className.ifBlank {
                    "Class"
                }

            binding.tvTime.text =
                if (
                    classSchedule.startTime.isNotBlank() &&
                    classSchedule.endTime.isNotBlank()
                ) {
                    "${classSchedule.startTime} - ${classSchedule.endTime}"
                } else {
                    "Time not available"
                }

            binding.tvRoom.text =
                classSchedule.roomNumber.ifBlank {
                    "Room not assigned"
                }

            binding.root.setOnClickListener {
                onClassClick(classSchedule)
            }
        }
    }
}