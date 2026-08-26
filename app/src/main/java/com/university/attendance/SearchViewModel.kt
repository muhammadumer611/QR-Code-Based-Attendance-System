package com.university.attendance

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchViewModel(
    private val repository: SearchRepository = SearchRepository()
) : ViewModel() {

    private val _results = MutableLiveData<List<SearchResult>>(emptyList())
    val results: LiveData<List<SearchResult>> = _results

    private val _isSearching = MutableLiveData(false)
    val isSearching: LiveData<Boolean> = _isSearching

    private var searchJob: Job? = null

    /**
     * Called on every text change. Debounces by 300ms so we don't fire a
     * Firestore query on every keystroke -- only after the user pauses
     * typing briefly.
     */
    fun onQueryChanged(query: String) {
        searchJob?.cancel()

        if (query.isBlank()) {
            _results.value = emptyList()
            _isSearching.value = false
            return
        }

        searchJob = viewModelScope.launch {
            _isSearching.value = true
            delay(300)
            _results.value = repository.search(query)
            _isSearching.value = false
        }
    }

    fun clearResults() {
        searchJob?.cancel()
        _results.value = emptyList()
    }
}