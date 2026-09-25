package com.university.attendance

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.university.attendance.databinding.ActivityStudentAttendanceHistoryBinding
import kotlinx.coroutines.launch

class ActivityStudentAttendanceHistory :
    AppCompatActivity() {

    private lateinit var binding:
            ActivityStudentAttendanceHistoryBinding

    private val repository =
        StudentAttendanceSummaryRepository()

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        binding =
            ActivityStudentAttendanceHistoryBinding
                .inflate(
                    layoutInflater
                )

        setContentView(
            binding.root
        )

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.recyclerSubjects.layoutManager =
            LinearLayoutManager(
                this
            )

        loadAttendance()
    }


    private fun loadAttendance() {

        if (
            FirebaseAuth
                .getInstance()
                .currentUser ==
            null
        ) {

            finish()

            return
        }


        binding.progressBar.visibility =
            View.VISIBLE

        lifecycleScope.launch {

            try {

                val student =
                    repository
                        .let {
                            StudentAttendanceRepository()
                                .getCurrentStudent()
                        }


                binding.tvStudentName.text =
                    student.fullName
                        .ifBlank {
                            "Student"
                        }


                binding.tvStudentInfo.text =
                    buildString {

                        if (
                            student.regNo
                                .isNotBlank()
                        ) {

                            append(
                                student.regNo
                            )
                        }

                        if (
                            student.programName
                                .isNotBlank()
                        ) {

                            if (
                                isNotEmpty()
                            ) {
                                append(" • ")
                            }

                            append(
                                student.programName
                            )
                        }

                        append(
                            " • Semester ${student.semester}"
                        )
                    }


                val summaries =
                    repository
                        .getStudentAttendance(
                            student
                        )


                binding.progressBar.visibility =
                    View.GONE


                if (
                    summaries.isEmpty()
                ) {

                    binding.tvEmptyState.visibility =
                        View.VISIBLE

                    binding.recyclerSubjects.visibility =
                        View.GONE

                    binding.tvOverallAttendance.visibility =
                        View.GONE

                    return@launch
                }


                binding.tvEmptyState.visibility =
                    View.GONE

                binding.recyclerSubjects.visibility =
                    View.VISIBLE


                binding.recyclerSubjects.adapter =
                    SubjectAttendanceAdapter(
                        summaries
                    )


                // ------------------------------------------------
                // OVERALL ATTENDANCE
                // ------------------------------------------------

                val totalClasses =
                    summaries.sumOf {
                        it.totalClassesHeld
                    }

                val totalPresent =
                    summaries.sumOf {
                        it.presentCount
                    }


                val overallPercentage =
                    if (
                        totalClasses == 0
                    ) {

                        0

                    } else {

                        (
                                totalPresent * 100
                                        /
                                        totalClasses
                                )
                    }


                binding.tvOverallAttendance.text =
                    "Overall Attendance: " +
                            "$overallPercentage%"


                binding.tvOverallAttendance.visibility =
                    View.VISIBLE


                // ------------------------------------------------
                // LOW ATTENDANCE ALERT
                // ------------------------------------------------

                val lowSubjects =
                    summaries.filter {

                        it.hasAnyData &&
                                it.percentage <= 75
                    }


                if (
                    lowSubjects.isEmpty()
                ) {

                    binding.tvAlert.visibility =
                        View.GONE

                } else {

                    binding.tvAlert.visibility =
                        View.VISIBLE

                    binding.tvAlert.text =
                        buildString {

                            append(
                                "⚠ Attendance Warning\n\n"
                            )

                            append(
                                "Your attendance is 75% or below in:\n\n"
                            )

                            lowSubjects.forEachIndexed {
                                    index,
                                    subject ->

                                append(
                                    "• ${subject.subjectName}: " +
                                            "${subject.percentage}%"
                                )

                                if (
                                    index <
                                    lowSubjects.lastIndex
                                ) {

                                    append("\n")
                                }
                            }

                            append(
                                "\n\nPlease maintain regular attendance."
                            )
                        }
                }

            } catch (
                e: Exception
            ) {

                binding.progressBar.visibility =
                    View.GONE

                Toast.makeText(
                    this@ActivityStudentAttendanceHistory,
                    e.message
                        ?: "Unable to load attendance history.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}