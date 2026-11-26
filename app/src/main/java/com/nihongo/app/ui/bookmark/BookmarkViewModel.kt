package com.nihongo.app.ui.bookmark

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihongo.app.di.AppContainer
import com.nihongo.app.domain.model.BookmarkedItems
import com.nihongo.app.ui.common.UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class BookmarkTab { VOCABULARY, KANJI, GRAMMAR }

data class BookmarkUiState(
    val tab: BookmarkTab = BookmarkTab.VOCABULARY,
    val content: UiState<BookmarkedItems> = UiState.Loading,
)

@OptIn(ExperimentalCoroutinesApi::class)
class BookmarkViewModel(container: AppContainer) : ViewModel() {

    private val repository = container.learningRepository
    private val preferencesRepository = container.preferencesRepository

    private val tab = MutableStateFlow(BookmarkTab.VOCABULARY)
    private val retryCounter = MutableStateFlow(0)

    private val content: Flow<UiState<BookmarkedItems>> = combine(
        preferencesRepository.preferences.map { it.bookmarks }.distinctUntilChanged(),
        retryCounter,
    ) { keys, _ -> keys }
        .mapLatest { keys ->
            val result: UiState<BookmarkedItems> = try {
                val items = repository.getBookmarks(keys)
                if (items.isEmpty) UiState.Empty else UiState.Success(items)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                UiState.Error(e.message)
            }
            result
        }

    val uiState: StateFlow<BookmarkUiState> = combine(tab, content) { t, c ->
        BookmarkUiState(tab = t, content = c)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BookmarkUiState(),
    )

    fun selectTab(newTab: BookmarkTab) {
        tab.value = newTab
    }

    fun retry() {
        retryCounter.update { it + 1 }
    }

    /** Kunci berformat "vocab:ID", "kanji:ID", atau "grammar:ID". */
    fun toggleBookmark(key: String) {
        viewModelScope.launch { preferencesRepository.toggleBookmark(key) }
    }
}