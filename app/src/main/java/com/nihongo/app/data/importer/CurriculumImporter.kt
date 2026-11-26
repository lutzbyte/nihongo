package com.nihongo.app.data.importer

import android.content.Context
import com.nihongo.app.data.db.DatasetStateEntity
import com.nihongo.app.data.db.GrammarEntity
import com.nihongo.app.data.db.KanaEntity
import com.nihongo.app.data.db.KanjiEntity
import com.nihongo.app.data.db.LessonEntity
import com.nihongo.app.data.db.NihongoDatabase
import com.nihongo.app.data.db.VocabularyEntity
import com.nihongo.app.domain.model.JlptLevel
import java.io.FileNotFoundException
import java.security.MessageDigest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class ImportException(message: String) : Exception(message)

sealed interface ImportResult {
    data class UpToDate(val datasetVersion: Int) : ImportResult

    data class Imported(
        val datasetVersion: Int,
        val vocabulary: Int,
        val kanji: Int,
        val grammar: Int,
        val kana: Int,
        val lessons: Int,
    ) : ImportResult

    data class Failed(val message: String, val hasExistingData: Boolean) : ImportResult
}

private val json = Json { ignoreUnknownKeys = true }

private const val ASSET_DIR = "curriculum"
private const val SUPPORTED_MANIFEST_SCHEMA = 1
private const val TYPE_VOCABULARY = "vocabulary"
private const val TYPE_KANJI = "kanji"
private const val TYPE_GRAMMAR = "grammar"
private const val TYPE_KANA = "kana"
private const val TYPE_LESSON = "lesson"
private val ALLOWED_STATUSES = setOf("none", "draft", "reviewed", "verified")
private val KANA_SCRIPTS = setOf("hiragana", "katakana")
private val KANA_GROUPS = setOf("basic", "dakuten", "handakuten", "yoon")

