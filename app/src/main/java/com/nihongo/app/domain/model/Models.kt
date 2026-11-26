package com.nihongo.app.domain.model

enum class JlptLevel { N5, N4, N3, N2, N1 }

data class VocabularyItem(
    val id: String,
    val level: JlptLevel,
    val word: String,
    val reading: String,
    val meaning: String,
)

data class VocabularyExample(
    val japanese: String,
    val furigana: String?,
    val english: String?,
)

data class VocabularyDetail(
    val id: String,
    val level: JlptLevel,
    val word: String,
    val reading: String,
    val romaji: String,
    val meaning: String,
    val meaningTranslated: Boolean,
    val partsOfSpeech: List<String>,
    val examples: List<VocabularyExample>,
)

data class KanjiExample(
    val japanese: String,
    val romaji: String,
    val meaning: String,
)

data class KanjiItem(
    val id: String,
    val level: JlptLevel,
    val character: String,
    val readingKana: String,
    val readingRomaji: String,
    val onyomi: String,
    val kunyomi: String,
    val strokes: Int,
    val meaning: String,
    val example: KanjiExample? = null,
)

data class GrammarExample(
    val japanese: String,
    val english: String,
)

data class GrammarItem(
    val id: String,
    val level: JlptLevel,
    val pattern: String,
    val reading: String?,
    val romaji: String?,
    val meaning: String,
    val meaningTranslated: Boolean,
    val formation: String,
    val notes: String?,
    val tags: List<String>,
    val examples: List<GrammarExample>,
)

data class QuizQuestion(
    val id: String,
    val level: JlptLevel,
    val word: String,
    val reading: String,
    val options: List<String>,
    val answerIndex: Int,
)

data class DayMinutes(
    val label: String,
    val minutes: Int,
)

data class ProgressSummary(
    val totalMinutes: Int = 0,
    val changePercent: Int? = null,
    val dailyMinutes: List<DayMinutes> = emptyList(),
    val vocabularyLearned: Int = 0,
    val kanjiMastered: Int = 0,
    val grammarProgress: Int = 0,
    val streakDays: Int = 0,
    val todayMinutes: Int = 0,
)

data class VocabularyPage(
    val items: List<VocabularyItem>,
    val total: Int,
)

enum class KanaScript { HIRAGANA, KATAKANA }

enum class KanaGroup { BASIC, DAKUTEN, HANDAKUTEN, YOON }

data class KanaItem(
    val id: String,
    val script: KanaScript,
    val kana: String,
    val romaji: String,
    val group: KanaGroup,
    val row: String,
)

data class LessonRow(
    val ja: String,
    val reading: String?,
    val romaji: String?,
    val meaning: String,
)

data class LessonSection(
    val heading: String,
    val body: String,
    val rows: List<LessonRow>,
)

data class LessonSummary(
    val id: String,
    val title: String,
    val summary: String,
)

data class Lesson(
    val id: String,
    val title: String,
    val summary: String,
    val sections: List<LessonSection>,
)

data class BookmarkedItems(
    val vocabulary: List<VocabularyItem>,
    val kanji: List<KanjiItem>,
    val grammar: List<GrammarItem>,
) {
    val isEmpty: Boolean get() = vocabulary.isEmpty() && kanji.isEmpty() && grammar.isEmpty()
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class TextSize(val scale: Float) {
    SMALL(0.9f),
    NORMAL(1f),
    LARGE(1.15f),
}

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val textSize: TextSize = TextSize.NORMAL,
    val dailyGoalMinutes: Int = 20,
    val bookmarks: Set<String> = emptySet(),
)