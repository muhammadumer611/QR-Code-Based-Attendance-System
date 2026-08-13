package com.university.attendance

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/**
 * Manages the Teacher-Subject Assignment screen:
 *   1. Show a list of all teachers.
 *   2. Admin taps a teacher -> show ALL subjects as a checklist, with
 *      subjects already assigned to THIS teacher pre-checked.
 *   3. Admin checks/unchecks subjects, taps Save -> assignment updates.
 */
class TeacherSubjectViewModel(
    private val repository: TeacherSubjectRepository = TeacherSubjectRepository()
) : ViewModel() {

    private val _uiState = MutableLiveData<UiState>(UiState.Idle)
    val uiState: LiveData<UiState> = _uiState

    private val _teachers = MutableLiveData<List<Teacher>>(emptyList())
    val teachers: LiveData<List<Teacher>> = _teachers

    private val _subjects = MutableLiveData<List<Subject>>(emptyList())
    val subjects: LiveData<List<Subject>> = _subjects

    /** Subject IDs currently checked in the UI (mutable working state before Save). */
    private val _selectedSubjectIds = MutableLiveData<Set<String>>(emptySet())
    val selectedSubjectIds: LiveData<Set<String>> = _selectedSubjectIds

    var selectedTeacherId: String = ""
    var selectedTeacherName: String = ""

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        object Loaded : UiState()
        object SaveSuccess : UiState()
        data class Error(val message: String) : UiState()
    }

    fun loadTeachers() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            try {
                _teachers.value = repository.getAllTeachers()
                _uiState.value = UiState.Loaded
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load teachers.")
            }
        }
    }

    /** Loads all subjects and pre-checks whichever ones already belong to the selected teacher. */
    fun loadSubjectsForTeacher(teacherId: String, teacherName: String) {
        selectedTeacherId = teacherId
        selectedTeacherName = teacherName

        _uiState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val allSubjects = repository.getAllSubjects()
                _subjects.value = allSubjects
                _selectedSubjectIds.value = allSubjects
                    .filter { it.teacherId == teacherId }
                    .map { it.subjectId }
                    .toSet()
                _uiState.value = UiState.Loaded
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load subjects.")
            }
        }
    }

    /** Toggles one subject's checked state in the working selection. */
    fun toggleSubject(subjectId: String) {
        val current = _selectedSubjectIds.value.orEmpty().toMutableSet()
        if (current.contains(subjectId)) current.remove(subjectId) else current.add(subjectId)
        _selectedSubjectIds.value = current
    }

    fun saveAssignment() {
        val subjects = _subjects.value.orEmpty()
        val selected = _selectedSubjectIds.value.orEmpty()

        if (selectedTeacherId.isBlank()) {
            _uiState.value = UiState.Error("No teacher selected.")
            return
        }

        _uiState.value = UiState.Loading
        viewModelScope.launch {
            when (val result = repository.saveAssignment(
                teacherId = selectedTeacherId,
                teacherName = selectedTeacherName,
                allSubjects = subjects,
                selectedSubjectIds = selected
            )) {
                is TeacherSubjectRepository.OpResult.Success -> _uiState.value = UiState.SaveSuccess
                is TeacherSubjectRepository.OpResult.Error -> _uiState.value = UiState.Error(result.message)
            }
        }
    }
}