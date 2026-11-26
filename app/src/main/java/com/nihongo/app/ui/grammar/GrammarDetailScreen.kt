package com.nihongo.app.ui.grammar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihongo.app.R
import com.nihongo.app.domain.model.GrammarItem
import com.nihongo.app.ui.common.AppViewModelProvider
import com.nihongo.app.ui.common.UiState
import com.nihongo.app.ui.components.AppTopBar
import com.nihongo.app.ui.components.BookmarkIconButton
import com.nihongo.app.ui.components.EmptyState
import com.nihongo.app.ui.components.ErrorState
import com.nihongo.app.ui.components.LoadingState
import com.nihongo.app.ui.theme.Elevation
import com.nihongo.app.ui.theme.PillShape
import com.nihongo.app.ui.theme.Spacing

@Composable
fun GrammarDetailScreen(
    id: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GrammarViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    LaunchedEffect(id) { viewModel.loadDetail(id) }
    val state by viewModel.detailState.collectAsStateWithLifecycle()
    val item = (state.content as? UiState.Success)?.data

    Column(modifier = modifier.fillMaxSize()) {
        AppTopBar(
            title = "",
            onBack = onBack,
            actions = {
                if (item != null) {
                    BookmarkIconButton(
                        bookmarked = "grammar:${item.id}" in state.bookmarks,
                        onToggle = { viewModel.toggleBookmark(item.id) },
                    )
                }
            },
        )

        when (val content = state.content) {
            UiState.Loading -> LoadingState()
            UiState.Empty -> EmptyState(
                icon = Icons.Filled.SearchOff,
                title = stringResource(R.string.grammar_missing),
                message = "",
            )
            is UiState.Error -> ErrorState(onRetry = viewModel::retry)
            is UiState.Success -> GrammarDetailContent(content.data)
        }
    }
}

@Composable
private fun GrammarDetailContent(item: GrammarItem) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                text = item.pattern,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            item.reading?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            item.romaji?.let {
                Text(
                    text = "($it)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Text(
                text = stringResource(R.string.jlpt_badge, item.level.name),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(PillShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = Spacing.md, vertical = Spacing.xs),
            )
        }

        InfoCard(title = stringResource(R.string.kanji_meaning)) {
            Text(
                text = item.meaning,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (!item.meaningTranslated) {
                Text(
                    text = stringResource(R.string.meaning_source_en),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (item.formation.isNotBlank()) {
            InfoCard(title = stringResource(R.string.grammar_formation)) {
                Text(
                    text = item.formation,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        item.notes?.let { notes ->
            InfoCard(title = stringResource(R.string.grammar_notes)) {
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        if (item.examples.isNotEmpty()) {
            InfoCard(title = stringResource(R.string.grammar_examples)) {
                item.examples.forEachIndexed { index, example ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text(
                        text = example.japanese,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (example.english.isNotBlank()) {
                        Text(
                            text = example.english,
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.example_translation_en),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Column(modifier = Modifier.padding(bottom = Spacing.lg)) {}
    }
}

@Composable
private fun InfoCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.card),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            content()
        }
    }
}