package com.nihongo.app.ui.practice

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihongo.app.R
import com.nihongo.app.domain.model.JlptLevel
import com.nihongo.app.domain.model.PracticeQuestion
import com.nihongo.app.domain.model.PracticeSection
import com.nihongo.app.domain.model.QuestionKind
import com.nihongo.app.domain.model.SessionKind
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
import com.nihongo.app.ui.theme.Spacing

@StringRes
private fun promptRes(kind: QuestionKind): Int = when (kind) {
    QuestionKind.VOCAB_MEANING -> R.string.quiz_prompt_vocab_meaning
    QuestionKind.VOCAB_FROM_MEANING -> R.string.quiz_prompt_vocab_from_meaning
    QuestionKind.VOCAB_READING -> R.string.quiz_prompt_vocab_reading
    QuestionKind.KANJI_MEANING -> R.string.quiz_prompt_kanji_meaning
    QuestionKind.KANJI_READING -> R.string.quiz_prompt_kanji_reading
    QuestionKind.GRAMMAR_MEANING -> R.string.quiz_prompt_grammar_meaning
    QuestionKind.GRAMMAR_PATTERN -> R.string.quiz_prompt_grammar_pattern
}

@StringRes
private fun sectionRes(section: PracticeSection): Int = when (section) {
    PracticeSection.MOJI_GOI -> R.string.section_moji_goi
    PracticeSection.BUNPO -> R.string.section_bunpo
    PracticeSection.MIXED -> R.string.section_mixed
}

@Composable
fun QuizScreen(
    level: JlptLevel,
    section: PracticeSection,
    mode: SessionKind,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuizViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    LaunchedEffect(level, section, mode) { viewModel.start(level, section, mode) }
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
                    QuestionContent(
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
private fun QuestionContent(
    state: QuizUiState,
    onSelect: (Int) -> Unit,
    onNext: () -> Unit,
) {
    val question = state.questions[state.index]
    val total = state.questions.size
    val isLast = state.index == state.questions.lastIndex
    val clock = state.remainingSeconds ?: state.elapsedSeconds
    val urgent = state.remainingSeconds != null && state.remainingSeconds < 60

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
                tint = if (urgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(IconSize.sm),
            )
            Spacer(Modifier.width(Spacing.xs))
            Text(
                text = formatDuration(clock),
                style = MaterialTheme.typography.labelLarge,
                color = if (urgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = stringResource(R.string.quiz_counter, state.index + 1, total),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(Spacing.md))
        Text(
            text = stringResource(promptRes(question.kind)),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.height(Spacing.lg))
        PromptCard(question = question)

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
                    japanese = question.optionsAreJapanese,
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
private fun PromptCard(question: PracticeQuestion) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(vertical = Spacing.xl, horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = question.display,
            style = if (question.displayIsJapanese) NihongoTextStyles.kanjiLarge
            else MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            textAlign = TextAlign.Center,
        )
        question.displaySub?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun QuizOption(
    text: String,
    japanese: Boolean,
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
                border = BorderStroke(
                    width = if (selected) 1.5.dp else 1.dp,
                    color = if (selected) colors.primary else colors.outlineVariant,
                ),
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
            style = if (japanese) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
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
    val sectionScores = state.questions.indices
        .groupBy { state.questions[it].section }
        .map { (section, indices) ->
            Triple(section, indices.count { state.answers[it] == state.questions[it].answerIndex }, indices.size)
        }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(IconSize.xl + 8.dp),
                    )
                    Text(
                        text = stringResource(
                            if (state.isExam) R.string.exam_result_title else R.string.quiz_result_title,
                        ),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    if (state.timedOut) {
                        Text(
                            text = stringResource(R.string.quiz_time_up),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Text(
                        text = stringResource(R.string.quiz_result_score, state.correctCount, state.questions.size),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    if (state.isExam) {
                        sectionScores.forEach { (section, correct, total) ->
                            Text(
                                text = stringResource(
                                    R.string.quiz_section_score,
                                    stringResource(sectionRes(section)),
                                    correct,
                                    total,
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.quiz_result_time, formatDuration(state.elapsedSeconds)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (state.isExam) {
                        Text(
                            text = stringResource(R.string.quiz_exam_disclaimer),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item {
                Text(
                    text = stringResource(R.string.quiz_review_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            items(state.questions.size) { index ->
                ReviewItem(
                    number = index + 1,
                    question = state.questions[index],
                    answer = state.answers[index],
                )
            }
        }
        PrimaryButton(
            text = stringResource(R.string.quiz_finish),
            onClick = onFinish,
            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
        )
    }
}

@Composable
private fun ReviewItem(
    number: Int,
    question: PracticeQuestion,
    answer: Int?,
) {
    val isCorrect = answer == question.answerIndex
    val success = LocalNihongoExtraColors.current.success
    val explanation = question.explanation

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
                text = "$number. ${stringResource(promptRes(question.kind))}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = listOfNotNull(question.display, question.displaySub).joinToString(" "),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (answer == null) {
                    stringResource(R.string.quiz_your_answer, stringResource(R.string.quiz_not_answered))
                } else {
                    stringResource(R.string.quiz_your_answer, question.options[answer])
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (isCorrect) success else MaterialTheme.colorScheme.error,
            )
            if (!isCorrect) {
                Text(
                    text = stringResource(R.string.quiz_correct_answer, question.options[question.answerIndex]),
                    style = MaterialTheme.typography.bodyMedium,
                    color = success,
                )
            }
            Text(
                text = explanation.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            explanation.detail?.let { detail ->
                Text(
                    text = if (question.section == PracticeSection.BUNPO) {
                        stringResource(R.string.quiz_formation, detail)
                    } else {
                        detail
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            explanation.exampleJa?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            explanation.exampleEn?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}