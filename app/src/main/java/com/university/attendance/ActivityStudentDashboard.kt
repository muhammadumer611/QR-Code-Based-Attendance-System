package com.university.attendance

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.university.attendance.databinding.ActivityStudentDashboardBinding
import kotlinx.coroutines.launch

class ActivityStudentDashboard :
    AppCompatActivity() {

    private lateinit var binding:
            ActivityStudentDashboardBinding

    private val repository =
        StudentAttendanceRepository()

    private lateinit var adapter:
            ActiveAttendanceAdapter

    private var listener:
            com.google.firebase.firestore.ListenerRegistration?
            = null

    private var currentStudent:
            Student? = null

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        binding =
            ActivityStudentDashboardBinding
                .inflate(layoutInflater)

        setContentView(
            binding.root
        )

        adapter =
            ActiveAttendanceAdapter {

                    session ->

                saveAttendance(
                    session
                )
            }

        binding.rvActiveAttendance.layoutManager =
            LinearLayoutManager(this)

        binding.rvActiveAttendance.adapter =
            adapter

        binding.btnNotifications.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    ActivityNotifications::class.java
                )
            )
        }

        binding.btnScanQr.visibility =
            View.GONE

        binding.tvScanHint.visibility =
            View.GONE

        loadStudent()
    }

    private fun loadStudent() {

        if (
            FirebaseAuth
                .getInstance()
                .currentUser == null
        ) {

            finish()

            return
        }

        lifecycleScope.launch {

            try {

                val student =
                    repository
                        .getCurrentStudent()

                currentStudent =
                    student

                StudentSession.save(
                    this@ActivityStudentDashboard,
                    student
                )

                binding.tvStudentName.text =
                    student.fullName
                        .ifBlank {
                            "Student"
                        }

                binding.tvStudentMeta.text =
                    buildString {

                        append(
                            student.programName
                                .ifBlank {
                                    "Program"
                                }
                        )

                        append(
                            " • Semester ${student.semester}"
                        )

                        if (
                            student.section.isNotBlank()
                        ) {

                            append(
                                " • Section ${student.section}"
                            )
                        }

                        if (
                            student.regNo.isNotBlank()
                        ) {

                            append(
                                " • ${student.regNo}"
                            )
                        }
                    }

                binding.tvNextClass.text =
                    "Class: ${
                        student.classId
                            .ifBlank {
                                "Not assigned"
                            }
                    }"

                startLiveListener(
                    student
                )

            } catch (e: Exception) {

                Toast.makeText(
                    this@ActivityStudentDashboard,
                    e.message
                        ?: "Unable to load student profile.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun startLiveListener(
        student: Student
    ) {

        listener?.remove()

        listener =
            repository.listenForActiveSessions(

                student,

                { sessions ->

                    lifecycleScope.launch {

                        try {

                            val eligible =
                                repository
                                    .filterEligibleSessions(
                                        student,
                                        sessions
                                    )

                            renderSessions(
                                eligible
                            )

                        } catch (e: Exception) {

                            Toast.makeText(
                                this@ActivityStudentDashboard,
                                e.message
                                    ?: "Unable to load eligible attendance.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                },

                { e ->

                    runOnUiThread {

                        Toast.makeText(
                            this,
                            e.message
                                ?: "Attendance listener failed.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            )
    }

    private fun renderSessions(
        sessions:
        List<AttendanceSession>
    ) {

        adapter.submitList(
            sessions
        )

        val empty =
            sessions.isEmpty()

        binding.rvActiveAttendance.visibility =
            if (empty) {
                View.GONE
            } else {
                View.VISIBLE
            }

        binding.tvLiveAttendanceEmptyTitle.visibility =
            if (empty) {
                View.VISIBLE
            } else {
                View.GONE
            }

        binding.tvLiveAttendanceEmpty.visibility =
            if (empty) {
                View.VISIBLE
            } else {
                View.GONE
            }

        binding.tvTodayStatusValue.text =
            if (empty) {
                "No Live Attendance"
            } else {
                "Attendance Available"
            }
    }

    private fun saveAttendance(
        session:
        AttendanceSession
    ) {

        val student =
            currentStudent
                ?: return

        lifecycleScope.launch {

            try {

                repository.markAttendance(
                    session.sessionId,
                    student
                )

                Toast.makeText(
                    this@ActivityStudentDashboard,
                    "Attendance marked successfully.",
                    Toast.LENGTH_SHORT
                ).show()

                renderSessions(
                    repository
                        .getActiveSessionsForStudent(
                            student
                        )
                )

            } catch (e: Exception) {

                Toast.makeText(
                    this@ActivityStudentDashboard,
                    e.message
                        ?: "Attendance could not be marked.",
                    Toast.LENGTH_LONG
                ).show()

                renderSessions(
                    repository
                        .getActiveSessionsForStudent(
                            student
                        )
                )
            }
        }
    }

    override fun onDestroy() {

        listener?.remove()

        listener =
            null

        super.onDestroy()
    }
}