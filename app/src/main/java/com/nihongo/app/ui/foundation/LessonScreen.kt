package com.nihongo.app.ui.foundation

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
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihongo.app.R
import com.nihongo.app.domain.model.LessonRow
import com.nihongo.app.domain.model.LessonSection
import com.nihongo.app.ui.common.AppViewModelProvider
import com.nihongo.app.ui.common.UiState
import com.nihongo.app.ui.components.AppTopBar
import com.nihongo.app.ui.components.EmptyState
import com.nihongo.app.ui.components.ErrorState
import com.nihongo.app.ui.components.LoadingState
import com.nihongo.app.ui.theme.Elevation
import com.nihongo.app.ui.theme.Spacing

@Composable
fun LessonScreen(
    id: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FoundationViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    LaunchedEffect(id) { viewModel.loadLesson(id) }
    val state by viewModel.lesson.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        val title = (state as? UiState.Success)?.data?.title.orEmpty()
        AppTopBar(title = title, onBack = onBack)

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (val content = state) {
                UiState.Loading -> LoadingState()
                UiState.Empty -> EmptyState(
                    icon = Icons.Filled.SearchOff,
                    title = stringResource(R.string.lesson_missing),
                    message = "",
                )
                is UiState.Error -> ErrorState(onRetry = viewModel::retry)
                is UiState.Success -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    items(content.data.sections) { section -> SectionCard(section) }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(section: LessonSection) {
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
                text = section.heading,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (section.body.isNotBlank()) {
                Text(
                    text = section.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            section.rows.forEachIndexed { index, row ->
                if (index > 0 || section.body.isNotBlank()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                RowItem(row)
            }
        }
    }
}

@Composable
private fun RowItem(row: LessonRow) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = row.ja,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        val sub = listOfNotNull(row.reading, row.romaji).joinToString(" · ")
        if (sub.isNotBlank()) {
            Text(
                text = sub,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = row.meaning,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}