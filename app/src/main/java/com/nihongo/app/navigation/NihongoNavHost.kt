package com.nihongo.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.nihongo.app.domain.model.JlptLevel
import com.nihongo.app.ui.common.ComingSoonScreen
import com.nihongo.app.ui.home.HomeScreen
import com.nihongo.app.ui.kanji.KanjiDetailScreen
import com.nihongo.app.ui.kanji.KanjiListScreen
import com.nihongo.app.ui.practice.PracticeScreen
import com.nihongo.app.ui.practice.QuizScreen
import com.nihongo.app.ui.progress.ProgressScreen
import com.nihongo.app.ui.settings.SettingsScreen
import com.nihongo.app.ui.splash.SplashScreen
import com.nihongo.app.ui.vocabulary.VocabularyScreen

@Composable
fun NihongoNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = SplashRoute,
        modifier = modifier,
    ) {
        composable<SplashRoute> {
            SplashScreen(
                onFinished = {
                    navController.navigate(HomeRoute) {
                        popUpTo<SplashRoute> { inclusive = true }
                    }
                },
            )
        }

        composable<HomeRoute> {
            HomeScreen(
                onOpenVocabulary = { navController.navigate(VocabularyRoute) },
                onOpenKanji = { navController.navigate(KanjiListRoute) },
                onOpenFeature = { feature ->
                    navController.navigate(ComingSoonRoute(feature, featurePhase(feature)))
                },
                onOpenSettings = {
                    navController.navigateToTopLevel(
                        TopLevelDestinations.first { it.route is SettingsRoute },
                    )
                },
            )
        }

        composable<PracticeRoute> {
            PracticeScreen(
                onBack = { navController.navigateToHomeTab() },
                onStartQuiz = { level -> navController.navigate(QuizRoute(level.name)) },
            )
        }

        composable<ProgressRoute> {
            ProgressScreen(onBack = { navController.navigateToHomeTab() })
        }

        composable<SettingsRoute> {
            SettingsScreen(onBack = { navController.navigateToHomeTab() })
        }

        composable<VocabularyRoute> {
            VocabularyScreen(onBack = { navController.popBackStack() })
        }

        composable<KanjiListRoute> {
            KanjiListScreen(
                onBack = { navController.popBackStack() },
                onOpenKanji = { id -> navController.navigate(KanjiDetailRoute(id)) },
            )
        }

        composable<KanjiDetailRoute> { entry ->
            val route = entry.toRoute<KanjiDetailRoute>()
            KanjiDetailScreen(
                id = route.id,
                onBack = { navController.popBackStack() },
                onContinuePractice = { level -> navController.navigate(QuizRoute(level.name)) },
            )
        }

        composable<QuizRoute> { entry ->
            val route = entry.toRoute<QuizRoute>()
            val level = JlptLevel.entries.firstOrNull { it.name == route.level } ?: JlptLevel.N5
            QuizScreen(
                level = level,
                onFinish = { navController.popBackStack() },
            )
        }

        composable<ComingSoonRoute> { entry ->
            val route = entry.toRoute<ComingSoonRoute>()
            ComingSoonScreen(
                feature = route.feature,
                phase = route.phase,
                onBack = { navController.popBackStack() },
            )
        }
    }
}