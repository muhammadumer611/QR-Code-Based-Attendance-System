package com.university.attendance

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class TeacherDashboardViewModel(
    private val repository: TeacherDashboardRepository =
        TeacherDashboardRepository()
) : ViewModel() {


    // ============================================================
    // UI STATE
    // ============================================================

    sealed class UiState {

        object Loading : UiState()

        object Success : UiState()

        data class Error(
            val message: String
        ) : UiState()
    }


    private val _uiState =
        MutableLiveData<UiState>()

    val uiState: LiveData<UiState> =
        _uiState


    // ============================================================
    // TEACHER
    // ============================================================

    private val _teacher =
        MutableLiveData<Teacher>()

    val teacher: LiveData<Teacher> =
        _teacher


    // ============================================================
    // SUBJECTS
    // ============================================================

    private val _subjects =
        MutableLiveData<List<Subject>>(
            emptyList()
        )

    val subjects: LiveData<List<Subject>> =
        _subjects


    // ============================================================
    // TOTAL STUDENTS
    // ============================================================

    private val _totalStudents =
        MutableLiveData(0)

    val totalStudents: LiveData<Int> =
        _totalStudents


    // ============================================================
    // ATTENDANCE
    // ============================================================

    private val _attendancePercentage =
        MutableLiveData(0)

    val attendancePercentage: LiveData<Int> =
        _attendancePercentage


    // ============================================================
    // TODAY'S CLASSES
    // ============================================================

    private val _todayClasses =
        MutableLiveData<List<ClassSchedule>>(
            emptyList()
        )

    val todayClasses: LiveData<List<ClassSchedule>> =
        _todayClasses


    // ============================================================
    // UPCOMING CLASSES
    // ============================================================

    private val _upcomingClasses =
        MutableLiveData<List<ClassSchedule>>(
            emptyList()
        )

    val upcomingClasses: LiveData<List<ClassSchedule>> =
        _upcomingClasses


    // ============================================================
    // LOAD DASHBOARD
    // ============================================================

    fun loadDashboard() {

        _uiState.value =
            UiState.Loading


        viewModelScope.launch {

            try {

                val data =
                    repository.loadDashboard()


                // ------------------------------------------------
                // TEACHER
                // ------------------------------------------------

                _teacher.value =
                    data.teacher


                // ------------------------------------------------
                // SUBJECTS
                // ------------------------------------------------

                _subjects.value =
                    data.assignedSubjects


                // ------------------------------------------------
                // STUDENTS
                // ------------------------------------------------

                _totalStudents.value =
                    data.totalStudents


                // ------------------------------------------------
                // ATTENDANCE
                // ------------------------------------------------

                _attendancePercentage.value =
                    data.attendancePercentage


                // ------------------------------------------------
                // TODAY
                // ------------------------------------------------

                _todayClasses.value =
                    data.todayClasses


                // ------------------------------------------------
                // UPCOMING
                // ------------------------------------------------

                _upcomingClasses.value =
                    data.upcomingClasses


                // ------------------------------------------------
                // SUCCESS
                // ------------------------------------------------

                _uiState.value =
                    UiState.Success

            } catch (e: Exception) {

                _uiState.value =
                    UiState.Error(
                        e.message
                            ?: "Failed to load teacher dashboard."
                    )
            }
        }
    }


    // ============================================================
    // REFRESH
    // ============================================================

    fun refresh() {
        loadDashboard()
    }
}