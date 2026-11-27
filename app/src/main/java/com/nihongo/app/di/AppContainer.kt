package com.nihongo.app.di

import android.content.Context
import com.nihongo.app.data.content.LearningRepository
import com.nihongo.app.data.content.RoomLearningRepository
import com.nihongo.app.data.content.StudyAwareLearningRepository
import com.nihongo.app.data.db.NihongoDatabase
import com.nihongo.app.data.importer.CurriculumImporter
import com.nihongo.app.data.practice.PracticeRepository
import com.nihongo.app.data.practice.RoomPracticeRepository
import com.nihongo.app.data.preferences.UserPreferencesRepository
import com.nihongo.app.data.user.UserDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(val context: Context) {

    val applicationScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val preferencesRepository: UserPreferencesRepository =
        UserPreferencesRepository(context)

    val database: NihongoDatabase by lazy { NihongoDatabase.create(context) }

    val userDatabase: UserDatabase by lazy { UserDatabase.create(context) }

    val curriculumImporter: CurriculumImporter by lazy {
        CurriculumImporter(context, database)
    }

    val learningRepository: LearningRepository by lazy {
        StudyAwareLearningRepository(
            delegate = RoomLearningRepository(database.curriculumDao()),
            studyDao = userDatabase.studyDao(),
        )
    }

    val practiceRepository: PracticeRepository by lazy {
        RoomPracticeRepository(
            curriculumDao = database.curriculumDao(),
            practiceDao = database.practiceDao(),
            studyDao = userDatabase.studyDao(),
        )
    }
}