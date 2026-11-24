package com.nihongo.app.ui.vocabulary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihongo.app.di.AppContainer
import com.nihongo.app.domain.model.JlptLevel
import com.nihongo.app.domain.model.VocabularyPage
import com.nihongo.app.ui.common.UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VocabularyUiState(
    val level: JlptLevel = JlptLevel.N5,
    val query: String = "",
    val page: Int = 0,
    val pageSize: Int = PAGE_SIZE,
    val content: UiState<VocabularyPage> = UiState.Loading,
    val bookmarks: Set<String> = emptySet(),
    val isSample: Boolean = false,
)

const val PAGE_SIZE = 20

@OptIn(ExperimentalCoroutinesApi::class)
class VocabularyViewModel(container: AppContainer) : ViewModel() {

    private val repository = container.learningRepository
    private val preferencesRepository = container.preferencesRepository

    private data class Params(
        val level: JlptLevel = JlptLevel.N5,
        val query: String = "",
        val page: Int = 0,
        val retry: Int = 0,
    )

    private val params = MutableStateFlow(Params())

    private val content: Flow<UiState<VocabularyPage>> = params.flatMapLatest { p ->
        flow<UiState<VocabularyPage>> {
            emit(UiState.Loading)
            val result: UiState<VocabularyPage> = try {
                val page = repository.getVocabulary(p.level, p.query, p.page, PAGE_SIZE)
                if (page.total == 0) UiState.Empty else UiState.Success(page)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                UiState.Error(e.message)
            }
            emit(result)
        }
    }

    val uiState: StateFlow<VocabularyUiState> = combine(
        params,
        content,
        preferencesRepository.preferences,
    ) { p, c, prefs ->
        VocabularyUiState(
            level = p.level,
            query = p.query,
            page = p.page,
            content = c,
            bookmarks = prefs.bookmarks,
            isSample = repository.isSample,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = VocabularyUiState(isSample = repository.isSample),
    )

    fun onLevelSelected(level: JlptLevel) {
        params.update { it.copy(level = level, page = 0) }
    }

    fun onQueryChange(query: String) {
        params.update { it.copy(query = query, page = 0) }
    }

    fun nextPage() {
        params.update { it.copy(page = it.page + 1) }
    }

    fun previousPage() {
        params.update { it.copy(page = (it.page - 1).coerceAtLeast(0)) }
    }

    fun retry() {
        params.update { it.copy(retry = it.retry + 1) }
    }

    fun toggleBookmark(id: String) {
        viewModelScope.launch { preferencesRepository.toggleBookmark("vocab:$id") }
    }
}