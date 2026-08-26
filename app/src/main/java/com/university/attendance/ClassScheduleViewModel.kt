package com.university.attendance

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class ClassScheduleViewModel(
    private val repository: ClassScheduleRepository =
        ClassScheduleRepository()
) : ViewModel() {

    // ============================================================
    // CLASSES
    // ============================================================

    private val _classes =
        MutableLiveData<List<ClassSchedule>>(
            emptyList()
        )

    val classes: LiveData<List<ClassSchedule>> =
        _classes

    // ============================================================
    // UI STATE
    // ============================================================

    private val _uiState =
        MutableLiveData<UiState>(
            UiState.Idle
        )

    val uiState: LiveData<UiState> =
        _uiState

    // ============================================================
    // UI STATE
    // ============================================================

    sealed class UiState {

        object Idle : UiState()

        object Loading : UiState()

        object Success : UiState()

        data class Error(
            val message: String
        ) : UiState()
    }

    // ============================================================
    // SAVE CLASS
    // ============================================================

    fun saveClass(
        classSchedule: ClassSchedule
    ) {

        if (
            classSchedule.teacherAuthUid
                .isBlank()
        ) {

            _uiState.value =
                UiState.Error(
                    "Please select a valid teacher."
                )

            return
        }

        if (
            classSchedule.className
                .isBlank()
        ) {

            _uiState.value =
                UiState.Error(
                    "Please enter class name."
                )

            return
        }

        if (
            classSchedule.subjectName
                .isBlank()
        ) {

            _uiState.value =
                UiState.Error(
                    "Please enter subject name."
                )

            return
        }

        if (
            classSchedule.date
                .isBlank()
        ) {

            _uiState.value =
                UiState.Error(
                    "Please select a date."
                )

            return
        }

        if (
            classSchedule.startTime
                .isBlank()
        ) {

            _uiState.value =
                UiState.Error(
                    "Please select start time."
                )

            return
        }

        if (
            classSchedule.endTime
                .isBlank()
        ) {

            _uiState.value =
                UiState.Error(
                    "Please select end time."
                )

            return
        }

        viewModelScope.launch {

            _uiState.value =
                UiState.Loading

            when (
                val result =
                    repository.saveClass(
                        classSchedule
                    )
            ) {

                is ClassScheduleRepository.OpResult.Success -> {

                    _uiState.value =
                        UiState.Success

                    loadAllClassesForTeacher(
                        classSchedule.teacherAuthUid
                    )
                }

                is ClassScheduleRepository.OpResult.Error -> {

                    _uiState.value =
                        UiState.Error(
                            result.message
                        )
                }
            }
        }
    }

    // ============================================================
    // LOAD ALL CLASSES FOR TEACHER
    // ============================================================

    fun loadAllClassesForTeacher(
        teacherAuthUid: String
    ) {

        if (
            teacherAuthUid.isBlank()
        ) {

            _classes.value =
                emptyList()

            return
        }

        viewModelScope.launch {

            try {

                _uiState.value =
                    UiState.Loading

                _classes.value =
                    repository
                        .getClassesForTeacher(
                            teacherAuthUid
                        )

                _uiState.value =
                    UiState.Success

            } catch (e: Exception) {

                _uiState.value =
                    UiState.Error(
                        e.message
                            ?: "Failed to load classes."
                    )
            }
        }
    }

    // ============================================================
    // LOAD TODAY
    // ============================================================

    fun loadTodayClasses(
        teacherAuthUid: String
    ) {

        if (
            teacherAuthUid.isBlank()
        ) {

            _classes.value =
                emptyList()

            return
        }

        viewModelScope.launch {

            try {

                _uiState.value =
                    UiState.Loading

                _classes.value =
                    repository
                        .getTodayClassesForTeacher(
                            teacherAuthUid
                        )

                _uiState.value =
                    UiState.Success

            } catch (e: Exception) {

                _uiState.value =
                    UiState.Error(
                        e.message
                            ?: "Failed to load today's classes."
                    )
            }
        }
    }

    // ============================================================
    // LOAD SPECIFIC DATE
    // ============================================================

    fun loadClassesForDate(
        teacherAuthUid: String,
        date: String
    ) {

        if (
            teacherAuthUid.isBlank() ||
            date.isBlank()
        ) {

            _classes.value =
                emptyList()

            return
        }

        viewModelScope.launch {

            try {

                _uiState.value =
                    UiState.Loading

                _classes.value =
                    repository
                        .getClassesForDate(
                            teacherAuthUid,
                            date
                        )

                _uiState.value =
                    UiState.Success

            } catch (e: Exception) {

                _uiState.value =
                    UiState.Error(
                        e.message
                            ?: "Failed to load classes."
                    )
            }
        }
    }

    // ============================================================
    // DELETE CLASS
    // ============================================================

    fun deleteClass(
        scheduleId: String,
        teacherAuthUid: String
    ) {

        if (
            scheduleId.isBlank()
        ) {

            _uiState.value =
                UiState.Error(
                    "Invalid class."
                )

            return
        }

        viewModelScope.launch {

            try {

                _uiState.value =
                    UiState.Loading

                when (
                    val result =
                        repository.deleteClass(
                            scheduleId
                        )
                ) {

                    is ClassScheduleRepository.OpResult.Success -> {

                        _uiState.value =
                            UiState.Success

                        loadAllClassesForTeacher(
                            teacherAuthUid
                        )
                    }

                    is ClassScheduleRepository.OpResult.Error -> {

                        _uiState.value =
                            UiState.Error(
                                result.message
                            )
                    }
                }

            } catch (e: Exception) {

                _uiState.value =
                    UiState.Error(
                        e.message
                            ?: "Failed to delete class."
                    )
            }
        }
    }

    // ============================================================
    // RESET
    // ============================================================

    fun resetState() {

        _uiState.value =
            UiState.Idle
    }
}