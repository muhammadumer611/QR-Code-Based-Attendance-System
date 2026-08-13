package com.university.attendance

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DailyOverviewViewModel(
    private val repository: DailyOverviewRepository = DailyOverviewRepository()
) : ViewModel() {

    private val _uiState = MutableLiveData<UiState>(UiState.Idle)
    val uiState: LiveData<UiState> = _uiState

    private val _groups = MutableLiveData<List<DailyDepartmentGroup>>(emptyList())
    val groups: LiveData<List<DailyDepartmentGroup>> = _groups

    private val _summary = MutableLiveData<DailySummary>(DailySummary(0, 0))
    val summary: LiveData<DailySummary> = _summary

    var selectedDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        private set

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        object Loaded : UiState()
        data class Error(val message: String) : UiState()
    }

    data class DailySummary(val heldCount: Int, val notMarkedCount: Int)

    fun setDate(date: String) {
        selectedDate = date
        loadOverview()
    }

    fun loadOverview() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val result = repository.getDailyOverview(selectedDate)
                _groups.value = result

                val allStatuses = result.flatMap { dept -> dept.sessionGroups.flatMap { it.classStatuses } }
                val held = allStatuses.count { it.wasMarked }
                val notMarked = allStatuses.count { !it.wasMarked }
                _summary.value = DailySummary(heldCount = held, notMarkedCount = notMarked)

                _uiState.value = UiState.Loaded
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load daily overview.")
            }
        }
    }
}