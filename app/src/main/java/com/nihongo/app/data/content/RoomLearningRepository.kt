package com.nihongo.app.data.content

import com.nihongo.app.data.db.CurriculumDao
import com.nihongo.app.data.db.GrammarEntity
import com.nihongo.app.data.db.KanjiEntity
import com.nihongo.app.data.db.VocabularyEntity
import com.nihongo.app.data.importer.ExampleDto
import com.nihongo.app.data.importer.LessonSectionDto
import com.nihongo.app.domain.model.BookmarkedItems
import com.nihongo.app.domain.model.GrammarExample
import com.nihongo.app.domain.model.GrammarItem
import com.nihongo.app.domain.model.JlptLevel
import com.nihongo.app.domain.model.KanaGroup
import com.nihongo.app.domain.model.KanaItem
import com.nihongo.app.domain.model.KanaScript
import com.nihongo.app.domain.model.KanjiExample
import com.nihongo.app.domain.model.KanjiItem
import com.nihongo.app.domain.model.Lesson
import com.nihongo.app.domain.model.LessonRow
import com.nihongo.app.domain.model.LessonSection
import com.nihongo.app.domain.model.LessonSummary
import com.nihongo.app.domain.model.ProgressSummary
import com.nihongo.app.domain.model.QuizQuestion
import com.nihongo.app.domain.model.VocabularyDetail
import com.nihongo.app.domain.model.VocabularyExample
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

    // Kosakata

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

    override suspend fun getVocabularyDetail(id: String): VocabularyDetail? =
        withContext(Dispatchers.IO) {
            val entity = dao.vocabularyById(id) ?: return@withContext null
            VocabularyDetail(
                id = entity.id,
                level = JlptLevel.valueOf(entity.level),
                word = entity.word,
                reading = entity.reading,
                romaji = entity.romaji,
                meaning = displayMeaning(entity.meaningId, entity.meaningEn),
                meaningTranslated = !entity.meaningId.isNullOrBlank(),
                partsOfSpeech = splitList(entity.pos).map { posLabel(it) }.distinct(),
                examples = decodeExamples(entity.examplesJson).map {
                    VocabularyExample(japanese = it.ja, furigana = it.furigana, english = it.en)
                },
            )
        }

    // Kanji

    override suspend fun getKanjiList(level: JlptLevel, query: String): List<KanjiItem> =
        withContext(Dispatchers.IO) {
            dao.kanjiByLevel(level.name, escapeLike(query.trim())).map { it.toDomain(example = null) }
        }

    override suspend fun getKanji(id: String): KanjiItem? = withContext(Dispatchers.IO) {
        val entity = dao.kanjiById(id) ?: return@withContext null
        entity.toDomain(example = findExample(entity))
    }

    // Grammar

    override suspend fun getGrammarList(level: JlptLevel, query: String): List<GrammarItem> =
        withContext(Dispatchers.IO) {
            dao.grammarByLevel(level.name, escapeLike(query.trim())).map { it.toDomain() }
        }

    override suspend fun getGrammar(id: String): GrammarItem? = withContext(Dispatchers.IO) {
        dao.grammarById(id)?.toDomain()
    }

    // Foundation

    override suspend fun getKana(script: KanaScript): List<KanaItem> = withContext(Dispatchers.IO) {
        dao.kanaByScript(script.name.lowercase()).map {
            KanaItem(
                id = it.id,
                script = script,
                kana = it.kana,
                romaji = it.romaji,
                group = KanaGroup.valueOf(it.kanaGroup.uppercase()),
                row = it.kanaRow,
            )
        }
    }

    override suspend fun getLessons(): List<LessonSummary> = withContext(Dispatchers.IO) {
        dao.lessons().map { LessonSummary(id = it.id, title = it.title, summary = it.summary) }
    }

    override suspend fun getLesson(id: String): Lesson? = withContext(Dispatchers.IO) {
        val entity = dao.lessonById(id) ?: return@withContext null
        val sections = try {
            json.decodeFromString(ListSerializer(LessonSectionDto.serializer()), entity.sectionsJson)
        } catch (e: Exception) {
            emptyList()
        }
        Lesson(
            id = entity.id,
            title = entity.title,
            summary = entity.summary,
            sections = sections.map { section ->
                LessonSection(
                    heading = section.heading,
                    body = section.body,
                    rows = section.rows.map {
                        LessonRow(ja = it.ja, reading = it.reading, romaji = it.romaji, meaning = it.meaning)
                    },
                )
            },
        )
    }

    // Bookmark

    override suspend fun getBookmarks(keys: Set<String>): BookmarkedItems = withContext(Dispatchers.IO) {
        fun idsOf(prefix: String) = keys.filter { it.startsWith(prefix) }.map { it.removePrefix(prefix) }

        val vocabulary = idsOf("vocab:").chunked(ID_CHUNK)
            .flatMap { dao.vocabularyByIds(it) }
            .sortedWith(compareBy({ it.reading }, { it.word }))
            .map { it.toDomain() }
        val kanji = idsOf("kanji:").chunked(ID_CHUNK)
            .flatMap { dao.kanjiByIds(it) }
            .sortedBy { it.freq ?: Int.MAX_VALUE }
            .map { it.toDomain(example = null) }
        val grammar = idsOf("grammar:").chunked(ID_CHUNK)
            .flatMap { dao.grammarByIds(it) }
            .sortedBy { it.sortOrder }
            .map { it.toDomain() }
        BookmarkedItems(vocabulary = vocabulary, kanji = kanji, grammar = grammar)
    }

    // Kuis dan progress

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

    // Pembantu

    private suspend fun findExample(kanji: KanjiEntity): KanjiExample? {
        val words = splitList(kanji.words)
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

    private fun splitList(raw: String): List<String> =
        raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    private fun escapeLike(value: String): String =
        value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

    private fun displayMeaning(meaningId: String?, meaningEn: String): String =
        meaningId?.takeIf { it.isNotBlank() }
            ?: meaningEn.split("; ").take(3).joinToString("; ")

    private fun posLabel(code: String): String = when {
        code == "n" || code == "noun" -> "kata benda"
        code == "v1" || code == "ichidan verb" -> "kata kerja (ichidan)"
        code.startsWith("v5") || code == "godan verb" -> "kata kerja (godan)"
        code.startsWith("vs") || code == "suru verb" -> "kata kerja する"
        code.startsWith("vk") -> "kata kerja 来る"
        code == "aux-v" -> "kata kerja bantu"
        code == "adj-i" || code == "i-adjective" -> "kata sifat-i"
        code == "adj-na" || code == "na-adjective" -> "kata sifat-na"
        code == "adv" || code == "adverb" -> "kata keterangan"
        code == "pn" || code == "pronoun" -> "kata ganti"
        code == "prt" || code == "particle" -> "partikel"
        code == "conj" || code == "conjunction" -> "konjungsi"
        code == "int" || code == "interjection" -> "kata seru"
        code == "exp" || code == "expression" -> "ungkapan"
        code == "ctr" || code == "counter" -> "penghitung"
        code == "num" || code == "numeric" -> "bilangan"
        code == "pref" || code == "prefix" -> "awalan"
        code == "suf" || code == "suffix" -> "akhiran"
        else -> code
    }

    private fun VocabularyEntity.toDomain() = VocabularyItem(
        id = id,
        level = JlptLevel.valueOf(level),
        word = word,
        reading = reading,
        meaning = displayMeaning(meaningId, meaningEn),
    )

    private fun GrammarEntity.toDomain() = GrammarItem(
        id = id,
        level = JlptLevel.valueOf(level),
        pattern = pattern,
        reading = reading?.takeIf { it.isNotBlank() },
        romaji = romaji?.takeIf { it.isNotBlank() },
        meaning = displayMeaning(meaningId, meaningEn),
        meaningTranslated = !meaningId.isNullOrBlank(),
        formation = formation,
        notes = notes?.takeIf { it.isNotBlank() },
        tags = splitList(tags),
        examples = decodeExamples(examplesJson).map {
            GrammarExample(japanese = it.ja, english = it.en.orEmpty())
        },
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

    private companion object {
        const val ID_CHUNK = 500
    }
}