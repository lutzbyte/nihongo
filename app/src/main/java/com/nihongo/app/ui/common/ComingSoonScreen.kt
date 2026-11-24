package com.nihongo.app.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.nihongo.app.R
import com.nihongo.app.ui.components.AppTopBar
import com.nihongo.app.ui.components.EmptyState

@StringRes
private fun featureTitleRes(feature: String): Int = when (feature) {
    "grammar" -> R.string.category_grammar
    "listening" -> R.string.category_listening
    "reading" -> R.string.category_reading
    else -> R.string.category_conversation
}

@Composable
fun ComingSoonScreen(
    feature: String,
    phase: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        AppTopBar(title = stringResource(featureTitleRes(feature)), onBack = onBack)
        EmptyState(
            icon = Icons.Filled.Schedule,
            title = stringResource(R.string.coming_soon_title),
            message = stringResource(R.string.coming_soon_phase, phase),
        )
    }
}