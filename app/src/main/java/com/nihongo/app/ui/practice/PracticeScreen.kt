package com.nihongo.app.ui.practice

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihongo.app.R
import com.nihongo.app.ui.common.AppViewModelProvider
import com.nihongo.app.ui.components.AppTopBar
import com.nihongo.app.ui.components.PrimaryButton
import com.nihongo.app.ui.theme.Elevation
import com.nihongo.app.ui.theme.Spacing

@Composable
fun PracticeScreen(
    onBack: () -> Unit,
    onOpenSetup: (String) -> Unit,
    onStartReview: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PracticeViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val dueCount by viewModel.dueCount.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        AppTopBar(title = stringResource(R.string.nav_practice), onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            ReviewCard(dueCount = dueCount, onStart = onStartReview)

            ModeCard(
                icon = Icons.Filled.Style,
                titleRes = R.string.practice_mode_flashcard,
                descRes = R.string.practice_mode_flashcard_desc,
                onClick = { onOpenSetup("FLASHCARD") },
            )
            ModeCard(
                icon = Icons.Filled.Psychology,
                titleRes = R.string.practice_quiz_title,
                descRes = R.string.practice_quiz_desc,
                onClick = { onOpenSetup("QUIZ") },
            )
            ModeCard(
                icon = Icons.Filled.Timer,
                titleRes = R.string.practice_mode_exam,
                descRes = R.string.practice_mode_exam_desc,
                onClick = { onOpenSetup("EXAM") },
            )
            Spacer(Modifier.height(Spacing.sm))
        }
    }
}

@Composable
private fun ReviewCard(dueCount: Int, onStart: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.none),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                text = stringResource(R.string.practice_review_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = if (dueCount > 0) {
                    stringResource(R.string.practice_review_due, dueCount)
                } else {
                    stringResource(R.string.practice_review_none)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            PrimaryButton(
                text = stringResource(R.string.practice_review_start),
                onClick = onStart,
                enabled = dueCount > 0,
            )
        }
    }
}

@Composable
private fun ModeCard(
    icon: ImageVector,
    @StringRes titleRes: Int,
    @StringRes descRes: Int,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.card),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = Spacing.md),
            ) {
                Text(
                    text = stringResource(titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(descRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}