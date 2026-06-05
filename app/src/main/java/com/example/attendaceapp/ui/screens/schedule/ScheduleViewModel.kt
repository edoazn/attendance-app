package com.example.attendaceapp.ui.screens.schedule

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.attendaceapp.data.remote.response.ScheduleDto
import com.example.attendaceapp.data.repository.ApiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScheduleUiState(
    val isLoadingToday: Boolean = false,
    val isLoadingAll: Boolean = false,
    val todaySchedules: List<ScheduleDto> = emptyList(),
    val allSchedules: List<ScheduleDto> = emptyList(),
    val error: String? = null
) {
    /** Filter jadwal berdasarkan nama hari (English, case-insensitive). */
    fun schedulesForDay(dayName: String): List<ScheduleDto> =
        allSchedules.filter { it.day.orEmpty().equals(dayName, ignoreCase = true) }
}

class ScheduleViewModel(private val repository: ApiRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    // ─── Load jadwal hari ini (HomePage) ──────────────────────────────────────

    fun loadTodaySchedules() {
        if (_uiState.value.isLoadingToday) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingToday = true, error = null) }
            repository.getTodaySchedules()
                .onSuccess { list ->
                    _uiState.update { it.copy(isLoadingToday = false, todaySchedules = list) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoadingToday = false, error = e.message) }
                }
        }
    }

    // ─── Load semua jadwal (SchedulePage) ─────────────────────────────────────

    fun loadAllSchedules() {
        if (_uiState.value.isLoadingAll) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAll = true, error = null) }
            repository.getAllSchedules()
                .onSuccess { list ->
                    _uiState.update { it.copy(isLoadingAll = false, allSchedules = list) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoadingAll = false, error = e.message) }
                }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }

    // ─── Factory ───────────────────────────────────────────────────────────────

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ScheduleViewModel(ApiRepository(context = context)) as T
            }
    }
}
