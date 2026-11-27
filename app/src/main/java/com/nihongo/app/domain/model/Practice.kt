package com.nihongo.app.domain.model

import com.nihongo.app.domain.srs.SrsState

enum class ItemType(val keyPrefix: String) {
    VOCABULARY("vocab"),
    KANJI("kanji"),
    GRAMMAR("grammar"),
}

enum class StudyScope(val types: List<ItemType>) {
    VOCABULARY(listOf(ItemType.VOCABULARY)),
    KANJI(listOf(ItemType.KANJI)),
    GRAMMAR(listOf(ItemType.GRAMMAR)),
    ALL(listOf(ItemType.VOCABULARY, ItemType.KANJI, ItemType.GRAMMAR)),
}

enum class SessionKind { FLASHCARD, REVIEW, QUIZ, EXAM }

/** MOJI_GOI = 文字・語彙 (kanji dan kosakata), BUNPO = 文法 (tata bahasa). */
enum class PracticeSection { MOJI_GOI, BUNPO, MIXED }

enum class QuestionKind(val section: PracticeSection) {
    VOCAB_MEANING(PracticeSection.MOJI_GOI),
    VOCAB_FROM_MEANING(PracticeSection.MOJI_GOI),
    VOCAB_READING(PracticeSection.MOJI_GOI),
    KANJI_MEANING(PracticeSection.MOJI_GOI),
    KANJI_READING(PracticeSection.MOJI_GOI),
    GRAMMAR_MEANING(PracticeSection.BUNPO),
    GRAMMAR_PATTERN(PracticeSection.BUNPO),
}

data class QuestionExplanation(
    val summary: String,
    val detail: String?,
    val exampleJa: String?,
    val exampleEn: String?,
)

data class PracticeQuestion(
    val id: String,
    val itemKey: String,
    val level: JlptLevel,
    val kind: QuestionKind,
    val display: String,
    val displaySub: String?,
    val displayIsJapanese: Boolean,
    val options: List<String>,
    val optionsAreJapanese: Boolean,
    val answerIndex: Int,
    val explanation: QuestionExplanation,
) {
    val section: PracticeSection get() = kind.section
}

data class FlashCard(
    val itemKey: String,
    val type: ItemType,
    val level: JlptLevel,
    val front: String,
    val reading: String?,
    val meaning: String,
    val extra: String?,
    val isNew: Boolean,
    val state: SrsState,
)