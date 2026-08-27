package com.university.attendance

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.university.attendance.databinding.ActivityTeacherClassDetailBinding

class ActivityTeacherClassDetail : AppCompatActivity() {

    private lateinit var binding: ActivityTeacherClassDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding =
            ActivityTeacherClassDetailBinding.inflate(
                layoutInflater
            )

        setContentView(binding.root)

        loadSubjectData()

        setupClicks()
    }

    // ============================================================
    // LOAD SUBJECT DATA
    // ============================================================

    private fun loadSubjectData() {

        val subjectId =
            intent.getStringExtra("subjectId")
                ?: ""

        val courseCode =
            intent.getStringExtra("courseCode")
                ?: ""

        val subjectName =
            intent.getStringExtra("subjectName")
                ?: ""

        val programName =
            intent.getStringExtra("programName")
                ?: ""

        val semester =
            intent.getStringExtra("semester")
                ?: ""

        val departmentName =
            intent.getStringExtra("departmentName")
                ?: ""

        val teacherId =
            intent.getStringExtra("teacherId")
                ?: ""


        binding.tvSubjectName.text =
            subjectName.ifBlank {
                "Subject"
            }

        binding.tvCourseCode.text =
            courseCode.ifBlank {
                "Course Code"
            }

        binding.tvProgram.text =
            programName.ifBlank {
                "Program"
            }

        binding.tvSemester.text =
            semester.ifBlank {
                "Semester"
            }

        binding.tvDepartment.text =
            departmentName.ifBlank {
                "Department"
            }
    }

    // ============================================================
    // CLICKS
    // ============================================================

    private fun setupClicks() {

        binding.btnBack.setOnClickListener {
            finish()
        }
    }
}