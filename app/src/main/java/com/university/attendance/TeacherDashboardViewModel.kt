package com.university.attendance

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.activity.OnBackPressedCallback

class TeacherDashboardViewModel(
    private val repository: TeacherDashboardRepository =
        TeacherDashboardRepository()
) : ViewModel() {

    private val _uiState =
        MutableLiveData<UiState>()

    val uiState: LiveData<UiState> =
        _uiState


    private val _teacher =
        MutableLiveData<Teacher>()

    val teacher: LiveData<Teacher> =
        _teacher


    private val _subjects =
        MutableLiveData<List<Subject>>(
            emptyList()
        )

    val subjects: LiveData<List<Subject>> =
        _subjects


    private val _totalStudents =
        MutableLiveData(0)

    val totalStudents: LiveData<Int> =
        _totalStudents


    private val _attendancePercentage =
        MutableLiveData(0)

    val attendancePercentage: LiveData<Int> =
        _attendancePercentage


    private val _todayClasses =
        MutableLiveData<List<ClassSchedule>>(
            emptyList()
        )

    val todayClasses: LiveData<List<ClassSchedule>> =
        _todayClasses


    private val _weekClasses =
        MutableLiveData<List<ClassSchedule>>(
            emptyList()
        )

    val weekClasses: LiveData<List<ClassSchedule>> =
        _weekClasses


    private val _semesterClasses =
        MutableLiveData<List<ClassSchedule>>(
            emptyList()
        )

    val semesterClasses: LiveData<List<ClassSchedule>> =
        _semesterClasses


    fun loadDashboard() {

        _uiState.value =
            UiState.Loading


        viewModelScope.launch {

            try {

                val data =
                    repository.loadDashboard()


                _teacher.value =
                    data.teacher


                _subjects.value =
                    data.assignedSubjects


                _totalStudents.value =
                    data.totalStudents


                _attendancePercentage.value =
                    data.attendancePercentage


                _todayClasses.value =
                    data.todayClasses


                _weekClasses.value =
                    data.weekClasses


                _semesterClasses.value =
                    data.semesterClasses


                _uiState.value =
                    UiState.Success


            } catch (e: Exception) {

                _uiState.value =
                    UiState.Error(
                        e.message
                            ?: "Failed to load dashboard."
                    )
            }
        }
    }


    sealed class UiState {

        object Loading : UiState()

        object Success : UiState()

        data class Error(
            val message: String
        ) : UiState()
    }
}