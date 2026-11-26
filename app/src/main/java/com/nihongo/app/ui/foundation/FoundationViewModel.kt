package com.nihongo.app.ui.foundation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihongo.app.di.AppContainer
import com.nihongo.app.domain.model.KanaItem
import com.nihongo.app.domain.model.KanaScript
import com.nihongo.app.domain.model.Lesson
import com.nihongo.app.domain.model.LessonSummary
import com.nihongo.app.ui.common.UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalCoroutinesApi::class)
class FoundationViewModel(container: AppContainer) : ViewModel() {

    private val repository = container.learningRepository

    private val retryCounter = MutableStateFlow(0)
    private val script = MutableStateFlow<KanaScript?>(null)
    private val lessonId = MutableStateFlow<String?>(null)

    val lessons: StateFlow<UiState<List<LessonSummary>>> = retryCounter
        .flatMapLatest {
            loadState<List<LessonSummary>> {
                val items = repository.getLessons()
                if (items.isEmpty()) UiState.Empty else UiState.Success(items)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    val kana: StateFlow<UiState<List<KanaItem>>> = script.filterNotNull()
        .combine(retryCounter) { s, _ -> s }
        .flatMapLatest { s ->
            loadState<List<KanaItem>> {
                val items = repository.getKana(s)
                if (items.isEmpty()) UiState.Empty else UiState.Success(items)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    val lesson: StateFlow<UiState<Lesson>> = lessonId.filterNotNull()
        .combine(retryCounter) { id, _ -> id }
        .flatMapLatest { id ->
            loadState<Lesson> {
                val item = repository.getLesson(id)
                if (item == null) UiState.Empty else UiState.Success(item)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    fun loadKana(newScript: KanaScript) {
        script.value = newScript
    }

    fun loadLesson(id: String) {
        lessonId.value = id
    }

    fun retry() {
        retryCounter.update { it + 1 }
    }

    private fun <T> loadState(block: suspend () -> UiState<T>): Flow<UiState<T>> = flow {
        emit(UiState.Loading)
        val result: UiState<T> = try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            UiState.Error(e.message)
        }
        emit(result)
    }
}