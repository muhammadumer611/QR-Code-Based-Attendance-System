package com.university.attendance

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.university.attendance.databinding.ActivityTeacherClassDetailBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ActivityTeacherClassDetail :
    AppCompatActivity() {

    private lateinit var binding:
            ActivityTeacherClassDetailBinding

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        binding =
            ActivityTeacherClassDetailBinding
                .inflate(
                    layoutInflater
                )

        setContentView(
            binding.root
        )

        loadClassData()
        setupClicks()
    }

    private fun loadClassData() {

        val subjectName =
            intent
                .getStringExtra(
                    "subjectName"
                )
                .orEmpty()

        val courseCode =
            intent
                .getStringExtra(
                    "courseCode"
                )
                .orEmpty()

        val programName =
            intent
                .getStringExtra(
                    "programName"
                )
                .orEmpty()

        val semester =
            intent.getIntExtra(
                "semester",
                1
            )

        val session =
            intent
                .getStringExtra(
                    "session"
                )
                .orEmpty()

        val section =
            intent
                .getStringExtra(
                    "section"
                )
                .orEmpty()

        val department =
            intent
                .getStringExtra(
                    "departmentName"
                )
                .orEmpty()

        val className =
            intent
                .getStringExtra(
                    "className"
                )
                .orEmpty()

        val room =
            intent.getIntExtra(
                "roomNumber",
                0
            )

        val start =
            intent
                .getStringExtra(
                    "startTime"
                )
                .orEmpty()

        val end =
            intent
                .getStringExtra(
                    "endTime"
                )
                .orEmpty()

        val date =
            intent
                .getStringExtra(
                    "date"
                )
                .orEmpty()

        val day =
            intent
                .getStringExtra(
                    "dayName"
                )
                .orEmpty()

        val period =
            intent
                .getStringExtra(
                    "periodType"
                )
                .orEmpty()

        binding.tvSubjectName.text =
            subjectName.ifBlank {
                "Subject"
            }

        binding.tvCourseCode.text =
            courseCode.ifBlank {
                "Course Code"
            }

        binding.tvProgram.text =
            "Program: ${programName.ifBlank { "-" }}"

        binding.tvSemester.text =
            "Semester: $semester  •  Session: ${
                session.ifBlank { "-" }
            }"

        binding.tvDepartment.text =
            "Department: ${
                department.ifBlank { "-" }
            }"

        binding.tvClassName.text =
            "Class: ${
                className.ifBlank { "-" }
            }  •  Section: ${
                section.ifBlank { "-" }
            }"

        binding.tvScheduleTime.text =
            "${
                start.ifBlank { "-" }
            } - ${
                end.ifBlank { "-" }
            }  •  ${
                if (day.isBlank()) date
                else day
            }"

        binding.tvRoom.text =
            if (room > 0) {
                "Room: $room"
            } else {
                "Room not specified"
            }

        // --------------------------------------------------------
        // IS TODAY?
        // --------------------------------------------------------

        val today =
            SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.US
            ).format(
                Date()
            )

        val todayDay =
            SimpleDateFormat(
                "EEEE",
                Locale.US
            ).format(
                Date()
            )

        val isToday = when {
            period.equals("Daily", true) -> date == today
            period.equals("Monthly", true) -> date == today
            period.equals("Weekly", true) ->
                day.equals(todayDay, true) || date == today
            period.equals("Semester", true) ->
                day.equals(todayDay, true) || date == today
            else -> date == today
        }

        binding.cardGenerateQr.visibility =
            if (isToday) {
                View.VISIBLE
            } else {
                View.GONE
            }

        binding.tvQrInfo.visibility =
            if (isToday) {
                View.VISIBLE
            } else {
                View.GONE
            }
    }

    private fun setupClicks() {

        binding.btnBack
            .setOnClickListener {
                finish()
            }

        binding.cardGenerateQr
            .setOnClickListener {

                val scheduleId =
                    intent
                        .getStringExtra(
                            "scheduleId"
                        )
                        .orEmpty()

                if (
                    scheduleId.isBlank()
                ) {

                    Toast.makeText(
                        this,
                        "Class schedule ID is missing.",
                        Toast.LENGTH_LONG
                    ).show()

                    return@setOnClickListener
                }

                startActivity(

                    Intent(
                        this,
                        ActivityTeacherAttendance::class.java
                    ).apply {

                        putExtra(
                            "scheduleId",
                            scheduleId
                        )

                        putExtra(
                            "subjectName",
                            intent.getStringExtra(
                                "subjectName"
                            ).orEmpty()
                        )

                        putExtra(
                            "courseCode",
                            intent.getStringExtra(
                                "courseCode"
                            ).orEmpty()
                        )

                        putExtra(
                            "className",
                            intent.getStringExtra(
                                "className"
                            ).orEmpty()
                        )

                        putExtra(
                            "startTime",
                            intent.getStringExtra(
                                "startTime"
                            ).orEmpty()
                        )

                        putExtra(
                            "endTime",
                            intent.getStringExtra(
                                "endTime"
                            ).orEmpty()
                        )


                        putExtra("subjectId", intent.getStringExtra("subjectId").orEmpty())
                        putExtra("classId", intent.getStringExtra("classId").orEmpty())
                        putExtra("departmentName", intent.getStringExtra("departmentName").orEmpty())
                        putExtra("programName", intent.getStringExtra("programName").orEmpty())
                        putExtra("semester", intent.getIntExtra("semester", 1))
                        putExtra("session", intent.getStringExtra("session").orEmpty())
                        putExtra("section", intent.getStringExtra("section").orEmpty())
                        putExtra("teacherId", intent.getStringExtra("teacherId").orEmpty())
                        putExtra("teacherName", intent.getStringExtra("teacherName").orEmpty())
                        putExtra("roomNumber", intent.getIntExtra("roomNumber", 0))
                        putExtra("periodType", intent.getStringExtra("periodType").orEmpty())
                        putExtra(
                            "date",
                            intent.getStringExtra("date").orEmpty()
                        )
                        putExtra("dayName", intent.getStringExtra("dayName").orEmpty())
                    }
                )
            }
    }
}