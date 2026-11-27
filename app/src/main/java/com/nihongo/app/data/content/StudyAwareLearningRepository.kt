package com.nihongo.app.data.content

import com.nihongo.app.data.user.StudyDao
import com.nihongo.app.domain.model.ProgressSummary
import com.nihongo.app.domain.srs.SrsScheduler
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Semua fungsi materi diteruskan ke [delegate]. Hanya [getProgress] yang dihitung dari
 * data belajar di UserDatabase.
 */
class StudyAwareLearningRepository(
    private val delegate: LearningRepository,
    private val studyDao: StudyDao,
) : LearningRepository by delegate {

    override suspend fun getProgress(days: Int): ProgressSummary = withContext(Dispatchers.IO) {
        val today = LocalDate.now().toEpochDay()
        val from = today - days + 1
        val secondsByDay = studyDao.secondsPerDay(from, today).associate { it.epochDay to it.seconds }

        val daily = emptyDays(days).mapIndexed { index, day ->
            day.copy(minutes = (secondsByDay[from + index] ?: 0) / 60)
        }
        val totalSeconds = secondsByDay.values.sum()
        val previousSeconds = studyDao.totalSeconds(from - days, from - 1) ?: 0
        val change = if (previousSeconds > 0) {
            ((totalSeconds - previousSeconds) * 100.0 / previousSeconds).roundToInt()
        } else {
            null
        }

        val studied = studyDao.studyDays().toSet()
        var cursor = if (today in studied) today else today - 1
        var streak = 0
        while (cursor in studied) {
            streak += 1
            cursor -= 1
        }

        ProgressSummary(
            totalMinutes = totalSeconds / 60,
            changePercent = change,
            dailyMinutes = daily,
            vocabularyLearned = studyDao.countCards("vocab"),
            kanjiMastered = studyDao.countMatureCards("kanji", SrsScheduler.MATURE_INTERVAL_DAYS),
            grammarProgress = studyDao.countCards("grammar"),
            streakDays = streak,
            todayMinutes = (secondsByDay[today] ?: 0) / 60,
        )
    }
}