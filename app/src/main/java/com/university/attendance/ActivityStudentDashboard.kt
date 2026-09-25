package com.university.attendance

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.university.attendance.databinding.ActivityStudentDashboardBinding
import kotlinx.coroutines.launch

class ActivityStudentDashboard :
    AppCompatActivity() {

    private lateinit var binding:
            ActivityStudentDashboardBinding

    private val attendanceSummaryRepository =
        StudentAttendanceSummaryRepository()

    private val repository =
        StudentAttendanceRepository()

    private lateinit var attendanceAdapter:
            ActiveAttendanceAdapter

    private lateinit var scheduleAdapter:
            StudentTodayClassAdapter

    private var listener:
            ListenerRegistration? = null

    private var currentStudent:
            Student? = null

    private lateinit var bleManager:
            BleAttendanceManager

    private var activeSessions:
            List<AttendanceSession> = emptyList()

    private var nearbySessionIds:
            Set<String> = emptySet()

    private val SINGLE_DEVICE_TEST_MODE = true


    private val blePermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            if (result.values.all { it }) {
                ensureBluetoothEnabledAndScan()
            } else {
                Toast.makeText(
                    this,
                    "Bluetooth permission is required to verify classroom proximity.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    private val bluetoothEnableLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (bleManager.isBluetoothEnabled()) {
                startBleScanning()
            } else {
                Toast.makeText(
                    this,
                    "Turn Bluetooth on to mark classroom attendance.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }


    // ============================================================
    // ON CREATE
    // ============================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )


        binding =
            ActivityStudentDashboardBinding
                .inflate(
                    layoutInflater
                )

        setContentView(
            binding.root
        )

        bleManager =
            BleAttendanceManager(this)


        // ========================================================
        // ATTENDANCE HISTORY
        // ========================================================

        binding.cardAttendanceHistory
            .setOnClickListener {

                startActivity(
                    Intent(
                        this,
                        ActivityStudentAttendanceHistory::class.java
                    )
                )
            }


        // ========================================================
        // ACTIVE ATTENDANCE ADAPTER
        // ========================================================

        attendanceAdapter =
            ActiveAttendanceAdapter { session ->

                saveAttendance(
                    session
                )
            }


        // ========================================================
        // TODAY'S CLASS ADAPTER
        // ========================================================

        scheduleAdapter =
            StudentTodayClassAdapter()


        // ========================================================
        // ACTIVE ATTENDANCE RECYCLER
        // ========================================================

        binding.rvActiveAttendance
            .layoutManager =
            LinearLayoutManager(
                this
            )

        binding.rvActiveAttendance
            .adapter =
            attendanceAdapter


        // ========================================================
        // TODAY CLASS RECYCLER
        // ========================================================

        binding.rvTodayClasses
            .layoutManager =
            LinearLayoutManager(
                this
            )

        binding.rvTodayClasses
            .adapter =
            scheduleAdapter


        // ========================================================
        // NOTIFICATIONS
        // ========================================================

        binding.btnNotifications
            .setOnClickListener {

                startActivity(
                    Intent(
                        this,
                        ActivityNotifications::class.java
                    )
                )
            }


        // ========================================================
        // QR SCANNER
        // ========================================================
        //
        // Current project uses live attendance sessions.
        // Therefore the old standalone QR scanner button
        // remains hidden.
        //

        binding.btnScanQr
            .visibility =
            View.GONE

        binding.tvScanHint
            .visibility =
            View.GONE


        // ========================================================
        // LOAD STUDENT
        // ========================================================

        loadStudent()
    }


    // ============================================================
    // LOAD CURRENT STUDENT
    // ============================================================

    private fun loadStudent() {

        // --------------------------------------------------------
        // AUTH CHECK FIRST
        // --------------------------------------------------------

        val firebaseUser =
            FirebaseAuth
                .getInstance()
                .currentUser


        if (
            firebaseUser == null
        ) {

            finish()

            return
        }


        // --------------------------------------------------------
        // LOAD FROM FIRESTORE
        // --------------------------------------------------------

        lifecycleScope.launch {

            try {

                val student =
                    repository
                        .getCurrentStudent()


                // ------------------------------------------------
                // SAVE CURRENT STUDENT
                // ------------------------------------------------

                currentStudent =
                    student


                StudentSession.save(
                    this@ActivityStudentDashboard,
                    student
                )


                // ------------------------------------------------
                // STUDENT NAME
                // ------------------------------------------------

                binding.tvStudentName.text =
                    student.fullName
                        .ifBlank {
                            "Student"
                        }


                // ------------------------------------------------
                // STUDENT META
                // ------------------------------------------------

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
                            student.section
                                .isNotBlank()
                        ) {

                            append(
                                " • Section ${student.section}"
                            )
                        }


                        if (
                            student.regNo
                                .isNotBlank()
                        ) {

                            append(
                                " • ${student.regNo}"
                            )
                        }
                    }


                // ------------------------------------------------
                // NEXT / CURRENT CLASS TEXT
                // ------------------------------------------------

                binding.tvNextClass.text =
                    "Class: " +
                            "${student.programName} • " +
                            "${student.session}-" +
                            "${student.section}"


                // ------------------------------------------------
                // TODAY'S SCHEDULE
                // ------------------------------------------------

                loadTodaySchedule(
                    student
                )


                // ------------------------------------------------
                // LIVE ATTENDANCE
                // ------------------------------------------------

                startLiveListener(
                    student
                )


                // ------------------------------------------------
                // ATTENDANCE WARNING
                // ------------------------------------------------
                //
                // IMPORTANT:
                // Student object now exists.
                //
                // This was the bug in your old code where
                // loadAttendanceWarning(student) was called
                // before "student" was declared.
                //

                loadAttendanceWarning(
                    student
                )

                ensureBluetoothAndStartScanning()

            } catch (
                e: Exception
            ) {

                Toast.makeText(
                    this@ActivityStudentDashboard,
                    e.message
                        ?: "Unable to load student profile.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    // ============================================================
    // ATTENDANCE WARNING
    // ============================================================

    private fun loadAttendanceWarning(
        student: Student
    ) {

        lifecycleScope.launch {

            try {

                val lowSubjects =
                    attendanceSummaryRepository
                        .getLowAttendanceSubjects(
                            student
                        )


                // ------------------------------------------------
                // NO LOW ATTENDANCE
                // ------------------------------------------------

                if (
                    lowSubjects.isEmpty()
                ) {

                    binding.tvAttendanceWarning
                        .visibility =
                        View.GONE

                    return@launch
                }


                // ------------------------------------------------
                // SHOW WARNING
                // ------------------------------------------------

                binding.tvAttendanceWarning
                    .visibility =
                    View.VISIBLE


                binding.tvAttendanceWarning.text =
                    buildString {

                        append(
                            "⚠ Attendance Warning\n\n"
                        )

                        append(
                            "Your attendance is 75% or below in:"
                        )


                        lowSubjects.forEach { subject ->

                            append(
                                "\n\n• "
                            )

                            append(
                                subject.subjectName
                                    .ifBlank {
                                        "Unknown Subject"
                                    }
                            )

                            append(
                                ": "
                            )

                            append(
                                "${subject.percentage}%"
                            )
                        }


                        append(
                            "\n\nPlease maintain regular attendance."
                        )
                    }

            } catch (
                _: Exception
            ) {

                // Do not disturb the student dashboard if
                // attendance summary temporarily fails.

                binding.tvAttendanceWarning
                    .visibility =
                    View.GONE
            }
        }
    }


    // ============================================================
    // TODAY'S SCHEDULE
    // ============================================================

    private fun loadTodaySchedule(
        student: Student
    ) {

        lifecycleScope.launch {

            try {

                val list =
                    repository
                        .getTodayScheduleForStudent(
                            student
                        )


                scheduleAdapter
                    .submitList(
                        list
                    )


                val empty =
                    list.isEmpty()


                // ------------------------------------------------
                // RECYCLER
                // ------------------------------------------------

                binding.rvTodayClasses
                    .visibility =
                    if (empty) {
                        View.GONE
                    } else {
                        View.VISIBLE
                    }


                // ------------------------------------------------
                // EMPTY MESSAGE
                // ------------------------------------------------

                binding.tvTodayClassesEmpty
                    .visibility =
                    if (empty) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }

            } catch (
                e: Exception
            ) {

                Toast.makeText(
                    this@ActivityStudentDashboard,
                    e.message
                        ?: "Unable to load today's classes.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    // ============================================================
    // LIVE ATTENDANCE LISTENER
    // ============================================================

    private fun startLiveListener(
        student: Student
    ) {

        // Remove old listener first.
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

                        } catch (
                            e: Exception
                        ) {

                            Toast.makeText(
                                this@ActivityStudentDashboard,
                                e.message
                                    ?: "Unable to load eligible attendance.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                },

                { error ->

                    runOnUiThread {

                        Toast.makeText(
                            this,
                            error.message
                                ?: "Attendance listener failed.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            )
    }


    // ============================================================
    // RENDER ACTIVE ATTENDANCE
    // ============================================================

    private fun renderSessions(
        sessions:
        List<AttendanceSession>
    ) {

        activeSessions =
            sessions

        attendanceAdapter
            .submitList(
                sessions
            )

        nearbySessionIds =
            nearbySessionIds
                .filter { id ->
                    sessions.any {
                        it.sessionId == id
                    }
                }
                .toSet()

        attendanceAdapter
            .setNearbySessionIds(
                nearbySessionIds
            )

        bleManager.updateTargetTokens(
            sessions
                .mapNotNull { session ->
                    session.bleToken
                        .takeIf { it.isNotBlank() }
                }
                .toSet()
        )


        val empty =
            sessions.isEmpty()


        // --------------------------------------------------------
        // ACTIVE ATTENDANCE RECYCLER
        // --------------------------------------------------------

        binding.rvActiveAttendance
            .visibility =
            if (empty) {
                View.GONE
            } else {
                View.VISIBLE
            }


        // --------------------------------------------------------
        // EMPTY TITLE
        // --------------------------------------------------------
        //
        // Keep title visible because the layout uses it as
        // the section heading.
        //

        binding.tvLiveAttendanceEmptyTitle
            .visibility =
            View.VISIBLE


        // --------------------------------------------------------
        // EMPTY MESSAGE
        // --------------------------------------------------------

        binding.tvLiveAttendanceEmpty
            .visibility =
            if (empty) {
                View.VISIBLE
            } else {
                View.GONE
            }


        // --------------------------------------------------------
        // TODAY STATUS
        // --------------------------------------------------------

        binding.tvTodayStatusValue.text =
            if (empty) {
                "No Live Attendance"
            } else {
                "Attendance Available"
            }
    }


    // ============================================================
    // SAVE / MARK ATTENDANCE
    // ============================================================

//    private fun saveAttendance(
//        session:
//        AttendanceSession
//    ) {
//
//        val student =
//            currentStudent
//                ?: return
//
//
//        lifecycleScope.launch {
//
//            try {
//
//                if (!nearbySessionIds.contains(session.sessionId)) {
//                    throw IllegalStateException(
//                        "Teacher Bluetooth signal is not detected nearby."
//                    )
//                }
//
//                repository.markAttendance(
//                    session.sessionId,
//                    student,
//                    session.bleToken
//                )
//
//
//                Toast.makeText(
//                    this@ActivityStudentDashboard,
//                    "Attendance marked successfully.",
//                    Toast.LENGTH_SHORT
//                ).show()
//
//
//                // ------------------------------------------------
//                // REFRESH ACTIVE SESSIONS
//                // ------------------------------------------------
//
//                renderSessions(
//                    repository
//                        .getActiveSessionsForStudent(
//                            student
//                        )
//                )
//
//
//            } catch (
//                e: Exception
//            ) {
//
//                Toast.makeText(
//                    this@ActivityStudentDashboard,
//                    e.message
//                        ?: "Attendance could not be marked.",
//                    Toast.LENGTH_LONG
//                ).show()
//
//
//                // ------------------------------------------------
//                // REFRESH EVEN AFTER ERROR
//                // ------------------------------------------------
//
//                try {
//
//                    renderSessions(
//                        repository
//                            .getActiveSessionsForStudent(
//                                student
//                            )
//                    )
//
//                } catch (
//                    _: Exception
//                ) {
//                    // Ignore secondary refresh error.
//                }
//            }
//        }
//    }
//
private fun saveAttendance(
    session: AttendanceSession
) {

    val student =
        currentStudent
            ?: return

    lifecycleScope.launch {

        try {

            // ====================================================
            // BLE PROXIMITY CHECK
            // ====================================================
            //
            // Real mode:
            // Student must detect teacher BLE nearby.
            //
            // Single-device test mode:
            // BLE proximity is skipped so you can test
            // attendance using only one phone.
            //

            if (!SINGLE_DEVICE_TEST_MODE) {

                if (
                    !nearbySessionIds.contains(
                        session.sessionId
                    )
                ) {

                    throw IllegalStateException(
                        "Teacher Bluetooth signal is not detected nearby."
                    )
                }
            }


            // ====================================================
            // MARK ATTENDANCE
            // ====================================================

            repository.markAttendance(
                session.sessionId,
                student,
                session.bleToken
            )


            Toast.makeText(
                this@ActivityStudentDashboard,
                if (SINGLE_DEVICE_TEST_MODE) {
                    "Attendance marked successfully. (Test Mode)"
                } else {
                    "Attendance marked successfully."
                },
                Toast.LENGTH_SHORT
            ).show()


            // ====================================================
            // REFRESH ACTIVE SESSIONS
            // ====================================================

            renderSessions(
                repository
                    .getActiveSessionsForStudent(
                        student
                    )
            )

        } catch (
            e: Exception
        ) {

            Toast.makeText(
                this@ActivityStudentDashboard,
                e.message
                    ?: "Attendance could not be marked.",
                Toast.LENGTH_LONG
            ).show()


            // ====================================================
            // REFRESH AFTER ERROR
            // ====================================================

            try {

                renderSessions(
                    repository
                        .getActiveSessionsForStudent(
                            student
                        )
                )

            } catch (
                _: Exception
            ) {
                // Ignore secondary refresh error.
            }
        }
    }
}

    // ============================================================
    // BLE CLASSROOM PROXIMITY
    // ============================================================

    private fun ensureBluetoothAndStartScanning() {

        if (!bleManager.isBluetoothSupported()) {
            Toast.makeText(
                this,
                "This phone does not support Bluetooth Low Energy.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (!bleManager.hasScanPermissions()) {
            blePermissionLauncher.launch(
                BleAttendanceManager.scanPermissions()
            )
            return
        }

        ensureBluetoothEnabledAndScan()
    }

    private fun ensureBluetoothEnabledAndScan() {

        if (!bleManager.hasScanPermissions()) {
            return
        }

        if (!bleManager.isBluetoothEnabled()) {
            try {
                bluetoothEnableLauncher.launch(
                    Intent(
                        android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE
                    )
                )
            } catch (_: Exception) {
                Toast.makeText(
                    this,
                    "Please turn Bluetooth on and try again.",
                    Toast.LENGTH_LONG
                ).show()
            }
            return
        }

        startBleScanning()
    }

    private fun startBleScanning() {

        bleManager.startScanning(
            sessionTokens =
                activeSessions
                    .mapNotNull { session ->
                        session.bleToken
                            .takeIf { it.isNotBlank() }
                    }
                    .toSet(),

            onNearbyTokensChanged = { tokens ->
                runOnUiThread {
                    nearbySessionIds =
                        activeSessions
                            .filter { session ->
                                session.bleToken in tokens
                            }
                            .map { it.sessionId }
                            .toSet()

                    attendanceAdapter
                        .setNearbySessionIds(
                            nearbySessionIds
                        )
                }
            },

            onError = { error ->
                runOnUiThread {
                    Toast.makeText(
                        this,
                        error,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
    }


    // ============================================================
    // ON DESTROY
    // ============================================================

    override fun onDestroy() {

        bleManager.stopScanning()

        listener?.remove()

        listener =
            null

        super.onDestroy()
    }
}