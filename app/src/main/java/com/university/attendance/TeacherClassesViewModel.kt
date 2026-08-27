package com.university.attendance

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class TeacherClassesViewModel(
    private val repository: TeacherClassesRepository =
        TeacherClassesRepository()
) : ViewModel() {

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

    private val _uiState =
        MutableLiveData<UiState>(
            UiState.Idle
        )

    val uiState: LiveData<UiState> =
        _uiState

    // ============================================================
    // TEACHER CLASSES / SUBJECTS
    // ============================================================

    private val _classes =
        MutableLiveData<List<Subject>>(
            emptyList()
        )

    val classes: LiveData<List<Subject>> =
        _classes

    // ============================================================
    // STUDENT COUNT
    // ============================================================

    private val _studentCounts =
        MutableLiveData<Map<String, Int>>(
            emptyMap()
        )

    val studentCounts: LiveData<Map<String, Int>> =
        _studentCounts

    // ============================================================
    // LOAD CLASSES
    // ============================================================

    fun loadClasses(
        teacherId: String
    ) {

        if (teacherId.isBlank()) {

            _uiState.value =
                UiState.Error(
                    "Teacher ID is missing."
                )

            return
        }

        viewModelScope.launch {

            _uiState.value =
                UiState.Loading

            try {

                val subjects =
                    repository.getTeacherClasses(
                        teacherId
                    )

                _classes.value =
                    subjects

                val counts =
                    mutableMapOf<String, Int>()

                subjects.forEach { subject ->

                    try {

                        counts[subject.subjectId] =
                            repository.getStudentCount(
                                subject
                            )

                    } catch (_: Exception) {

                        counts[subject.subjectId] =
                            0
                    }
                }

                _studentCounts.value =
                    counts

                _uiState.value =
                    UiState.Success

            } catch (e: Exception) {

                _uiState.value =
                    UiState.Error(
                        e.message
                            ?: "Failed to load teacher classes."
                    )
            }
        }
    }

    // ============================================================
    // GET STUDENTS FOR SELECTED SUBJECT
    // ============================================================

    fun loadStudents(
        subject: Subject
    ): LiveData<List<Student>> {

        val result =
            MutableLiveData<List<Student>>(
                emptyList()
            )

        viewModelScope.launch {

            try {

                result.value =
                    repository.getStudentsForSubject(
                        subject
                    )

            } catch (_: Exception) {

                result.value =
                    emptyList()
            }
        }

        return result
    }

    // ============================================================
    // GET SINGLE SUBJECT
    // ============================================================

    fun loadSubject(
        subjectId: String
    ): LiveData<Subject?> {

        val result =
            MutableLiveData<Subject?>()

        viewModelScope.launch {

            try {

                result.value =
                    repository.getSubjectById(
                        subjectId
                    )

            } catch (_: Exception) {

                result.value =
                    null
            }
        }

        return result
    }

    // ============================================================
    // RESET
    // ============================================================

    fun resetState() {

        _uiState.value =
            UiState.Idle
    }
}

