package com.nihongo.app.data.content

import android.content.Context
import com.nihongo.app.domain.model.DayMinutes
import com.nihongo.app.domain.model.JlptLevel
import com.nihongo.app.domain.model.KanjiItem
import com.nihongo.app.domain.model.ProgressSummary
import com.nihongo.app.domain.model.QuizQuestion
import com.nihongo.app.domain.model.VocabularyItem
import com.nihongo.app.domain.model.VocabularyPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class SampleContent(
    val vocabulary: List<VocabularyItem> = emptyList(),
    val kanji: List<KanjiItem> = emptyList(),
    val quiz: List<QuizQuestion> = emptyList(),
    val progress: ProgressSummary = ProgressSummary(),
)

class SampleLearningRepository(private val context: Context) : LearningRepository {

    override val isSample: Boolean = true

    private val json = Json { ignoreUnknownKeys = true }

    private val content: SampleContent by lazy {
        try {
            context.assets.open(ASSET_PATH).bufferedReader().use { reader ->
                json.decodeFromString<SampleContent>(reader.readText())
            }
        } catch (e: Exception) {
            SampleContent()
        }
    }

    override suspend fun getVocabulary(
        level: JlptLevel,
        query: String,
        page: Int,
        pageSize: Int,
    ): VocabularyPage = withContext(Dispatchers.IO) {
        val q = query.trim()
        val filtered = content.vocabulary.filter { item ->
            item.level == level && (
                q.isEmpty() ||
                    item.word.contains(q, ignoreCase = true) ||
                    item.reading.contains(q, ignoreCase = true) ||
                    item.meaning.contains(q, ignoreCase = true)
                )
        }
        VocabularyPage(
            items = filtered.drop(page * pageSize).take(pageSize),
            total = filtered.size,
        )
    }

    override suspend fun getKanjiList(level: JlptLevel): List<KanjiItem> =
        withContext(Dispatchers.IO) { content.kanji.filter { it.level == level } }

    override suspend fun getKanji(id: String): KanjiItem? =
        withContext(Dispatchers.IO) { content.kanji.firstOrNull { it.id == id } }

    override suspend fun getQuizQuestions(level: JlptLevel, limit: Int): List<QuizQuestion> =
        withContext(Dispatchers.IO) { content.quiz.filter { it.level == level }.take(limit) }

    override suspend fun getProgress(days: Int): ProgressSummary = withContext(Dispatchers.IO) {
        val base = content.progress
        val daily = base.dailyMinutes
        val adjusted = when {
            daily.isEmpty() -> emptyDays(days)
            daily.size >= days -> daily.takeLast(days)
            else -> List(days - daily.size) { DayMinutes(label = "", minutes = 0) } + daily
        }
        base.copy(dailyMinutes = adjusted)
    }

    private companion object {
        const val ASSET_PATH = "sample/sample_content.json"
    }
}