package com.nihongo.app.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihongo.app.di.AppContainer
import com.nihongo.app.domain.model.ProgressSummary
import com.nihongo.app.ui.common.UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class ProgressUiState(
    val days: Int = 7,
    val content: UiState<ProgressSummary> = UiState.Loading,
    val goalMinutes: Int = 20,
    val isSample: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressViewModel(container: AppContainer) : ViewModel() {

    private val repository = container.learningRepository

    private val days = MutableStateFlow(7)
    private val retryCounter = MutableStateFlow(0)

    private data class Loaded(val days: Int, val content: UiState<ProgressSummary>)

    private val loaded = combine(
        days,
        retryCounter,
        container.userDatabase.studyDao().observeSessionCount(),
    ) { d, _, _ -> d }
        .flatMapLatest { d ->
            flow {
                emit(Loaded(d, UiState.Loading))
                val result: UiState<ProgressSummary> = try {
                    UiState.Success(repository.getProgress(d))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    UiState.Error(e.message)
                }
                emit(Loaded(d, result))
            }
        }

    val uiState: StateFlow<ProgressUiState> = combine(
        loaded,
        container.preferencesRepository.preferences,
    ) { l, prefs ->
        ProgressUiState(
            days = l.days,
            content = l.content,
            goalMinutes = prefs.dailyGoalMinutes,
            isSample = repository.isSample,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProgressUiState(isSample = repository.isSample),
    )

    fun selectRange(newDays: Int) {
        days.value = newDays
    }

    fun retry() {
        retryCounter.update { it + 1 }
    }
}