package com.university.attendance

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.university.attendance.databinding.ItemStudentTodayClassBinding

class StudentTodayClassAdapter :
    RecyclerView.Adapter<StudentTodayClassAdapter.VH>() {

    private val items =
        mutableListOf<ClassSchedule>()

    fun submitList(
        list: List<ClassSchedule>
    ) {

        items.clear()
        items.addAll(list)

        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): VH {

        return VH(
            ItemStudentTodayClassBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(
        holder: VH,
        position: Int
    ) {

        holder.bind(
            items[position]
        )
    }

    override fun getItemCount(): Int =
        items.size

    class VH(
        private val binding:
        ItemStudentTodayClassBinding
    ) :
        RecyclerView.ViewHolder(
            binding.root
        ) {

        fun bind(
            item: ClassSchedule
        ) {

            binding.tvSubject.text =
                if (
                    item.courseCode.isBlank()
                ) {

                    item.subjectName

                } else {

                    "${item.courseCode} • " +
                            item.subjectName
                }

            binding.tvTeacher.text =
                "Teacher: ${
                    item.teacherName
                        .ifBlank {
                            "Teacher"
                        }
                }"

            binding.tvClass.text =
                "${item.programName} • " +
                        "Semester ${item.semester} • " +
                        "Section ${item.section}"

            binding.tvTime.text =
                "${item.startTime} - ${item.endTime}" +
                        if (
                            item.roomNumber > 0
                        ) {
                            " • Room ${item.roomNumber}"
                        } else {
                            ""
                        }

            binding.tvPeriod.text =
                "${item.periodType} class"
        }
    }
}