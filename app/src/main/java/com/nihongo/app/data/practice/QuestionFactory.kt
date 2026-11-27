package com.nihongo.app.data.practice

import com.nihongo.app.data.db.GrammarEntity
import com.nihongo.app.data.db.KanjiEntity
import com.nihongo.app.data.db.VocabularyEntity
import com.nihongo.app.data.importer.ExampleDto
import com.nihongo.app.domain.model.JlptLevel
import com.nihongo.app.domain.model.PracticeQuestion
import com.nihongo.app.domain.model.QuestionExplanation
import com.nihongo.app.domain.model.QuestionKind
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

internal fun meaningText(meaningId: String?, meaningEn: String): String =
    meaningId?.takeIf { it.isNotBlank() }
        ?: meaningEn.split("; ").take(3).joinToString("; ")

internal class QuestionFactory(private val json: Json) {

    fun vocabulary(
        level: JlptLevel,
        target: VocabularyEntity,
        pool: List<VocabularyEntity>,
        kind: QuestionKind,
    ): PracticeQuestion? {
        val others = pool.asSequence().filter { it.id != target.id }
        val meaning = meaningText(target.meaningId, target.meaningEn)
        val example = firstExample(target.examplesJson)
        val readingPart = if (target.reading != target.word) " (${target.reading})" else ""
        val explanation = QuestionExplanation(
            summary = "${target.word}$readingPart — $meaning",
            detail = null,
            exampleJa = example?.ja,
            exampleEn = example?.en,
        )
        val key = "vocab:${target.id}"

        return when (kind) {
            QuestionKind.VOCAB_MEANING ->
                pickOptions(meaning, others.map { meaningText(it.meaningId, it.meaningEn) })?.let { (options, answer) ->
                    build(
                        key, level, kind,
                        display = target.word,
                        sub = target.reading.takeIf { it != target.word }?.let { "($it)" },
                        displayIsJapanese = true,
                        options = options,
                        optionsAreJapanese = false,
                        answer = answer,
                        explanation = explanation,
                    )
                }
            QuestionKind.VOCAB_FROM_MEANING ->
                pickOptions(target.word, others.map { it.word })?.let { (options, answer) ->
                    build(
                        key, level, kind,
                        display = meaning,
                        sub = null,
                        displayIsJapanese = false,
                        options = options,
                        optionsAreJapanese = true,
                        answer = answer,
                        explanation = explanation,
                    )
                }
            QuestionKind.VOCAB_READING ->
                if (target.reading == target.word) {
                    null
                } else {
                    pickOptions(target.reading, others.map { it.reading })?.let { (options, answer) ->
                        build(
                            key, level, kind,
                            display = target.word,
                            sub = null,
                            displayIsJapanese = true,
                            options = options,
                            optionsAreJapanese = true,
                            answer = answer,
                            explanation = explanation,
                        )
                    }
                }
            else -> null
        }
    }

    fun kanji(
        level: JlptLevel,
        target: KanjiEntity,
        pool: List<KanjiEntity>,
        kind: QuestionKind,
    ): PracticeQuestion? {
        val others = pool.asSequence().filter { it.id != target.id }
        val meaning = meaningText(target.meaningId, target.meaningEn)
        if (meaning.isBlank()) return null
        val explanation = QuestionExplanation(
            summary = "${target.character} — $meaning",
            detail = readingLine(target),
            exampleJa = null,
            exampleEn = null,
        )
        val key = "kanji:${target.id}"

        return when (kind) {
            QuestionKind.KANJI_MEANING ->
                pickOptions(
                    meaning,
                    others.map { meaningText(it.meaningId, it.meaningEn) },
                )?.let { (options, answer) ->
                    build(
                        key, level, kind,
                        display = target.character,
                        sub = null,
                        displayIsJapanese = true,
                        options = options,
                        optionsAreJapanese = false,
                        answer = answer,
                        explanation = explanation,
                    )
                }
            QuestionKind.KANJI_READING -> {
                val own = readingsOf(target)
                if (own.isEmpty()) {
                    null
                } else {
                    val correct = own.random()
                    val correctIsKatakana = isKatakana(correct)
                    val candidates = others
                        .flatMap { readingsOf(it).asSequence() }
                        .filter { it !in own && isKatakana(it) == correctIsKatakana }
                    pickOptions(correct, candidates)?.let { (options, answer) ->
                        build(
                            key, level, kind,
                            display = target.character,
                            sub = null,
                            displayIsJapanese = true,
                            options = options,
                            optionsAreJapanese = true,
                            answer = answer,
                            explanation = explanation,
                        )
                    }
                }
            }
            else -> null
        }
    }

