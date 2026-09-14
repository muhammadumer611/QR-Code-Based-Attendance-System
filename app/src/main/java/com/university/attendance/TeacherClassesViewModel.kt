package com.university.attendance
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class TeacherClassesViewModel(
    private val repository: TeacherSubjectsRepository =
        TeacherSubjectsRepository()
) : ViewModel() {

    sealed class UiState {

        object Idle : UiState()

        object Loading : UiState()

        object Success : UiState()

        data class Error(
            val message: String
        ) : UiState()
    }

    private val _uiState =
        MutableLiveData<UiState>(UiState.Idle)

    val uiState: LiveData<UiState> =
        _uiState

    private val _classes =
        MutableLiveData<List<Subject>>(emptyList())

    val classes: LiveData<List<Subject>> =
        _classes

    fun loadClasses(
        teacherId: String
    ) {

        if (teacherId.isBlank()) {

            _uiState.value =
                UiState.Error(
                    "Teacher ID not found."
                )

            return
        }

        _uiState.value =
            UiState.Loading

        viewModelScope.launch {

            try {

                val subjects =
                    repository.getAssignedSubjects(
                        teacherId
                    )

                _classes.value =
                    subjects

                _uiState.value =
                    UiState.Success

            } catch (e: Exception) {

                _classes.value =
                    emptyList()

                _uiState.value =
                    UiState.Error(
                        e.message
                            ?: "Failed to load teacher classes."
                    )
            }
        }
    }
}

