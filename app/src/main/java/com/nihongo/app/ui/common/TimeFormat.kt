package com.nihongo.app.ui.common

import java.util.Locale

fun formatDuration(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    return String.format(Locale.ROOT, "%02d:%02d", safe / 60, safe % 60)
}