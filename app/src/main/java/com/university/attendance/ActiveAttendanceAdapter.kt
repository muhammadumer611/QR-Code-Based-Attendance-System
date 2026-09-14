package com.university.attendance

import android.graphics.Bitmap
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
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
            b.tvLive.text = "LIVE • Teacher Attendance"
            b.tvQrHint.text = "Live QR received from teacher. You do not need to scan it."
            renderQr(s.qrPayload)
            b.btnSave.isEnabled = s.isActive
            b.btnSave.text = "Save Attendance"
            b.btnSave.setOnClickListener { b.btnSave.isEnabled = false; b.btnSave.text = "Saving..."; onSave(s) }
        }

        private fun renderQr(payload: String) {
            if (payload.isBlank()) {
                b.imgLiveQr.visibility = android.view.View.GONE
                return
            }
            try {
                val matrix = MultiFormatWriter().encode(payload, BarcodeFormat.QR_CODE, 520, 520)
                val bitmap = Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.RGB_565)
                for (x in 0 until matrix.width) for (y in 0 until matrix.height) {
                    bitmap.setPixel(x, y, if (matrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
                b.imgLiveQr.setImageBitmap(bitmap)
                b.imgLiveQr.visibility = android.view.View.VISIBLE
            } catch (_: Exception) {
                b.imgLiveQr.visibility = android.view.View.GONE
            }
        }
    }
}
