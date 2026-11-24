package com.nihongo.app.ui.kanji

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihongo.app.di.AppContainer
import com.nihongo.app.domain.model.JlptLevel
import com.nihongo.app.domain.model.KanjiItem
import com.nihongo.app.ui.common.UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class KanjiListUiState(
    val level: JlptLevel = JlptLevel.N5,
    val content: UiState<List<KanjiItem>> = UiState.Loading,
    val isSample: Boolean = false,
)

data class KanjiDetailUiState(
    val content: UiState<KanjiItem> = UiState.Loading,
    val bookmarks: Set<String> = emptySet(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class KanjiViewModel(container: AppContainer) : ViewModel() {

    private val repository = container.learningRepository
    private val preferencesRepository = container.preferencesRepository

    private val level = MutableStateFlow(JlptLevel.N5)
    private val detailId = MutableStateFlow<String?>(null)
    private val retryCounter = MutableStateFlow(0)

    val listState: StateFlow<KanjiListUiState> = combine(level, retryCounter) { l, _ -> l }
        .flatMapLatest { l ->
            flow {
                emit(KanjiListUiState(level = l, content = UiState.Loading, isSample = repository.isSample))
                val result: UiState<List<KanjiItem>> = try {
                    val items = repository.getKanjiList(l)
                    if (items.isEmpty()) UiState.Empty else UiState.Success(items)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    UiState.Error(e.message)
                }
                emit(KanjiListUiState(level = l, content = result, isSample = repository.isSample))
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = KanjiListUiState(isSample = repository.isSample),
        )

    val detailState: StateFlow<KanjiDetailUiState> = combine(
        detailId.filterNotNull(),
        retryCounter,
    ) { id, _ -> id }
        .flatMapLatest { id ->
            flow<UiState<KanjiItem>> {
                emit(UiState.Loading)
                val result: UiState<KanjiItem> = try {
                    val kanji = repository.getKanji(id)
                    if (kanji == null) UiState.Empty else UiState.Success(kanji)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    UiState.Error(e.message)
                }
                emit(result)
            }
        }
        .let { contentFlow ->
            combine(contentFlow, preferencesRepository.preferences) { content, prefs ->
                KanjiDetailUiState(content = content, bookmarks = prefs.bookmarks)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = KanjiDetailUiState(),
        )

    fun onLevelSelected(newLevel: JlptLevel) {
        level.value = newLevel
    }

    fun loadDetail(id: String) {
        detailId.value = id
    }

    fun retry() {
        retryCounter.update { it + 1 }
    }

    fun toggleBookmark(id: String) {
        viewModelScope.launch { preferencesRepository.toggleBookmark("kanji:$id") }
    }
}