package com.university.attendance

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class ScheduleViewModel(
    private val repository: ScheduleRepository = ScheduleRepository()
) : ViewModel() {

    private val _schedule = MutableLiveData<Schedule?>(null)
    val schedule: LiveData<Schedule?> = _schedule

    private val _teachers = MutableLiveData<List<Teacher>>(emptyList())
    val teachers: LiveData<List<Teacher>> = _teachers

    private val _uiState = MutableLiveData<UiState>(UiState.Idle)
    val uiState: LiveData<UiState> = _uiState

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        object Loaded : UiState()
        object SaveSuccess : UiState()
        data class Error(val message: String) : UiState()
    }

    // ============================================================
    // LOAD ALL TEACHERS
    // ============================================================

    fun loadTeachers() {

        viewModelScope.launch {

            try {

                val teacherRepository = TeacherRepository()

                val teachers =
                    teacherRepository.getAllTeachers()

                _teachers.value = teachers

            } catch (e: Exception) {

                _uiState.value =
                    UiState.Error(
                        e.message ?: "Failed to load teachers."
                    )
            }
        }
    }

    // ============================================================
    // LOAD SELECTED TEACHER'S SCHEDULE
    // ============================================================

    fun loadScheduleForTeacher(
        teacherAuthUid: String
    ) {

        if (teacherAuthUid.isBlank()) {
            _schedule.value = null
            return
        }

        _uiState.value = UiState.Loading

        viewModelScope.launch {

            try {

                _schedule.value =
                    repository.getScheduleForTeacher(
                        teacherAuthUid
                    )

                _uiState.value = UiState.Loaded

            } catch (e: Exception) {

                _uiState.value =
                    UiState.Error(
                        e.message
                            ?: "Failed to load schedule."
                    )
            }
        }
    }

    // ============================================================
    // SAVE SCHEDULE FOR SELECTED TEACHER
    // ============================================================

    fun saveSchedule(
        teacher: Teacher,
        fileName: String,
        note: String
    ) {

        if (teacher.authUid.isBlank()) {

            _uiState.value =
                UiState.Error(
                    "Selected teacher has no Firebase Auth ID."
                )

            return
        }

        if (fileName.isBlank()) {

            _uiState.value =
                UiState.Error(
                    "Please enter a file name."
                )

            return
        }

        _uiState.value = UiState.Loading

        viewModelScope.launch {

            when (
                val result = repository.saveSchedule(
                    teacherAuthUid = teacher.authUid,
                    teacherName = teacher.fullName,
                    fileName = fileName,
                    note = note
                )
            ) {

                is ScheduleRepository.OpResult.Success -> {

                    _uiState.value =
                        UiState.SaveSuccess

                    loadScheduleForTeacher(
                        teacher.authUid
                    )
                }

                is ScheduleRepository.OpResult.Error -> {

                    _uiState.value =
                        UiState.Error(
                            result.message
                        )
                }
            }
        }
    }
}