package com.nihongo.app.data.practice

import com.nihongo.app.data.db.CurriculumDao
import com.nihongo.app.data.db.GrammarEntity
import com.nihongo.app.data.db.KanjiEntity
import com.nihongo.app.data.db.PracticeDao
import com.nihongo.app.data.db.VocabularyEntity
import com.nihongo.app.data.user.SrsCardEntity
import com.nihongo.app.data.user.StudyDao
import com.nihongo.app.data.user.StudySessionEntity
import com.nihongo.app.domain.model.FlashCard
import com.nihongo.app.domain.model.ItemType
import com.nihongo.app.domain.model.JlptLevel
import com.nihongo.app.domain.model.PracticeQuestion
import com.nihongo.app.domain.model.PracticeSection
import com.nihongo.app.domain.model.QuestionKind
import com.nihongo.app.domain.model.SessionKind
import com.nihongo.app.domain.model.StudyScope
import com.nihongo.app.domain.srs.SrsGrade
import com.nihongo.app.domain.srs.SrsScheduler
import com.nihongo.app.domain.srs.SrsState
import java.time.LocalDate
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class RoomPracticeRepository(
    private val curriculumDao: CurriculumDao,
    private val practiceDao: PracticeDao,
    private val studyDao: StudyDao,
) : PracticeRepository {

    private val factory = QuestionFactory(Json { ignoreUnknownKeys = true })

    private data class CardRef(
        val type: ItemType,
        val id: String,
        val state: SrsState,
        val isNew: Boolean,
    )

    override fun observeDueCount(): Flow<Int> =
        studyDao.observeDueCount(LocalDate.now().toEpochDay())

    // Kartu

    override suspend fun buildDeck(
        kind: SessionKind,
        scope: StudyScope,
        level: JlptLevel?,
    ): List<FlashCard> = withContext(Dispatchers.IO) {
        val today = LocalDate.now().toEpochDay()
        val refs = ArrayList<CardRef>()

        val due = studyDao.dueCards(
            today = today,
            types = scope.types.map { it.keyPrefix },
            level = level?.name.orEmpty(),
            limit = DUE_LIMIT,
        )
        due.forEach { entity ->
            val type = ItemType.entries.firstOrNull { it.keyPrefix == entity.itemType } ?: return@forEach
            refs += CardRef(type, entity.itemKey.substringAfter(':'), entity.toState(), isNew = false)
        }

        if (kind == SessionKind.FLASHCARD && level != null) {
            val perType = max(1, NEW_LIMIT / scope.types.size)
            scope.types.forEach { type -> refs += newRefs(type, level, perType, today) }
        }
        loadCards(refs, level)
    }

    override suspend fun gradeCard(card: FlashCard, grade: SrsGrade): SrsState =
        withContext(Dispatchers.IO) {
            val today = LocalDate.now().toEpochDay()
            val next = SrsScheduler.next(card.state, grade, today)
            val now = System.currentTimeMillis()
            val existing = studyDao.cardsByKeys(listOf(card.itemKey)).firstOrNull()
            studyDao.upsertCard(
                SrsCardEntity(
                    itemKey = card.itemKey,
                    itemType = card.type.keyPrefix,
                    level = card.level.name,
                    ease = next.ease,
                    intervalDays = next.intervalDays,
                    repetitions = next.repetitions,
                    lapses = next.lapses,
                    dueEpochDay = next.dueEpochDay,
                    lastReviewedEpochMs = now,
                    createdEpochMs = existing?.createdEpochMs ?: now,
                ),
            )
            next
        }

    override suspend fun recordFlashcardSession(
        kind: SessionKind,
        level: JlptLevel?,
        startedAtEpochMs: Long,
        durationSeconds: Int,
        reviewed: Int,
        againCount: Int,
    ) = withContext(Dispatchers.IO) {
        if (reviewed <= 0) return@withContext
        studyDao.insertSession(
            StudySessionEntity(
                kind = kind.name,
                level = level?.name,
                section = null,
                startedAtEpochMs = startedAtEpochMs,
                epochDay = LocalDate.now().toEpochDay(),
                durationSeconds = durationSeconds,
                itemCount = reviewed,
                correctCount = max(0, reviewed - againCount),
                detail = null,
            ),
        )
        Unit
    }

    private suspend fun newRefs(type: ItemType, level: JlptLevel, limit: Int, today: Long): List<CardRef> {
        val ids = when (type) {
            ItemType.VOCABULARY -> practiceDao.vocabularyIdsRandom(level.name)
            ItemType.KANJI -> practiceDao.kanjiIdsOrdered(level.name)
            ItemType.GRAMMAR -> practiceDao.grammarIdsOrdered(level.name)
        }
        val existing = ids.chunked(ID_CHUNK)
            .flatMap { chunk -> studyDao.existingKeys(chunk.map { "${type.keyPrefix}:$it" }) }
            .toSet()
        return ids
            .filter { "${type.keyPrefix}:$it" !in existing }
            .take(limit)
            .map { CardRef(type, it, SrsScheduler.initial(today), isNew = true) }
    }

    private suspend fun loadCards(refs: List<CardRef>, level: JlptLevel?): List<FlashCard> {
        fun idsOf(type: ItemType) = refs.filter { it.type == type }.map { it.id }.chunked(ID_CHUNK)

        val vocabulary = idsOf(ItemType.VOCABULARY)
            .flatMap { curriculumDao.vocabularyByIds(it) }.associateBy { it.id }
        val kanji = idsOf(ItemType.KANJI)
            .flatMap { curriculumDao.kanjiByIds(it) }.associateBy { it.id }
        val grammar = idsOf(ItemType.GRAMMAR)
            .flatMap { curriculumDao.grammarByIds(it) }.associateBy { it.id }

        return refs.mapNotNull { ref ->
            when (ref.type) {
                ItemType.VOCABULARY -> vocabulary[ref.id]?.let { vocabularyCard(it, ref) }
                ItemType.KANJI -> kanji[ref.id]?.let { kanjiCard(it, ref) }
                ItemType.GRAMMAR -> grammar[ref.id]?.let { grammarCard(it, ref) }
            }
        }.filter { level == null || it.level == level }
    }

    private fun vocabularyCard(entity: VocabularyEntity, ref: CardRef) = FlashCard(
        itemKey = "vocab:${entity.id}",
        type = ItemType.VOCABULARY,
        level = JlptLevel.valueOf(entity.level),
        front = entity.word,
        reading = entity.reading.takeIf { it != entity.word },
        meaning = meaningText(entity.meaningId, entity.meaningEn),
        extra = null,
        isNew = ref.isNew,
        state = ref.state,
    )

    private fun kanjiCard(entity: KanjiEntity, ref: CardRef) = FlashCard(
        itemKey = "kanji:${entity.id}",
        type = ItemType.KANJI,
        level = JlptLevel.valueOf(entity.level),
        front = entity.character,
        reading = null,
        meaning = meaningText(entity.meaningId, entity.meaningEn).ifBlank { entity.character },
        extra = QuestionFactory.readingLine(entity),
        isNew = ref.isNew,
        state = ref.state,
    )

    private fun grammarCard(entity: GrammarEntity, ref: CardRef) = FlashCard(
        itemKey = "grammar:${entity.id}",
        type = ItemType.GRAMMAR,
        level = JlptLevel.valueOf(entity.level),
        front = entity.pattern,
        reading = entity.reading?.takeIf { it.isNotBlank() },
        meaning = meaningText(entity.meaningId, entity.meaningEn),
        extra = entity.formation.takeIf { it.isNotBlank() },
        isNew = ref.isNew,
        state = ref.state,
    )

    // Soal

    override suspend fun buildQuestions(
        level: JlptLevel,
        section: PracticeSection,
        count: Int,
    ): List<PracticeQuestion> = withContext(Dispatchers.IO) {
        val bunpoCount = when (section) {
            PracticeSection.MOJI_GOI -> 0
            PracticeSection.BUNPO -> count
            PracticeSection.MIXED -> count / 3
        }
        val mojiCount = count - bunpoCount

        val result = ArrayList<PracticeQuestion>()
        if (mojiCount > 0) result += mojiGoiQuestions(level, mojiCount)
        if (bunpoCount > 0) result += bunpoQuestions(level, bunpoCount)
        if (section == PracticeSection.MIXED) result.shuffle()
        result
    }

    override suspend fun buildExam(level: JlptLevel): List<PracticeQuestion> =
        buildQuestions(level, PracticeSection.MOJI_GOI, EXAM_MOJI_GOI) +
            buildQuestions(level, PracticeSection.BUNPO, EXAM_BUNPO)

    private suspend fun mojiGoiQuestions(level: JlptLevel, count: Int): List<PracticeQuestion> {
        val vocabCount = (count * 3 + 4) / 5
        val kanjiCount = count - vocabCount
        val questions = ArrayList<PracticeQuestion>()

        if (vocabCount > 0) {
            val pool = curriculumDao.randomVocabulary(level.name, vocabCount * POOL_FACTOR + POOL_EXTRA)
            pool.take(vocabCount).forEachIndexed { index, target ->
                val kind = VOCAB_KINDS[index % VOCAB_KINDS.size]
                (factory.vocabulary(level, target, pool, kind)
                    ?: factory.vocabulary(level, target, pool, QuestionKind.VOCAB_MEANING))
                    ?.let { questions += it }
            }
        }
        if (kanjiCount > 0) {
            val pool = practiceDao.randomKanji(level.name, kanjiCount * POOL_FACTOR + POOL_EXTRA)
            pool.take(kanjiCount).forEachIndexed { index, target ->
                val kind = KANJI_KINDS[index % KANJI_KINDS.size]
                (factory.kanji(level, target, pool, kind)
                    ?: factory.kanji(level, target, pool, QuestionKind.KANJI_MEANING))
                    ?.let { questions += it }
            }
        }
        questions.shuffle()
        return questions
    }

    private suspend fun bunpoQuestions(level: JlptLevel, count: Int): List<PracticeQuestion> {
        val pool = practiceDao.randomGrammar(level.name, count * POOL_FACTOR + POOL_EXTRA)
        val questions = ArrayList<PracticeQuestion>()
        pool.take(count).forEachIndexed { index, target ->
            val kind = GRAMMAR_KINDS[index % GRAMMAR_KINDS.size]
            (factory.grammar(level, target, pool, kind)
                ?: factory.grammar(level, target, pool, QuestionKind.GRAMMAR_MEANING))
                ?.let { questions += it }
        }
        return questions
    }

    // Hasil

    override suspend fun recordQuizResult(
        kind: SessionKind,
        section: PracticeSection,
        level: JlptLevel,
        questions: List<PracticeQuestion>,
        answers: Map<Int, Int>,
        startedAtEpochMs: Long,
        durationSeconds: Int,
    ) = withContext(Dispatchers.IO) {
        val today = LocalDate.now().toEpochDay()
        var correct = 0
        val perSection = linkedMapOf<PracticeSection, IntArray>()

        questions.forEachIndexed { index, question ->
            val answer = answers[index]
            val isCorrect = answer == question.answerIndex
            val score = perSection.getOrPut(question.section) { IntArray(2) }
            score[1] += 1
            if (isCorrect) {
                correct += 1
                score[0] += 1
            } else if (answer != null) {
                markLapse(question.itemKey, level, today)
            }
        }

        val detail = if (kind == SessionKind.EXAM) {
            perSection.entries.joinToString(";") { (key, score) -> "${key.name}=${score[0]}/${score[1]}" }
        } else {
            null
        }
        studyDao.insertSession(
            StudySessionEntity(
                kind = kind.name,
                level = level.name,
                section = section.name,
                startedAtEpochMs = startedAtEpochMs,
                epochDay = today,
                durationSeconds = durationSeconds,
                itemCount = questions.size,
                correctCount = correct,
                detail = detail,
            ),
        )
        Unit
    }

    private suspend fun markLapse(itemKey: String, level: JlptLevel, today: Long) {
        val type = ItemType.entries.firstOrNull { itemKey.startsWith("${it.keyPrefix}:") } ?: return
        val existing = studyDao.cardsByKeys(listOf(itemKey)).firstOrNull()
        val next = SrsScheduler.next(existing?.toState() ?: SrsScheduler.initial(today), SrsGrade.AGAIN, today)
        val now = System.currentTimeMillis()
        studyDao.upsertCard(
            SrsCardEntity(
                itemKey = itemKey,
                itemType = type.keyPrefix,
                level = existing?.level ?: level.name,
                ease = next.ease,
                intervalDays = next.intervalDays,
                repetitions = next.repetitions,
                lapses = next.lapses,
                dueEpochDay = next.dueEpochDay,
                lastReviewedEpochMs = now,
                createdEpochMs = existing?.createdEpochMs ?: now,
            ),
        )
    }

    private fun SrsCardEntity.toState() = SrsState(
        ease = ease,
        intervalDays = intervalDays,
        repetitions = repetitions,
        lapses = lapses,
        dueEpochDay = dueEpochDay,
    )

    private companion object {
        const val DUE_LIMIT = 30
        const val NEW_LIMIT = 10
        const val ID_CHUNK = 500
        const val POOL_FACTOR = 6
        const val POOL_EXTRA = 12
        const val EXAM_MOJI_GOI = 20
        const val EXAM_BUNPO = 10

        val VOCAB_KINDS = listOf(
            QuestionKind.VOCAB_MEANING,
            QuestionKind.VOCAB_FROM_MEANING,
            QuestionKind.VOCAB_READING,
        )
        val KANJI_KINDS = listOf(QuestionKind.KANJI_MEANING, QuestionKind.KANJI_READING)
        val GRAMMAR_KINDS = listOf(QuestionKind.GRAMMAR_MEANING, QuestionKind.GRAMMAR_PATTERN)
    }
}