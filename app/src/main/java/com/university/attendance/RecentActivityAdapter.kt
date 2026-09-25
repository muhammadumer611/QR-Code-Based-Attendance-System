package com.university.attendance

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.university.attendance.databinding.ItemRecentActivityBinding
import java.util.concurrent.TimeUnit

/**
 * Adapter for the "Recent Activities" list on the Admin Dashboard.
 * Each row shows a type-specific icon, title, description, and a
 * relative timestamp ("2 mins ago", "Yesterday", etc.).
 */
class RecentActivityAdapter(
    private val logs: List<Log>
) : RecyclerView.Adapter<RecentActivityAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemRecentActivityBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(log: Log) {
            binding.tvTitle.text = log.title
            binding.tvDescription.text = log.description
            binding.tvTimeAgo.text = formatRelativeTime(log.timestamp?.time)
            binding.ivIcon.setImageResource(iconForType(log.type))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecentActivityBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(logs[position])
    }

    override fun getItemCount(): Int = logs.size

    companion object {
        /** Reuses existing drawables already in the project -- no new icons needed. */
        fun iconForType(type: String): Int = when (type) {
            Type.STUDENT_ADDED -> R.drawable.ic_students
            Type.TEACHER_ADDED -> R.drawable.ic_teacher
            Type.DEPARTMENT_ADDED -> R.drawable.ic_department
            Type.SUBJECT_ADDED -> R.drawable.ic_subject
            Type.ATTENDANCE_MARKED -> R.drawable.ic_attendance
            Type.TEACHER_ASSIGNED -> R.drawable.ic_teacher
            Type.LOW_ATTENDANCE -> R.drawable.ic_notification
            else -> R.drawable.ic_notification
        }

        /** Formats a timestamp as "Just now" / "5 mins ago" / "3 hours ago" / "2 days ago". */
        fun formatRelativeTime(timeMillis: Long?): String {
            if (timeMillis == null) return ""
            val diff = System.currentTimeMillis() - timeMillis
            return when {
                diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
                diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)} mins ago"
                diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)} hours ago"
                diff < TimeUnit.DAYS.toMillis(2) -> "Yesterday"
                else -> "${TimeUnit.MILLISECONDS.toDays(diff)} days ago"
            }
        }
    }
}