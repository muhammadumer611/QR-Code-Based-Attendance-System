//package com.university.attendance
//
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import androidx.recyclerview.widget.RecyclerView
//import com.university.attendance.databinding.ItemActiveAttendanceBinding
//
//class ActiveAttendanceAdapter(
//    private val onSave: (AttendanceSession) -> Unit
//) : RecyclerView.Adapter<ActiveAttendanceAdapter.VH>() {
//
//    private val items =
//        mutableListOf<AttendanceSession>()
//
//    private val nearbySessionIds =
//        mutableSetOf<String>()
//
//    fun submitList(
//        list: List<AttendanceSession>
//    ) {
//        items.clear()
//        items.addAll(list)
//        notifyDataSetChanged()
//    }
//
//    fun setNearbySessionIds(
//        sessionIds: Set<String>
//    ) {
//        nearbySessionIds.clear()
//        nearbySessionIds.addAll(sessionIds)
//        notifyDataSetChanged()
//    }
//
//    override fun onCreateViewHolder(
//        parent: ViewGroup,
//        viewType: Int
//    ): VH =
//        VH(
//            ItemActiveAttendanceBinding.inflate(
//                LayoutInflater.from(parent.context),
//                parent,
//                false
//            )
//        )
//
//    override fun onBindViewHolder(
//        holder: VH,
//        position: Int
//    ) {
//        holder.bind(items[position])
//    }
//
//    override fun getItemCount(): Int =
//        items.size
//
//    inner class VH(
//        private val b: ItemActiveAttendanceBinding
//    ) : RecyclerView.ViewHolder(b.root) {
//
//        fun bind(
//            session: AttendanceSession
//        ) {
//            b.tvSubject.text =
//                if (session.courseCode.isBlank()) {
//                    session.subjectName
//                } else {
//                    "${session.courseCode} • ${session.subjectName}"
//                }
//
//            b.tvTeacher.text =
//                "Teacher: ${session.teacherName.ifBlank { "Teacher" }}"
//
//            b.tvClass.text =
//                "Class: ${session.className.ifBlank { session.section }}"
//
//            b.tvTime.text =
//                "${session.startTime} - ${session.endTime}" +
//                        if (session.roomNumber > 0) {
//                            " • Room ${session.roomNumber}"
//                        } else {
//                            ""
//                        }
//
//            b.tvLive.text =
//                "LIVE • Classroom proximity required"
//
//            val nearby =
//                nearbySessionIds.contains(
//                    session.sessionId
//                )
//
//            b.btnSave.isEnabled =
//                session.isActive && nearby
//
//            b.btnSave.text =
//                when {
//                    !session.isActive ->
//                        "Attendance Closed"
//
//                    nearby ->
//                        "Save Attendance"
//
//                    else ->
//                        "Move Near Teacher"
//                }
//
//            b.btnSave.setOnClickListener {
//                if (
//                    !session.isActive ||
//                    !nearbySessionIds.contains(
//                        session.sessionId
//                    )
//                ) {
//                    return@setOnClickListener
//                }
//
//                b.btnSave.isEnabled = false
//                b.btnSave.text = "Saving..."
//                onSave(session)
//            }
//        }
//    }
//}
package com.university.attendance

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.university.attendance.databinding.ItemActiveAttendanceBinding

