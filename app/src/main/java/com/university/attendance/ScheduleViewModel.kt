package com.university.attendance

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class ScheduleViewModel(
    private val repository: ScheduleRepository = ScheduleRepository()
) : ViewModel() {

    private val _schedule = MutableLiveData<Schedule?>(null)
    val schedule: LiveData<Schedule?> = _schedule

    private val _uiState = MutableLiveData<UiState>(UiState.Idle)
    val uiState: LiveData<UiState> = _uiState

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        object Loaded : UiState()
        object SaveSuccess : UiState()
        data class Error(val message: String) : UiState()
    }

    fun loadSchedule() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            try {
                _schedule.value = repository.getCurrentSchedule()
                _uiState.value = UiState.Loaded
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load schedule.")
            }
        }
    }

    fun saveSchedule(fileName: String, note: String) {
        if (fileName.isBlank()) {
            _uiState.value = UiState.Error("Please enter a file name.")
            return
        }
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            when (val result = repository.saveSchedule(fileName, note)) {
                is ScheduleRepository.OpResult.Success -> {
                    _uiState.value = UiState.SaveSuccess
                    loadSchedule() // refresh with the newly saved record
                }
                is ScheduleRepository.OpResult.Error -> _uiState.value = UiState.Error(result.message)
            }
        }
    }
}