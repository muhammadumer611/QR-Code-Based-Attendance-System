package com.university.attendance


import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.university.attendance.databinding.ActivityClassScheduleBinding
import com.university.attendance.databinding.DialogAddClassScheduleBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ActivityClassSchedule : AppCompatActivity() {

    private lateinit var binding: ActivityClassScheduleBinding
    private lateinit var viewModel: ClassScheduleViewModel

    private var selectedDate = ""
    private var selectedStartTime = ""
    private var selectedEndTime = ""

    private var selectedPeriod = "Weekly"

    private var allLoadedClasses =
        emptyList<ClassSchedule>()

    private var selectedTeacher: Teacher? = null

    private var teachers: List<Teacher> = emptyList()

    private var selectedSubject: Subject? = null

    private var availableSubjects =
        emptyList<Subject>()

    private val semesterList =
        (1..8).map {
            "Semester $it"
        }

    private val dateFormat =
        SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        )

    private val displayDateFormat =
        SimpleDateFormat(
            "dd MMM yyyy",
            Locale.US
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding =
            ActivityClassScheduleBinding.inflate(
                layoutInflater
            )

        setContentView(binding.root)

        viewModel =
            ViewModelProvider(this)[
                ClassScheduleViewModel::class.java
            ]

        setupRecyclerView()

        observeViewModel()

        setupClickListeners()

        loadTeachers()

        selectPeriod("Weekly")
    }

    // ============================================================
    // CLICK LISTENERS
    // ============================================================

    private fun setupClickListeners() {

        binding.btnBackHeader.setOnClickListener {
            finish()
        }

        binding.fabAddClass.setOnClickListener {
            showAddClassDialog()
        }

        // --------------------------------------------------------
        // Period buttons
        // --------------------------------------------------------

        binding.btnDaily.setOnClickListener {
            selectPeriod("Daily")
        }

        binding.btnWeekly.setOnClickListener {
            selectPeriod("Weekly")
        }

        binding.btnMonthly.setOnClickListener {
            selectPeriod("Monthly")
        }

        binding.btnSemester.setOnClickListener {
            selectPeriod("Semester")
        }
    }

    // ============================================================
    // PERIOD
    // ============================================================

    private fun selectPeriod(period: String) {

        selectedPeriod = period

        binding.tvSelectedPeriod.text =
            "$period Schedule"

        binding.tvPeriodDates.text =
            when (period) {

                "Daily" ->
                    "Classes for today"

                "Weekly" ->
                    "Classes for this week"

                "Monthly" ->
                    "Classes for this month"

                "Semester" ->
                    "Classes for current semester"

                else ->
                    "Set classes for the selected period"
            }

        // Immediately filter already loaded classes
        renderFilteredClasses(
            allLoadedClasses
        )

        Toast.makeText(
            this,
            "$period schedule selected.",
            Toast.LENGTH_SHORT
        ).show()
    }

    // ============================================================
    // RECYCLER
    // ============================================================

    private fun setupRecyclerView() {

        binding.recyclerClasses.layoutManager =
            LinearLayoutManager(this)
    }

    // ============================================================
    // LOAD TEACHERS
    // ============================================================

    private fun loadTeachers() {

        lifecycleScope.launch {

            try {

                binding.progressBar.visibility =
                    View.VISIBLE

                teachers =
                    TeacherRepository()
                        .getAllTeachers()

            } catch (e: Exception) {

                Toast.makeText(
                    this@ActivityClassSchedule,
                    e.message
                        ?: "Failed to load teachers.",
                    Toast.LENGTH_LONG
                ).show()

            } finally {

                binding.progressBar.visibility =
                    View.GONE
            }
        }
    }

    // ============================================================
    // ADD CLASS
    // ============================================================

    private fun showAddClassDialog() {

        if (teachers.isEmpty()) {

            Toast.makeText(
                this,
                "No teachers found. Please add a teacher first.",
                Toast.LENGTH_LONG
            ).show()

            loadTeachers()

            return
        }

        selectedDate = ""
        selectedStartTime = ""
        selectedEndTime = ""
        selectedTeacher = null
        selectedSubject = null
        availableSubjects = emptyList()

        val dialogBinding =
            DialogAddClassScheduleBinding.inflate(
                LayoutInflater.from(this)
            )

        // ========================================================
        // TEACHER
        // ========================================================

        val teacherNames =
            teachers.map { teacher ->

                val name =
                    teacher.fullName.ifBlank {
                        teacher.email.ifBlank {
                            "Teacher"
                        }
                    }

                val id =
                    teacher.teacherId.ifBlank {
                        "No ID"
                    }

                "$name  •  $id"
            }

        val teacherAdapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                teacherNames
            )

        dialogBinding.etTeacher.setAdapter(
            teacherAdapter
        )

        dialogBinding.etTeacher.setOnClickListener {
            dialogBinding.etTeacher.showDropDown()
        }

        dialogBinding.etTeacher.setOnFocusChangeListener {
                _, hasFocus ->

            if (hasFocus) {
                dialogBinding.etTeacher.showDropDown()
            }
        }

        dialogBinding.etTeacher.setOnItemClickListener {
                _, _, position, _ ->

            selectedTeacher =
                teachers[position]

            selectedSubject =
                null

            availableSubjects =
                emptyList()

            dialogBinding.etSubject.setText(
                ""
            )

            dialogBinding.etSubject.isEnabled =
                false
        }

        // ========================================================
        // SEMESTER
        // ========================================================

        dialogBinding.etSemester.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                semesterList
            )
        )

        dialogBinding.etSemester.setOnClickListener {
            dialogBinding.etSemester.showDropDown()
        }

        dialogBinding.etSemester.setOnItemClickListener {
                _, _, _, _ ->

            loadSubjectsForScheduleDialog(
                dialogBinding
            )
        }

        // ========================================================
        // SESSION
        // ========================================================

        dialogBinding.etSession.setOnFocusChangeListener {
                _, hasFocus ->

            if (!hasFocus) {

                loadSubjectsForScheduleDialog(
                    dialogBinding
                )
            }
        }

        // ========================================================
        // SUBJECT
        // ========================================================

        dialogBinding.etSubject.isEnabled =
            false

        dialogBinding.etSubject.setOnItemClickListener {
                _, _, position, _ ->

            selectedSubject =
                availableSubjects.getOrNull(
                    position
                )
        }

        // ========================================================
        // DATE
        // ========================================================

        dialogBinding.etDate.setOnClickListener {

            showDatePicker(
                dialogBinding
            )
        }

        // ========================================================
        // START TIME
        // ========================================================

        dialogBinding.etStartTime.setOnClickListener {

            showTimePicker(
                isStartTime = true,
                dialogBinding = dialogBinding
            )
        }

        // ========================================================
        // END TIME
        // ========================================================

        dialogBinding.etEndTime.setOnClickListener {

            showTimePicker(
                isStartTime = false,
                dialogBinding = dialogBinding
            )
        }

        // ========================================================
        // DIALOG
        // ========================================================

        val dialog =
            AlertDialog.Builder(this)
                .setTitle("Add Class")
                .setView(dialogBinding.root)
                .setPositiveButton(
                    "Save",
                    null
                )
                .setNegativeButton(
                    "Cancel",
                    null
                )
                .create()

        dialog.setOnShowListener {

            val saveButton =
                dialog.getButton(
                    AlertDialog.BUTTON_POSITIVE
                )

            saveButton.setOnClickListener {

                saveClassFromDialog(
                    dialogBinding,
                    dialog
                )
            }
        }

        dialog.show()
    }

    // ============================================================
    // LOAD ASSIGNED SUBJECTS FOR TEACHER + SEMESTER + SESSION
    // ============================================================

    private fun loadSubjectsForScheduleDialog(
        dialogBinding:
        DialogAddClassScheduleBinding
    ) {

        val teacher =
            selectedTeacher
                ?: return

        val semesterText =
            dialogBinding.etSemester
                .text
                .toString()

        val semester =
            Regex("\\d+")
                .find(
                    semesterText
                )
                ?.value
                ?.toIntOrNull()

        val session =
            dialogBinding.etSession
                .text
                .toString()
                .trim()

        if (semester == null) {
            return
        }

        if (session.isBlank()) {
            return
        }

        lifecycleScope.launch {

            try {

                dialogBinding.etSubject.isEnabled =
                    false

                val subjects =
                    ClassScheduleRepository()
                        .getAssignedSubjects(
                            teacherId =
                                teacher.teacherId,
                            semester =
                                semester,
                            session =
                                session
                        )

                availableSubjects =
                    subjects

                val subjectLabels =
                    subjects.map {

                        if (
                            it.courseCode.isNotBlank()
                        ) {

                            "${it.courseCode} • ${it.subjectName}"

                        } else {

                            it.subjectName
                        }
                    }

                dialogBinding.etSubject.setAdapter(
                    ArrayAdapter(
                        this@ActivityClassSchedule,
                        android.R.layout.simple_dropdown_item_1line,
                        subjectLabels
                    )
                )

                dialogBinding.etSubject.isEnabled =
                    subjects.isNotEmpty()

                if (subjects.isEmpty()) {

                    dialogBinding.etSubject.setText(
                        ""
                    )

                    Toast.makeText(
                        this@ActivityClassSchedule,
                        "No subject assigned to this teacher for this semester/session.",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {

                Toast.makeText(
                    this@ActivityClassSchedule,
                    e.message
                        ?: "Failed to load assigned subjects.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // ============================================================
    // SAVE CLASS
    // ============================================================

    private fun saveClassFromDialog(
        dialogBinding:
        DialogAddClassScheduleBinding,
        dialog: AlertDialog
    ) {

        val teacher =
            selectedTeacher

        if (teacher == null) {

            showError("Please select a teacher.")
            return
        }

        if (teacher.teacherId.isBlank()) {

            showError(
                "Selected teacher does not have a valid Teacher ID."
            )

            return
        }

        // --------------------------------------------------------
        // Class name
        // --------------------------------------------------------

        val className =
            dialogBinding.etClassName
                .text
                .toString()
                .trim()

        if (className.isBlank()) {

            showError(
                "Please enter class name."
            )

            return
        }

        // --------------------------------------------------------
        // Subject
        // --------------------------------------------------------

        val subject =
            selectedSubject

        if (subject == null) {

            showError(
                "Please select an assigned subject."
            )

            return
        }

        // --------------------------------------------------------
        // Date
        // --------------------------------------------------------

        if (selectedDate.isBlank()) {

            showError(
                "Please select a date."
            )

            return
        }

        // --------------------------------------------------------
        // Start
        // --------------------------------------------------------

        if (selectedStartTime.isBlank()) {

            showError(
                "Please select start time."
            )

            return
        }

        // --------------------------------------------------------
        // End
        // --------------------------------------------------------

        if (selectedEndTime.isBlank()) {

            showError(
                "Please select end time."
            )

            return
        }

        if (
            !isEndTimeAfterStartTime(
                selectedStartTime,
                selectedEndTime
            )
        ) {

            showError(
                "End time must be after start time."
            )

            return
        }

        // --------------------------------------------------------
        // Semester + Session
        // --------------------------------------------------------

        val semester =
            Regex("\\d+")
                .find(
                    dialogBinding.etSemester
                        .text
                        .toString()
                )
                ?.value
                ?.toIntOrNull()

        if (semester == null) {

            showError(
                "Please select a valid semester."
            )

            return
        }

        val session =
            dialogBinding.etSession
                .text
                .toString()
                .trim()

        if (session.isBlank()) {

            showError(
                "Please enter session."
            )

            return
        }

        // --------------------------------------------------------
        // Optional fields
        // --------------------------------------------------------

        val roomNumber =
            dialogBinding.etRoom
                .text
                .toString()
                .trim()

        val note =
            dialogBinding.etNote
                .text
                .toString()
                .trim()

        // --------------------------------------------------------
        // Day
        // --------------------------------------------------------

        val dayName =
            try {

                SimpleDateFormat(
                    "EEEE",
                    Locale.US
                ).format(
                    dateFormat.parse(
                        selectedDate
                    )!!
                )

            } catch (_: Exception) {

                ""
            }

        // ========================================================
        // CLASS OBJECT
        // ========================================================

        val teacherName =
            teacher.fullName.ifBlank {
                teacher.email.ifBlank {
                    "Teacher"
                }
            }

        val classSchedule =
            ClassSchedule(

                // ========================================================
                // PRIMARY TEACHER ID
                // ========================================================

                teacherId =
                    teacher.teacherId,

                // ========================================================
                // TEACHER NAME
                // ========================================================

                teacherName =
                    teacherName,

                // ========================================================
                // OPTIONAL FIREBASE AUTH UID
                // ========================================================

                teacherAuthUid =
                    teacher.authUid,

                // ========================================================
                // CLASS
                // ========================================================

                className =
                    className,

                subjectId =
                    subject.subjectId,

                subjectName =
                    subject.subjectName,

                courseCode =
                    subject.courseCode,

                programName =
                    subject.programName,

                roomNumber =
                    roomNumber,

                // ========================================================
                // DATE
                // ========================================================

                date =
                    selectedDate,

                dayName =
                    dayName,

                // ========================================================
                // TIME
                // ========================================================

                startTime =
                    selectedStartTime,

                endTime =
                    selectedEndTime,

                // ========================================================
                // SEMESTER / SESSION / SECTION
                // ========================================================

                semester =
                    semester,

                session =
                    session,

                section =
                    className
                        .substringAfterLast(
                            " "
                        )
                        .trim(),

                periodType =
                    selectedPeriod,

                // ========================================================
                // NOTE
                // ========================================================

                note =
                    note,

                createdBy =
                    "Admin"
            )

        // ========================================================
        // FIRESTORE
        // ========================================================

        viewModel.saveClass(
            classSchedule
        )

        dialog.dismiss()
    }

    // ============================================================
    // DATE PICKER
    // ============================================================

    private fun showDatePicker(
        dialogBinding:
        DialogAddClassScheduleBinding
    ) {

        val calendar =
            Calendar.getInstance()

        val dialog =
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->

                    calendar.set(
                        Calendar.YEAR,
                        year
                    )

                    calendar.set(
                        Calendar.MONTH,
                        month
                    )

                    calendar.set(
                        Calendar.DAY_OF_MONTH,
                        dayOfMonth
                    )

                    selectedDate =
                        dateFormat.format(
                            calendar.time
                        )

                    dialogBinding.etDate.setText(
                        displayDateFormat.format(
                            calendar.time
                        )
                    )
                },

                calendar.get(
                    Calendar.YEAR
                ),

                calendar.get(
                    Calendar.MONTH
                ),

                calendar.get(
                    Calendar.DAY_OF_MONTH
                )
            )

        dialog.show()
    }

    // ============================================================
    // TIME PICKER
    // ============================================================

    private fun showTimePicker(
        isStartTime: Boolean,
        dialogBinding:
        DialogAddClassScheduleBinding
    ) {

        val calendar =
            Calendar.getInstance()

        val dialog =
            TimePickerDialog(
                this,
                { _, hour, minute ->

                    calendar.set(
                        Calendar.HOUR_OF_DAY,
                        hour
                    )

                    calendar.set(
                        Calendar.MINUTE,
                        minute
                    )

                    val formattedTime =
                        SimpleDateFormat(
                            "hh:mm a",
                            Locale.US
                        ).format(
                            calendar.time
                        )

                    if (isStartTime) {

                        selectedStartTime =
                            formattedTime

                        dialogBinding.etStartTime
                            .setText(
                                formattedTime
                            )

                    } else {

                        selectedEndTime =
                            formattedTime

                        dialogBinding.etEndTime
                            .setText(
                                formattedTime
                            )
                    }
                },

                calendar.get(
                    Calendar.HOUR_OF_DAY
                ),

                calendar.get(
                    Calendar.MINUTE
                ),

                false
            )

        dialog.show()
    }

    // ============================================================
    // TIME VALIDATION
    // ============================================================

    private fun isEndTimeAfterStartTime(
        start: String,
        end: String
    ): Boolean {

        return try {

            val format =
                SimpleDateFormat(
                    "hh:mm a",
                    Locale.US
                )

            val startDate =
                format.parse(start)

            val endDate =
                format.parse(end)

            startDate != null &&
                    endDate != null &&
                    endDate.after(startDate)

        } catch (_: Exception) {

            false
        }
    }

    // ============================================================
    // OBSERVE
    // ============================================================

    private fun observeViewModel() {

        viewModel.uiState.observe(
            this
        ) { state ->

            binding.progressBar.visibility =
                if (
                    state is
                            ClassScheduleViewModel.UiState.Loading
                ) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

            when (state) {

                is ClassScheduleViewModel.UiState.Error -> {

                    Toast.makeText(
                        this,
                        state.message,
                        Toast.LENGTH_LONG
                    ).show()
                }

                is ClassScheduleViewModel.UiState.Success -> {

                    Toast.makeText(
                        this,
                        "Class schedule saved successfully.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                else -> Unit
            }
        }

        viewModel.classes.observe(
            this
        ) { classes ->

            allLoadedClasses = classes

            renderFilteredClasses(
                classes
            )
        }
    }

    // ============================================================
    // FILTER + RENDER
    // ============================================================

    private fun renderFilteredClasses(
        classes: List<ClassSchedule>
    ) {

        val filteredClasses =
            when (selectedPeriod) {

                "Daily" -> {

                    val today =
                        dateFormat.format(
                            Calendar.getInstance().time
                        )

                    classes.filter {

                        it.periodType
                            .trim()
                            .equals(
                                "Daily",
                                ignoreCase = true
                            ) &&
                                it.date == today
                    }
                }

                "Weekly" -> {

                    getCurrentWeekRange()
                        .let { (startDate, endDate) ->

                            classes.filter { classSchedule ->

                                classSchedule.periodType
                                    .trim()
                                    .equals(
                                        "Weekly",
                                        ignoreCase = true
                                    ) &&

                                        classSchedule.date >= startDate &&
                                        classSchedule.date <= endDate
                            }
                        }
                }

                "Monthly" -> {

                    val calendar =
                        Calendar.getInstance()

                    val month =
                        calendar.get(
                            Calendar.MONTH
                        )

                    val year =
                        calendar.get(
                            Calendar.YEAR
                        )

                    classes.filter { classSchedule ->

                        try {

                            val date =
                                dateFormat.parse(
                                    classSchedule.date
                                )

                            if (date == null) {
                                false
                            } else {

                                val classCalendar =
                                    Calendar.getInstance()

                                classCalendar.time =
                                    date

                                classSchedule.periodType
                                    .trim()
                                    .equals(
                                        "Monthly",
                                        ignoreCase = true
                                    ) &&

                                        classCalendar.get(
                                            Calendar.MONTH
                                        ) == month &&

                                        classCalendar.get(
                                            Calendar.YEAR
                                        ) == year
                            }

                        } catch (_: Exception) {

                            false
                        }
                    }
                }

                "Semester" -> {

                    classes.filter {

                        it.periodType
                            .trim()
                            .equals(
                                "Semester",
                                ignoreCase = true
                            )
                    }
                }

                else ->
                    emptyList()
            }

        binding.tvClassCount.text =
            "${filteredClasses.size} Classes"

        binding.emptyState.visibility =
            if (filteredClasses.isEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }

        binding.recyclerClasses.visibility =
            if (filteredClasses.isEmpty()) {
                View.GONE
            } else {
                View.VISIBLE
            }

        binding.recyclerClasses.adapter =
            if (filteredClasses.isEmpty()) {
                null
            } else {

                ClassScheduleAdapter(

                    classes =
                        filteredClasses,

                    onDelete = { classSchedule ->

                        showDeleteConfirmation(
                            classSchedule
                        )
                    }
                )
            }
    }

    // ============================================================
    // WEEK RANGE HELPER
    // ============================================================

    private fun getCurrentWeekRange(): Pair<String, String> {

        val calendar =
            Calendar.getInstance()

        calendar.firstDayOfWeek =
            Calendar.MONDAY

        calendar.set(
            Calendar.DAY_OF_WEEK,
            Calendar.MONDAY
        )

        calendar.set(
            Calendar.HOUR_OF_DAY,
            0
        )

        calendar.set(
            Calendar.MINUTE,
            0
        )

        calendar.set(
            Calendar.SECOND,
            0
        )

        calendar.set(
            Calendar.MILLISECOND,
            0
        )

        val startDate =
            dateFormat.format(
                calendar.time
            )

        calendar.add(
            Calendar.DAY_OF_YEAR,
            6
        )

        val endDate =
            dateFormat.format(
                calendar.time
            )

        return Pair(
            startDate,
            endDate
        )
    }

    // ============================================================
    // DELETE
    // ============================================================

    private fun showDeleteConfirmation(
        classSchedule: ClassSchedule
    ) {

        AlertDialog.Builder(this)
            .setTitle("Delete Class")
            .setMessage(
                "Are you sure you want to delete " +
                        "${classSchedule.subjectName} " +
                        "from ${classSchedule.date}?"
            )
            .setPositiveButton(
                "Delete"
            ) { _, _ ->

                viewModel.deleteClass(
                    scheduleId =
                        classSchedule.scheduleId,

                    teacherId =
                        classSchedule.teacherId
                )
            }
            .setNegativeButton(
                "Cancel",
                null
            )
            .show()
    }

    // ============================================================
    // ERROR
    // ============================================================

    private fun showError(
        message: String
    ) {

        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }
}