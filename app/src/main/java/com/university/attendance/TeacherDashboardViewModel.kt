package com.university.attendance

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class TeacherDashboardViewModel(
    private val repository: TeacherDashboardRepository = TeacherDashboardRepository()
) : ViewModel() {

    sealed class UiState {
        object Loading : UiState()
        object Success : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    private val _teacher = MutableLiveData<Teacher>()
    val teacher: LiveData<Teacher> = _teacher

    private val _subjects = MutableLiveData<List<Subject>>(emptyList())
    val subjects: LiveData<List<Subject>> = _subjects

    private val _totalStudents = MutableLiveData(0)
    val totalStudents: LiveData<Int> = _totalStudents

    private val _attendancePercentage = MutableLiveData(0)
    val attendancePercentage: LiveData<Int> = _attendancePercentage

    fun loadDashboard() {
        _uiState.value = UiState.Loading

        viewModelScope.launch {
            try {
                val data = repository.loadDashboard()

                _teacher.value = data.teacher
                _subjects.value = data.assignedSubjects
                _totalStudents.value = data.totalStudents
                _attendancePercentage.value = data.attendancePercentage

                _uiState.value = UiState.Success
            } catch (e: Exception) {
                _uiState.value = UiState.Error(
                    e.message ?: "Failed to load teacher dashboard."
                )
            }
        }
    }
}
