package com.jobalerts.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jobalerts.app.domain.models.JobPost
import com.jobalerts.app.domain.repository.JobRepository
import com.jobalerts.app.presentation.settings.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class JobsUiState {
    object Loading : JobsUiState()
    data class Success(val jobs: List<JobPost>) : JobsUiState()
    data class Error(val message: String) : JobsUiState()
}

/**
 * ViewModel for the Jobs feed (and the Results tab, which reuses the same data
 * filtered by category).
 *
 * Exposes:
 *   - [uiState]          — Loading / Success(filtered) / Error
 *   - [selectedFilter]   — active qualification filter chip
 *   - [searchQuery]      — current search box text
 *   - [allJobs]          — full unfiltered list, so the detail screen can
 *                          always find a job by ID even when the active filter
 *                          would have hidden it
 *
 * Construct via [factory] so it can be wired up with the repository from
 * [com.jobalerts.app.core.di.AppContainer] in AppNavigation.
 */
class JobsViewModel(
    private val repository: JobRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<JobsUiState>(JobsUiState.Loading)
    val uiState: StateFlow<JobsUiState> = _uiState.asStateFlow()

    private val _selectedFilter = MutableStateFlow("All")
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Full unfiltered list exposed so detail screen can always find a job by ID
    private val _allJobs = MutableStateFlow<List<JobPost>>(emptyList())
    val allJobs: StateFlow<List<JobPost>> = _allJobs.asStateFlow()

    private var isRefreshing = false

    init { fetchJobs() }

    fun fetchJobs() {
        if (isRefreshing) return
        isRefreshing = true
        _uiState.value = JobsUiState.Loading
        AppLogger.info("JobsViewModel", "Fetching jobs…")

        viewModelScope.launch {
            try {
                repository.syncLatestJobs()
                repository.getJobs().collect { jobs ->
                    AppLogger.info("JobsViewModel", "${jobs.size} jobs received")
                    _allJobs.value = jobs
                    applyFilter()
                    isRefreshing = false
                }
            } catch (e: Exception) {
                val msg = e.message ?: "Unknown error"
                AppLogger.error("JobsViewModel", "Fetch failed: $msg")
                _uiState.value = JobsUiState.Error(msg)
                isRefreshing = false
            }
        }
    }

    fun retry() {
        AppLogger.info("JobsViewModel", "Retry triggered")
        fetchJobs()
    }

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
        applyFilter()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilter()
    }

    private fun applyFilter() {
        val filter = _selectedFilter.value
        val query = _searchQuery.value.trim()

        var filtered = when (filter) {
            "10th Pass" -> _allJobs.value.filter {
                it.qualificationTag.contains("10th", ignoreCase = true)
            }
            "12th Pass" -> _allJobs.value.filter {
                it.qualificationTag.contains("12th", ignoreCase = true)
            }
            "Graduation" -> _allJobs.value.filter {
                it.qualificationTag.contains("Grad", ignoreCase = true) ||
                it.qualificationTag.contains("Bachelor", ignoreCase = true) ||
                it.qualificationTag.contains("Graduate", ignoreCase = true)
            }
            else -> _allJobs.value
        }

        if (query.isNotEmpty()) {
            filtered = filtered.filter { job ->
                job.title.contains(query, ignoreCase = true) ||
                job.category.contains(query, ignoreCase = true) ||
                job.qualification.contains(query, ignoreCase = true) ||
                job.qualificationTag.contains(query, ignoreCase = true)
            }
        }

        _uiState.value = JobsUiState.Success(filtered)
    }

    companion object {
        fun factory(repository: JobRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>) =
                JobsViewModel(repository) as T
        }
    }
}
