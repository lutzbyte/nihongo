package com.nihongo.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihongo.app.R
import com.nihongo.app.ui.common.AppViewModelProvider
import com.nihongo.app.ui.components.AppTopBar
import com.nihongo.app.ui.components.DailyTargetCard
import com.nihongo.app.ui.components.LearningCard
import com.nihongo.app.ui.components.SampleDataNotice
import com.nihongo.app.ui.illustration.MenekiNeko
import com.nihongo.app.ui.theme.LocalNihongoExtraColors
import com.nihongo.app.ui.theme.Spacing

private data class HomeTile(
    val key: String,
    val titleRes: Int,
    val icon: ImageVector,
    val container: Color,
    val tint: Color,
)

@Composable
fun HomeScreen(
    onOpenVocabulary: () -> Unit,
    onOpenKanji: () -> Unit,
    onOpenFeature: (String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val extra = LocalNihongoExtraColors.current
    val colors = MaterialTheme.colorScheme

    val tiles = listOf(
        HomeTile("vocabulary", R.string.category_vocabulary, Icons.AutoMirrored.Filled.MenuBook, extra.tileBlue, colors.primary),
        HomeTile("kanji", R.string.category_kanji, Icons.Filled.Translate, extra.tileOrange, colors.tertiary),
        HomeTile("grammar", R.string.category_grammar, Icons.Filled.Spellcheck, extra.tileOrange, colors.tertiary),
        HomeTile("listening", R.string.category_listening, Icons.Filled.Headphones, extra.tilePurple, colors.primary),
        HomeTile("reading", R.string.category_reading, Icons.Filled.AutoStories, extra.tileGreen, extra.success),
        HomeTile("conversation", R.string.category_conversation, Icons.Filled.ChatBubble, extra.tilePurple, colors.primary),
    )

    Column(modifier = modifier.fillMaxSize()) {
        AppTopBar(
            title = stringResource(R.string.app_name),
            actions = {
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = stringResource(R.string.nav_settings),
                    )
                }
            },
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            if (state.showSampleNotice) SampleDataNotice()

            GreetingBanner()

            DailyTargetCard(todayMinutes = state.todayMinutes, goalMinutes = state.goalMinutes)

            Text(
                text = stringResource(R.string.home_start_learning),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.onBackground,
            )

            tiles.chunked(2).forEach { rowTiles ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    rowTiles.forEach { tile ->
                        LearningCard(
                            title = stringResource(tile.titleRes),
                            subtitle = stringResource(R.string.category_level_range),
                            icon = tile.icon,
                            containerColor = tile.container,
                            iconTint = tile.tint,
                            onClick = {
                                when (tile.key) {
                                    "vocabulary" -> onOpenVocabulary()
                                    "kanji" -> onOpenKanji()
                                    else -> onOpenFeature(tile.key)
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GreetingBanner(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 104.dp)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth(0.62f)
                .padding(Spacing.lg),
        ) {
            Text(
                text = stringResource(R.string.home_greeting_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = stringResource(R.string.home_greeting_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        MenekiNeko(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = Spacing.sm)
                .size(96.dp),
        )
    }
}