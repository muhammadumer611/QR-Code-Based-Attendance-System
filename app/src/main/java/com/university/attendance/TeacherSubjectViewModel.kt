package com.university.attendance

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class TeacherSubjectViewModel(
    private val repository:
    AdminTeacherAssignmentRepository =
        AdminTeacherAssignmentRepository()
) : ViewModel() {

    private val _uiState =
        MutableLiveData<UiState>(
            UiState.Idle
        )

    val uiState:
            LiveData<UiState> =
        _uiState

    private val _teachers =
        MutableLiveData<List<Teacher>>(
            emptyList()
        )

    val teachers:
            LiveData<List<Teacher>> =
        _teachers

    private val _subjects =
        MutableLiveData<List<Subject>>(
            emptyList()
        )

    val subjects:
            LiveData<List<Subject>> =
        _subjects

    private val _selectedSubjectIds =
        MutableLiveData<Set<String>>(
            emptySet()
        )

    val selectedSubjectIds:
            LiveData<Set<String>> =
        _selectedSubjectIds

    var selectedTeacherId = ""
    var selectedTeacherName = ""

    var selectedSemester = 1
    var selectedSession = ""

    sealed class UiState {

        object Idle :
            UiState()

        object Loading :
            UiState()

        object Loaded :
            UiState()

        object SaveSuccess :
            UiState()

        data class Error(
            val message: String
        ) : UiState()
    }

    // ============================================================
    // TEACHERS
    // ============================================================

    fun loadTeachers() {

        _uiState.value =
            UiState.Loading

        viewModelScope.launch {

            try {

                _teachers.value =
                    repository.getAllTeachers()

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
    // LOAD SUBJECTS
    // ============================================================

    fun loadSubjects(
        semester: Int,
        session: String
    ) {

        selectedSemester =
            semester

        selectedSession =
            session.trim()

        if (selectedSession.isBlank()) {

            _uiState.value =
                UiState.Error(
                    "Please select/enter session."
                )

            return
        }

        _uiState.value =
            UiState.Loading

        viewModelScope.launch {

            try {

                val subjects =
                    repository.getSubjectsBySemester(
                        semester
                    )

                _subjects.value =
                    subjects

                _selectedSubjectIds.value =
                    repository.getAssignedSubjectIds(
                        teacherId =
                            selectedTeacherId,

                        semester =
                            selectedSemester,

                        session =
                            selectedSession
                    )

                _uiState.value =
                    UiState.Loaded

            } catch (e: Exception) {

                _uiState.value =
                    UiState.Error(
                        e.message
                            ?: "Failed to load subjects."
                    )
            }
        }
    }

    // ============================================================
    // SELECT TEACHER
    // ============================================================

    fun selectTeacher(
        teacher: Teacher
    ) {

        selectedTeacherId =
            teacher.teacherId

        selectedTeacherName =
            teacher.fullName

        _selectedSubjectIds.value =
            emptySet()
    }

    // ============================================================
    // TOGGLE
    // ============================================================

    fun toggleSubject(
        subjectId: String
    ) {

        val current =
            _selectedSubjectIds
                .value
                .orEmpty()
                .toMutableSet()

        if (
            current.contains(
                subjectId
            )
        ) {

            current.remove(
                subjectId
            )

        } else {

            current.add(
                subjectId
            )
        }

        _selectedSubjectIds.value =
            current
    }

    // ============================================================
    // SAVE
    // ============================================================

    fun saveAssignment(
        teacher: Teacher
    ) {

        if (
            selectedTeacherId.isBlank()
        ) {

            _uiState.value =
                UiState.Error(
                    "No teacher selected."
                )

            return
        }

        if (
            selectedSession.isBlank()
        ) {

            _uiState.value =
                UiState.Error(
                    "Session is required."
                )

            return
        }

        _uiState.value =
            UiState.Loading

        viewModelScope.launch {

            val result =
                repository.saveAssignment(

                    teacher =
                        teacher,

                    semester =
                        selectedSemester,

                    session =
                        selectedSession,

                    subjects =
                        _subjects.value
                            .orEmpty(),

                    selectedSubjectIds =
                        _selectedSubjectIds.value
                            .orEmpty()
                )

            when (result) {

                is AdminTeacherAssignmentRepository.OpResult.Success -> {

                    _uiState.value =
                        UiState.SaveSuccess
                }

                is AdminTeacherAssignmentRepository.OpResult.Error -> {

                    _uiState.value =
                        UiState.Error(
                            result.message
                        )
                }
            }
        }
    }
}