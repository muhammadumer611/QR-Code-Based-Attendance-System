package com.university.attendance

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/**
 * Holds all form state across the 2-step "Add Teacher" flow, so navigating
 * between Step 1 (department/designation/subject) and Step 2 (personal
 * info) never loses data.
 */
class AddTeacherViewModel(
    private val repository: TeacherRepository = TeacherRepository()
) : ViewModel() {

    // ---------- Step 1: Department / Role info ----------
    var departmentName: String = ""
    var designation: String = ""       // Professor, Assistant Professor, Lecturer, Visiting Faculty
    var mainSubject: String = ""

    // ---------- Step 2: Personal info ----------
    var fullName: String = ""
    var fatherName: String = ""
    var cnicNumber: String = ""
    var fatherCnicNumber: String = ""
    var contactNumber: String = ""

    private val _saveState = MutableLiveData<SaveUiState>()
    val saveState: LiveData<SaveUiState> = _saveState

    private val _departments = MutableLiveData<List<String>>()
    val departments: LiveData<List<String>> = _departments

    sealed class SaveUiState {
        object Idle : SaveUiState()
        object Loading : SaveUiState()
        data class Success(val teacherId: String) : SaveUiState()
        data class Error(val message: String) : SaveUiState()
    }

    /** Loads existing department names (from the classes collection) for the Step 1 dropdown. */
    fun loadDepartments() {
        viewModelScope.launch {
            _departments.value = try {
                repository.getDistinctDepartments()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    /**
     * Validates Step 1 fields. Returns null if valid, or an error message
     * for the first invalid field found.
     */
    fun validateStep1(): String? {
        return when {
            !TeacherValidationUtils.isNotBlank(departmentName) -> "Please select a department"
            !TeacherValidationUtils.isNotBlank(designation) -> "Please select a designation"
            !TeacherValidationUtils.isNotBlank(mainSubject) -> "Please enter the main subject"
            else -> null
        }
    }

    /**
     * Validates Step 2 fields, including format checks for CNIC and phone
     * numbers. Returns null if valid, or the first error message found.
     */
    fun validateStep2(): String? {
        return when {
            !TeacherValidationUtils.isNotBlank(fullName) -> "Please enter teacher's full name"
            !TeacherValidationUtils.isNotBlank(fatherName) -> "Please enter father's name"
            !TeacherValidationUtils.isValidCnic(cnicNumber) -> "Enter a valid CNIC (13 digits, e.g. 12345-1234567-1)"
            !TeacherValidationUtils.isValidCnic(fatherCnicNumber) -> "Enter a valid Father CNIC (13 digits)"
            !TeacherValidationUtils.isValidPhone(contactNumber) -> "Enter a valid contact number (e.g. 03001234567)"
            else -> null
        }
    }

    /** Builds a Teacher object from all currently held form state. */
    private fun buildTeacher(): Teacher {
        return Teacher(
            departmentName = departmentName.trim(),
            designation = designation.trim(),
            mainSubject = mainSubject.trim(),
            fullName = fullName.trim(),
            fatherName = fatherName.trim(),
            cnicNumber = TeacherValidationUtils.normalizeCnic(cnicNumber),
            fatherCnicNumber = TeacherValidationUtils.normalizeCnic(fatherCnicNumber),
            contactNumber = TeacherValidationUtils.normalizePhone(contactNumber)
        )
    }

    /** Saves the teacher to Firestore. Emits Loading -> Success/Error via saveState. */
    fun saveTeacher() {
        val teacher = buildTeacher()

        _saveState.value = SaveUiState.Loading
        viewModelScope.launch {
            when (val result = repository.addTeacher(teacher)) {
                is TeacherRepository.SaveResult.Success -> {
                    _saveState.value = SaveUiState.Success(result.teacherId)
                }
                is TeacherRepository.SaveResult.Error -> {
                    _saveState.value = SaveUiState.Error(result.message)
                }
            }
        }
    }

    /** Clears all form fields, e.g. after a successful save to add another teacher. */
    fun resetForm() {
        departmentName = ""
        designation = ""
        mainSubject = ""
        fullName = ""
        fatherName = ""
        cnicNumber = ""
        fatherCnicNumber = ""
        contactNumber = ""
    }
}