class CurriculumImporter(
    private val context: Context,
    private val database: NihongoDatabase,
) {
    private val dao get() = database.curriculumDao()

    suspend fun run(onProgress: (Float) -> Unit = {}): ImportResult = withContext(Dispatchers.IO) {
        try {
            execute(onProgress)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val message = if (e is ImportException) {
                e.message.orEmpty()
            } else {
                "Impor gagal: ${e.message ?: e::class.simpleName}"
            }
            ImportResult.Failed(message, hasExistingData())
        }
    }

    private suspend fun hasExistingData(): Boolean =
        try {
            dao.getState() != null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }

    private suspend fun execute(onProgress: (Float) -> Unit): ImportResult {
        val manifest = readManifest()
        onProgress(0.02f)

        val state = dao.getState()
        if (state != null &&
            state.datasetVersion >= manifest.datasetVersion &&
            previousImportIsIntact(state)
        ) {
            onProgress(1f)
            return ImportResult.UpToDate(state.datasetVersion)
        }

        val vocabulary = ArrayList<VocabularyEntity>()
        val kanji = ArrayList<KanjiEntity>()
        val grammar = ArrayList<GrammarEntity>()
        val kana = ArrayList<KanaEntity>()
        val lessons = ArrayList<LessonEntity>()

        manifest.files.forEachIndexed { index, file ->
            val bytes = readAsset(file.path)
            val checksum = sha256(bytes)
            if (!checksum.equals(file.sha256, ignoreCase = true)) {
                throw ImportException("Checksum tidak cocok untuk ${file.path}")
            }
            val text = bytes.decodeToString()
            when (file.type) {
                TYPE_VOCABULARY -> {
                    val level = parseLevel(file.level, file.path)
                    val items = json.decodeFromString<VocabFileDto>(text).items
                    checkCount(file, items.size)
                    items.forEach { vocabulary += it.toEntity(level, file.path) }
                }
                TYPE_KANJI -> {
                    val level = parseLevel(file.level, file.path)
                    val items = json.decodeFromString<KanjiFileDto>(text).items
                    checkCount(file, items.size)
                    items.forEach { kanji += it.toEntity(level, file.path) }
                }
                TYPE_GRAMMAR -> {
                    val level = parseLevel(file.level, file.path)
                    val items = json.decodeFromString<GrammarFileDto>(text).items
                    checkCount(file, items.size)
                    items.forEachIndexed { order, item -> grammar += item.toEntity(level, file.path, order) }
                }
                TYPE_KANA -> {
                    val items = json.decodeFromString<KanaFileDto>(text).items
                    checkCount(file, items.size)
                    items.forEach { kana += it.toEntity(file.path) }
                }
                TYPE_LESSON -> {
                    val items = json.decodeFromString<LessonFileDto>(text).items
                    checkCount(file, items.size)
                    items.forEach { lessons += it.toEntity(file.path) }
                }
                else -> throw ImportException("Tipe file tidak dikenal: ${file.type} (${file.path})")
            }
            onProgress(0.05f + 0.85f * (index + 1) / manifest.files.size)
        }

        ensureUnique(vocabulary.map { it.id }, "kosakata")
        ensureUnique(kanji.map { it.id }, "kanji")
        ensureUnique(grammar.map { it.id }, "grammar")
        ensureUnique(kana.map { it.id }, "kana")
        ensureUnique(lessons.map { it.id }, "pelajaran")

        if (vocabulary.size != manifest.counts.vocabulary ||
            kanji.size != manifest.counts.kanji ||
            grammar.size != manifest.counts.grammar ||
            kana.size != manifest.counts.kana ||
            lessons.size != manifest.counts.lessons
        ) {
            throw ImportException("Jumlah data tidak sesuai dengan manifest")
        }

        dao.replaceAll(
            vocabulary = vocabulary,
            kanji = kanji,
            grammar = grammar,
            kana = kana,
            lessons = lessons,
            state = DatasetStateEntity(
                datasetVersion = manifest.datasetVersion,
                sourceName = manifest.source.name,
                sourceVersion = manifest.source.version,
                sourceCommit = manifest.source.commit,
                license = manifest.source.license,
                generatedAt = manifest.generatedAt,
                importedAtEpochMs = System.currentTimeMillis(),
                vocabularyCount = vocabulary.size,
                kanjiCount = kanji.size,
                grammarCount = grammar.size,
                kanaCount = kana.size,
                lessonCount = lessons.size,
            ),
        )
        onProgress(1f)
        return ImportResult.Imported(
            datasetVersion = manifest.datasetVersion,
            vocabulary = vocabulary.size,
            kanji = kanji.size,
            grammar = grammar.size,
            kana = kana.size,
            lessons = lessons.size,
        )
    }

    private suspend fun previousImportIsIntact(state: DatasetStateEntity): Boolean =
        dao.countVocabulary() == state.vocabularyCount &&
            dao.countKanji() == state.kanjiCount &&
            dao.countGrammar() == state.grammarCount &&
            dao.countKana() == state.kanaCount &&
            dao.countLessons() == state.lessonCount

    private fun readManifest(): ManifestDto {
        val bytes = try {
            readAsset("manifest.json")
        } catch (e: FileNotFoundException) {
            throw ImportException(
                "manifest.json tidak ditemukan. Jalankan workflow Build Curriculum Dataset lalu build ulang.",
            )
        }
        val manifest = try {
            json.decodeFromString<ManifestDto>(bytes.decodeToString())
        } catch (e: Exception) {
            throw ImportException("manifest.json tidak valid: ${e.message}")
        }
        if (manifest.schemaVersion != SUPPORTED_MANIFEST_SCHEMA) {
            throw ImportException(
                "Versi skema manifest ${manifest.schemaVersion} tidak didukung (diharapkan $SUPPORTED_MANIFEST_SCHEMA)",
            )
        }
        if (manifest.files.isEmpty()) throw ImportException("Manifest tidak memuat file apa pun")
        return manifest
    }

    private fun readAsset(relativePath: String): ByteArray =
        context.assets.open("$ASSET_DIR/$relativePath").use { it.readBytes() }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }

    private fun parseLevel(value: String, path: String): String =
        JlptLevel.entries.firstOrNull { it.name == value }?.name
            ?: throw ImportException("Level tidak valid '$value' pada $path")

    private fun checkCount(file: ManifestFileDto, actual: Int) {
        if (actual != file.count) {
            throw ImportException("Jumlah entri ${file.path} ($actual) tidak sama dengan manifest (${file.count})")
        }
    }

    private fun ensureUnique(ids: List<String>, label: String) {
        val duplicates = ids.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        if (duplicates.isNotEmpty()) {
            throw ImportException("ID $label duplikat: ${duplicates.take(5).joinToString()}")
        }
    }
}