class ActiveAttendanceAdapter(
    private val onSave: (AttendanceSession) -> Unit
) : RecyclerView.Adapter<ActiveAttendanceAdapter.VH>() {

    // ============================================================
    // SINGLE DEVICE TEST MODE
    // ============================================================
    //
    // true  = BLE proximity is NOT required
    //         for testing on one phone.
    //
    // false = Real BLE proximity is required.
    //
    // When you have two phones, change this to false.
    //
    private val SINGLE_DEVICE_TEST_MODE = true


    private val items =
        mutableListOf<AttendanceSession>()


    private val nearbySessionIds =
        mutableSetOf<String>()


    // ============================================================
    // UPDATE ATTENDANCE SESSIONS
    // ============================================================

    fun submitList(
        list: List<AttendanceSession>
    ) {

        items.clear()

        items.addAll(
            list
        )

        notifyDataSetChanged()
    }


    // ============================================================
    // UPDATE BLE NEARBY SESSIONS
    // ============================================================

    fun setNearbySessionIds(
        sessionIds: Set<String>
    ) {

        nearbySessionIds.clear()

        nearbySessionIds.addAll(
            sessionIds
        )

        notifyDataSetChanged()
    }


    // ============================================================
    // CREATE VIEW HOLDER
    // ============================================================

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): VH {

        return VH(
            ItemActiveAttendanceBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }


    // ============================================================
    // BIND VIEW HOLDER
    // ============================================================

    override fun onBindViewHolder(
        holder: VH,
        position: Int
    ) {

        holder.bind(
            items[position]
        )
    }


    // ============================================================
    // ITEM COUNT
    // ============================================================

    override fun getItemCount(): Int =
        items.size


    // ============================================================
    // VIEW HOLDER
    // ============================================================

    inner class VH(
        private val b:
        ItemActiveAttendanceBinding
    ) : RecyclerView.ViewHolder(
        b.root
    ) {


        fun bind(
            session: AttendanceSession
        ) {

            // ====================================================
            // SUBJECT
            // ====================================================

            b.tvSubject.text =
                if (
                    session.courseCode.isBlank()
                ) {

                    session.subjectName

                } else {

                    "${session.courseCode} • " +
                            session.subjectName
                }


            // ====================================================
            // TEACHER
            // ====================================================

            b.tvTeacher.text =
                "Teacher: ${
                    session.teacherName
                        .ifBlank {
                            "Teacher"
                        }
                }"


            // ====================================================
            // CLASS
            // ====================================================

            b.tvClass.text =
                "Class: ${
                    session.className
                        .ifBlank {
                            session.section
                        }
                }"


            // ====================================================
            // TIME / ROOM
            // ====================================================

            b.tvTime.text =
                "${session.startTime} - " +
                        "${session.endTime}" +
                        if (
                            session.roomNumber > 0
                        ) {

                            " • Room " +
                                    session.roomNumber

                        } else {

                            ""
                        }


            // ====================================================
            // BLE STATUS
            // ====================================================

            val nearby =
                nearbySessionIds.contains(
                    session.sessionId
                )


            // ====================================================
            // TEST MODE
            // ====================================================
            //
            // In single-device test mode:
            //
            // nearby = automatically treated as true
            //
            // Therefore the button becomes clickable.
            //

            val proximityAllowed =
                SINGLE_DEVICE_TEST_MODE ||
                        nearby


            // ====================================================
            // STATUS TEXT
            // ====================================================

            b.tvLive.text =
                when {

                    !session.isActive ->
                        "ATTENDANCE CLOSED"

                    SINGLE_DEVICE_TEST_MODE ->
                        "LIVE • TEST MODE"

                    nearby ->
                        "LIVE • Teacher nearby"

                    else ->
                        "LIVE • Move near teacher"
                }


            // ====================================================
            // BUTTON ENABLE / DISABLE
            // ====================================================

            b.btnSave.isEnabled =
                session.isActive &&
                        proximityAllowed


            // ====================================================
            // BUTTON TEXT
            // ====================================================

            b.btnSave.text =
                when {

                    !session.isActive ->
                        "Attendance Closed"

                    SINGLE_DEVICE_TEST_MODE ->
                        "Save Attendance"

                    nearby ->
                        "Save Attendance"

                    else ->
                        "Move Near Teacher"
                }


            // ====================================================
            // BUTTON CLICK
            // ====================================================

            b.btnSave.setOnClickListener {

                // ------------------------------------------------
                // SESSION MUST STILL BE ACTIVE
                // ------------------------------------------------

                if (
                    !session.isActive
                ) {

                    return@setOnClickListener
                }


                // ------------------------------------------------
                // REAL BLE MODE
                // ------------------------------------------------
                //
                // Only check nearby when test mode is OFF.
                //

                if (
                    !SINGLE_DEVICE_TEST_MODE &&
                    !nearbySessionIds.contains(
                        session.sessionId
                    )
                ) {

                    return@setOnClickListener
                }


                // ------------------------------------------------
                // DISABLE WHILE SAVING
                // ------------------------------------------------

                b.btnSave.isEnabled =
                    false

                b.btnSave.text =
                    "Saving..."


                // ------------------------------------------------
                // CALL DASHBOARD
                // ------------------------------------------------

                onSave(
                    session
                )
            }
        }
    }
}