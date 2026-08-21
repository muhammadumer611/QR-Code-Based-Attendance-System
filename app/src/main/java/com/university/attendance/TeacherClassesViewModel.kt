package com.university.attendance

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class TeacherClassesViewModel(
    private val repository: TeacherSubjectsRepository = TeacherSubjectsRepository()
) : ViewModel() {

    sealed class UiState {
        object Loading : UiState()
        object Success : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    private val _filteredSubjects = MutableLiveData<List<Subject>>(emptyList())
    val filteredSubjects: LiveData<List<Subject>> = _filteredSubjects

    // Full, unfiltered list -- kept in memory so search() doesn't need to
    // re-query Firestore on every keystroke. A teacher's subject count is
    // small, so client-side filtering is appropriate here.
    private var allSubjects: List<Subject> = emptyList()
    private var lastQuery: String = ""

    fun loadClasses() {
        _uiState.value = UiState.Loading

        viewModelScope.launch {
            try {
                val teacher = repository.getCurrentTeacher()
                val subjects = repository.getAssignedSubjects(teacher.teacherId)

                allSubjects = subjects
                applyFilter()

                _uiState.value = UiState.Success
            } catch (e: Exception) {
                _uiState.value = UiState.Error(
                    e.message ?: "Failed to load your classes."
                )
            }
        }
    }

    fun search(query: String) {
        lastQuery = query
        applyFilter()
    }

    private fun applyFilter() {
        val q = lastQuery.trim().lowercase()

        _filteredSubjects.value = if (q.isEmpty()) {
            allSubjects
        } else {
            allSubjects.filter { subject ->
                subject.courseCode.lowercase().contains(q) ||
                        subject.programName.lowercase().contains(q) ||
                        subject.departmentName.lowercase().contains(q)
            }
        }
    }
}