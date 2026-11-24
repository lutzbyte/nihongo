package com.nihongo.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

internal const val VOCAB_FILTER =
    "level = :level AND (:query = '' " +
        "OR word LIKE '%' || :query || '%' ESCAPE '\\' " +
        "OR reading LIKE '%' || :query || '%' ESCAPE '\\' " +
        "OR romaji LIKE '%' || :query || '%' ESCAPE '\\' " +
        "OR meaningEn LIKE '%' || :query || '%' ESCAPE '\\' " +
        "OR meaningId LIKE '%' || :query || '%' ESCAPE '\\')"

@Dao
abstract class CurriculumDao {

    @Query("SELECT * FROM dataset_state WHERE id = 1")
    abstract suspend fun getState(): DatasetStateEntity?

    @Query("SELECT COUNT(*) FROM vocabulary")
    abstract suspend fun countVocabulary(): Int

    @Query("SELECT COUNT(*) FROM kanji")
    abstract suspend fun countKanji(): Int

    @Query("SELECT COUNT(*) FROM grammar")
    abstract suspend fun countGrammar(): Int

    @Query("SELECT * FROM vocabulary WHERE $VOCAB_FILTER ORDER BY reading, word LIMIT :limit OFFSET :offset")
    abstract suspend fun vocabularyPage(level: String, query: String, limit: Int, offset: Int): List<VocabularyEntity>

    @Query("SELECT COUNT(*) FROM vocabulary WHERE $VOCAB_FILTER")
    abstract suspend fun vocabularyCount(level: String, query: String): Int

    @Query("SELECT * FROM vocabulary WHERE level = :level ORDER BY RANDOM() LIMIT :limit")
    abstract suspend fun randomVocabulary(level: String, limit: Int): List<VocabularyEntity>

    @Query("SELECT * FROM vocabulary WHERE word IN (:words)")
    abstract suspend fun vocabularyByWords(words: List<String>): List<VocabularyEntity>

    @Query("SELECT * FROM kanji WHERE level = :level ORDER BY freq IS NULL, freq, character")
    abstract suspend fun kanjiByLevel(level: String): List<KanjiEntity>

    @Query("SELECT * FROM kanji WHERE id = :id LIMIT 1")
    abstract suspend fun kanjiById(id: String): KanjiEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertVocabulary(items: List<VocabularyEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertKanji(items: List<KanjiEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertGrammar(items: List<GrammarEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertState(state: DatasetStateEntity)

    @Query("DELETE FROM vocabulary")
    abstract suspend fun clearVocabulary()

    @Query("DELETE FROM kanji")
    abstract suspend fun clearKanji()

    @Query("DELETE FROM grammar")
    abstract suspend fun clearGrammar()

    /**
     * Mengganti seluruh materi dalam satu transaksi. Jika proses terhenti di tengah jalan,
     * SQLite membatalkan transaksi sehingga data lama tetap utuh.
     */
    @Transaction
    open suspend fun replaceAll(
        vocabulary: List<VocabularyEntity>,
        kanji: List<KanjiEntity>,
        grammar: List<GrammarEntity>,
        state: DatasetStateEntity,
    ) {
        clearVocabulary()
        clearKanji()
        clearGrammar()
        vocabulary.chunked(CHUNK).forEach { insertVocabulary(it) }
        kanji.chunked(CHUNK).forEach { insertKanji(it) }
        grammar.chunked(CHUNK).forEach { insertGrammar(it) }
        upsertState(state)
    }

    private companion object {
        const val CHUNK = 500
    }
}