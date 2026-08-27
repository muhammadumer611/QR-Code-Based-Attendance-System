package com.university.attendance

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.university.attendance.databinding.ItemClassScheduleBinding

class ClassScheduleAdapter(
    private val classes: List<ClassSchedule>,
    private val onDelete: (ClassSchedule) -> Unit
) : RecyclerView.Adapter<ClassScheduleAdapter.ClassScheduleViewHolder>() {

    // ============================================================
    // VIEW HOLDER
    // ============================================================

    inner class ClassScheduleViewHolder(
        private val binding: ItemClassScheduleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(classSchedule: ClassSchedule) {

            // ----------------------------------------------------
            // SUBJECT
            // ----------------------------------------------------

            binding.tvSubject.text =
                classSchedule.subjectName.ifBlank {
                    "Subject"
                }

            // ----------------------------------------------------
            // CLASS
            // ----------------------------------------------------

            binding.tvClassName.text =
                classSchedule.className.ifBlank {
                    "Class"
                }

            // ----------------------------------------------------
            // TEACHER
            // ----------------------------------------------------

            binding.tvTeacherName.text =
                classSchedule.teacherName.ifBlank {
                    "Teacher not assigned"
                }

            // ----------------------------------------------------
            // DATE
            // ----------------------------------------------------

            binding.tvDate.text =
                classSchedule.date.ifBlank {
                    "Date not set"
                }

            // ----------------------------------------------------
            // DAY
            // ----------------------------------------------------

            binding.tvDay.text =
                classSchedule.dayName.ifBlank {
                    ""
                }

            // ----------------------------------------------------
            // TIME
            // ----------------------------------------------------

            binding.tvTime.text =
                if (
                    classSchedule.startTime.isNotBlank() &&
                    classSchedule.endTime.isNotBlank()
                ) {

                    "${classSchedule.startTime} - ${classSchedule.endTime}"

                } else {

                    "Time not set"
                }

            // ----------------------------------------------------
            // ROOM
            // ----------------------------------------------------

            if (classSchedule.roomNumber.isNotBlank()) {

                binding.tvRoom.visibility =
                    View.VISIBLE

                binding.tvRoom.text =
                    "Room ${classSchedule.roomNumber}"

            } else {

                binding.tvRoom.visibility =
                    View.GONE
            }

            // ----------------------------------------------------
            // SEMESTER
            // ----------------------------------------------------

            if (classSchedule.semester.isNotBlank()) {

                binding.tvSemester.visibility =
                    View.VISIBLE

                binding.tvSemester.text =
                    classSchedule.semester

            } else {

                binding.tvSemester.visibility =
                    View.GONE
            }

            // ----------------------------------------------------
            // NOTE
            // ----------------------------------------------------

            if (classSchedule.note.isNotBlank()) {

                binding.tvNote.visibility =
                    View.VISIBLE

                binding.tvNote.text =
                    classSchedule.note

            } else {

                binding.tvNote.visibility =
                    View.GONE
            }

            // ----------------------------------------------------
            // DELETE
            // ----------------------------------------------------

            binding.btnDelete.setOnClickListener {

                onDelete(
                    classSchedule
                )
            }
        }
    }

    // ============================================================
    // CREATE VIEW HOLDER
    // ============================================================

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ClassScheduleViewHolder {

        val binding =
            ItemClassScheduleBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

        return ClassScheduleViewHolder(
            binding
        )
    }

    // ============================================================
    // BIND
    // ============================================================

    override fun onBindViewHolder(
        holder: ClassScheduleViewHolder,
        position: Int
    ) {

        holder.bind(
            classes[position]
        )
    }

    // ============================================================
    // COUNT
    // ============================================================

    override fun getItemCount(): Int =
        classes.size
}