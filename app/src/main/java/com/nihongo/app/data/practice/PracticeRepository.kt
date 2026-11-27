package com.nihongo.app.data.practice

import com.nihongo.app.domain.model.FlashCard
import com.nihongo.app.domain.model.JlptLevel
import com.nihongo.app.domain.model.PracticeQuestion
import com.nihongo.app.domain.model.PracticeSection
import com.nihongo.app.domain.model.SessionKind
import com.nihongo.app.domain.model.StudyScope
import com.nihongo.app.domain.srs.SrsGrade
import com.nihongo.app.domain.srs.SrsState
import kotlinx.coroutines.flow.Flow

interface PracticeRepository {

    /** Jumlah kartu yang jatuh tempo hari ini (semua level), diperbarui otomatis. */
    fun observeDueCount(): Flow<Int>

    /**
     * FLASHCARD: kartu jatuh tempo pada [scope] dan [level], lalu kartu baru.
     * REVIEW: hanya kartu jatuh tempo; [level] null berarti semua level.
     */
    suspend fun buildDeck(kind: SessionKind, scope: StudyScope, level: JlptLevel?): List<FlashCard>

    /** Menyimpan nilai kartu dan mengembalikan jadwal baru. */
    suspend fun gradeCard(card: FlashCard, grade: SrsGrade): SrsState

    suspend fun recordFlashcardSession(
        kind: SessionKind,
        level: JlptLevel?,
        startedAtEpochMs: Long,
        durationSeconds: Int,
        reviewed: Int,
        againCount: Int,
    )

    suspend fun buildQuestions(level: JlptLevel, section: PracticeSection, count: Int): List<PracticeQuestion>

    /** Simulasi: soal Kosakata/Kanji lalu soal Tata Bahasa, urutan per bagian dipertahankan. */
    suspend fun buildExam(level: JlptLevel): List<PracticeQuestion>

    /**
     * Menyimpan hasil. Jawaban salah menjadikan item jatuh tempo hari ini.
     * Soal tanpa jawaban (waktu habis) dihitung salah tetapi tidak mengubah jadwal.
     */
    suspend fun recordQuizResult(
        kind: SessionKind,
        section: PracticeSection,
        level: JlptLevel,
        questions: List<PracticeQuestion>,
        answers: Map<Int, Int>,
        startedAtEpochMs: Long,
        durationSeconds: Int,
    )
}