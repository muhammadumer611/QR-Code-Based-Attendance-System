package com.university.attendance

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.university.attendance.databinding.ItemTeacherTodayClassBinding

class TeacherTodayClassAdapter(
    private val onClassClick:
        (ClassSchedule) -> Unit
) : RecyclerView.Adapter<
        TeacherTodayClassAdapter.ClassViewHolder>() {

    private val classList =
        mutableListOf<ClassSchedule>()

    fun submitList(
        newList: List<ClassSchedule>
    ) {

        classList.clear()

        classList.addAll(
            newList
        )

        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ClassViewHolder {

        val binding =
            ItemTeacherTodayClassBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ),
                parent,
                false
            )

        return ClassViewHolder(
            binding
        )
    }

    override fun onBindViewHolder(
        holder: ClassViewHolder,
        position: Int
    ) {

        holder.bind(
            classList[position]
        )
    }

    override fun getItemCount(): Int =
        classList.size

    inner class ClassViewHolder(
        private val binding:
        ItemTeacherTodayClassBinding
    ) :
        RecyclerView.ViewHolder(
            binding.root
        ) {

        fun bind(
            item: ClassSchedule
        ) {

            binding.tvSubjectName.text =
                if (
                    item.courseCode.isNotBlank()
                ) {

                    "${item.courseCode} • ${item.subjectName}"

                } else {

                    item.subjectName.ifBlank {
                        "Subject"
                    }
                }

            binding.tvClassName.text =
                item.className.ifBlank {
                    "Class"
                }

            binding.tvTime.text =
                if (
                    item.startTime.isNotBlank() &&
                    item.endTime.isNotBlank()
                ) {

                    "${item.startTime} - ${item.endTime}"

                } else {

                    "Time not available"
                }

            // BSSE-A2025-001

            binding.root.setOnClickListener {

                onClassClick(
                    item
                )
            }
        }
    }
}