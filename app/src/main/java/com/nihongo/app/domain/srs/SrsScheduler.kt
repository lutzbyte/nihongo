package com.nihongo.app.domain.srs

import kotlin.math.max
import kotlin.math.roundToInt

enum class SrsGrade { AGAIN, HARD, GOOD, EASY }

data class SrsState(
    val ease: Double,
    val intervalDays: Int,
    val repetitions: Int,
    val lapses: Int,
    val dueEpochDay: Long,
)

object SrsScheduler {
    const val INITIAL_EASE = 2.5
    const val MIN_EASE = 1.3

    /** Kartu dengan interval minimal segini dianggap "dikuasai". */
    const val MATURE_INTERVAL_DAYS = 21

    fun initial(today: Long): SrsState = SrsState(INITIAL_EASE, 0, 0, 0, today)

    fun next(state: SrsState, grade: SrsGrade, today: Long): SrsState =
        when (grade) {
            SrsGrade.AGAIN -> SrsState(
                ease = max(MIN_EASE, state.ease - 0.2),
                intervalDays = 0,
                repetitions = 0,
                lapses = state.lapses + 1,
                dueEpochDay = today,
            )
            SrsGrade.HARD -> {
                val days = if (state.repetitions == 0) {
                    1
                } else {
                    max(state.intervalDays + 1, (state.intervalDays * 1.2).roundToInt())
                }
                SrsState(
                    ease = max(MIN_EASE, state.ease - 0.15),
                    intervalDays = days,
                    repetitions = state.repetitions + 1,
                    lapses = state.lapses,
                    dueEpochDay = today + days,
                )
            }
            SrsGrade.GOOD -> {
                val days = when (state.repetitions) {
                    0 -> 1
                    1 -> 3
                    else -> max(state.intervalDays + 1, (state.intervalDays * state.ease).roundToInt())
                }
                SrsState(
                    ease = state.ease,
                    intervalDays = days,
                    repetitions = state.repetitions + 1,
                    lapses = state.lapses,
                    dueEpochDay = today + days,
                )
            }
            SrsGrade.EASY -> {
                val days = when (state.repetitions) {
                    0 -> 3
                    1 -> 6
                    else -> max(state.intervalDays + 1, (state.intervalDays * state.ease * 1.3).roundToInt())
                }
                SrsState(
                    ease = state.ease + 0.15,
                    intervalDays = days,
                    repetitions = state.repetitions + 1,
                    lapses = state.lapses,
                    dueEpochDay = today + days,
                )
            }
        }
}