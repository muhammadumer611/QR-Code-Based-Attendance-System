package com.university.attendance

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class AddStudentViewModel(
    private val repository: StudentRepository =
        StudentRepository()
) : ViewModel() {

    // ============================================================
    // STEP 1
    // ============================================================

    var universityName = ""
    var departmentName = ""
    var programName = ""
    var semester = 1
    var session = ""
    var section = ""

    // ============================================================
    // STEP 2
    // ============================================================

    var fullName = ""
    var fatherName = ""
    var personalEmail = ""
    var contactNumber = ""
    var cnicNumber = ""
    var fatherCnicNumber = ""
    var guardianNumber = ""
    var regNo = ""

    // ============================================================
    // STATE
    // ============================================================

    private val _saveState =
        MutableLiveData<SaveUiState>(
            SaveUiState.Idle
        )

    val saveState:
            LiveData<SaveUiState> =
        _saveState

    sealed class SaveUiState {

        object Idle :
            SaveUiState()

        object Loading :
            SaveUiState()

        data class Success(
            val studentId: String,
            val generatedStudentId: String,
            val classId: String
        ) : SaveUiState()

        data class Error(
            val message: String
        ) : SaveUiState()
    }

    // ============================================================
    // VALIDATE STEP 1
    // ============================================================

    fun validateStep1(): String? {

        return when {

            universityName.isBlank() ->
                "Please enter university name."

            departmentName.isBlank() ->
                "Please enter department name."

            programName.isBlank() ->
                "Please select program."

            semester !in 1..8 ->
                "Please select a valid semester."

            session.isBlank() ->
                "Please enter session."

            section.isBlank() ->
                "Please select class/section."

            else ->
                null
        }
    }

    // ============================================================
    // VALIDATE STEP 2
    // ============================================================

    fun validateStep2(): String? {

        return when {

            fullName.isBlank() ->
                "Please enter student's full name."

            fatherName.isBlank() ->
                "Please enter father's/guardian name."

            personalEmail.isBlank() ||
                    !android.util.Patterns.EMAIL_ADDRESS
                        .matcher(personalEmail.trim())
                        .matches() ->
                "Please enter a valid personal email."

            !ValidationUtils.isValidPhone(
                contactNumber
            ) ->
                "Enter a valid student phone number."

            !ValidationUtils.isValidCnic(
                cnicNumber
            ) ->
                "Enter a valid student CNIC."

            !ValidationUtils.isValidCnic(
                fatherCnicNumber
            ) ->
                "Enter a valid father CNIC."

            !ValidationUtils.isValidPhone(
                guardianNumber
            ) ->
                "Enter a valid guardian phone number."

            else ->
                null
        }
    }

    // ============================================================
    // BUILD STUDENT
    // ============================================================

    private fun buildStudent(): Student {

        return Student(

            universityName =
                universityName.trim(),

            departmentName =
                departmentName.trim(),

            programName =
                programName.trim().uppercase(),

            semester =
                semester,

            session =
                session.trim(),

            section =
                section.trim().uppercase(),

            fullName =
                fullName.trim(),

            fatherName =
                fatherName.trim(),

            personalEmail =
                personalEmail.trim().lowercase(),

            contactNumber =
                ValidationUtils.normalizePhone(
                    contactNumber
                ),

            cnicNumber =
                ValidationUtils.normalizeCnic(
                    cnicNumber
                ),

            fatherCnicNumber =
                ValidationUtils.normalizeCnic(
                    fatherCnicNumber
                ),

            guardianNumber =
                ValidationUtils.normalizePhone(
                    guardianNumber
                ),

            regNo =
                regNo.trim()
        )
    }

    // ============================================================
    // SAVE
    // ============================================================

    fun saveStudent() {

        val student =
            buildStudent()

        _saveState.value =
            SaveUiState.Loading

        viewModelScope.launch {

            when (
                val result =
                    repository.addStudent(
                        student
                    )
            ) {

                is StudentRepository.SaveResult.Success -> {

                    _saveState.value =
                        SaveUiState.Success(

                            studentId =
                                result.studentId,

                            generatedStudentId =
                                result.generatedStudentId,

                            classId =
                                result.classId
                        )
                }

                is StudentRepository.SaveResult.Error -> {

                    _saveState.value =
                        SaveUiState.Error(
                            result.message
                        )
                }
            }
        }
    }

    // ============================================================
    // RESET
    // ============================================================

    fun resetPersonalInfoOnly() {

        fullName = ""
        fatherName = ""
        personalEmail = ""
        contactNumber = ""
        cnicNumber = ""
        fatherCnicNumber = ""
        guardianNumber = ""
        regNo = ""
    }
}