package com.nihongo.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class JlptLevel { N5, N4, N3, N2, N1 }

@Serializable
data class VocabularyItem(
    val id: String,
    val level: JlptLevel,
    val word: String,
    val reading: String,
    val meaning: String,
)

@Serializable
data class KanjiExample(
    val japanese: String,
    val romaji: String,
    val meaning: String,
)

@Serializable
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

@Serializable
data class QuizQuestion(
    val id: String,
    val level: JlptLevel,
    val word: String,
    val reading: String,
    val options: List<String>,
    val answerIndex: Int,
)

@Serializable
data class DayMinutes(
    val label: String,
    val minutes: Int,
)

@Serializable
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