package com.university.attendance

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

import com.university.attendance.databinding.ItemDailyClassStatusBinding
import com.university.attendance.databinding.ItemDailyDepartmentHeaderBinding
import com.university.attendance.databinding.ItemDailySessionHeaderBinding

/**
 * Flattens the grouped Department -> Session -> ClassStatus structure into
 * a single list of rows with 3 view types, so it renders as one scrolling
 * RecyclerView with section headers (Department, then Session, then each
 * class's status).
 */
class DailyOverviewAdapter(
    private var groups: List<DailyDepartmentGroup>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private sealed class Row {
        data class DeptHeader(val name: String) : Row()
        data class SessionHeader(val session: String) : Row()
        data class ClassRow(val status: DailyClassStatus) : Row()
    }

    private var flatRows: List<Row> = flatten(groups)

    private fun flatten(groups: List<DailyDepartmentGroup>): List<Row> {
        val rows = mutableListOf<Row>()
        groups.forEach { dept ->
            rows.add(Row.DeptHeader(dept.departmentName))
            dept.sessionGroups.forEach { sessionGroup ->
                rows.add(Row.SessionHeader(sessionGroup.session))
                sessionGroup.classStatuses.forEach { status ->
                    rows.add(Row.ClassRow(status))
                }
            }
        }
        return rows
    }

    fun updateGroups(newGroups: List<DailyDepartmentGroup>) {
        groups = newGroups
        flatRows = flatten(newGroups)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int = when (flatRows[position]) {
        is Row.DeptHeader -> TYPE_DEPT_HEADER
        is Row.SessionHeader -> TYPE_SESSION_HEADER
        is Row.ClassRow -> TYPE_CLASS_ROW
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_DEPT_HEADER -> DeptHeaderViewHolder(
                ItemDailyDepartmentHeaderBinding.inflate(inflater, parent, false)
            )
            TYPE_SESSION_HEADER -> SessionHeaderViewHolder(
                ItemDailySessionHeaderBinding.inflate(inflater, parent, false)
            )
            else -> ClassRowViewHolder(
                ItemDailyClassStatusBinding.inflate(inflater, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = flatRows[position]) {
            is Row.DeptHeader -> (holder as DeptHeaderViewHolder).bind(row.name)
            is Row.SessionHeader -> (holder as SessionHeaderViewHolder).bind(row.session)
            is Row.ClassRow -> (holder as ClassRowViewHolder).bind(row.status)
        }
    }

    override fun getItemCount(): Int = flatRows.size

    class DeptHeaderViewHolder(private val binding: ItemDailyDepartmentHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(name: String) {
            binding.tvDepartmentName.text = name
        }
    }

    class SessionHeaderViewHolder(private val binding: ItemDailySessionHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(session: String) {
            binding.tvSessionLabel.text = "Session $session"
        }
    }

    class ClassRowViewHolder(private val binding: ItemDailyClassStatusBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(status: DailyClassStatus) {
            binding.tvClassCourse.text = "${status.classTitle} • ${status.courseCode}"
            binding.tvSubjectName.text = status.subjectName

            if (status.wasMarked) {
                binding.tvStatus.text = "${status.presentCount}/${status.totalStudents} Present"
                binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#00E5FF"))
                binding.statusDot.setBackgroundResource(R.drawable.bg_status_dot_present)
            } else {
                binding.tvStatus.text = "Not Marked"
                binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#FF5252"))
                binding.statusDot.setBackgroundResource(R.drawable.bg_status_dot_absent)
            }
        }
    }

    companion object {
        private const val TYPE_DEPT_HEADER = 0
        private const val TYPE_SESSION_HEADER = 1
        private const val TYPE_CLASS_ROW = 2
    }
}