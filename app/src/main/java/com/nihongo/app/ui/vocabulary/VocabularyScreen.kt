package com.nihongo.app.ui.vocabulary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihongo.app.R
import com.nihongo.app.ui.common.AppViewModelProvider
import com.nihongo.app.ui.common.UiState
import com.nihongo.app.ui.components.AppSearchBar
import com.nihongo.app.ui.components.AppTopBar
import com.nihongo.app.ui.components.EmptyState
import com.nihongo.app.ui.components.ErrorState
import com.nihongo.app.ui.components.LevelSelector
import com.nihongo.app.ui.components.LoadingState
import com.nihongo.app.ui.components.SampleDataNotice
import com.nihongo.app.ui.components.VocabularyCard
import com.nihongo.app.ui.theme.Spacing

@Composable
fun VocabularyScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VocabularyViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var searching by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(state.page, state.level) { listState.scrollToItem(0) }

    Column(modifier = modifier.fillMaxSize()) {
        if (searching) {
            AppSearchBar(
                query = state.query,
                onQueryChange = viewModel::onQueryChange,
                onClose = {
                    searching = false
                    viewModel.onQueryChange("")
                },
                placeholder = stringResource(R.string.vocab_search_hint),
            )
        } else {
            AppTopBar(
                title = stringResource(R.string.category_vocabulary),
                onBack = onBack,
                actions = {
                    IconButton(onClick = { searching = true }) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = stringResource(R.string.action_search),
                        )
                    }
                },
            )
        }

        if (state.isSample) {
            SampleDataNotice(modifier = Modifier.padding(horizontal = Spacing.lg))
        }

        LevelSelector(
            selected = state.level,
            onSelect = viewModel::onLevelSelected,
            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (val content = state.content) {
                UiState.Loading -> LoadingState()
                UiState.Empty -> {
                    if (state.query.isNotBlank()) {
                        EmptyState(
                            icon = Icons.Filled.SearchOff,
                            title = stringResource(R.string.empty_search_title),
                            message = stringResource(R.string.empty_search_message),
                        )
                    } else {
                        EmptyState(
                            icon = Icons.Filled.Inbox,
                            title = stringResource(R.string.empty_vocab_title),
                            message = stringResource(R.string.empty_vocab_message),
                        )
                    }
                }
                is UiState.Error -> ErrorState(onRetry = viewModel::retry)
                is UiState.Success -> LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    items(content.data.items, key = { it.id }) { item ->
                        VocabularyCard(
                            word = item.word,
                            reading = item.reading.takeIf { it != item.word },
                            meaning = item.meaning,
                            bookmarked = "vocab:${item.id}" in state.bookmarks,
                            onToggleBookmark = { viewModel.toggleBookmark(item.id) },
                        )
                    }
                }
            }
        }

        val success = state.content as? UiState.Success
        if (success != null) {
            val start = state.page * state.pageSize + 1
            val end = start + success.data.items.size - 1
            PaginationFooter(
                rangeText = stringResource(R.string.pagination_range, start, end, success.data.total),
                hasPrevious = state.page > 0,
                hasNext = end < success.data.total,
                onPrevious = viewModel::previousPage,
                onNext = viewModel::nextPage,
            )
        }
    }
}

@Composable
private fun PaginationFooter(
    rangeText: String,
    hasPrevious: Boolean,
    hasNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text = rangeText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        OutlinedIconButton(onClick = onPrevious, enabled = hasPrevious) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = stringResource(R.string.page_previous))
        }
        OutlinedIconButton(onClick = onNext, enabled = hasNext) {
            Icon(Icons.Filled.ChevronRight, contentDescription = stringResource(R.string.page_next))
        }
    }
}