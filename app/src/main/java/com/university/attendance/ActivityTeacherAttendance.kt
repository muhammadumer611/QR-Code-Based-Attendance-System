package com.university.attendance

import android.graphics.Bitmap
import android.graphics.Color
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.university.attendance.databinding.ActivityTeacherAttendanceBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ActivityTeacherAttendance : AppCompatActivity() {

    private lateinit var binding:
            ActivityTeacherAttendanceBinding

    private lateinit var historyAdapter:
            TeacherAttendanceHistoryAdapter

    private var attendanceListener:
            com.google.firebase.firestore.ListenerRegistration? =
        null

    private val repository =
        TeacherAttendanceRepository()

    private lateinit var bleManager:
            BleAttendanceManager

    private var pendingBleStart = false

    private val blePermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            if (result.values.all { it }) {
                ensureBluetoothEnabledAndStart()
            } else {
                pendingBleStart = false
                Toast.makeText(
                    this,
                    "Bluetooth permission is required to start classroom attendance.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    private val bluetoothEnableLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (bleManager.isBluetoothEnabled()) {
                pendingBleStart = false
                startAttendanceSession()
            } else {
                pendingBleStart = false
                Toast.makeText(
                    this,
                    "Bluetooth must be turned on to start classroom attendance.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    private var scheduleId = ""
    private var subjectId = ""
    private var subjectName = ""

    private var date = ""
    private var courseCode = ""
    private var className = ""
    private var classId = ""
    private var departmentName = ""
    private var programName = ""
    private var semester = 1
    private var session = ""
    private var section = ""
    private var teacherId = ""
    private var teacherName = ""
    private var startTime = ""
    private var endTime = ""
    private var roomNumber = 0
    private var periodType = ""
    private var dayName = ""

    private var currentSession:
            AttendanceSession? = null

    private val handler =
        Handler(Looper.getMainLooper())

    private val timerRunnable =
        object : Runnable {

            override fun run() {

                updateCountdown()

                if (
                    currentSession != null
                ) {
                    handler.postDelayed(
                        this,
                        1000
                    )
                }
            }
        }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        binding =
            ActivityTeacherAttendanceBinding
                .inflate(layoutInflater)

        setContentView(binding.root)

        bleManager =
            BleAttendanceManager(this)

        readIntent()

        setupUi()

        setupAttendanceHistory()

        loadRoster()

        updateInitialUi()
    }

    private fun readIntent() {

        scheduleId =
            intent.getStringExtra(
                "scheduleId"
            ).orEmpty()


        subjectId =
            intent.getStringExtra(
                "subjectId"
            ).orEmpty()

        subjectName =
            intent.getStringExtra(
                "subjectName"
            ).orEmpty()

        courseCode =
            intent.getStringExtra(
                "courseCode"
            ).orEmpty()

        className =
            intent.getStringExtra(
                "className"
            ).orEmpty()

        classId =
            intent.getStringExtra(
                "classId"
            ).orEmpty()

        departmentName =
            intent.getStringExtra(
                "departmentName"
            ).orEmpty()

        programName =
            intent.getStringExtra(
                "programName"
            ).orEmpty()

        semester =
            intent.getIntExtra(
                "semester",
                1
            )

        session =
            intent.getStringExtra(
                "session"
            ).orEmpty()

        section =
            intent.getStringExtra(
                "section"
            ).orEmpty()

        teacherId =
            intent.getStringExtra(
                "teacherId"
            ).orEmpty()

        teacherName =
            intent.getStringExtra(
                "teacherName"
            ).orEmpty()

        startTime =
            intent.getStringExtra(
                "startTime"
            ).orEmpty()

        endTime =
            intent.getStringExtra(
                "endTime"
            ).orEmpty()

        roomNumber =
            intent.getIntExtra(
                "roomNumber",
                0
            )

        periodType =
            intent.getStringExtra(
                "periodType"
            ).orEmpty()

        dayName =
            intent.getStringExtra(
                "dayName"
            ).orEmpty()
        date =
            intent.getStringExtra(
                "date"
            ).orEmpty()
    }

    private fun setupUi() {

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnGenerateQr.setOnClickListener {
            generateQr()
        }

        binding.btnEndAttendance.setOnClickListener {
            endAttendance()
        }
    }
    private fun setupAttendanceHistory() {

        historyAdapter =
            TeacherAttendanceHistoryAdapter()

        binding.recyclerAttendanceHistory
            .layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(
                this
            )

        binding.recyclerAttendanceHistory.adapter =
            historyAdapter

        binding.recyclerAttendanceHistory
            .isNestedScrollingEnabled =
            false
    }
    private fun startAttendanceHistoryListener(
        sessionId: String
    ) {

        attendanceListener?.remove()

        attendanceListener =
            repository.listenToSessionAttendance(

                sessionId =
                    sessionId,

                onChanged = { records ->

                    runOnUiThread {

                        historyAdapter.updateData(
                            records
                        )

                        binding.tvPresentCount.text =
                            records.size.toString()

                        if (
                            records.isEmpty()
                        ) {

                            binding.tvNoAttendance.visibility =
                                View.VISIBLE

                            binding.recyclerAttendanceHistory
                                .visibility =
                                View.GONE

                        } else {

                            binding.tvNoAttendance.visibility =
                                View.GONE

                            binding.recyclerAttendanceHistory
                                .visibility =
                                View.VISIBLE
                        }
                    }
                },

                onError = { error ->

                    runOnUiThread {

                        Toast.makeText(
                            this,
                            error.message
                                ?: "Unable to load attendance history.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
    }
    private fun updateInitialUi() {

        binding.tvSubjectName.text =
            if (courseCode.isNotBlank()) {
                "$courseCode • $subjectName"
            } else {
                subjectName.ifBlank {
                    "Subject"
                }
            }

        binding.tvClassInfo.text =
            buildString {

                if (programName.isNotBlank()) {
                    append(programName)
                }

                if (semester > 0) {
                    if (isNotEmpty()) append(" • ")
                    append("Semester $semester")
                }

                if (section.isNotBlank()) {
                    append(" • Section $section")
                }
            }

        binding.tvTimeInfo.text =
            buildString {

                if (
                    startTime.isNotBlank() &&
                    endTime.isNotBlank()
                ) {
                    append(
                        "$startTime - $endTime"
                    )
                }

                if (roomNumber > 0) {
                    if (isNotEmpty()) append(" • ")
                    append("Room $roomNumber")
                }
            }

        binding.imgQrCode.visibility =
            View.INVISIBLE

        binding.tvSessionStatus.text =
            "Attendance not started"

        binding.tvExpiry.text =
            "Start live attendance"
    }

    private fun loadRoster() {

        lifecycleScope.launch {

            try {

                var resolvedClassId =
                    classId

                if (
                    resolvedClassId.isBlank()
                ) {

                    resolvedClassId =
                        ClassUtils.buildClassId(
                            universityName =
                                "University Of Lahore",

                            departmentName =
                                departmentName,

                            programName =
                                programName,

                            session =
                                session,

                            section =
                                section
                        ,
                            semester = semester
                        )
                }

                classId =
                    resolvedClassId

                val count =
                    repository.getRosterCount(
                        classId
                    )

                binding.tvTotalStudents.text =
                    count.toString()

            } catch (e: Exception) {

                binding.tvTotalStudents.text =
                    "0"
            }
        }
    }
//
//    private fun generateQr() {
//
//        if (scheduleId.isBlank()) {
//
//            Toast.makeText(
//                this,
//                "Invalid class schedule.",
//                Toast.LENGTH_LONG
//            ).show()
//
//            return
//        }
//
//        binding.btnGenerateQr.isEnabled =
//            false
//
//        lifecycleScope.launch {
//
//            try {
//
//                val schedule =
//                    ClassSchedule(
//
//                        scheduleId =
//                            scheduleId,
//
//                        teacherId =
//                            teacherId,
//
//                        teacherName =
//                            teacherName,
//
//                        classId =
//                            classId,
//
//                        departmentName =
//                            departmentName,
//
//                        className =
//                            className,
//
//                        subjectId =
//                            subjectId,
//
//                        subjectName =
//                            subjectName,
//
//                        courseCode =
//                            courseCode,
//
//                        roomNumber =
//                            roomNumber,
//
//                        programName =
//                            programName,
//
//                        semester =
//                            semester,
//
//                        session =
//                            session,
//
//                        section =
//                            section,
//
//                        startTime =
//                            startTime,
//
//                        endTime =
//                            endTime,
//
//                        periodType =
//                            periodType,
//
//                        dayName =
//                            dayName
//                    )
//
//                val created =
//                    repository.createSession(
//                        schedule
//                    )
//
//                currentSession =
//                    created
//
//                showQr(
//                    created.qrPayload
//                )
//
//                binding.btnGenerateQr.text =
//                    "Attendance Live • Reopen QR"
//
//                binding.btnGenerateQr.isEnabled =
//                    true
//
//                binding.btnEndAttendance.visibility =
//                    View.VISIBLE
//
//                binding.tvSessionStatus.text =
//                    "Attendance is ACTIVE"
//
//                binding.tvSessionStatus.setTextColor(
//                    getColor(
//                        R.color.status_present
//                    )
//                )
//
//                handler.removeCallbacks(
//                    timerRunnable
//                )
//
//                handler.post(
//                    timerRunnable
//                )
//
//                updatePresentCount()
//
//                Toast.makeText(
//                    this@ActivityTeacherAttendance,
//                    "Live attendance started. Students in this class will now see Save Attendance.",
//                    Toast.LENGTH_SHORT
//                ).show()
//
//            } catch (e: Exception) {
//
//                binding.btnGenerateQr.isEnabled =
//                    true
//
//                Toast.makeText(
//                    this@ActivityTeacherAttendance,
//                    e.message
//                        ?: "Unable to generate QR.",
//                    Toast.LENGTH_LONG
//                ).show()
//            }
//        }
//    }
private fun generateQr() {

    if (!bleManager.isBluetoothSupported()) {
        Toast.makeText(
            this,
            "This phone does not support Bluetooth Low Energy.",
            Toast.LENGTH_LONG
        ).show()
        return
    }

    if (!bleManager.hasAdvertisePermissions()) {
        pendingBleStart = true
        blePermissionLauncher.launch(
            BleAttendanceManager.advertisePermissions()
        )
        return
    }

    ensureBluetoothEnabledAndStart()
}

private fun ensureBluetoothEnabledAndStart() {

    if (!bleManager.hasAdvertisePermissions()) {
        pendingBleStart = false
        return
    }

    if (!bleManager.isBluetoothEnabled()) {
        pendingBleStart = true
        try {
            bluetoothEnableLauncher.launch(
                Intent(
                    android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE
                )
            )
        } catch (_: Exception) {
            pendingBleStart = false
            Toast.makeText(
                this,
                "Please turn Bluetooth on and try again.",
                Toast.LENGTH_LONG
            ).show()
        }
        return
    }

    pendingBleStart = false
    startAttendanceSession()
}

private fun startAttendanceSession() {

    if (scheduleId.isBlank()) {

        Toast.makeText(
            this,
            "Invalid class schedule.",
            Toast.LENGTH_LONG
        ).show()

        return
    }

    binding.btnGenerateQr.isEnabled =
        false

    lifecycleScope.launch {

        try {

            /*
             * IMPORTANT:
             *
             * Do NOT construct ClassSchedule from Intent extras.
             *
             * Read the real schedule from Firestore using scheduleId.
             *
             * This fixes date/day/time mismatches.
             */
            val actualSchedule =
                repository.getSchedule(
                    scheduleId
                )

            /*
             * Keep local UI values synchronized
             * with the actual Firestore schedule.
             */
            subjectId =
                actualSchedule.subjectId

            subjectName =
                actualSchedule.subjectName

            courseCode =
                actualSchedule.courseCode

            className =
                actualSchedule.className

            classId =
                actualSchedule.classId

            departmentName =
                actualSchedule.departmentName

            programName =
                actualSchedule.programName

            semester =
                actualSchedule.semester

            session =
                actualSchedule.session

            section =
                actualSchedule.section

            teacherId =
                actualSchedule.teacherId

            teacherName =
                actualSchedule.teacherName

            startTime =
                actualSchedule.startTime

            endTime =
                actualSchedule.endTime

            roomNumber =
                actualSchedule.roomNumber

            periodType =
                actualSchedule.periodType

            dayName =
                actualSchedule.dayName


            val created =
                repository.createSession(
                    actualSchedule
                )

            if (created.bleToken.isBlank()) {
                throw IllegalStateException(
                    "Unable to create the classroom Bluetooth token."
                )
            }

            bleManager.startAdvertising(
                token = created.bleToken,
                onStarted = {
                    runOnUiThread {
                        currentSession =
                            created

                        startAttendanceHistoryListener(
                            created.sessionId
                        )

                        showQr(
                            created.qrPayload
                        )

                        binding.btnGenerateQr.text =
                            "Attendance Live • Reopen QR"

                        binding.btnGenerateQr.isEnabled =
                            true

                        binding.btnEndAttendance.visibility =
                            View.VISIBLE

                        binding.tvSessionStatus.text =
                            "Attendance ACTIVE • Bluetooth proximity ON"

                        binding.tvSessionStatus.setTextColor(
                            getColor(
                                R.color.status_present
                            )
                        )

                        handler.removeCallbacks(
                            timerRunnable
                        )

                        handler.post(
                            timerRunnable
                        )

                        updatePresentCount()

                        Toast.makeText(
                            this@ActivityTeacherAttendance,
                            "Attendance started. Students must be near the teacher phone.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onError = { error ->
                    lifecycleScope.launch {
                        try {
                            repository.endSession(
                                created.sessionId
                            )
                        } catch (_: Exception) {
                        }

                        binding.btnGenerateQr.isEnabled =
                            true

                        Toast.makeText(
                            this@ActivityTeacherAttendance,
                            error,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            )

        } catch (
            e: Exception
        ) {

            binding.btnGenerateQr.isEnabled =
                true

            Toast.makeText(
                this@ActivityTeacherAttendance,
                e.message
                    ?: "Unable to generate QR.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}

    private fun showQr(
        payload: String
    ) {

        try {

            val matrix =
                MultiFormatWriter()
                    .encode(
                        payload,
                        BarcodeFormat.QR_CODE,
                        800,
                        800
                    )

            val width =
                matrix.width

            val height =
                matrix.height

            val bitmap =
                Bitmap.createBitmap(
                    width,
                    height,
                    Bitmap.Config.RGB_565
                )

            for (x in 0 until width) {
                for (y in 0 until height) {

                    bitmap.setPixel(
                        x,
                        y,
                        if (
                            matrix.get(
                                x,
                                y
                            )
                        ) {
                            Color.BLACK
                        } else {
                            Color.WHITE
                        }
                    )
                }
            }

            binding.imgQrCode.setImageBitmap(
                bitmap
            )

            binding.imgQrCode.visibility =
                View.VISIBLE

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Unable to create QR code.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun updateCountdown() {

        val session =
            currentSession
                ?: return

        val expiry =
            session.expiresAt
                ?: return

        val remaining =
            expiry.time -
                    System.currentTimeMillis()

        if (remaining <= 0) {

            binding.tvExpiry.text =
                "Attendance session expired"

            binding.tvSessionStatus.text =
                "Attendance EXPIRED"

            binding.btnEndAttendance.visibility =
                View.GONE

            lifecycleScope.launch {
                try {
                    repository.endSession(
                        session.sessionId
                    )

                    bleManager.stopAdvertising()
                } catch (_: Exception) {
                }
            }

            currentSession =
                null

            handler.removeCallbacks(
                timerRunnable
            )

            return
        }

        val totalSeconds =
            remaining / 1000

        val minutes =
            totalSeconds / 60

        val seconds =
            totalSeconds % 60

        binding.tvExpiry.text =
            String.format(
                Locale.US,
                "Expires in %02d:%02d",
                minutes,
                seconds
            )

        updatePresentCount()
    }
    private fun updatePresentCount() {
        // Present count is updated automatically
        // by the real-time attendance listener.
    }
//
//    private fun updatePresentCount() {
//
//        val session =
//            currentSession
//                ?: return
//
//        lifecycleScope.launch {
//
//            try {
//
//                val count =
//                    repository.getPresentCount(
//                        session.sessionId
//                    )
//
//                binding.tvPresentCount.text =
//                    count.toString()
//
//            } catch (_: Exception) {
//            }
//        }
//    }

    private fun endAttendance() {

        val session =
            currentSession
                ?: return

        lifecycleScope.launch {

            try {

                repository.endSession(
                    session.sessionId
                )

                bleManager.stopAdvertising()

                attendanceListener?.remove()

                attendanceListener =
                    null

                currentSession =
                    null

                handler.removeCallbacks(
                    timerRunnable
                )

                binding.tvSessionStatus.text =
                    "Attendance ended"

                binding.tvExpiry.text =
                    "Session closed"

                binding.btnEndAttendance.visibility =
                    View.GONE

                binding.imgQrCode.visibility =
                    View.INVISIBLE

                Toast.makeText(
                    this@ActivityTeacherAttendance,
                    "Attendance session ended.",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {

                Toast.makeText(
                    this@ActivityTeacherAttendance,
                    e.message
                        ?: "Unable to end attendance.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    override fun onDestroy() {

        bleManager.stopAdvertising()

        attendanceListener?.remove()

        attendanceListener =
            null

        handler.removeCallbacks(
            timerRunnable
        )

        super.onDestroy()
    }
//
//    override fun onDestroy() {
//
//        handler.removeCallbacks(
//            timerRunnable
//        )
//
//        super.onDestroy()
//    }
}