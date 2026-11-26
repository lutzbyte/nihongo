package com.nihongo.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.nihongo.app.R
import com.nihongo.app.ui.theme.Elevation
import com.nihongo.app.ui.theme.NihongoTextStyles
import com.nihongo.app.ui.theme.Spacing

@Composable
fun VocabularyCard(
    word: String,
    reading: String?,
    meaning: String,
    bookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val content: @Composable ColumnScope.() -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Spacing.lg, top = Spacing.md, bottom = Spacing.md, end = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = word,
                    style = NihongoTextStyles.japaneseItem,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (reading != null) {
                    Text(
                        text = reading,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = "($meaning)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onToggleBookmark) {
                Icon(
                    imageVector = if (bookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                    contentDescription = stringResource(
                        if (bookmarked) R.string.bookmark_remove else R.string.bookmark_add,
                    ),
                    tint = if (bookmarked) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = Elevation.card),
            content = content,
        )
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = Elevation.card),
            content = content,
        )
    }
}