package com.nihongo.app.di

import android.content.Context
import com.nihongo.app.BuildConfig
import com.nihongo.app.data.content.EmptyLearningRepository
import com.nihongo.app.data.content.LearningRepository
import com.nihongo.app.data.content.SampleLearningRepository
import com.nihongo.app.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(val context: Context) {

    val applicationScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val preferencesRepository: UserPreferencesRepository =
        UserPreferencesRepository(context)

    val learningRepository: LearningRepository =
        if (BuildConfig.DEBUG) SampleLearningRepository(context) else EmptyLearningRepository()
}