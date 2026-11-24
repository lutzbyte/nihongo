package com.nihongo.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.nihongo.app.R
import com.nihongo.app.ui.components.PlaceholderScreen

@Composable
fun NihongoNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier,
    ) {
        composable<HomeRoute> { PlaceholderScreen(titleRes = R.string.nav_home) }
        composable<PracticeRoute> { PlaceholderScreen(titleRes = R.string.nav_practice) }
        composable<ProgressRoute> { PlaceholderScreen(titleRes = R.string.nav_progress) }
        composable<SettingsRoute> { PlaceholderScreen(titleRes = R.string.nav_settings) }
    }
}