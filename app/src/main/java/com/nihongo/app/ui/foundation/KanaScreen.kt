package com.nihongo.app.ui.foundation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihongo.app.R
import com.nihongo.app.domain.model.KanaGroup
import com.nihongo.app.domain.model.KanaItem
import com.nihongo.app.domain.model.KanaScript
import com.nihongo.app.ui.common.AppViewModelProvider
import com.nihongo.app.ui.common.UiState
import com.nihongo.app.ui.components.AppTopBar
import com.nihongo.app.ui.components.ChoiceChips
import com.nihongo.app.ui.components.EmptyState
import com.nihongo.app.ui.components.ErrorState
import com.nihongo.app.ui.components.LoadingState
import com.nihongo.app.ui.theme.Elevation
import com.nihongo.app.ui.theme.Spacing

@Composable
fun KanaScreen(
    script: KanaScript,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FoundationViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    LaunchedEffect(script) { viewModel.loadKana(script) }
    val state by viewModel.kana.collectAsStateWithLifecycle()
    var group by rememberSaveable { mutableStateOf(KanaGroup.BASIC) }

    val title = stringResource(
        if (script == KanaScript.HIRAGANA) R.string.foundation_hiragana else R.string.foundation_katakana,
    )
    val groupOptions = listOf(
        KanaGroup.BASIC to stringResource(R.string.kana_group_basic),
        KanaGroup.DAKUTEN to stringResource(R.string.kana_group_dakuten),
        KanaGroup.HANDAKUTEN to stringResource(R.string.kana_group_handakuten),
        KanaGroup.YOON to stringResource(R.string.kana_group_yoon),
    )

    Column(modifier = modifier.fillMaxSize()) {
        AppTopBar(title = title, onBack = onBack)

        ChoiceChips(
            options = groupOptions,
            selected = group,
            onSelect = { group = it },
            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (val content = state) {
                UiState.Loading -> LoadingState()
                UiState.Empty -> EmptyState(
                    icon = Icons.Filled.Inbox,
                    title = stringResource(R.string.foundation_empty_title),
                    message = stringResource(R.string.foundation_empty_message),
                )
                is UiState.Error -> ErrorState(onRetry = viewModel::retry)
                is UiState.Success -> {
                    val rows = content.data
                        .filter { it.group == group }
                        .groupBy { it.row }
                        .values
                        .toList()
                    val slots = if (group == KanaGroup.YOON) 3 else 5
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        items(rows, key = { it.first().id }) { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                            ) {
                                for (index in 0 until slots) {
                                    if (index < row.size) {
                                        KanaCell(item = row[index], modifier = Modifier.weight(1f))
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KanaCell(item: KanaItem, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.card),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = item.kana,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = item.romaji,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}