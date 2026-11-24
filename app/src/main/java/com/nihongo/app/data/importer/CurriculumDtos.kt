package com.nihongo.app.data.importer

import kotlinx.serialization.Serializable

@Serializable
data class ManifestDto(
    val schemaVersion: Int,
    val datasetVersion: Int,
    val generatedAt: String = "",
    val source: SourceDto,
    val counts: CountsDto,
    val files: List<ManifestFileDto>,
)

@Serializable
data class SourceDto(
    val name: String,
    val version: String = "",
    val commit: String = "",
    val license: String = "",
    val url: String = "",
)

@Serializable
data class CountsDto(
    val vocabulary: Int,
    val kanji: Int,
    val grammar: Int,
)

@Serializable
data class ManifestFileDto(
    val path: String,
    val type: String,
    val level: String,
    val count: Int,
    val sha256: String,
)

@Serializable
data class ExampleDto(
    val ja: String,
    val en: String? = null,
    val furigana: String? = null,
)

@Serializable
data class VocabItemDto(
    val id: String,
    val word: String,
    val reading: String,
    val romaji: String = "",
    val level: String,
    val meaningsEn: List<String>,
    val meaningId: String? = null,
    val translationStatus: String = "none",
    val pos: List<String> = emptyList(),
    val jmdictId: Int? = null,
    val examples: List<ExampleDto> = emptyList(),
)

@Serializable
data class VocabFileDto(val items: List<VocabItemDto>)

@Serializable
data class KanjiItemDto(
    val id: String,
    val character: String,
    val level: String,
    val strokes: Int,
    val grade: Int? = null,
    val freq: Int? = null,
    val radical: String? = null,
    val onyomi: List<String> = emptyList(),
    val kunyomi: List<String> = emptyList(),
    val meaningsEn: List<String> = emptyList(),
    val meaningId: String? = null,
    val translationStatus: String = "none",
    val words: List<String> = emptyList(),
)

@Serializable
data class KanjiFileDto(val items: List<KanjiItemDto>)

@Serializable
data class GrammarItemDto(
    val id: String,
    val pattern: String,
    val reading: String? = null,
    val romaji: String? = null,
    val level: String,
    val meaningEn: String,
    val meaningId: String? = null,
    val translationStatus: String = "none",
    val formation: String = "",
    val notes: String? = null,
    val tags: List<String> = emptyList(),
    val examples: List<ExampleDto> = emptyList(),
)

@Serializable
data class GrammarFileDto(val items: List<GrammarItemDto>)