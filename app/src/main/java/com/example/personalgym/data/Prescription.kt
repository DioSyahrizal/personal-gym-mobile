package com.example.personalgym.data

/**
 * Sets and targets mean different things for reps and timed moves (12 = reps, 600 = seconds), so
 * when a move is replaced by one tracked differently the prescription has to be translated.
 */
object Prescription {
    const val DEFAULT_REPS = 12
    const val DEFAULT_SECONDS = 45

    /** A single block this long is steady cardio (e.g. 10 min rowing), not one hard set. */
    private const val STEADY_BLOCK_SEC = 300

    /** [pe] as written for [from], adapted for doing [to] instead (e.g. the dumbbell alternative). */
    fun adapt(pe: PlanExercise, from: Exercise, to: Exercise): PlanExercise {
        if (from.id == to.id) return pe
        val steadyBlock = from.track == TrackType.TIME && pe.sets == 1 && pe.target >= STEADY_BLOCK_SEC
        return when {
            // 10 min on the rower becomes intervals of the replacement move, not 600 reps.
            steadyBlock -> PlanExercise(
                to.id, (pe.target / 150).coerceIn(2, 6),
                if (to.track == TrackType.TIME) DEFAULT_SECONDS else DEFAULT_REPS,
            )
            from.track == to.track -> pe.copy(exerciseId = to.id)
            else -> PlanExercise(to.id, pe.sets, targetFor(to))
        }
    }

    /** Target to use for one set when swapping into [to] from a move tracked differently. */
    fun targetFor(to: Exercise): Int = if (to.track == TrackType.TIME) DEFAULT_SECONDS else DEFAULT_REPS
}
