package com.university.attendance

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.university.attendance.databinding.ItemSubjectAttendanceBinding

/**
 * Screen 4 adapter: one card per subject, showing:
 *   - Subject name, course code, teacher name
 *   - Present/Total count + percentage + progress bar
 *   - Expandable list of present dates (tap card to expand/collapse)
 *
 * If a subject has no attendance data yet (totalClassesHeld == 0, i.e. no
 * QR sessions have happened for it yet), shows "No attendance recorded
 * yet" instead of a misleading 0% bar.
 */
class SubjectAttendanceAdapter(
    private val summaries: List<SubjectAttendanceSummary>
) : RecyclerView.Adapter<SubjectAttendanceAdapter.ViewHolder>() {

    private val expandedPositions = mutableSetOf<Int>()

    inner class ViewHolder(private val binding: ItemSubjectAttendanceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(summary: SubjectAttendanceSummary, position: Int) {
            binding.tvSubjectName.text = summary.subjectName
            binding.tvCourseCode.text = summary.courseCode
            binding.tvTeacherName.text = "Taught by: ${summary.teacherName}"

            if (summary.hasAnyData) {
                binding.tvAttendanceCount.visibility = View.VISIBLE
                binding.progressAttendance.visibility = View.VISIBLE
                binding.tvPercentage.visibility = View.VISIBLE
                binding.tvNoData.visibility = View.GONE

                binding.tvAttendanceCount.text = "${summary.presentCount} / ${summary.totalClassesHeld} classes"
                binding.tvPercentage.text = "${summary.percentage}%"
                binding.progressAttendance.progress = summary.percentage

                // Color the percentage red if below 75% (common university
                // attendance shortage threshold), cyan otherwise.
                val color = if (summary.percentage < 75) {
                    android.graphics.Color.parseColor("#FF5252")
                } else {
                    android.graphics.Color.parseColor("#00E5FF")
                }
                binding.tvPercentage.setTextColor(color)
            } else {
                binding.tvAttendanceCount.visibility = View.GONE
                binding.progressAttendance.visibility = View.GONE
                binding.tvPercentage.visibility = View.GONE
                binding.tvNoData.visibility = View.VISIBLE
            }

            // Expandable date history
            val isExpanded = expandedPositions.contains(position)
            binding.dateHistoryContainer.visibility = if (isExpanded) View.VISIBLE else View.GONE
            binding.ivExpandArrow.rotation = if (isExpanded) 180f else 0f

            if (isExpanded && summary.hasAnyData) {
                binding.tvDateHistory.text = summary.presentDates.joinToString("\n") { "✓  Present — $it" }
            }

            binding.root.setOnClickListener {
                if (summary.hasAnyData) {
                    if (isExpanded) expandedPositions.remove(position) else expandedPositions.add(position)
                    notifyItemChanged(position)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSubjectAttendanceBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(summaries[position], position)
    }

    override fun getItemCount(): Int = summaries.size
}