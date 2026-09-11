package com.university.attendance

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.university.attendance.databinding.ItemActiveAttendanceBinding

class ActiveAttendanceAdapter(private val onSave: (AttendanceSession) -> Unit) : RecyclerView.Adapter<ActiveAttendanceAdapter.VH>() {
    private val items = mutableListOf<AttendanceSession>()
    fun submitList(list: List<AttendanceSession>) { items.clear(); items.addAll(list); notifyDataSetChanged() }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(ItemActiveAttendanceBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size
    inner class VH(private val b: ItemActiveAttendanceBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(s: AttendanceSession) {
            b.tvSubject.text = if (s.courseCode.isBlank()) s.subjectName else "${s.courseCode} • ${s.subjectName}"
            b.tvTeacher.text = "Teacher: ${s.teacherName.ifBlank { "Teacher" }}"
            b.tvClass.text = "Class: ${s.className.ifBlank { s.section }}"
            b.tvTime.text = "${s.startTime} - ${s.endTime}" + if (s.roomNumber > 0) " • Room ${s.roomNumber}" else ""
            b.tvLive.text = "LIVE • Attendance Open"
            b.btnSave.isEnabled = s.isActive
            b.btnSave.text = "Save Attendance"
            b.btnSave.setOnClickListener { b.btnSave.isEnabled = false; b.btnSave.text = "Saving..."; onSave(s) }
        }
    }
}
