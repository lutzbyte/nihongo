package com.nihongo.app.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "vocabulary",
    indices = [Index("level"), Index("word"), Index("reading")],
)
data class VocabularyEntity(
    @PrimaryKey val id: String,
    val level: String,
    val word: String,
    val reading: String,
    val romaji: String,
    val meaningEn: String,
    val meaningId: String?,
    val translationStatus: String,
    val pos: String,
    val jmdictId: Int?,
    val examplesJson: String,
)

@Entity(
    tableName = "kanji",
    indices = [Index("level")],
)
data class KanjiEntity(
    @PrimaryKey val id: String,
    val level: String,
    val character: String,
    val strokes: Int,
    val grade: Int?,
    val freq: Int?,
    val radical: String?,
    val onyomi: String,
    val kunyomi: String,
    val meaningEn: String,
    val meaningId: String?,
    val translationStatus: String,
    val words: String,
)

@Entity(
    tableName = "grammar",
    indices = [Index("level")],
)
data class GrammarEntity(
    @PrimaryKey val id: String,
    val level: String,
    val sortOrder: Int,
    val pattern: String,
    val reading: String?,
    val romaji: String?,
    val meaningEn: String,
    val meaningId: String?,
    val translationStatus: String,
    val formation: String,
    val notes: String?,
    val tags: String,
    val examplesJson: String,
)

@Entity(
    tableName = "kana",
    indices = [Index("script")],
)
data class KanaEntity(
    @PrimaryKey val id: String,
    val script: String,
    val kana: String,
    val romaji: String,
    val kanaGroup: String,
    val kanaRow: String,
    val sortOrder: Int,
)

@Entity(tableName = "lesson")
data class LessonEntity(
    @PrimaryKey val id: String,
    val category: String,
    val sortOrder: Int,
    val title: String,
    val summary: String,
    val sectionsJson: String,
)

@Entity(tableName = "dataset_state")
data class DatasetStateEntity(
    @PrimaryKey val id: Int = 1,
    val datasetVersion: Int,
    val sourceName: String,
    val sourceVersion: String,
    val sourceCommit: String,
    val license: String,
    val generatedAt: String,
    val importedAtEpochMs: Long,
    val vocabularyCount: Int,
    val kanjiCount: Int,
    val grammarCount: Int,
    val kanaCount: Int,
    val lessonCount: Int,
)