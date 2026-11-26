package com.nihongo.app.ui.vocabulary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihongo.app.di.AppContainer
import com.nihongo.app.domain.model.VocabularyDetail
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

data class VocabularyDetailUiState(
    val content: UiState<VocabularyDetail> = UiState.Loading,
    val bookmarks: Set<String> = emptySet(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class VocabularyDetailViewModel(container: AppContainer) : ViewModel() {

    private val repository = container.learningRepository
    private val preferencesRepository = container.preferencesRepository

    private val vocabularyId = MutableStateFlow<String?>(null)
    private val retryCounter = MutableStateFlow(0)

    private val content: Flow<UiState<VocabularyDetail>> =
        combine(vocabularyId.filterNotNull(), retryCounter) { id, _ -> id }
            .flatMapLatest { id ->
                flow {
                    emit(UiState.Loading)
                    val result: UiState<VocabularyDetail> = try {
                        val detail = repository.getVocabularyDetail(id)
                        if (detail == null) UiState.Empty else UiState.Success(detail)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        UiState.Error(e.message)
                    }
                    emit(result)
                }
            }

    val state: StateFlow<VocabularyDetailUiState> = combine(
        content,
        preferencesRepository.preferences,
    ) { detail, prefs ->
        VocabularyDetailUiState(content = detail, bookmarks = prefs.bookmarks)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = VocabularyDetailUiState(),
    )

    fun load(id: String) {
        vocabularyId.value = id
    }

    fun retry() {
        retryCounter.update { it + 1 }
    }

    fun toggleBookmark(id: String) {
        viewModelScope.launch { preferencesRepository.toggleBookmark("vocab:$id") }
    }
}