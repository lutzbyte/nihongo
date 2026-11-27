package com.nihongo.app.data.db

import androidx.room.Dao
import androidx.room.Query

@Dao
interface PracticeDao {

    @Query("SELECT id FROM vocabulary WHERE level = :level ORDER BY RANDOM()")
    suspend fun vocabularyIdsRandom(level: String): List<String>

    @Query("SELECT id FROM kanji WHERE level = :level ORDER BY freq IS NULL, freq, character")
    suspend fun kanjiIdsOrdered(level: String): List<String>

    @Query("SELECT id FROM grammar WHERE level = :level ORDER BY sortOrder")
    suspend fun grammarIdsOrdered(level: String): List<String>

    @Query("SELECT * FROM kanji WHERE level = :level ORDER BY RANDOM() LIMIT :limit")
    suspend fun randomKanji(level: String, limit: Int): List<KanjiEntity>

    @Query("SELECT * FROM grammar WHERE level = :level ORDER BY RANDOM() LIMIT :limit")
    suspend fun randomGrammar(level: String, limit: Int): List<GrammarEntity>
}