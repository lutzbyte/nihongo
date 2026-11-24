package com.nihongo.app.di

import android.content.Context
import com.nihongo.app.data.content.LearningRepository
import com.nihongo.app.data.content.RoomLearningRepository
import com.nihongo.app.data.db.NihongoDatabase
import com.nihongo.app.data.importer.CurriculumImporter
import com.nihongo.app.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(val context: Context) {

    val applicationScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val preferencesRepository: UserPreferencesRepository =
        UserPreferencesRepository(context)

    val database: NihongoDatabase by lazy { NihongoDatabase.create(context) }

    val curriculumImporter: CurriculumImporter by lazy {
        CurriculumImporter(context, database)
    }

    val learningRepository: LearningRepository by lazy {
        RoomLearningRepository(database.curriculumDao())
    }
}