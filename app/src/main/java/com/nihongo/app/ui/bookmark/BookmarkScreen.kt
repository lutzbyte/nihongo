package com.nihongo.app.ui.bookmark

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihongo.app.R
import com.nihongo.app.domain.model.BookmarkedItems
import com.nihongo.app.ui.common.AppViewModelProvider
import com.nihongo.app.ui.common.UiState
import com.nihongo.app.ui.components.AppTopBar
import com.nihongo.app.ui.components.ChoiceChips
import com.nihongo.app.ui.components.EmptyState
import com.nihongo.app.ui.components.ErrorState
import com.nihongo.app.ui.components.GrammarCard
import com.nihongo.app.ui.components.KanjiCard
import com.nihongo.app.ui.components.LoadingState
import com.nihongo.app.ui.components.VocabularyCard
import com.nihongo.app.ui.theme.Spacing

@Composable
fun BookmarkScreen(
    onBack: () -> Unit,
    onOpenVocabulary: (String) -> Unit,
    onOpenKanji: (String) -> Unit,
    onOpenGrammar: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookmarkViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val tabs = listOf(
        BookmarkTab.VOCABULARY to stringResource(R.string.category_vocabulary),
        BookmarkTab.KANJI to stringResource(R.string.category_kanji),
        BookmarkTab.GRAMMAR to stringResource(R.string.category_grammar),
    )

    Column(modifier = modifier.fillMaxSize()) {
        AppTopBar(title = stringResource(R.string.bookmark_title), onBack = onBack)

        ChoiceChips(
            options = tabs,
            selected = state.tab,
            onSelect = viewModel::selectTab,
            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (val content = state.content) {
                UiState.Loading -> LoadingState()
                UiState.Empty -> BookmarkEmpty()
                is UiState.Error -> ErrorState(onRetry = viewModel::retry)
                is UiState.Success -> BookmarkList(
                    tab = state.tab,
                    items = content.data,
                    onToggle = viewModel::toggleBookmark,
                    onOpenVocabulary = onOpenVocabulary,
                    onOpenKanji = onOpenKanji,
                    onOpenGrammar = onOpenGrammar,
                )
            }
        }
    }
}

@Composable
private fun BookmarkEmpty() {
    EmptyState(
        icon = Icons.Filled.BookmarkBorder,
        title = stringResource(R.string.bookmark_empty_title),
        message = stringResource(R.string.bookmark_empty_message),
    )
}

@Composable
private fun BookmarkList(
    tab: BookmarkTab,
    items: BookmarkedItems,
    onToggle: (String) -> Unit,
    onOpenVocabulary: (String) -> Unit,
    onOpenKanji: (String) -> Unit,
    onOpenGrammar: (String) -> Unit,
) {
    val isTabEmpty = when (tab) {
        BookmarkTab.VOCABULARY -> items.vocabulary.isEmpty()
        BookmarkTab.KANJI -> items.kanji.isEmpty()
        BookmarkTab.GRAMMAR -> items.grammar.isEmpty()
    }
    if (isTabEmpty) {
        BookmarkEmpty()
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        when (tab) {
            BookmarkTab.VOCABULARY -> items(items.vocabulary, key = { it.id }) { item ->
                VocabularyCard(
                    word = item.word,
                    reading = item.reading.takeIf { it != item.word },
                    meaning = item.meaning,
                    bookmarked = true,
                    onToggleBookmark = { onToggle("vocab:${item.id}") },
                    onClick = { onOpenVocabulary(item.id) },
                )
            }
            BookmarkTab.KANJI -> items(items.kanji, key = { it.id }) { item ->
                KanjiCard(
                    character = item.character,
                    meaning = item.meaning,
                    onyomi = item.onyomi,
                    kunyomi = item.kunyomi,
                    onClick = { onOpenKanji(item.id) },
                )
            }
            BookmarkTab.GRAMMAR -> items(items.grammar, key = { it.id }) { item ->
                GrammarCard(
                    pattern = item.pattern,
                    meaning = item.meaning,
                    bookmarked = true,
                    onToggleBookmark = { onToggle("grammar:${item.id}") },
                    onClick = { onOpenGrammar(item.id) },
                )
            }
        }
    }
}