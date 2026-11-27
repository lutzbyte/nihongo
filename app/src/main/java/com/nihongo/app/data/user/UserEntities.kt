package com.nihongo.app.data.user

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Satu kartu SRS. itemKey berformat "vocab:ID", "kanji:ID", atau "grammar:ID". */
@Entity(
    tableName = "srs_card",
    indices = [Index("dueEpochDay"), Index("itemType", "level")],
)
data class SrsCardEntity(
    @PrimaryKey val itemKey: String,
    val itemType: String,
    val level: String,
    val ease: Double,
    val intervalDays: Int,
    val repetitions: Int,
    val lapses: Int,
    val dueEpochDay: Long,
    val lastReviewedEpochMs: Long,
    val createdEpochMs: Long,
)

/**
 * Satu sesi belajar selesai. kind: FLASHCARD, REVIEW, QUIZ, EXAM.
 * detail (hanya EXAM): skor per bagian, misalnya "MOJI_GOI=12/20;BUNPO=6/10".
 */
@Entity(
    tableName = "study_session",
    indices = [Index("epochDay")],
)
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: String,
    val level: String?,
    val section: String?,
    val startedAtEpochMs: Long,
    val epochDay: Long,
    val durationSeconds: Int,
    val itemCount: Int,
    val correctCount: Int,
    val detail: String?,
)

data class DaySeconds(
    val epochDay: Long,
    val seconds: Int,
)