package com.nihongo.app.data.content

import com.nihongo.app.data.db.CurriculumDao
import com.nihongo.app.data.db.KanjiEntity
import com.nihongo.app.data.db.VocabularyEntity
import com.nihongo.app.data.importer.ExampleDto
import com.nihongo.app.domain.model.JlptLevel
import com.nihongo.app.domain.model.KanjiExample
import com.nihongo.app.domain.model.KanjiItem
import com.nihongo.app.domain.model.ProgressSummary
import com.nihongo.app.domain.model.QuizQuestion
import com.nihongo.app.domain.model.VocabularyItem
import com.nihongo.app.domain.model.VocabularyPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class RoomLearningRepository(private val dao: CurriculumDao) : LearningRepository {

    /** Data selalu berasal dari dataset yang diimpor, bukan data contoh. */
    override val isSample: Boolean = false

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getVocabulary(
        level: JlptLevel,
        query: String,
        page: Int,
        pageSize: Int,
    ): VocabularyPage = withContext(Dispatchers.IO) {
        val q = escapeLike(query.trim())
        val total = dao.vocabularyCount(level.name, q)
        val items = dao.vocabularyPage(level.name, q, pageSize, page * pageSize)
            .map { it.toDomain() }
        VocabularyPage(items = items, total = total)
    }

    override suspend fun getKanjiList(level: JlptLevel): List<KanjiItem> =
        withContext(Dispatchers.IO) {
            dao.kanjiByLevel(level.name).map { it.toDomain(example = null) }
        }

    override suspend fun getKanji(id: String): KanjiItem? = withContext(Dispatchers.IO) {
        val entity = dao.kanjiById(id) ?: return@withContext null
        entity.toDomain(example = findExample(entity))
    }

    override suspend fun getQuizQuestions(level: JlptLevel, limit: Int): List<QuizQuestion> =
        withContext(Dispatchers.IO) {
            val targets = dao.randomVocabulary(level.name, limit)
            if (targets.isEmpty()) return@withContext emptyList()
            val pool = dao.randomVocabulary(level.name, limit * 6 + 12)

            targets.mapNotNull { target ->
                val correct = displayMeaning(target.meaningId, target.meaningEn)
                val distractors = pool.asSequence()
                    .filter { it.id != target.id }
                    .map { displayMeaning(it.meaningId, it.meaningEn) }
                    .filter { it != correct }
                    .distinct()
                    .take(3)
                    .toList()
                if (distractors.size < 3) {
                    null
                } else {
                    val options = (distractors + correct).shuffled()
                    QuizQuestion(
                        id = target.id,
                        level = level,
                        word = target.word,
                        reading = target.reading,
                        options = options,
                        answerIndex = options.indexOf(correct),
                    )
                }
            }
        }

    /** Belum ada data aktivitas belajar (dikerjakan di Phase 6), jadi semua nilai nol. */
    override suspend fun getProgress(days: Int): ProgressSummary =
        ProgressSummary(dailyMinutes = emptyDays(days))

    private suspend fun findExample(kanji: KanjiEntity): KanjiExample? {
        val words = kanji.words.split(",").filter { it.isNotBlank() }
        if (words.isEmpty()) return null
        val byWord = dao.vocabularyByWords(words).associateBy { it.word }
        for (word in words) {
            val vocabulary = byWord[word] ?: continue
            val example = decodeExamples(vocabulary.examplesJson).firstOrNull() ?: continue
            return KanjiExample(
                japanese = example.ja,
                romaji = "",
                meaning = example.en.orEmpty(),
            )
        }
        return null
    }

    private fun decodeExamples(raw: String): List<ExampleDto> =
        try {
            json.decodeFromString(ListSerializer(ExampleDto.serializer()), raw)
        } catch (e: Exception) {
            emptyList()
        }

    private fun escapeLike(value: String): String =
        value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

    private fun displayMeaning(meaningId: String?, meaningEn: String): String =
        meaningId?.takeIf { it.isNotBlank() }
            ?: meaningEn.split("; ").take(3).joinToString("; ")

    private fun VocabularyEntity.toDomain() = VocabularyItem(
        id = id,
        level = JlptLevel.valueOf(level),
        word = word,
        reading = reading,
        meaning = displayMeaning(meaningId, meaningEn),
    )

    private fun KanjiEntity.toDomain(example: KanjiExample?): KanjiItem {
        val onReadings = onyomi.split("、").filter { it.isNotBlank() }
        val kunReadings = kunyomi.split("、").filter { it.isNotBlank() }
            .map { it.replace(".", "") }
        val kunTitle = kunReadings.firstOrNull { !it.startsWith("-") }
        val onTitle = onReadings.firstOrNull()?.let { katakanaToHiragana(it) }
        return KanjiItem(
            id = id,
            level = JlptLevel.valueOf(level),
            character = character,
            readingKana = listOfNotNull(kunTitle, onTitle).joinToString(" / ").ifBlank { character },
            readingRomaji = "",
            onyomi = onReadings.joinToString("、").ifBlank { "-" },
            kunyomi = kunReadings.joinToString("、").ifBlank { "-" },
            strokes = strokes,
            meaning = displayMeaning(meaningId, meaningEn).ifBlank { character },
            example = example,
        )
    }

    private fun katakanaToHiragana(text: String): String =
        buildString {
            text.forEach { ch ->
                append(if (ch in 'ァ'..'ヶ') (ch.code - 0x60).toChar() else ch)
            }
        }
}