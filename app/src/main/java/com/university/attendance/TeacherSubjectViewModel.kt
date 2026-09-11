package com.university.attendance

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class TeacherSubjectViewModel(
    private val repository: AdminTeacherAssignmentRepository = AdminTeacherAssignmentRepository()
) : ViewModel() {
    private val _uiState = MutableLiveData<UiState>(UiState.Idle)
    val uiState: LiveData<UiState> = _uiState
    private val _teachers = MutableLiveData<List<Teacher>>(emptyList())
    val teachers: LiveData<List<Teacher>> = _teachers
    private val _classes = MutableLiveData<List<StudentClass>>(emptyList())
    val classes: LiveData<List<StudentClass>> = _classes
    private val _subjects = MutableLiveData<List<Subject>>(emptyList())
    val subjects: LiveData<List<Subject>> = _subjects
    private val _selectedSubjectIds = MutableLiveData<Set<String>>(emptySet())
    val selectedSubjectIds: LiveData<Set<String>> = _selectedSubjectIds
    var selectedTeacher: Teacher? = null
    var selectedClass: StudentClass? = null
    var selectedSemester = 1
    var selectedSession = ""

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        object Loaded : UiState()
        object SaveSuccess : UiState()
        data class Error(val message: String) : UiState()
    }

    fun loadTeachers() = viewModelScope.launch {
        _uiState.value = UiState.Loading
        try { _teachers.value = repository.getAllTeachers(); _uiState.value = UiState.Loaded }
        catch (e: Exception) { _uiState.value = UiState.Error(e.message ?: "Failed to load teachers.") }
    }

    fun selectTeacher(teacher: Teacher) {
        selectedTeacher = teacher
        selectedClass = null
        _classes.value = emptyList()
        _subjects.value = emptyList()
        _selectedSubjectIds.value = emptySet()
    }

    fun loadClasses(semester: Int, session: String) = viewModelScope.launch {
        selectedSemester = semester; selectedSession = session.trim()
        if (selectedTeacher == null) return@launch
        if (selectedSession.isBlank()) { _uiState.value = UiState.Error("Session is required."); return@launch }
        _uiState.value = UiState.Loading
        try { _classes.value = repository.getAllClasses(selectedSession, semester); _uiState.value = UiState.Loaded }
        catch (e: Exception) { _uiState.value = UiState.Error(e.message ?: "Failed to load classes.") }
    }

    fun selectClass(studentClass: StudentClass) = viewModelScope.launch {
        selectedClass = studentClass
        _uiState.value = UiState.Loading
        try {
            _subjects.value = repository.getSubjectsForClass(studentClass.classId, selectedSemester)
            _selectedSubjectIds.value = repository.getAssignedSubjectIds(selectedTeacher?.teacherId.orEmpty(), studentClass.classId, selectedSemester, selectedSession)
            _uiState.value = UiState.Loaded
        } catch (e: Exception) { _uiState.value = UiState.Error(e.message ?: "Failed to load subjects.") }
    }

    fun toggleSubject(id: String) {
        val set = _selectedSubjectIds.value.orEmpty().toMutableSet()
        if (!set.add(id)) set.remove(id)
        _selectedSubjectIds.value = set
    }

    fun saveAssignment() {
        val teacher = selectedTeacher ?: return error("Select teacher.")
        val cls = selectedClass ?: return error("Select class.")
        if (_selectedSubjectIds.value.orEmpty().isEmpty()) return error("Select at least one subject.")
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            when (val result = repository.saveAssignment(teacher, cls, selectedSemester, selectedSession, _subjects.value.orEmpty(), _selectedSubjectIds.value.orEmpty())) {
                is AdminTeacherAssignmentRepository.OpResult.Success -> _uiState.value = UiState.SaveSuccess
                is AdminTeacherAssignmentRepository.OpResult.Error -> _uiState.value = UiState.Error(result.message)
            }
        }
    }

    private fun error(message: String) { _uiState.value = UiState.Error(message) }
}
