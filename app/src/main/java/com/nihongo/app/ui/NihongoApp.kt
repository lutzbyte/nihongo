package com.nihongo.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nihongo.app.navigation.NihongoNavHost
import com.nihongo.app.navigation.SplashRoute
import com.nihongo.app.navigation.isTopLevel
import com.nihongo.app.navigation.navigateToTopLevel
import com.nihongo.app.ui.components.NihongoBottomBar

@Composable
fun NihongoApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val isSplash = currentDestination == null || currentDestination.hasRoute(SplashRoute::class)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (currentDestination.isTopLevel()) {
                NihongoBottomBar(
                    currentDestination = currentDestination,
                    onNavigate = { navController.navigateToTopLevel(it) },
                )
            }
        },
    ) { innerPadding ->
        NihongoNavHost(
            navController = navController,
            modifier = if (isSplash) Modifier else Modifier.padding(innerPadding),
        )
    }
}