package com.nihongo.app.ui.practice

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihongo.app.di.AppContainer
import com.nihongo.app.domain.model.FlashCard
import com.nihongo.app.domain.model.JlptLevel
import com.nihongo.app.domain.model.SessionKind
import com.nihongo.app.domain.model.StudyScope
import com.nihongo.app.domain.srs.SrsGrade
import com.nihongo.app.domain.srs.SrsScheduler
import com.nihongo.app.ui.common.UiState
import java.time.LocalDate
import kotlin.math.min
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FlashcardUiState(
    val status: UiState<Unit> = UiState.Loading,
    val current: FlashCard? = null,
    val flipped: Boolean = false,
    val reviewed: Int = 0,
    val againCount: Int = 0,
    val remaining: Int = 0,
    val finished: Boolean = false,
    val elapsedSeconds: Int = 0,
    /** Jarak ulang berikutnya (hari) untuk tiap nilai, ditampilkan di tombol. */
    val previews: Map<SrsGrade, Int> = emptyMap(),
)

private const val CARD_TIME_CAP_MS = 60_000L

class FlashcardViewModel(private val container: AppContainer) : ViewModel() {

    private val repository = container.practiceRepository

    private val _state = MutableStateFlow(FlashcardUiState())
    val state: StateFlow<FlashcardUiState> = _state.asStateFlow()

    private val queue = ArrayDeque<FlashCard>()
    private var key: String? = null
    private var kind = SessionKind.FLASHCARD
    private var scope = StudyScope.ALL
    private var level: JlptLevel? = null
    private var startedAtMs = 0L
    private var activeMillis = 0L
    private var shownAt = 0L
    private var saved = false
    private var busy = false

    fun start(kind: SessionKind, scope: StudyScope, level: JlptLevel?) {
        val newKey = "$kind/$scope/${level?.name}"
        if (key == newKey) return
        key = newKey
        this.kind = kind
        this.scope = scope
        this.level = level
        load()
    }

    fun retry() {
        load()
    }

    fun flip() {
        _state.update { if (it.current != null) it.copy(flipped = true) else it }
    }

    fun grade(grade: SrsGrade) {
        val card = _state.value.current ?: return
        if (busy || !_state.value.flipped) return
        busy = true
        viewModelScope.launch {
            try {
                val next = repository.gradeCard(card, grade)
                activeMillis += min(SystemClock.elapsedRealtime() - shownAt, CARD_TIME_CAP_MS)
                queue.removeFirst()
                if (grade == SrsGrade.AGAIN) {
                    queue.addLast(card.copy(state = next, isNew = false))
                }
                _state.update {
                    it.copy(
                        reviewed = it.reviewed + 1,
                        againCount = it.againCount + if (grade == SrsGrade.AGAIN) 1 else 0,
                    )
                }
                showNext()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(status = UiState.Error(e.message)) }
            } finally {
                busy = false
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            queue.clear()
            activeMillis = 0L
            saved = false
            busy = false
            _state.value = FlashcardUiState()
            val deck: List<FlashCard>? = try {
                repository.buildDeck(kind, scope, level)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            when {
                deck == null -> _state.update { it.copy(status = UiState.Error()) }
                deck.isEmpty() -> _state.update { it.copy(status = UiState.Empty) }
                else -> {
                    queue.addAll(deck)
                    startedAtMs = System.currentTimeMillis()
                    _state.update { it.copy(status = UiState.Success(Unit)) }
                    showNext()
                }
            }
        }
    }

    private fun showNext() {
        val next = queue.firstOrNull()
        if (next == null) {
            finish()
            return
        }
        shownAt = SystemClock.elapsedRealtime()
        val today = LocalDate.now().toEpochDay()
        val previews = SrsGrade.entries.associateWith {
            SrsScheduler.next(next.state, it, today).intervalDays
        }
        _state.update {
            it.copy(current = next, flipped = false, remaining = queue.size, previews = previews)
        }
    }

    private fun finish() {
        _state.update {
            it.copy(
                current = null,
                finished = true,
                remaining = 0,
                elapsedSeconds = (activeMillis / 1000).toInt(),
            )
        }
        saveSession()
    }

    private fun saveSession() {
        val snapshot = _state.value
        if (saved || snapshot.reviewed == 0) return
        saved = true
        val duration = (activeMillis / 1000).toInt()
        container.applicationScope.launch {
            repository.recordFlashcardSession(
                kind = kind,
                level = level,
                startedAtEpochMs = startedAtMs,
                durationSeconds = duration,
                reviewed = snapshot.reviewed,
                againCount = snapshot.againCount,
            )
        }
    }

    override fun onCleared() {
        saveSession()
    }
}