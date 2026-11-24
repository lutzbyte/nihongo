package com.nihongo.app.ui.kanji

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
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihongo.app.R
import com.nihongo.app.ui.common.AppViewModelProvider
import com.nihongo.app.ui.common.UiState
import com.nihongo.app.ui.components.AppTopBar
import com.nihongo.app.ui.components.EmptyState
import com.nihongo.app.ui.components.ErrorState
import com.nihongo.app.ui.components.KanjiCard
import com.nihongo.app.ui.components.LevelSelector
import com.nihongo.app.ui.components.LoadingState
import com.nihongo.app.ui.components.SampleDataNotice
import com.nihongo.app.ui.theme.Spacing

@Composable
fun KanjiListScreen(
    onBack: () -> Unit,
    onOpenKanji: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: KanjiViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.listState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        AppTopBar(title = stringResource(R.string.category_kanji), onBack = onBack)

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
                UiState.Empty -> EmptyState(
                    icon = Icons.Filled.Inbox,
                    title = stringResource(R.string.empty_kanji_title),
                    message = stringResource(R.string.empty_kanji_message),
                )
                is UiState.Error -> ErrorState(onRetry = viewModel::retry)
                is UiState.Success -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    items(content.data, key = { it.id }) { kanji ->
                        KanjiCard(
                            character = kanji.character,
                            meaning = kanji.meaning,
                            onyomi = kanji.onyomi,
                            kunyomi = kanji.kunyomi,
                            onClick = { onOpenKanji(kanji.id) },
                        )
                    }
                }
            }
        }
    }
}