package com.nihongo.app.ui.kanji

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihongo.app.R
import com.nihongo.app.domain.model.JlptLevel
import com.nihongo.app.domain.model.KanjiItem
import com.nihongo.app.ui.common.AppViewModelProvider
import com.nihongo.app.ui.common.UiState
import com.nihongo.app.ui.components.AppTopBar
import com.nihongo.app.ui.components.EmptyState
import com.nihongo.app.ui.components.ErrorState
import com.nihongo.app.ui.components.LoadingState
import com.nihongo.app.ui.components.PrimaryButton
import com.nihongo.app.ui.theme.Elevation
import com.nihongo.app.ui.theme.NihongoTextStyles
import com.nihongo.app.ui.theme.PillShape
import com.nihongo.app.ui.theme.Spacing

@Composable
fun KanjiDetailScreen(
    id: String,
    onBack: () -> Unit,
    onContinuePractice: (JlptLevel) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: KanjiViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    LaunchedEffect(id) { viewModel.loadDetail(id) }
    val state by viewModel.detailState.collectAsStateWithLifecycle()
    val kanji = (state.content as? UiState.Success)?.data
    val bookmarked = kanji != null && "kanji:${kanji.id}" in state.bookmarks

    Column(modifier = modifier.fillMaxSize()) {
        AppTopBar(
            title = "",
            onBack = onBack,
            actions = {
                if (kanji != null) {
                    IconButton(onClick = { viewModel.toggleBookmark(kanji.id) }) {
                        Icon(
                            imageVector = if (bookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = stringResource(
                                if (bookmarked) R.string.bookmark_remove else R.string.bookmark_add,
                            ),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            },
        )

        when (val content = state.content) {
            UiState.Loading -> LoadingState()
            UiState.Empty -> EmptyState(
                icon = Icons.Filled.SearchOff,
                title = stringResource(R.string.kanji_detail_missing),
                message = "",
            )
            is UiState.Error -> ErrorState(onRetry = viewModel::retry)
            is UiState.Success -> KanjiDetailContent(
                kanji = content.data,
                onContinuePractice = { onContinuePractice(content.data.level) },
            )
        }
    }
}

@Composable
private fun KanjiDetailContent(
    kanji: KanjiItem,
    onContinuePractice: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = kanji.character,
                    style = NihongoTextStyles.kanjiHero,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }

            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = kanji.readingKana,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (kanji.readingRomaji.isNotBlank()) {
                Text(
                    text = "(${kanji.readingRomaji})",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = stringResource(R.string.jlpt_badge, kanji.level.name),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(PillShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = Spacing.md, vertical = Spacing.xs),
            )

            Text(
                text = stringResource(R.string.kanji_meaning),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = kanji.meaning,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                InfoBlock(
                    label = stringResource(R.string.kanji_onyomi),
                    value = kanji.onyomi,
                    modifier = Modifier.weight(1f),
                )
                InfoBlock(
                    label = stringResource(R.string.kanji_kunyomi),
                    value = kanji.kunyomi,
                    modifier = Modifier.weight(1f),
                )
            }
            InfoBlock(
                label = stringResource(R.string.kanji_strokes),
                value = kanji.strokes.toString(),
                modifier = Modifier.fillMaxWidth(),
            )

            kanji.example?.let { example ->
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
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        Text(
                            text = stringResource(R.string.kanji_example),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = example.japanese,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (example.romaji.isNotBlank()) {
                            Text(
                                text = "(${example.romaji})",
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (example.meaning.isNotBlank()) {
                            Text(
                                text = example.meaning,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(Spacing.sm))
        }

        PrimaryButton(
            text = stringResource(R.string.kanji_continue_practice),
            onClick = onContinuePractice,
            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
        )
    }
}

@Composable
private fun InfoBlock(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.card),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}