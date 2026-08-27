package com.university.attendance

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.university.attendance.databinding.ItemTeacherClassBinding

class TeacherClassesAdapter(
    private val onClassClick: (Subject) -> Unit
) : RecyclerView.Adapter<TeacherClassesAdapter.ClassViewHolder>() {

    private val items =
        mutableListOf<Subject>()

    private var studentCounts =
        emptyMap<String, Int>()

    fun submitList(
        list: List<Subject>,
        counts: Map<String, Int> = emptyMap()
    ) {

        items.clear()
        items.addAll(list)

        studentCounts =
            counts

        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ClassViewHolder {

        val binding =
            ItemTeacherClassBinding.inflate(
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

        holder.bind(
            items[position]
        )
    }

    override fun getItemCount(): Int =
        items.size

    inner class ClassViewHolder(private val binding: ItemTeacherClassBinding) : RecyclerView.ViewHolder(binding.root
    ) {

        fun bind(subject: Subject) {

            binding.txtCourseCode.text =subject.courseCode

            binding.txtSubjectName.text =subject.subjectName

            binding.txtProgram.text =subject.programName

            binding.txtSemester.text =subject.semester

            binding.txtDepartment.text =subject.departmentName

            val count =studentCounts[subject.subjectId] ?: 0

            binding.txtStudentCount.text ="$count Students"

            binding.root.setOnClickListener {

                onClassClick(
                    subject
                )
            }
        }
    }
}
