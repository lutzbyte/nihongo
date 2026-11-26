package com.nihongo.app.data.content

import com.nihongo.app.domain.model.BookmarkedItems
import com.nihongo.app.domain.model.DayMinutes
import com.nihongo.app.domain.model.GrammarItem
import com.nihongo.app.domain.model.JlptLevel
import com.nihongo.app.domain.model.KanaItem
import com.nihongo.app.domain.model.KanaScript
import com.nihongo.app.domain.model.KanjiItem
import com.nihongo.app.domain.model.Lesson
import com.nihongo.app.domain.model.LessonSummary
import com.nihongo.app.domain.model.ProgressSummary
import com.nihongo.app.domain.model.QuizQuestion
import com.nihongo.app.domain.model.VocabularyDetail
import com.nihongo.app.domain.model.VocabularyPage
import java.time.LocalDate

interface LearningRepository {
    val isSample: Boolean

    suspend fun getVocabulary(level: JlptLevel, query: String, page: Int, pageSize: Int): VocabularyPage

    suspend fun getVocabularyDetail(id: String): VocabularyDetail?

    suspend fun getKanjiList(level: JlptLevel, query: String): List<KanjiItem>

    suspend fun getKanji(id: String): KanjiItem?

    suspend fun getGrammarList(level: JlptLevel, query: String): List<GrammarItem>

    suspend fun getGrammar(id: String): GrammarItem?

    suspend fun getKana(script: KanaScript): List<KanaItem>

    suspend fun getLessons(): List<LessonSummary>

    suspend fun getLesson(id: String): Lesson?

    /** Kunci bookmark berformat "vocab:ID", "kanji:ID", atau "grammar:ID". */
    suspend fun getBookmarks(keys: Set<String>): BookmarkedItems

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