    fun grammar(
        level: JlptLevel,
        target: GrammarEntity,
        pool: List<GrammarEntity>,
        kind: QuestionKind,
    ): PracticeQuestion? {
        val others = pool.asSequence().filter { it.id != target.id }
        val meaning = meaningText(target.meaningId, target.meaningEn)
        val example = firstExample(target.examplesJson)
        val explanation = QuestionExplanation(
            summary = "${target.pattern} — $meaning",
            detail = target.formation.takeIf { it.isNotBlank() },
            exampleJa = example?.ja,
            exampleEn = example?.en,
        )
        val key = "grammar:${target.id}"

        return when (kind) {
            QuestionKind.GRAMMAR_MEANING ->
                pickOptions(meaning, others.map { meaningText(it.meaningId, it.meaningEn) })?.let { (options, answer) ->
                    build(
                        key, level, kind,
                        display = target.pattern,
                        sub = null,
                        displayIsJapanese = true,
                        options = options,
                        optionsAreJapanese = false,
                        answer = answer,
                        explanation = explanation,
                    )
                }
            QuestionKind.GRAMMAR_PATTERN ->
                pickOptions(target.pattern, others.map { it.pattern })?.let { (options, answer) ->
                    build(
                        key, level, kind,
                        display = meaning,
                        sub = null,
                        displayIsJapanese = false,
                        options = options,
                        optionsAreJapanese = true,
                        answer = answer,
                        explanation = explanation,
                    )
                }
            else -> null
        }
    }

    private fun build(
        itemKey: String,
        level: JlptLevel,
        kind: QuestionKind,
        display: String,
        sub: String?,
        displayIsJapanese: Boolean,
        options: List<String>,
        optionsAreJapanese: Boolean,
        answer: Int,
        explanation: QuestionExplanation,
    ) = PracticeQuestion(
        id = "${kind.name}:$itemKey",
        itemKey = itemKey,
        level = level,
        kind = kind,
        display = display,
        displaySub = sub,
        displayIsJapanese = displayIsJapanese,
        options = options,
        optionsAreJapanese = optionsAreJapanese,
        answerIndex = answer,
        explanation = explanation,
    )

    /** Mengambil 3 pengecoh berbeda dari [candidates]; null jika kurang dari 3. */
    private fun pickOptions(correct: String, candidates: Sequence<String>): Pair<List<String>, Int>? {
        val distractors = candidates
            .filter { it.isNotBlank() && it != correct }
            .distinct()
            .take(3)
            .toList()
        if (distractors.size < 3) return null
        val options = (distractors + correct).shuffled()
        return options to options.indexOf(correct)
    }

    private fun firstExample(raw: String): ExampleDto? =
        try {
            json.decodeFromString(ListSerializer(ExampleDto.serializer()), raw).firstOrNull()
        } catch (e: Exception) {
            null
        }

    private fun isKatakana(text: String): Boolean = text.firstOrNull()?.let { it in 'ァ'..'ヶ' } ?: false

    companion object {
        /** Bacaan on dan kun yang utuh (bentuk berimbuhan dengan tanda "-" dibuang). */
        fun readingsOf(entity: KanjiEntity): List<String> =
            (entity.onyomi.split("、") + entity.kunyomi.split("、").map { it.replace(".", "") })
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.contains("-") }
                .distinct()

        fun readingLine(entity: KanjiEntity): String {
            val on = entity.onyomi.split("、").filter { it.isNotBlank() }
            val kun = entity.kunyomi.split("、").filter { it.isNotBlank() }.map { it.replace(".", "") }
            return "On: ${on.joinToString("、").ifBlank { "-" }} · Kun: ${kun.joinToString("、").ifBlank { "-" }}"
        }
    }
}