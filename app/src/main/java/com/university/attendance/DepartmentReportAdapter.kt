package com.university.attendance

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.university.attendance.databinding.ItemDepartmentReportBinding

class DepartmentReportAdapter(
    private val rows: List<DepartmentReportRow>
) : RecyclerView.Adapter<DepartmentReportAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemDepartmentReportBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(row: DepartmentReportRow) {
            binding.tvDepartmentName.text = row.departmentName
            binding.tvStudentCount.text = "${row.studentCount} Students"
            binding.tvPercentage.text = "${row.todayAttendancePercentage}%"
            binding.progressBar.progress = row.todayAttendancePercentage

            val color = if (row.todayAttendancePercentage < 75) {
                Color.parseColor("#FF5252")
            } else {
                Color.parseColor("#00E5FF")
            }
            binding.tvPercentage.setTextColor(color)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDepartmentReportBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(rows[position])
    }

    override fun getItemCount(): Int = rows.size
}