package com.university.attendance

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class ScheduleViewModel(
    private val repository: ScheduleRepository =
        ScheduleRepository()
) : ViewModel() {

    // ============================================================
    // SCHEDULE
    // ============================================================

    private val _schedule =
        MutableLiveData<Schedule?>(null)

    val schedule: LiveData<Schedule?> =
        _schedule


    // ============================================================
    // TEACHERS
    // ============================================================

    private val _teachers =
        MutableLiveData<List<Teacher>>(emptyList())

    val teachers: LiveData<List<Teacher>> =
        _teachers


    // ============================================================
    // UI STATE
    // ============================================================

    private val _uiState =
        MutableLiveData<UiState>(UiState.Idle)

    val uiState: LiveData<UiState> =
        _uiState


    // ============================================================
    // UI STATE CLASS
    // ============================================================

    sealed class UiState {

        object Idle : UiState()

        object Loading : UiState()

        object Loaded : UiState()

        object SaveSuccess : UiState()

        data class Error(
            val message: String
        ) : UiState()
    }


    // ============================================================
    // LOAD ALL TEACHERS
    // ============================================================

    fun loadTeachers() {

        viewModelScope.launch {

            try {

                _uiState.value =
                    UiState.Loading

                val teacherRepository =
                    TeacherRepository()

                val teacherList =
                    teacherRepository.getAllTeachers()

                _teachers.value =
                    teacherList

                _uiState.value =
                    UiState.Loaded

            } catch (e: Exception) {

                _uiState.value =
                    UiState.Error(
                        e.message
                            ?: "Failed to load teachers."
                    )
            }
        }
    }


    // ============================================================
    // LOAD SCHEDULE FOR SELECTED TEACHER
    // ============================================================

    fun loadScheduleForTeacher(
        teacherAuthUid: String
    ) {

        if (teacherAuthUid.isBlank()) {

            _schedule.value =
                null

            return
        }

        viewModelScope.launch {

            try {

                _uiState.value =
                    UiState.Loading

                val result =
                    repository.getScheduleForTeacher(
                        teacherAuthUid
                    )

                _schedule.value =
                    result

                _uiState.value =
                    UiState.Loaded

            } catch (e: Exception) {

                _schedule.value =
                    null

                _uiState.value =
                    UiState.Error(
                        e.message
                            ?: "Failed to load schedule."
                    )
            }
        }
    }


    // ============================================================
    // SAVE / UPLOAD SCHEDULE PDF
    // ============================================================

    fun saveSchedule(
        teacher: Teacher,
        fileName: String,
        note: String,
        pdfUri: Uri
    ) {

        // --------------------------------------------------------
        // Teacher UID validation
        // --------------------------------------------------------

        if (teacher.authUid.isBlank()) {

            _uiState.value =
                UiState.Error(
                    "Selected teacher does not have a valid Firebase Auth UID."
                )

            return
        }


        // --------------------------------------------------------
        // File name validation
        // --------------------------------------------------------

        if (fileName.isBlank()) {

            _uiState.value =
                UiState.Error(
                    "Please enter a file name."
                )

            return
        }


        // --------------------------------------------------------
        // PDF URI validation
        // --------------------------------------------------------

        if (pdfUri.toString().isBlank()) {

            _uiState.value =
                UiState.Error(
                    "Please select a PDF file."
                )

            return
        }


        // --------------------------------------------------------
        // Loading
        // --------------------------------------------------------

        _uiState.value =
            UiState.Loading


        // --------------------------------------------------------
        // Upload in background
        // --------------------------------------------------------

        viewModelScope.launch {

            try {

                val result =
                    repository.saveSchedule(

                        teacherAuthUid =
                            teacher.authUid,

                        teacherName =
                            if (
                                teacher.fullName.isNotBlank()
                            ) {
                                teacher.fullName
                            } else {
                                teacher.email
                            },

                        fileName =
                            fileName.trim(),

                        note =
                            note.trim(),

                        pdfUri =
                            pdfUri
                    )


                // =================================================
                // RESULT
                // =================================================

                when (result) {

                    is ScheduleRepository.OpResult.Success -> {

                        _uiState.value =
                            UiState.SaveSuccess


                        // ------------------------------------------------
                        // Immediately load uploaded schedule
                        // ------------------------------------------------

                        val updatedSchedule =
                            repository
                                .getScheduleForTeacher(
                                    teacher.authUid
                                )

                        _schedule.value =
                            updatedSchedule
                    }


                    is ScheduleRepository.OpResult.Error -> {

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
                            ?: "Failed to upload schedule."
                    )
            }
        }
    }


    // ============================================================
    // CLEAR CURRENT SCHEDULE
    // ============================================================

    fun clearSchedule() {

        _schedule.value =
            null
    }


    // ============================================================
    // RESET UI STATE
    // ============================================================

    fun resetUiState() {

        _uiState.value =
            UiState.Idle
    }
}