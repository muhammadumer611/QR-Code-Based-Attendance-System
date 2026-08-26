package com.university.attendance

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

/**
 * Manages the Department -> Program -> Semester filter selection, and the
 * resulting real-time subject list for that exact combination.
 *
 * Subjects are only ever loaded for ONE selected combination at a time, so
 * subjects from different departments/programs/semesters never mix in the
 * list -- matching how a real course catalogue works.
 */
class SubjectViewModel(
    private val repository: SubjectRepository = SubjectRepository()
) : ViewModel() {

    // ---------- Filter selection ----------
    var selectedDepartmentId: String = ""
    var selectedDepartmentName: String = ""
    var selectedProgram: String = ""
    var selectedSemester: Int = 0 // 0 = not yet selected

    private val _departments = MutableLiveData<List<Department>>(emptyList())
    val departments: LiveData<List<Department>> = _departments

    private val _subjects = MutableLiveData<List<Subject>>(emptyList())
    val subjects: LiveData<List<Subject>> = _subjects

    private val _actionState = MutableLiveData<ActionState>(ActionState.Idle)
    val actionState: LiveData<ActionState> = _actionState

    private var listenerRegistration: ListenerRegistration? = null

    sealed class ActionState {
        object Idle : ActionState()
        object Loading : ActionState()
        data class Success(val message: String) : ActionState()
        data class Error(val message: String) : ActionState()
    }

    /** Loads the department list for the filter dropdown. */
    fun loadDepartments() {
        viewModelScope.launch {
            _departments.value = try {
                repository.getAllDepartments()
            } catch (e: Exception) {
                _actionState.value = ActionState.Error("Failed to load departments: ${e.message}")
                emptyList()
            }
        }
    }

    /**
     * Call this whenever Department, Program, or Semester filter changes.
     * Replaces any existing listener with a new one scoped to the new
     * combination -- this guarantees no cross-contamination between e.g.
     * BSSE Semester 3 and BSCS Semester 3 subjects.
     */
    fun applyFilter(departmentId: String, departmentName: String, program: String, semester: Int) {
        selectedDepartmentId = departmentId
        selectedDepartmentName = departmentName
        selectedProgram = program
        selectedSemester = semester

        listenerRegistration?.remove() // stop listening to the previous combination

        if (departmentId.isBlank() || program.isBlank() || semester == 0) {
            _subjects.value = emptyList()
            return
        }

        listenerRegistration = repository.listenToSubjects(
            departmentId = departmentId,
            programName = program,
            semester = semester,
            onUpdate = { list ->
                // Sort client-side by courseCode (fixes the previous bug
                // where combining orderBy() with 3 whereEqualTo() filters
                // required a Firestore composite index and silently failed).
                _subjects.value = list.sortedBy { it.courseCode }
            },
            onError = { e ->
                // Surfaced clearly now (previously easy to miss) so any
                // future Firestore permission/index problems are obvious
                // immediately instead of just showing an empty list.
                _actionState.value = ActionState.Error("Failed to load subjects: ${e.message}")
            }
        )
    }

    /** Adds a new subject under the CURRENTLY selected Department/Program/Semester. */
    fun addSubject(courseCode: String, subjectName: String, creditHours: Int) {
        if (selectedDepartmentId.isBlank() || selectedProgram.isBlank() || selectedSemester == 0) {
            _actionState.value = ActionState.Error("Please select Department, Program, and Semester first.")
            return
        }
        if (courseCode.isBlank() || subjectName.isBlank()) {
            _actionState.value = ActionState.Error("Course Code and Subject Name are required.")
            return
        }

        val subject = Subject(
            departmentId = selectedDepartmentId,
            departmentName = selectedDepartmentName,
            programName = selectedProgram,
            semester = selectedSemester,
            courseCode = courseCode.trim(),
            subjectName = subjectName.trim(),
            creditHours = creditHours
        )

        _actionState.value = ActionState.Loading
        viewModelScope.launch {
            when (val result = repository.addSubject(subject)) {
                is SubjectRepository.OpResult.Success ->
                    _actionState.value = ActionState.Success("Subject added.")
                is SubjectRepository.OpResult.Error ->
                    _actionState.value = ActionState.Error(result.message)
            }
        }
    }

    fun updateSubject(subjectId: String, courseCode: String, subjectName: String, creditHours: Int) {
        if (courseCode.isBlank() || subjectName.isBlank()) {
            _actionState.value = ActionState.Error("Course Code and Subject Name are required.")
            return
        }
        _actionState.value = ActionState.Loading
        viewModelScope.launch {
            when (val result = repository.updateSubject(subjectId, courseCode, subjectName, creditHours)) {
                is SubjectRepository.OpResult.Success ->
                    _actionState.value = ActionState.Success("Subject updated.")
                is SubjectRepository.OpResult.Error ->
                    _actionState.value = ActionState.Error(result.message)
            }
        }
    }

    fun deleteSubject(subjectId: String) {
        _actionState.value = ActionState.Loading
        viewModelScope.launch {
            when (val result = repository.deleteSubject(subjectId)) {
                is SubjectRepository.OpResult.Success ->
                    _actionState.value = ActionState.Success("Subject deleted.")
                is SubjectRepository.OpResult.Error ->
                    _actionState.value = ActionState.Error(result.message)
            }
        }
    }

    fun consumeActionState() {
        _actionState.value = ActionState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
        listenerRegistration = null
    }
}