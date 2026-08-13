package com.university.attendance

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Shared ViewModel for the Manual Update flow:
 *   Department -> Class -> Subject -> Date -> Register (toggle) -> Save
 */
class ManualAttendanceViewModel(
    private val repository: ManualAttendanceRepository = ManualAttendanceRepository(),
    private val attendanceRepository: AttendanceRepository = AttendanceRepository()
) : ViewModel() {

    // ---------- Selections carried across screens ----------
    var selectedDepartmentId: String = ""
    var selectedDepartmentName: String = ""
    var selectedClassId: String = ""
    var selectedClassTitle: String = ""
    var selectedProgramName: String = ""
    var selectedSemester: Int = 0
    var selectedSubjectId: String = ""
    var selectedSubjectName: String = ""
    var selectedCourseCode: String = ""
    var selectedDate: String = ""

    private val _uiState = MutableLiveData<UiState>(UiState.Idle)
    val uiState: LiveData<UiState> = _uiState

    private val _departments = MutableLiveData<List<Department>>(emptyList())
    val departments: LiveData<List<Department>> = _departments

    private val _classes = MutableLiveData<List<StudentClass>>(emptyList())
    val classes: LiveData<List<StudentClass>> = _classes

    private val _subjects = MutableLiveData<List<Subject>>(emptyList())
    val subjects: LiveData<List<Subject>> = _subjects

    private val _roster = MutableLiveData<List<AttendanceRosterRow>>(emptyList())
    val roster: LiveData<List<AttendanceRosterRow>> = _roster

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        object Loaded : UiState()
        data class SaveSuccess(val presentCount: Int, val totalCount: Int) : UiState()
        data class Error(val message: String) : UiState()
    }

    fun loadDepartments() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            try {
                _departments.value = attendanceRepository.getAllDepartments()
                _uiState.value = UiState.Loaded
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load departments.")
            }
        }
    }

    fun loadClasses(departmentName: String) {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            try {
                _classes.value = attendanceRepository.getClassesByDepartment(departmentName)
                _uiState.value = UiState.Loaded
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load classes.")
            }
        }
    }

    /**
     * Loads subjects offered for the selected class's department + program,
     * using the same "subjects" collection Subject Management writes to.
     */
    fun loadSubjects(departmentId: String, programName: String) {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val snapshot = FirebaseFirestore.getInstance().collection("subjects")
                    .whereEqualTo("departmentId", departmentId)
                    .whereEqualTo("programName", programName)
                    .get()
                    .await()
                _subjects.value = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Subject::class.java)?.apply { subjectId = doc.id }
                }.sortedBy { it.courseCode }
                _uiState.value = UiState.Loaded
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load subjects.")
            }
        }
    }

    /**
     * Loads the class roster for the currently selected Class + Subject +
     * Date, pre-filled with any existing attendance for that exact
     * combination (so editing an already-marked date shows correct state).
     */
    fun loadRoster() {
        if (selectedClassId.isBlank() || selectedSubjectId.isBlank() || selectedDate.isBlank()) {
            _uiState.value = UiState.Error("Please select Class, Subject, and Date first.")
            return
        }
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            try {
                _roster.value = repository.loadRoster(selectedClassId, selectedSubjectId, selectedDate)
                _uiState.value = UiState.Loaded
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load class roster.")
            }
        }
    }

    /** Toggles one student's Present/Absent state in the currently loaded roster. */
    fun toggleStudent(studentId: String) {
        val updated = _roster.value.orEmpty().map { row ->
            if (row.student.studentId == studentId) {
                AttendanceRosterRow(student = row.student, isPresent = !row.isPresent)
            } else {
                row
            }
        }
        _roster.value = updated
    }

    /** Marks everyone in the roster Present in one tap (common "whole class present" shortcut). */
    fun markAllPresent() {
        _roster.value = _roster.value.orEmpty().map { AttendanceRosterRow(it.student, true) }
    }

    /** Marks everyone Absent in one tap. */
    fun markAllAbsent() {
        _roster.value = _roster.value.orEmpty().map { AttendanceRosterRow(it.student, false) }
    }

    fun saveRoster(teacherName: String) {
        val currentRoster = _roster.value.orEmpty()
        if (currentRoster.isEmpty()) {
            _uiState.value = UiState.Error("No students to save.")
            return
        }

        _uiState.value = UiState.Loading
        viewModelScope.launch {
            when (val result = repository.saveRoster(
                rows = currentRoster,
                subjectId = selectedSubjectId,
                subjectName = selectedSubjectName,
                courseCode = selectedCourseCode,
                teacherName = teacherName,
                classId = selectedClassId,
                departmentId = selectedDepartmentId,
                departmentName = selectedDepartmentName,
                date = selectedDate
            )) {
                is ManualAttendanceRepository.OpResult.Success -> {
                    val presentCount = currentRoster.count { it.isPresent }
                    _uiState.value = UiState.SaveSuccess(presentCount, currentRoster.size)
                }
                is ManualAttendanceRepository.OpResult.Error ->
                    _uiState.value = UiState.Error(result.message)
            }
        }
    }
}