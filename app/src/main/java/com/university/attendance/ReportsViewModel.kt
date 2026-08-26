package com.university.attendance

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class ReportsViewModel(
    private val repository: ReportsRepository = ReportsRepository()
) : ViewModel() {

    private val _summary = MutableLiveData<ReportsSummary?>(null)
    val summary: LiveData<ReportsSummary?> = _summary

    private val _departmentBreakdown = MutableLiveData<List<DepartmentReportRow>>(emptyList())
    val departmentBreakdown: LiveData<List<DepartmentReportRow>> = _departmentBreakdown

    private val _uiState = MutableLiveData<UiState>(UiState.Idle)
    val uiState: LiveData<UiState> = _uiState

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        object Loaded : UiState()
        data class Error(val message: String) : UiState()
    }

    fun loadReports() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            try {
                _summary.value = repository.getReportsSummary()
                _departmentBreakdown.value = repository.getDepartmentBreakdown()
                _uiState.value = UiState.Loaded
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load reports.")
            }
        }
    }
}