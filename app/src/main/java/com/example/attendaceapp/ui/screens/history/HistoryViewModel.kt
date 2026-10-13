package com.example.attendaceapp.ui.screens.history

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.attendaceapp.data.remote.response.AttendanceHistoryDto
import com.example.attendaceapp.data.repository.SupabaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State riwayat kehadiran (Task 4: dari Supabase, tanpa dummy).
 * List kosong berarti belum pernah absen → UI tampilkan empty state.
 */
data class HistoryUiState(
    val isLoading: Boolean = false,
    val history: List<AttendanceHistoryDto> = emptyList(),
    val error: String? = null
) {
    /** True bila tidak loading, tidak error, dan belum ada riwayat. */
    val isEmpty: Boolean
        get() = !isLoading && error == null && history.isEmpty()
}

class HistoryViewModel(private val repository: SupabaseRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    // ─── Load riwayat (HistoryPage, sekali via LaunchedEffect) ─────────────────

    fun loadHistory() {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getHistory()
                .onSuccess { list ->
                    _uiState.update { it.copy(isLoading = false, history = list) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
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
                    HistoryViewModel(SupabaseRepository(context = context)) as T
            }
    }
}
