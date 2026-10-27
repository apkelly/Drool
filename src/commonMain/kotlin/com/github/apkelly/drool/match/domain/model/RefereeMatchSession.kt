package com.github.apkelly.drool.match.domain.model

import com.github.apkelly.drool.common.domain.model.MatchHalf
import kotlinx.serialization.Serializable

@Serializable
enum class RefereeMatchPreset(
    val halfDurationMinutes: Int,
    val requiresPlayerSelection: Boolean,
) {
    Under8s(
        halfDurationMinutes = 15,
        requiresPlayerSelection = false,
    ),
    Under18s(
        halfDurationMinutes = 25,
        requiresPlayerSelection = true,
    ),
    Adult(
        halfDurationMinutes = 45,
        requiresPlayerSelection = true,
    ),
}

@Serializable
enum class RefereeTimerPhase {
    NotStarted,
    FirstHalf,
    HalfTime,
    SecondHalf,
    FullTime,
}

@Serializable
data class RecordedRefereeGoal(
    val id: String,
    val isHomeScoringTeam: Boolean,
    val half: MatchHalf,
    val minute: Int,
    val matchSheetMemberId: String? = null,
    val playerName: String? = null,
    val jerseyNumber: String? = null,
    val ownGoal: Boolean = false,
)

@Serializable
data class RefereeMatchSession(
    val profileId: String,
    val matchId: String,
    val preset: RefereeMatchPreset = RefereeMatchPreset.Under18s,
    val phase: RefereeTimerPhase = RefereeTimerPhase.NotStarted,
    val isRunning: Boolean = false,
    val accumulatedMillis: Long = 0L,
    val startedAtEpochMillis: Long? = null,
    val recordedGoals: List<RecordedRefereeGoal> = emptyList(),
    val submittedAtEpochMillis: Long? = null,
) {
    val activeHalf: MatchHalf
        get() = when (phase) {
            RefereeTimerPhase.SecondHalf,
            RefereeTimerPhase.FullTime,
            -> MatchHalf.SecondHalf

            RefereeTimerPhase.NotStarted,
            RefereeTimerPhase.FirstHalf,
            RefereeTimerPhase.HalfTime,
            -> MatchHalf.FirstHalf
        }

    val homeHalfTimeScore: Int
        get() = recordedGoals.count {
            it.isHomeScoringTeam && it.half == MatchHalf.FirstHalf
        }

    val awayHalfTimeScore: Int
        get() = recordedGoals.count {
            !it.isHomeScoringTeam && it.half == MatchHalf.FirstHalf
        }

    val homeFullTimeScore: Int
        get() = recordedGoals.count { it.isHomeScoringTeam }

    val awayFullTimeScore: Int
        get() = recordedGoals.count { !it.isHomeScoringTeam }

    fun elapsedHalfMillis(
        nowEpochMillis: Long,
    ): Long {
        val runningDelta = if (isRunning && startedAtEpochMillis != null) {
            (nowEpochMillis - startedAtEpochMillis).coerceAtLeast(0L)
        } else {
            0L
        }
        return (accumulatedMillis + runningDelta).coerceAtLeast(0L)
    }

    fun currentMatchMinute(
        nowEpochMillis: Long,
    ): Int {
        val halfOffsetMinutes = if (activeHalf == MatchHalf.SecondHalf) {
            preset.halfDurationMinutes
        } else {
            0
        }
        val elapsedInHalfMinutes = (elapsedHalfMillis(nowEpochMillis) / MILLIS_PER_MINUTE).toInt()
        return (halfOffsetMinutes + elapsedInHalfMinutes + 1).coerceAtLeast(1)
    }
}

private const val MILLIS_PER_MINUTE = 60_000L
