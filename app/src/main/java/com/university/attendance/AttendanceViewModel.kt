package com.university.attendance

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/**
 * Shared ViewModel across the Attendance drill-down flow:
 *   Department List -> Class List -> Student List -> Subject Attendance
 *
 * Each screen calls the loadX() function relevant to it; results land in
 * the matching LiveData. Using one ViewModel (scoped per-Activity) keeps
 * things simple since each screen is a separate Activity in this flow.
 */
class AttendanceViewModel(
    private val repository: AttendanceRepository = AttendanceRepository()
) : ViewModel() {

    private val _uiState = MutableLiveData<UiState>(UiState.Idle)
    val uiState: LiveData<UiState> = _uiState

    private val _departments = MutableLiveData<List<Department>>(emptyList())
    val departments: LiveData<List<Department>> = _departments

    private val _classes = MutableLiveData<List<StudentClass>>(emptyList())
    val classes: LiveData<List<StudentClass>> = _classes

    private val _students = MutableLiveData<List<Student>>(emptyList())
    val students: LiveData<List<Student>> = _students

    private val _subjectSummaries = MutableLiveData<List<SubjectAttendanceSummary>>(emptyList())
    val subjectSummaries: LiveData<List<SubjectAttendanceSummary>> = _subjectSummaries

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        object Loaded : UiState()
        data class Error(val message: String) : UiState()
    }

    fun loadDepartments() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            try {
                _departments.value = repository.getAllDepartments()
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
                _classes.value = repository.getClassesByDepartment(departmentName)
                _uiState.value = UiState.Loaded
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load classes.")
            }
        }
    }

    fun loadStudents(classId: String) {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            try {
                _students.value = repository.getStudentsByClass(classId)
                _uiState.value = UiState.Loaded
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load students.")
            }
        }
    }

    fun loadSubjectAttendance(student: Student) {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            try {
                _subjectSummaries.value = repository.getSubjectWiseAttendance(student)
                _uiState.value = UiState.Loaded
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load attendance.")
            }
        }
    }
}