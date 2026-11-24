package com.nihongo.app.ui.practice

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.nihongo.app.domain.model.JlptLevel
import com.nihongo.app.domain.model.QuizQuestion
import com.nihongo.app.ui.common.AppViewModelProvider
import com.nihongo.app.ui.common.UiState
import com.nihongo.app.ui.components.AppTopBar
import com.nihongo.app.ui.components.EmptyState
import com.nihongo.app.ui.components.ErrorState
import com.nihongo.app.ui.components.LoadingState
import com.nihongo.app.ui.components.PrimaryButton
import com.nihongo.app.ui.components.ProgressBar
import com.nihongo.app.ui.components.SampleDataNotice
import com.nihongo.app.ui.theme.IconSize
import com.nihongo.app.ui.theme.NihongoTextStyles
import com.nihongo.app.ui.theme.Spacing
import java.util.Locale

@Composable
fun QuizScreen(
    level: JlptLevel,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuizViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    LaunchedEffect(level) { viewModel.start(level) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        when (state.status) {
            UiState.Loading -> LoadingState()
            UiState.Empty -> {
                AppTopBar(title = stringResource(R.string.nav_practice), onBack = onFinish)
                EmptyState(
                    icon = Icons.Filled.Inbox,
                    title = stringResource(R.string.empty_quiz_title),
                    message = stringResource(R.string.empty_quiz_message),
                )
            }
            is UiState.Error -> {
                AppTopBar(title = stringResource(R.string.nav_practice), onBack = onFinish)
                ErrorState(onRetry = viewModel::retry)
            }
            is UiState.Success -> {
                if (state.finished) {
                    QuizResult(state = state, onFinish = onFinish)
                } else {
                    QuizQuestionContent(
                        state = state,
                        onSelect = viewModel::select,
                        onNext = viewModel::next,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuizQuestionContent(
    state: QuizUiState,
    onSelect: (Int) -> Unit,
    onNext: () -> Unit,
) {
    val question = state.questions[state.index]
    val total = state.questions.size
    val isLast = state.index == state.questions.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressBar(
                fraction = (state.index + 1f) / total,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(Spacing.md))
            Icon(
                imageVector = Icons.Filled.Timer,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(IconSize.sm),
            )
            Spacer(Modifier.width(Spacing.xs))
            Text(
                text = formatTime(state.elapsedSeconds),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = stringResource(R.string.quiz_counter, state.index + 1, total),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.isSample) {
            Spacer(Modifier.height(Spacing.sm))
            SampleDataNotice()
        }

        Spacer(Modifier.height(Spacing.md))
        Text(
            text = stringResource(R.string.quiz_prompt),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.height(Spacing.lg))
        WordCard(question = question)

        Spacer(Modifier.height(Spacing.lg))
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            question.options.forEachIndexed { optionIndex, text ->
                QuizOption(
                    text = text,
                    selected = state.selected == optionIndex,
                    onClick = { onSelect(optionIndex) },
                )
            }
        }

        Spacer(Modifier.height(Spacing.md))
        PrimaryButton(
            text = stringResource(if (isLast) R.string.quiz_finish else R.string.quiz_next),
            onClick = onNext,
            enabled = state.selected != null,
        )
    }
}

@Composable
private fun WordCard(question: QuizQuestion) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(vertical = Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = question.word,
            style = NihongoTextStyles.kanjiLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            text = "(${question.reading})",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun QuizOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(if (selected) colors.primaryContainer else colors.surface)
            .border(
                border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) colors.primary else colors.outlineVariant),
                shape = MaterialTheme.shapes.medium,
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (selected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (selected) colors.primary else colors.outline,
        )
        Spacer(Modifier.width(Spacing.md))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = colors.onSurface,
        )
    }
}

@Composable
private fun QuizResult(
    state: QuizUiState,
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
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(IconSize.xl + 16.dp),
                )
            }
            Spacer(Modifier.height(Spacing.lg))
            Text(
                text = stringResource(R.string.quiz_result_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = stringResource(R.string.quiz_result_score, state.correctCount, state.questions.size),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.quiz_result_time, formatTime(state.elapsedSeconds)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        PrimaryButton(text = stringResource(R.string.quiz_finish), onClick = onFinish)
    }
}

private fun formatTime(totalSeconds: Int): String =
    String.format(Locale.ROOT, "%02d:%02d", totalSeconds / 60, totalSeconds % 60)