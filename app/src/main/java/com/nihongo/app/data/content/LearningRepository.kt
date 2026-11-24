package com.nihongo.app.data.content

import com.nihongo.app.domain.model.DayMinutes
import com.nihongo.app.domain.model.JlptLevel
import com.nihongo.app.domain.model.KanjiItem
import com.nihongo.app.domain.model.ProgressSummary
import com.nihongo.app.domain.model.QuizQuestion
import com.nihongo.app.domain.model.VocabularyPage
import java.time.LocalDate

interface LearningRepository {
    val isSample: Boolean

    suspend fun getVocabulary(level: JlptLevel, query: String, page: Int, pageSize: Int): VocabularyPage

    suspend fun getKanjiList(level: JlptLevel): List<KanjiItem>

    suspend fun getKanji(id: String): KanjiItem?

    suspend fun getQuizQuestions(level: JlptLevel, limit: Int): List<QuizQuestion>

    suspend fun getProgress(days: Int): ProgressSummary
}

private val DAY_LABELS = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")

/** Hari-hari kosong (0 menit) berakhir hari ini. Label hanya untuk rentang 7 hari. */
internal fun emptyDays(days: Int): List<DayMinutes> {
    val today = LocalDate.now()
    return (days - 1 downTo 0).map { offset ->
        val date = today.minusDays(offset.toLong())
        val label = if (days <= 7) DAY_LABELS[date.dayOfWeek.value - 1] else ""
        DayMinutes(label = label, minutes = 0)
    }
}

class EmptyLearningRepository : LearningRepository {
    override val isSample: Boolean = false

    override suspend fun getVocabulary(level: JlptLevel, query: String, page: Int, pageSize: Int) =
        VocabularyPage(items = emptyList(), total = 0)

    override suspend fun getKanjiList(level: JlptLevel): List<KanjiItem> = emptyList()

    override suspend fun getKanji(id: String): KanjiItem? = null

    override suspend fun getQuizQuestions(level: JlptLevel, limit: Int): List<QuizQuestion> = emptyList()

    override suspend fun getProgress(days: Int): ProgressSummary =
        ProgressSummary(dailyMinutes = emptyDays(days))
}