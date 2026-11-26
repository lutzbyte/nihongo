package com.nihongo.app.ui.foundation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihongo.app.R
import com.nihongo.app.ui.common.AppViewModelProvider
import com.nihongo.app.ui.common.UiState
import com.nihongo.app.ui.components.AppTopBar
import com.nihongo.app.ui.components.EmptyState
import com.nihongo.app.ui.components.ErrorState
import com.nihongo.app.ui.components.LoadingState
import com.nihongo.app.ui.theme.Elevation
import com.nihongo.app.ui.theme.LocalNihongoExtraColors
import com.nihongo.app.ui.theme.NihongoTextStyles
import com.nihongo.app.ui.theme.Spacing

@Composable
fun FoundationScreen(
    onBack: () -> Unit,
    onOpenKana: (String) -> Unit,
    onOpenLesson: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FoundationViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val lessons by viewModel.lessons.collectAsStateWithLifecycle()
    val extra = LocalNihongoExtraColors.current

    Column(modifier = modifier.fillMaxSize()) {
        AppTopBar(title = stringResource(R.string.foundation_title), onBack = onBack)

        LazyColumn(
            contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    KanaEntryCard(
                        glyph = "あ",
                        title = stringResource(R.string.foundation_hiragana),
                        containerColor = extra.tileBlue,
                        onClick = { onOpenKana("HIRAGANA") },
                        modifier = Modifier.weight(1f),
                    )
                    KanaEntryCard(
                        glyph = "ア",
                        title = stringResource(R.string.foundation_katakana),
                        containerColor = extra.tileOrange,
                        onClick = { onOpenKana("KATAKANA") },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                Text(
                    text = stringResource(R.string.foundation_lessons),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = Spacing.md),
                )
            }
            when (val state = lessons) {
                UiState.Loading -> item { Box(Modifier.height(200.dp)) { LoadingState() } }
                UiState.Empty -> item {
                    Box(Modifier.height(260.dp)) {
                        EmptyState(
                            icon = Icons.Filled.Inbox,
                            title = stringResource(R.string.foundation_empty_title),
                            message = stringResource(R.string.foundation_empty_message),
                        )
                    }
                }
                is UiState.Error -> item {
                    Box(Modifier.height(260.dp)) { ErrorState(onRetry = viewModel::retry) }
                }
                is UiState.Success -> items(state.data, key = { it.id }) { lesson ->
                    Card(
                        onClick = { onOpenLesson(lesson.id) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.card),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.lg),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = lesson.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                if (lesson.summary.isNotBlank()) {
                                    Text(
                                        text = lesson.summary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KanaEntryCard(
    glyph: String,
    title: String,
    containerColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.none),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = glyph,
                style = NihongoTextStyles.kanjiLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}