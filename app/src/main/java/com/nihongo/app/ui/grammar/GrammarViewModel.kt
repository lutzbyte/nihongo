package com.nihongo.app.ui.grammar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihongo.app.di.AppContainer
import com.nihongo.app.domain.model.GrammarItem
import com.nihongo.app.domain.model.JlptLevel
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
import kotlinx.coroutines.launch

data class GrammarListUiState(
    val level: JlptLevel = JlptLevel.N5,
    val query: String = "",
    val content: UiState<List<GrammarItem>> = UiState.Loading,
    val bookmarks: Set<String> = emptySet(),
)

data class GrammarDetailUiState(
    val content: UiState<GrammarItem> = UiState.Loading,
    val bookmarks: Set<String> = emptySet(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class GrammarViewModel(container: AppContainer) : ViewModel() {

    private val repository = container.learningRepository
    private val preferencesRepository = container.preferencesRepository

    private val level = MutableStateFlow(JlptLevel.N5)
    private val query = MutableStateFlow("")
    private val detailId = MutableStateFlow<String?>(null)
    private val retryCounter = MutableStateFlow(0)

    private data class ListLoaded(
        val level: JlptLevel,
        val query: String,
        val content: UiState<List<GrammarItem>>,
    )

    private val listLoaded: Flow<ListLoaded> =
        combine(level, query, retryCounter) { l, q, _ -> l to q }
            .flatMapLatest { (l, q) ->
                flow {
                    emit(ListLoaded(l, q, UiState.Loading))
                    val result: UiState<List<GrammarItem>> = try {
                        val items = repository.getGrammarList(l, q)
                        if (items.isEmpty()) UiState.Empty else UiState.Success(items)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        UiState.Error(e.message)
                    }
                    emit(ListLoaded(l, q, result))
                }
            }

    val listState: StateFlow<GrammarListUiState> = combine(
        listLoaded,
        preferencesRepository.preferences,
    ) { loaded, prefs ->
        GrammarListUiState(
            level = loaded.level,
            query = loaded.query,
            content = loaded.content,
            bookmarks = prefs.bookmarks,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GrammarListUiState(),
    )

    private val detailContent: Flow<UiState<GrammarItem>> =
        combine(detailId.filterNotNull(), retryCounter) { id, _ -> id }
            .flatMapLatest { id ->
                flow {
                    emit(UiState.Loading)
                    val result: UiState<GrammarItem> = try {
                        val item = repository.getGrammar(id)
                        if (item == null) UiState.Empty else UiState.Success(item)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        UiState.Error(e.message)
                    }
                    emit(result)
                }
            }

    val detailState: StateFlow<GrammarDetailUiState> = combine(
        detailContent,
        preferencesRepository.preferences,
    ) { content, prefs ->
        GrammarDetailUiState(content = content, bookmarks = prefs.bookmarks)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GrammarDetailUiState(),
    )

    fun onLevelSelected(newLevel: JlptLevel) {
        level.value = newLevel
    }

    fun onQueryChange(newQuery: String) {
        query.value = newQuery
    }

    fun loadDetail(id: String) {
        detailId.value = id
    }

    fun retry() {
        retryCounter.update { it + 1 }
    }

    fun toggleBookmark(id: String) {
        viewModelScope.launch { preferencesRepository.toggleBookmark("grammar:$id") }
    }
}