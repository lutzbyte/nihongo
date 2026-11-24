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
)