private fun requireText(value: String, field: String, path: String, id: String) {
    if (value.isBlank()) throw ImportException("Field '$field' kosong untuk '$id' pada $path")
}

private fun requireStatus(status: String, path: String, id: String) {
    if (status !in ALLOWED_STATUSES) {
        throw ImportException("Status terjemahan '$status' tidak valid untuk '$id' pada $path")
    }
}

private fun requireLevel(itemLevel: String, fileLevel: String, path: String, id: String) {
    if (itemLevel != fileLevel) {
        throw ImportException("Level '$itemLevel' pada '$id' tidak sesuai file $path ($fileLevel)")
    }
}

private fun encodeExamples(examples: List<ExampleDto>): String =
    json.encodeToString(ListSerializer(ExampleDto.serializer()), examples)

private fun VocabItemDto.toEntity(fileLevel: String, path: String): VocabularyEntity {
    requireText(id, "id", path, id)
    requireText(word, "word", path, id)
    requireText(reading, "reading", path, id)
    requireLevel(level, fileLevel, path, id)
    requireStatus(translationStatus, path, id)
    if (meaningsEn.isEmpty()) throw ImportException("Arti kosong untuk '$id' pada $path")
    return VocabularyEntity(
        id = id,
        level = fileLevel,
        word = word,
        reading = reading,
        romaji = romaji,
        meaningEn = meaningsEn.joinToString("; "),
        meaningId = meaningId?.takeIf { it.isNotBlank() },
        translationStatus = translationStatus,
        pos = pos.joinToString(","),
        jmdictId = jmdictId,
        examplesJson = encodeExamples(examples),
    )
}

private fun KanjiItemDto.toEntity(fileLevel: String, path: String): KanjiEntity {
    requireText(id, "id", path, id)
    requireText(character, "character", path, id)
    requireLevel(level, fileLevel, path, id)
    requireStatus(translationStatus, path, id)
    return KanjiEntity(
        id = id,
        level = fileLevel,
        character = character,
        strokes = strokes,
        grade = grade,
        freq = freq,
        radical = radical,
        onyomi = onyomi.joinToString("、"),
        kunyomi = kunyomi.joinToString("、"),
        meaningEn = meaningsEn.joinToString("; "),
        meaningId = meaningId?.takeIf { it.isNotBlank() },
        translationStatus = translationStatus,
        words = words.joinToString(","),
    )
}

private fun GrammarItemDto.toEntity(fileLevel: String, path: String, order: Int): GrammarEntity {
    requireText(id, "id", path, id)
    requireText(pattern, "pattern", path, id)
    requireText(meaningEn, "meaningEn", path, id)
    requireLevel(level, fileLevel, path, id)
    requireStatus(translationStatus, path, id)
    return GrammarEntity(
        id = id,
        level = fileLevel,
        sortOrder = order,
        pattern = pattern,
        reading = reading,
        romaji = romaji,
        meaningEn = meaningEn,
        meaningId = meaningId?.takeIf { it.isNotBlank() },
        translationStatus = translationStatus,
        formation = formation,
        notes = notes,
        tags = tags.joinToString(","),
        examplesJson = encodeExamples(examples),
    )
}

private fun KanaItemDto.toEntity(path: String): KanaEntity {
    requireText(id, "id", path, id)
    requireText(kana, "kana", path, id)
    requireText(romaji, "romaji", path, id)
    if (script !in KANA_SCRIPTS) throw ImportException("Script kana '$script' tidak valid untuk '$id' pada $path")
    if (group !in KANA_GROUPS) throw ImportException("Grup kana '$group' tidak valid untuk '$id' pada $path")
    return KanaEntity(
        id = id,
        script = script,
        kana = kana,
        romaji = romaji,
        kanaGroup = group,
        kanaRow = row,
        sortOrder = order,
    )
}

private fun LessonItemDto.toEntity(path: String): LessonEntity {
    requireText(id, "id", path, id)
    requireText(title, "title", path, id)
    if (sections.isEmpty()) throw ImportException("Pelajaran '$id' tidak punya section pada $path")
    return LessonEntity(
        id = id,
        category = category,
        sortOrder = order,
        title = title,
        summary = summary,
        sectionsJson = json.encodeToString(ListSerializer(LessonSectionDto.serializer()), sections),
    )
}