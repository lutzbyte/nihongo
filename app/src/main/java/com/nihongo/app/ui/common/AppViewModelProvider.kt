package com.nihongo.app.ui.common

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nihongo.app.NihongoApplication
import com.nihongo.app.di.AppContainer
import com.nihongo.app.ui.home.HomeViewModel
import com.nihongo.app.ui.kanji.KanjiViewModel
import com.nihongo.app.ui.practice.QuizViewModel
import com.nihongo.app.ui.progress.ProgressViewModel
import com.nihongo.app.ui.settings.SettingsViewModel
import com.nihongo.app.ui.splash.SplashViewModel
import com.nihongo.app.ui.vocabulary.VocabularyViewModel

object AppViewModelProvider {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer { SplashViewModel(nihongoContainer()) }
        initializer { HomeViewModel(nihongoContainer()) }
        initializer { VocabularyViewModel(nihongoContainer()) }
        initializer { KanjiViewModel(nihongoContainer()) }
        initializer { QuizViewModel(nihongoContainer()) }
        initializer { ProgressViewModel(nihongoContainer()) }
        initializer { SettingsViewModel(nihongoContainer()) }
    }
}

private fun CreationExtras.nihongoContainer(): AppContainer =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as NihongoApplication).container