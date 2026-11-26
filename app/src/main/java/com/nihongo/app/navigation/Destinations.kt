package com.nihongo.app.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.nihongo.app.R
import kotlinx.serialization.Serializable

@Serializable
data object SplashRoute

@Serializable
data object HomeRoute

@Serializable
data object PracticeRoute

@Serializable
data object ProgressRoute

@Serializable
data object SettingsRoute

@Serializable
data object VocabularyRoute

@Serializable
data class VocabularyDetailRoute(val id: String)

@Serializable
data object KanjiListRoute

@Serializable
data class KanjiDetailRoute(val id: String)

@Serializable
data object GrammarListRoute

@Serializable
data class GrammarDetailRoute(val id: String)

@Serializable
data object FoundationRoute

@Serializable
data class KanaRoute(val script: String)

@Serializable
data class LessonRoute(val id: String)

@Serializable
data object BookmarkRoute

@Serializable
data class QuizRoute(val level: String)

@Serializable
data class ComingSoonRoute(val feature: String, val phase: Int)

data class TopLevelDestination(
    val route: Any,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
)

val TopLevelDestinations: List<TopLevelDestination> = listOf(
    TopLevelDestination(HomeRoute, R.string.nav_home, Icons.Filled.Home),
    TopLevelDestination(PracticeRoute, R.string.nav_practice, Icons.Filled.Psychology),
    TopLevelDestination(ProgressRoute, R.string.nav_progress, Icons.Filled.BarChart),
    TopLevelDestination(SettingsRoute, R.string.nav_settings, Icons.Filled.Settings),
)

fun NavDestination?.isSelected(destination: TopLevelDestination): Boolean =
    this?.hierarchy?.any { it.hasRoute(destination.route::class) } == true

fun NavDestination?.isTopLevel(): Boolean =
    TopLevelDestinations.any { this.isSelected(it) }

fun NavController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

fun NavController.navigateToHomeTab() = navigateToTopLevel(TopLevelDestinations.first())

/** Phase tempat fitur dikerjakan, untuk halaman "Segera hadir" (Listening, Reading, Conversation). */
@Suppress("UNUSED_PARAMETER")
fun featurePhase(feature: String): Int = 7