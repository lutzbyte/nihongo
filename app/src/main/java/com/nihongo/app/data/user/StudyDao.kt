package com.nihongo.app.data.user

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCard(card: SrsCardEntity)

    @Query("SELECT * FROM srs_card WHERE itemKey IN (:keys)")
    suspend fun cardsByKeys(keys: List<String>): List<SrsCardEntity>

    @Query("SELECT itemKey FROM srs_card WHERE itemKey IN (:keys)")
    suspend fun existingKeys(keys: List<String>): List<String>

    @Query(
        "SELECT * FROM srs_card WHERE dueEpochDay <= :today AND itemType IN (:types) " +
            "AND (:level = '' OR level = :level) " +
            "ORDER BY dueEpochDay, lastReviewedEpochMs LIMIT :limit",
    )
    suspend fun dueCards(today: Long, types: List<String>, level: String, limit: Int): List<SrsCardEntity>

    @Query("SELECT COUNT(*) FROM srs_card WHERE dueEpochDay <= :today")
    fun observeDueCount(today: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM study_session")
    fun observeSessionCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM srs_card WHERE itemType = :type")
    suspend fun countCards(type: String): Int

    @Query("SELECT COUNT(*) FROM srs_card WHERE itemType = :type AND intervalDays >= :minInterval")
    suspend fun countMatureCards(type: String, minInterval: Int): Int

    @Insert
    suspend fun insertSession(session: StudySessionEntity): Long

    @Query(
        "SELECT epochDay, SUM(durationSeconds) AS seconds FROM study_session " +
            "WHERE epochDay BETWEEN :from AND :to GROUP BY epochDay",
    )
    suspend fun secondsPerDay(from: Long, to: Long): List<DaySeconds>

    @Query("SELECT SUM(durationSeconds) FROM study_session WHERE epochDay BETWEEN :from AND :to")
    suspend fun totalSeconds(from: Long, to: Long): Int?

    @Query("SELECT DISTINCT epochDay FROM study_session ORDER BY epochDay DESC")
    suspend fun studyDays(): List<Long>
}