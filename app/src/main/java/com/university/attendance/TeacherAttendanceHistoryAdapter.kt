package com.university.attendance

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.university.attendance.databinding.ItemTeacherAttendanceHistoryBinding
import java.text.SimpleDateFormat
import java.util.Locale

class TeacherAttendanceHistoryAdapter(
    private var items:
    List<TeacherAttendanceHistoryItem> = emptyList()
) : RecyclerView.Adapter<
        TeacherAttendanceHistoryAdapter.ViewHolder>() {

    inner class ViewHolder(
        private val binding:
        ItemTeacherAttendanceHistoryBinding
    ) : RecyclerView.ViewHolder(
        binding.root
    ) {

        fun bind(
            item: TeacherAttendanceHistoryItem
        ) {

            binding.tvStudentName.text =
                item.studentName.ifBlank {
                    "Unknown Student"
                }

            binding.tvStudentRegNo.text =
                item.regNo.ifBlank {
                    "Registration number unavailable"
                }

            binding.tvAttendanceStatus.text =
                "PRESENT"

            binding.tvMarkedTime.text =
                formatTime(
                    item.markedAt
                )
        }

        private fun formatTime(
            date: java.util.Date?
        ): String {

            if (date == null) {
                return "Marked just now"
            }

            return try {

                SimpleDateFormat(
                    "hh:mm a",
                    Locale.US
                ).format(date)

            } catch (
                _: Exception
            ) {

                "Marked"
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val binding =
            ItemTeacherAttendanceHistoryBinding
                .inflate(
                    LayoutInflater.from(
                        parent.context
                    ),
                    parent,
                    false
                )

        return ViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        holder.bind(
            items[position]
        )
    }

    override fun getItemCount(): Int =
        items.size

    fun updateData(
        newItems:
        List<TeacherAttendanceHistoryItem>
    ) {

        items =
            newItems

        notifyDataSetChanged()
    }
}