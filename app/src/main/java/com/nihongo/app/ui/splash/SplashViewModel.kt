package com.nihongo.app.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihongo.app.data.importer.ImportResult
import com.nihongo.app.di.AppContainer
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface SplashUiState {
    data class Loading(val progress: Float) : SplashUiState
    data object Ready : SplashUiState
    data class Error(val message: String) : SplashUiState
}

private const val MIN_SPLASH_MS = 1_000L

class SplashViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow<SplashUiState>(SplashUiState.Loading(0f))
    val state: StateFlow<SplashUiState> = _state.asStateFlow()

    private var job: Job? = null

    init {
        start()
    }

    fun retry() {
        start()
    }

    fun continueAnyway() {
        _state.value = SplashUiState.Ready
    }

    private fun start() {
        job?.cancel()
        _state.value = SplashUiState.Loading(0f)
        job = viewModelScope.launch {
            val minimumDisplay = async { delay(MIN_SPLASH_MS) }
            val result = container.curriculumImporter.run { progress ->
                _state.update { current ->
                    if (current is SplashUiState.Loading) SplashUiState.Loading(progress) else current
                }
            }
            minimumDisplay.await()
            _state.value = when (result) {
                is ImportResult.Failed ->
                    if (result.hasExistingData) SplashUiState.Ready else SplashUiState.Error(result.message)
                else -> SplashUiState.Ready
            }
        }
    }
}