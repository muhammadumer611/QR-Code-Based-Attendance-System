package com.university.attendance

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.university.attendance.databinding.ItemSubjectAttendanceBinding

/**
 * Shows one attendance card per subject.
 *
 * Displays:
 * - Subject name
 * - Course code
 * - Teacher name
 * - Present / Total classes
 * - Attendance percentage
 * - Progress bar
 * - Expandable present-date history
 *
 * Attendance rule:
 * - More than 75%  = normal
 * - Exactly 75%    = warning
 * - Below 75%      = warning
 *
 * If no classes have been held yet, no percentage is shown.
 */
class SubjectAttendanceAdapter(
    private val summaries: List<SubjectAttendanceSummary>
) : RecyclerView.Adapter<SubjectAttendanceAdapter.ViewHolder>() {

    private val expandedPositions =
        mutableSetOf<Int>()

    inner class ViewHolder(
        private val binding:
        ItemSubjectAttendanceBinding
    ) : RecyclerView.ViewHolder(
        binding.root
    ) {

        fun bind(
            summary: SubjectAttendanceSummary,
            position: Int
        ) {

            // ----------------------------------------------------
            // BASIC SUBJECT INFORMATION
            // ----------------------------------------------------

            binding.tvSubjectName.text =
                summary.subjectName
                    .ifBlank {
                        "Unknown Subject"
                    }

            binding.tvCourseCode.text =
                summary.courseCode
                    .ifBlank {
                        "Course code unavailable"
                    }

            binding.tvTeacherName.text =
                "Taught by: ${
                    summary.teacherName.ifBlank {
                        "Not available"
                    }
                }"


            // ----------------------------------------------------
            // ATTENDANCE INFORMATION
            // ----------------------------------------------------

            if (summary.hasAnyData) {

                binding.tvAttendanceCount.visibility =
                    View.VISIBLE

                binding.progressAttendance.visibility =
                    View.VISIBLE

                binding.tvPercentage.visibility =
                    View.VISIBLE

                binding.tvNoData.visibility =
                    View.GONE


                // Present / Total
                binding.tvAttendanceCount.text =
                    "${summary.presentCount} / " +
                            "${summary.totalClassesHeld} classes"


                // Percentage
                binding.tvPercentage.text =
                    "${summary.percentage}%"


                // Progress bar
                binding.progressAttendance.progress =
                    summary.percentage
                        .coerceIn(
                            0,
                            100
                        )


                // ------------------------------------------------
                // 75% ATTENDANCE RULE
                // ------------------------------------------------
                //
                // <= 75% = WARNING
                // > 75%  = NORMAL
                //

                val percentageColor =
                    if (
                        summary.percentage <= 75
                    ) {

                        Color.parseColor(
                            "#FF5252"
                        )

                    } else {

                        Color.parseColor(
                            "#00E5FF"
                        )
                    }


                binding.tvPercentage.setTextColor(
                    percentageColor
                )

            } else {

                // ------------------------------------------------
                // NO ATTENDANCE DATA YET
                // ------------------------------------------------

                binding.tvAttendanceCount.visibility =
                    View.GONE

                binding.progressAttendance.visibility =
                    View.GONE

                binding.tvPercentage.visibility =
                    View.GONE

                binding.tvNoData.visibility =
                    View.VISIBLE

                // Reset progress so RecyclerView does not
                // accidentally reuse an old value.
                binding.progressAttendance.progress =
                    0
            }


            // ----------------------------------------------------
            // EXPAND / COLLAPSE DATE HISTORY
            // ----------------------------------------------------

            val isExpanded =
                expandedPositions.contains(
                    position
                )


            binding.dateHistoryContainer.visibility =
                if (isExpanded) {
                    View.VISIBLE
                } else {
                    View.GONE
                }


            binding.ivExpandArrow.rotation =
                if (isExpanded) {
                    180f
                } else {
                    0f
                }


            // ----------------------------------------------------
            // PRESENT DATE HISTORY
            // ----------------------------------------------------

            if (
                isExpanded &&
                summary.hasAnyData
            ) {

                if (
                    summary.presentDates.isEmpty()
                ) {

                    binding.tvDateHistory.text =
                        "No present attendance dates recorded."

                } else {

                    binding.tvDateHistory.text =
                        summary.presentDates
                            .joinToString(
                                separator = "\n"
                            ) { date ->

                                "✓  Present — $date"
                            }
                }
            } else {

                binding.tvDateHistory.text =
                    ""
            }


            // ----------------------------------------------------
            // CARD CLICK
            // ----------------------------------------------------

            binding.root.setOnClickListener {

                if (
                    !summary.hasAnyData
                ) {
                    return@setOnClickListener
                }


                if (
                    expandedPositions.contains(
                        position
                    )
                ) {

                    expandedPositions.remove(
                        position
                    )

                } else {

                    expandedPositions.add(
                        position
                    )
                }


                notifyItemChanged(
                    position
                )
            }
        }
    }


    // ------------------------------------------------------------
    // CREATE VIEW HOLDER
    // ------------------------------------------------------------

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val binding =
            ItemSubjectAttendanceBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ),
                parent,
                false
            )

        return ViewHolder(
            binding
        )
    }


    // ------------------------------------------------------------
    // BIND VIEW HOLDER
    // ------------------------------------------------------------

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        holder.bind(
            summaries[position],
            position
        )
    }


    // ------------------------------------------------------------
    // ITEM COUNT
    // ------------------------------------------------------------

    override fun getItemCount(): Int =
        summaries.size
}