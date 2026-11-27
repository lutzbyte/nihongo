package com.nihongo.app.ui.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihongo.app.di.AppContainer
import com.nihongo.app.domain.model.JlptLevel
import com.nihongo.app.domain.model.PracticeQuestion
import com.nihongo.app.domain.model.PracticeSection
import com.nihongo.app.domain.model.SessionKind
import com.nihongo.app.ui.common.UiState
import kotlin.math.max
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Batas waktu Simulasi. Jumlah soal Simulasi ditentukan di RoomPracticeRepository. */
const val EXAM_MINUTES = 30

private const val QUIZ_SIZE = 10

data class QuizUiState(
    val status: UiState<Unit> = UiState.Loading,
    val questions: List<PracticeQuestion> = emptyList(),
    val index: Int = 0,
    val selected: Int? = null,
    val answers: Map<Int, Int> = emptyMap(),
    val elapsedSeconds: Int = 0,
    val remainingSeconds: Int? = null,
    val finished: Boolean = false,
    val timedOut: Boolean = false,
    val correctCount: Int = 0,
    val isExam: Boolean = false,
)

class QuizViewModel(private val container: AppContainer) : ViewModel() {

    private val repository = container.practiceRepository

    private val _state = MutableStateFlow(QuizUiState())
    val state: StateFlow<QuizUiState> = _state.asStateFlow()

    private var key: String? = null
    private var level = JlptLevel.N5
    private var section = PracticeSection.MIXED
    private var kind = SessionKind.QUIZ
    private var startedAtMs = 0L
    private var timerJob: Job? = null
    private var saved = false

    fun start(level: JlptLevel, section: PracticeSection, kind: SessionKind) {
        val newKey = "${level.name}/${section.name}/${kind.name}"
        if (key == newKey) return
        key = newKey
        this.level = level
        this.section = section
        this.kind = kind
        load()
    }

    fun retry() {
        load()
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
            _state.update { it.copy(answers = answers, selected = null) }
            finish(timedOut = false)
        } else {
            val nextIndex = current.index + 1
            _state.update {
                it.copy(answers = answers, index = nextIndex, selected = answers[nextIndex])
            }
        }
    }

    private fun load() {
        timerJob?.cancel()
        saved = false
        viewModelScope.launch {
            _state.value = QuizUiState(isExam = kind == SessionKind.EXAM)
            val questions: List<PracticeQuestion>? = try {
                if (kind == SessionKind.EXAM) {
                    repository.buildExam(level)
                } else {
                    repository.buildQuestions(level, section, QUIZ_SIZE)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            when {
                questions == null -> _state.update { it.copy(status = UiState.Error()) }
                questions.isEmpty() -> _state.update { it.copy(status = UiState.Empty) }
                else -> {
                    startedAtMs = System.currentTimeMillis()
                    _state.update {
                        it.copy(
                            status = UiState.Success(Unit),
                            questions = questions,
                            remainingSeconds = if (kind == SessionKind.EXAM) EXAM_MINUTES * 60 else null,
                        )
                    }
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
                val remaining = _state.value.remainingSeconds
                _state.update {
                    it.copy(
                        elapsedSeconds = it.elapsedSeconds + 1,
                        remainingSeconds = remaining?.let { r -> max(0, r - 1) },
                    )
                }
                if (remaining != null && remaining - 1 <= 0) {
                    finish(timedOut = true)
                    break
                }
            }
        }
    }

    private fun finish(timedOut: Boolean) {
        timerJob?.cancel()
        val current = _state.value
        if (current.finished) return

        val pending = current.selected
        val answers = if (pending != null && !current.answers.containsKey(current.index)) {
            current.answers + (current.index to pending)
        } else {
            current.answers
        }
        val correct = current.questions.indices.count { answers[it] == current.questions[it].answerIndex }
        _state.update {
            it.copy(
                answers = answers,
                finished = true,
                timedOut = timedOut,
                correctCount = correct,
                selected = null,
            )
        }

        if (!saved) {
            saved = true
            val resultKind = kind
            val resultSection = if (kind == SessionKind.EXAM) PracticeSection.MIXED else section
            val resultLevel = level
            val duration = current.elapsedSeconds
            val startedAt = startedAtMs
            container.applicationScope.launch {
                repository.recordQuizResult(
                    kind = resultKind,
                    section = resultSection,
                    level = resultLevel,
                    questions = current.questions,
                    answers = answers,
                    startedAtEpochMs = startedAt,
                    durationSeconds = duration,
                )
            }
        }
    }
}