package com.nihongo.app.ui.practice

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihongo.app.R
import com.nihongo.app.domain.model.FlashCard
import com.nihongo.app.domain.model.ItemType
import com.nihongo.app.domain.model.JlptLevel
import com.nihongo.app.domain.model.SessionKind
import com.nihongo.app.domain.model.StudyScope
import com.nihongo.app.domain.srs.SrsGrade
import com.nihongo.app.ui.common.AppViewModelProvider
import com.nihongo.app.ui.common.UiState
import com.nihongo.app.ui.common.formatDuration
import com.nihongo.app.ui.components.AppTopBar
import com.nihongo.app.ui.components.EmptyState
import com.nihongo.app.ui.components.ErrorState
import com.nihongo.app.ui.components.LoadingState
import com.nihongo.app.ui.components.PrimaryButton
import com.nihongo.app.ui.components.ProgressBar
import com.nihongo.app.ui.theme.Elevation
import com.nihongo.app.ui.theme.IconSize
import com.nihongo.app.ui.theme.LocalNihongoExtraColors
import com.nihongo.app.ui.theme.NihongoTextStyles
import com.nihongo.app.ui.theme.PillShape
import com.nihongo.app.ui.theme.Spacing

@Composable
fun FlashcardScreen(
    kind: String,
    scope: String,
    level: String,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FlashcardViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val sessionKind = if (kind == "REVIEW") SessionKind.REVIEW else SessionKind.FLASHCARD
    val studyScope = StudyScope.entries.firstOrNull { it.name == scope } ?: StudyScope.ALL
    val jlptLevel = JlptLevel.entries.firstOrNull { it.name == level }

    LaunchedEffect(kind, scope, level) { viewModel.start(sessionKind, studyScope, jlptLevel) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    val titleRes = if (sessionKind == SessionKind.REVIEW) R.string.review_title else R.string.flash_title

    Column(modifier = modifier.fillMaxSize()) {
        AppTopBar(title = stringResource(titleRes), onBack = onFinish)

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (state.status) {
                UiState.Loading -> LoadingState()
                UiState.Empty -> EmptyState(
                    icon = Icons.Filled.Inbox,
                    title = stringResource(R.string.flash_empty_title),
                    message = stringResource(
                        if (sessionKind == SessionKind.REVIEW) R.string.flash_empty_review
                        else R.string.flash_empty_deck,
                    ),
                )
                is UiState.Error -> ErrorState(onRetry = viewModel::retry)
                is UiState.Success -> {
                    val card = state.current
                    if (state.finished) {
                        FlashSummary(state = state, onFinish = onFinish)
                    } else if (card != null) {
                        CardContent(
                            card = card,
                            state = state,
                            onFlip = viewModel::flip,
                            onGrade = viewModel::grade,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CardContent(
    card: FlashCard,
    state: FlashcardUiState,
    onFlip: () -> Unit,
    onGrade: (SrsGrade) -> Unit,
) {
    val total = state.reviewed + state.remaining
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressBar(
                fraction = if (total > 0) state.reviewed.toFloat() / total else 0f,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(Spacing.md))
            Text(
                text = stringResource(R.string.quiz_counter, state.reviewed, total),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(Spacing.md))

        Card(
            onClick = onFlip,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = Elevation.raised),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Spacing.lg),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    if (card.isNew) {
                        Text(
                            text = stringResource(R.string.flash_new),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(PillShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = Spacing.md, vertical = Spacing.xs),
                        )
                    }
                    Text(
                        text = card.front,
                        style = when (card.type) {
                            ItemType.VOCABULARY -> NihongoTextStyles.kanjiLarge
                            ItemType.KANJI -> NihongoTextStyles.kanjiHero
                            ItemType.GRAMMAR -> MaterialTheme.typography.headlineMedium
                        },
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    if (state.flipped) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = Spacing.sm),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                        card.reading?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                            )
                        }
                        Text(
                            text = card.meaning,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                        card.extra?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.flash_tap_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(Spacing.md))

        if (state.flipped) {
            GradeRow(previews = state.previews, onGrade = onGrade)
        } else {
            PrimaryButton(text = stringResource(R.string.flash_show_answer), onClick = onFlip)
        }
    }
}

@Composable
private fun GradeRow(
    previews: Map<SrsGrade, Int>,
    onGrade: (SrsGrade) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val extra = LocalNihongoExtraColors.current
    val specs = listOf(
        Triple(SrsGrade.AGAIN, R.string.grade_again, colors.errorContainer to colors.onErrorContainer),
        Triple(SrsGrade.HARD, R.string.grade_hard, colors.tertiaryContainer to colors.onTertiaryContainer),
        Triple(SrsGrade.GOOD, R.string.grade_good, colors.primaryContainer to colors.onPrimaryContainer),
        Triple(SrsGrade.EASY, R.string.grade_easy, extra.tileGreen to colors.onSurface),
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        specs.forEach { (grade, labelRes, palette) ->
            GradeButton(
                label = stringResource(labelRes),
                interval = intervalLabel(previews[grade] ?: 0),
                container = palette.first,
                content = palette.second,
                onClick = { onGrade(grade) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun GradeButton(
    label: String,
    interval: String,
    container: Color,
    content: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.none),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = content,
            )
            Text(
                text = interval,
                style = MaterialTheme.typography.labelSmall,
                color = content,
            )
        }
    }
}

@Composable
private fun intervalLabel(days: Int): String =
    if (days <= 0) stringResource(R.string.interval_today) else stringResource(R.string.interval_days, days)

@Composable
private fun FlashSummary(
    state: FlashcardUiState,
    onFinish: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.xxl),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(IconSize.xl + 16.dp),
            )
            Spacer(Modifier.height(Spacing.lg))
            Text(
                text = stringResource(R.string.flash_done_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = stringResource(R.string.flash_done_reviewed, state.reviewed),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.flash_done_again, state.againCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.quiz_result_time, formatDuration(state.elapsedSeconds)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        PrimaryButton(text = stringResource(R.string.quiz_finish), onClick = onFinish)
    }
}