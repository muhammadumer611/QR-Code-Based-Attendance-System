package com.university.attendance

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.university.attendance.Student
import com.university.attendance.StudentRepository
import com.university.attendance.ValidationUtils
import kotlinx.coroutines.launch

/**
 * Holds all form state across the 2-step "Add Student" flow, so navigating
 * between Step 1 (class info) and Step 2 (personal info) never loses data.
 */
class AddStudentViewModel(
    private val repository: StudentRepository = StudentRepository()
) : ViewModel() {

    // ---------- Step 1: Class / Program info ----------
    var universityName: String = ""
    var departmentName: String = ""
    var programName: String = ""
    var session: String = ""
    var section: String = ""

    // ---------- Step 2: Personal info ----------
    var fullName: String = ""
    var contactNumber: String = ""
    var cnicNumber: String = ""
    var fatherCnicNumber: String = ""
    var guardianNumber: String = ""
    var regNo: String = ""

    private val _saveState = MutableLiveData<SaveUiState>()
    val saveState: LiveData<SaveUiState> = _saveState

    sealed class SaveUiState {
        object Idle : SaveUiState()
        object Loading : SaveUiState()
        data class Success(val classId: String) : SaveUiState()
        data class Error(val message: String) : SaveUiState()
    }

    /**
     * Validates Step 1 fields. Returns null if valid, or an error message
     * for the first invalid field found.
     */
    fun validateStep1(): String? {
        return when {
            !ValidationUtils.isNotBlank(universityName) -> "Please enter university name"
            !ValidationUtils.isNotBlank(departmentName) -> "Please enter department name"
            !ValidationUtils.isNotBlank(programName) -> "Please select/enter program (e.g. BSSE)"
            !ValidationUtils.isNotBlank(session) -> "Please select/enter session (e.g. 2022)"
            !ValidationUtils.isNotBlank(section) -> "Please select/enter class section (e.g. A)"
            else -> null
        }
    }

    /**
     * Validates Step 2 fields, including format checks for CNIC and phone
     * numbers. Returns null if valid, or the first error message found.
     */
    fun validateStep2(): String? {
        return when {
            !ValidationUtils.isNotBlank(fullName) -> "Please enter student's full name"
            !ValidationUtils.isNotBlank(regNo) -> "Please enter registration number"
            !ValidationUtils.isValidPhone(contactNumber) -> "Enter a valid contact number (e.g. 03001234567)"
            !ValidationUtils.isValidCnic(cnicNumber) -> "Enter a valid CNIC (13 digits, e.g. 12345-1234567-1)"
            !ValidationUtils.isValidCnic(fatherCnicNumber) -> "Enter a valid Father CNIC (13 digits)"
            !ValidationUtils.isValidPhone(guardianNumber) -> "Enter a valid guardian number (e.g. 03001234567)"
            else -> null
        }
    }

    /** Builds a Student object from all currently held form state. */
    private fun buildStudent(): Student {
        return Student(
            universityName = universityName.trim(),
            departmentName = departmentName.trim(),
            programName = programName.trim(),
            session = session.trim(),
            section = section.trim(),
            fullName = fullName.trim(),
            contactNumber = ValidationUtils.normalizePhone(contactNumber),
            cnicNumber = ValidationUtils.normalizeCnic(cnicNumber),
            fatherCnicNumber = ValidationUtils.normalizeCnic(fatherCnicNumber),
            guardianNumber = ValidationUtils.normalizePhone(guardianNumber),
            regNo = regNo.trim()
        )
    }

    /** Saves the student to Firestore. Emits Loading -> Success/Error via saveState. */
    fun saveStudent() {
        val student = buildStudent()

        _saveState.value = SaveUiState.Loading
        viewModelScope.launch {
            when (val result = repository.addStudent(student)) {
                is StudentRepository.SaveResult.Success -> {
                    _saveState.value = SaveUiState.Success(result.classId)
                }
                is StudentRepository.SaveResult.Error -> {
                    _saveState.value = SaveUiState.Error(result.message)
                }
            }
        }
    }

    /** Clears all form fields, e.g. after a successful save if adding another student. */
    fun resetPersonalInfoOnly() {
        // Keep Step 1 (class info) intact since Admin likely adds multiple
        // students to the SAME class in a row -- only clear Step 2 fields.
        fullName = ""
        contactNumber = ""
        cnicNumber = ""
        fatherCnicNumber = ""
        guardianNumber = ""
        regNo = ""
    }
}