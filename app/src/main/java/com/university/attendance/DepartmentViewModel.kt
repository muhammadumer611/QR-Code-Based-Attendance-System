package com.university.attendance

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

/**
 * Manages department list state (via a real-time Firestore listener) and
 * exposes CRUD actions for the Department Management screen.
 */
class DepartmentViewModel(
    private val repository: DepartmentRepository = DepartmentRepository()
) : ViewModel() {

    private val _departments = MutableLiveData<List<Department>>(emptyList())
    val departments: LiveData<List<Department>> = _departments

    private val _actionState = MutableLiveData<ActionState>(ActionState.Idle)
    val actionState: LiveData<ActionState> = _actionState

    private var listenerRegistration: ListenerRegistration? = null

    sealed class ActionState {
        object Idle : ActionState()
        object Loading : ActionState()
        data class Success(val message: String) : ActionState()
        data class Error(val message: String) : ActionState()
    }

    /** Starts listening for real-time department updates. Call once, e.g. in onCreate. */
    fun startListening() {
        if (listenerRegistration != null) return // already listening

        listenerRegistration = repository.listenToDepartments(
            onUpdate = { list -> _departments.value = list },
            onError = { e -> _actionState.value = ActionState.Error(e.message ?: "Failed to load departments.") }
        )
    }

    /** Adds a new department with the given name. */
    fun addDepartment(name: String) {
        if (name.isBlank()) {
            _actionState.value = ActionState.Error("Department name cannot be empty.")
            return
        }
        _actionState.value = ActionState.Loading
        viewModelScope.launch {
            when (val result = repository.addDepartment(name)) {
                is DepartmentRepository.OpResult.Success ->
                    _actionState.value = ActionState.Success("Department added.")
                is DepartmentRepository.OpResult.Error ->
                    _actionState.value = ActionState.Error(result.message)
            }
        }
    }

    /** Updates an existing department's name. */
    fun updateDepartment(departmentId: String, newName: String) {
        if (newName.isBlank()) {
            _actionState.value = ActionState.Error("Department name cannot be empty.")
            return
        }
        _actionState.value = ActionState.Loading
        viewModelScope.launch {
            when (val result = repository.updateDepartment(departmentId, newName)) {
                is DepartmentRepository.OpResult.Success ->
                    _actionState.value = ActionState.Success("Department updated.")
                is DepartmentRepository.OpResult.Error ->
                    _actionState.value = ActionState.Error(result.message)
            }
        }
    }

    /** Deletes a department (allowed even if referenced elsewhere, per requirements). */
    fun deleteDepartment(departmentId: String) {
        _actionState.value = ActionState.Loading
        viewModelScope.launch {
            when (val result = repository.deleteDepartment(departmentId)) {
                is DepartmentRepository.OpResult.Success ->
                    _actionState.value = ActionState.Success("Department deleted.")
                is DepartmentRepository.OpResult.Error ->
                    _actionState.value = ActionState.Error(result.message)
            }
        }
    }

    /** Resets actionState back to Idle after the UI has handled a Success/Error, to avoid re-triggering on rotation. */
    fun consumeActionState() {
        _actionState.value = ActionState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove() // stop listening when ViewModel is destroyed, avoids leaks
        listenerRegistration = null
    }
}