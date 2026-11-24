package com.nihongo.app.ui.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihongo.app.di.AppContainer
import com.nihongo.app.domain.model.JlptLevel
import com.nihongo.app.domain.model.QuizQuestion
import com.nihongo.app.ui.common.UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuizUiState(
    val status: UiState<Unit> = UiState.Loading,
    val questions: List<QuizQuestion> = emptyList(),
    val index: Int = 0,
    val selected: Int? = null,
    val answers: Map<Int, Int> = emptyMap(),
    val elapsedSeconds: Int = 0,
    val finished: Boolean = false,
    val correctCount: Int = 0,
    val isSample: Boolean = false,
)

private const val QUIZ_SIZE = 10

class QuizViewModel(container: AppContainer) : ViewModel() {

    private val repository = container.learningRepository

    private val _state = MutableStateFlow(QuizUiState(isSample = repository.isSample))
    val state: StateFlow<QuizUiState> = _state.asStateFlow()

    private var startedLevel: JlptLevel? = null
    private var timerJob: Job? = null

    fun start(level: JlptLevel) {
        if (startedLevel == level) return
        startedLevel = level
        load(level)
    }

    fun retry() {
        startedLevel?.let { load(it) }
    }

    fun select(optionIndex: Int) {
        _state.update { current ->
            if (current.finished) current else current.copy(selected = optionIndex)
        }
    }

    fun next() {
        val current = _state.value
        val selected = current.selected ?: return
        val answers = current.answers + (current.index to selected)

        if (current.index == current.questions.lastIndex) {
            timerJob?.cancel()
            val correct = answers.count { (questionIndex, answer) ->
                current.questions[questionIndex].answerIndex == answer
            }
            _state.update {
                it.copy(answers = answers, finished = true, correctCount = correct, selected = null)
            }
        } else {
            val nextIndex = current.index + 1
            _state.update {
                it.copy(answers = answers, index = nextIndex, selected = answers[nextIndex])
            }
        }
    }

    private fun load(level: JlptLevel) {
        timerJob?.cancel()
        viewModelScope.launch {
            _state.value = QuizUiState(status = UiState.Loading, isSample = repository.isSample)
            val questions: List<QuizQuestion>? = try {
                repository.getQuizQuestions(level, QUIZ_SIZE)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            when {
                questions == null -> _state.update { it.copy(status = UiState.Error()) }
                questions.isEmpty() -> _state.update { it.copy(status = UiState.Empty) }
                else -> {
                    _state.update { it.copy(status = UiState.Success(Unit), questions = questions) }
                    startTimer()
                }
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1_000)
                _state.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
            }
        }
    }
}