package com.nihongo.app.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihongo.app.R
import com.nihongo.app.domain.model.DayMinutes
import com.nihongo.app.domain.model.ProgressSummary
import com.nihongo.app.ui.common.AppViewModelProvider
import com.nihongo.app.ui.common.UiState
import com.nihongo.app.ui.components.AppTopBar
import com.nihongo.app.ui.components.EmptyState
import com.nihongo.app.ui.components.ErrorState
import com.nihongo.app.ui.components.InfoStatCard
import com.nihongo.app.ui.components.LoadingState
import com.nihongo.app.ui.components.SampleDataNotice
import com.nihongo.app.ui.components.StatCard
import com.nihongo.app.ui.theme.Elevation
import com.nihongo.app.ui.theme.LocalNihongoExtraColors
import com.nihongo.app.ui.theme.PillShape
import com.nihongo.app.ui.theme.Spacing

@Composable
fun ProgressScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProgressViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        AppTopBar(title = stringResource(R.string.progress_title), onBack = onBack)

        RangeTabs(
            selectedDays = state.days,
            onSelect = viewModel::selectRange,
            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (val content = state.content) {
                UiState.Loading -> LoadingState()
                UiState.Empty -> EmptyState(
                    icon = Icons.Filled.Inbox,
                    title = stringResource(R.string.progress_no_activity),
                    message = "",
                )
                is UiState.Error -> ErrorState(onRetry = viewModel::retry)
                is UiState.Success -> ProgressContent(
                    summary = content.data,
                    goalMinutes = state.goalMinutes,
                    showSampleNotice = state.isSample,
                )
            }
        }
    }
}

@Composable
private fun RangeTabs(
    selectedDays: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        listOf(7 to R.string.progress_range_7, 30 to R.string.progress_range_30).forEach { (days, labelRes) ->
            val selected = days == selectedDays
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(PillShape)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface,
                    )
                    .clickable(role = Role.Tab) { onSelect(days) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(labelRes),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ProgressContent(
    summary: ProgressSummary,
    goalMinutes: Int,
    showSampleNotice: Boolean,
) {
    val extra = LocalNihongoExtraColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        if (showSampleNotice) SampleDataNotice()

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
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
                    text = stringResource(R.string.progress_activity),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = summary.totalMinutes.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    Text(
                        text = stringResource(R.string.progress_minutes_unit),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = Spacing.xs),
                    )
                    Spacer(Modifier.weight(1f))
                    summary.changePercent?.let { change ->
                        val sign = if (change >= 0) "↑ +" else "↓ "
                        Text(
                            text = "$sign$change%",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = if (change >= 0) extra.success else MaterialTheme.colorScheme.error,
                        )
                    }
                }
                if (summary.totalMinutes == 0) {
                    Text(
                        text = stringResource(R.string.progress_no_activity),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ActivityBarChart(days = summary.dailyMinutes)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            StatCard(
                label = stringResource(R.string.stat_vocabulary),
                value = summary.vocabularyLearned.toString(),
                icon = Icons.AutoMirrored.Filled.MenuBook,
                tint = extra.success,
                containerColor = extra.tileGreen,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = stringResource(R.string.stat_kanji),
                value = summary.kanjiMastered.toString(),
                icon = Icons.Filled.Translate,
                tint = MaterialTheme.colorScheme.tertiary,
                containerColor = extra.tileOrange,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = stringResource(R.string.stat_grammar),
                value = summary.grammarProgress.toString(),
                icon = Icons.Filled.Spellcheck,
                tint = MaterialTheme.colorScheme.primary,
                containerColor = extra.tilePurple,
                modifier = Modifier.weight(1f),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            InfoStatCard(
                label = stringResource(R.string.stat_streak),
                value = stringResource(R.string.stat_streak_value, summary.streakDays),
                icon = Icons.Filled.LocalFireDepartment,
                tint = extra.streak,
                modifier = Modifier.weight(1f),
            )
            InfoStatCard(
                label = stringResource(R.string.stat_target),
                value = stringResource(R.string.stat_target_value, goalMinutes),
                icon = Icons.Filled.TrackChanges,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(Spacing.sm))
    }
}

@Composable
private fun ActivityBarChart(days: List<DayMinutes>) {
    if (days.isEmpty()) return
    val maxMinutes = (days.maxOf { it.minutes }).coerceAtLeast(1)
    val barColor = LocalNihongoExtraColors.current.chartBar
    val emptyColor = MaterialTheme.colorScheme.outlineVariant
    val manyDays = days.size > 10

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            days.forEach { day ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    val fraction = day.minutes.toFloat() / maxMinutes
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(if (manyDays) 0.6f else 0.45f)
                            .then(
                                if (day.minutes > 0) Modifier.fillMaxHeight(fraction.coerceAtLeast(0.04f))
                                else Modifier.height(3.dp),
                            )
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(if (day.minutes > 0) barColor else emptyColor),
                    )
                }
            }
        }
        Spacer(Modifier.height(Spacing.xs))
        Row(modifier = Modifier.fillMaxWidth()) {
            days.forEach { day ->
                Text(
                    text = day.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}