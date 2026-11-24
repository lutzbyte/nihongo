package com.nihongo.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihongo.app.di.AppContainer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val todayMinutes: Int = 0,
    val goalMinutes: Int = 20,
    val showSampleNotice: Boolean = false,
)

class HomeViewModel(container: AppContainer) : ViewModel() {

    private val repository = container.learningRepository

    val uiState: StateFlow<HomeUiState> = container.preferencesRepository.preferences
        .map { prefs ->
            val today = try {
                repository.getProgress(7).todayMinutes
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                0
            }
            HomeUiState(
                todayMinutes = today,
                goalMinutes = prefs.dailyGoalMinutes,
                showSampleNotice = repository.isSample,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(showSampleNotice = repository.isSample),
        )
}