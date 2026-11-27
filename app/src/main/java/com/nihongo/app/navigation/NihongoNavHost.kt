package com.nihongo.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.nihongo.app.domain.model.JlptLevel
import com.nihongo.app.domain.model.KanaScript
import com.nihongo.app.domain.model.PracticeSection
import com.nihongo.app.domain.model.SessionKind
import com.nihongo.app.ui.bookmark.BookmarkScreen
import com.nihongo.app.ui.common.ComingSoonScreen
import com.nihongo.app.ui.foundation.FoundationScreen
import com.nihongo.app.ui.foundation.KanaScreen
import com.nihongo.app.ui.foundation.LessonScreen
import com.nihongo.app.ui.grammar.GrammarDetailScreen
import com.nihongo.app.ui.grammar.GrammarListScreen
import com.nihongo.app.ui.home.HomeScreen
import com.nihongo.app.ui.kanji.KanjiDetailScreen
import com.nihongo.app.ui.kanji.KanjiListScreen
import com.nihongo.app.ui.practice.FlashcardScreen
import com.nihongo.app.ui.practice.PracticeScreen
import com.nihongo.app.ui.practice.PracticeSetupScreen
import com.nihongo.app.ui.practice.QuizScreen
import com.nihongo.app.ui.progress.ProgressScreen
import com.nihongo.app.ui.settings.SettingsScreen
import com.nihongo.app.ui.splash.SplashScreen
import com.nihongo.app.ui.vocabulary.VocabularyDetailScreen
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
                onOpenGrammar = { navController.navigate(GrammarListRoute) },
                onOpenFoundation = { navController.navigate(FoundationRoute) },
                onOpenBookmarks = { navController.navigate(BookmarkRoute) },
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
                onOpenSetup = { mode -> navController.navigate(PracticeSetupRoute(mode)) },
                onStartReview = { navController.navigate(FlashcardRoute("REVIEW", "ALL", "ALL")) },
            )
        }

        composable<PracticeSetupRoute> { entry ->
            val route = entry.toRoute<PracticeSetupRoute>()
            PracticeSetupScreen(
                mode = route.mode,
                onBack = { navController.popBackStack() },
                onStartFlashcard = { scope, level ->
                    navController.navigate(FlashcardRoute("FLASHCARD", scope.name, level.name))
                },
                onStartQuiz = { level, section, mode ->
                    navController.navigate(QuizRoute(level.name, section.name, mode))
                },
            )
        }

        composable<FlashcardRoute> { entry ->
            val route = entry.toRoute<FlashcardRoute>()
            FlashcardScreen(
                kind = route.kind,
                scope = route.scope,
                level = route.level,
                onFinish = { navController.popBackStack() },
            )
        }

        composable<ProgressRoute> {
            ProgressScreen(onBack = { navController.navigateToHomeTab() })
        }

        composable<SettingsRoute> {
            SettingsScreen(onBack = { navController.navigateToHomeTab() })
        }

        composable<VocabularyRoute> {
            VocabularyScreen(
                onBack = { navController.popBackStack() },
                onOpenDetail = { id -> navController.navigate(VocabularyDetailRoute(id)) },
            )
        }

        composable<VocabularyDetailRoute> { entry ->
            val route = entry.toRoute<VocabularyDetailRoute>()
            VocabularyDetailScreen(id = route.id, onBack = { navController.popBackStack() })
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
                onContinuePractice = { level ->
                    navController.navigate(QuizRoute(level.name, PracticeSection.MOJI_GOI.name))
                },
            )
        }

        composable<GrammarListRoute> {
            GrammarListScreen(
                onBack = { navController.popBackStack() },
                onOpenGrammar = { id -> navController.navigate(GrammarDetailRoute(id)) },
            )
        }

        composable<GrammarDetailRoute> { entry ->
            val route = entry.toRoute<GrammarDetailRoute>()
            GrammarDetailScreen(id = route.id, onBack = { navController.popBackStack() })
        }

        composable<FoundationRoute> {
            FoundationScreen(
                onBack = { navController.popBackStack() },
                onOpenKana = { script -> navController.navigate(KanaRoute(script)) },
                onOpenLesson = { id -> navController.navigate(LessonRoute(id)) },
            )
        }

        composable<KanaRoute> { entry ->
            val route = entry.toRoute<KanaRoute>()
            val script = KanaScript.entries.firstOrNull { it.name == route.script } ?: KanaScript.HIRAGANA
            KanaScreen(script = script, onBack = { navController.popBackStack() })
        }

        composable<LessonRoute> { entry ->
            val route = entry.toRoute<LessonRoute>()
            LessonScreen(id = route.id, onBack = { navController.popBackStack() })
        }

        composable<BookmarkRoute> {
            BookmarkScreen(
                onBack = { navController.popBackStack() },
                onOpenVocabulary = { id -> navController.navigate(VocabularyDetailRoute(id)) },
                onOpenKanji = { id -> navController.navigate(KanjiDetailRoute(id)) },
                onOpenGrammar = { id -> navController.navigate(GrammarDetailRoute(id)) },
            )
        }

        composable<QuizRoute> { entry ->
            val route = entry.toRoute<QuizRoute>()
            val level = JlptLevel.entries.firstOrNull { it.name == route.level } ?: JlptLevel.N5
            val section = PracticeSection.entries.firstOrNull { it.name == route.section } ?: PracticeSection.MIXED
            val mode = if (route.mode == "EXAM") SessionKind.EXAM else SessionKind.QUIZ
            QuizScreen(
                level = level,
                section = section,
                mode = mode,
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