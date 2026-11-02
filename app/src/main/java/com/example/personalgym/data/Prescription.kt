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

    /**
     * A plan day done in dumbbell-only mode. Each machine/cable/barbell move becomes its dumbbell
     * alternative, unless that move is already in the day (e.g. rowing → thrusters on a day that
     * starts with thrusters); then another dumbbell/bodyweight move for the same muscle group is
     * used, preferring one tracked the same way as the usual alternative.
     */
    fun dumbbellDay(exercises: List<PlanExercise>): List<Pair<Exercise, PlanExercise>> {
        val originals = exercises.map { ExerciseLibrary.get(it.exerciseId) }
        // Moves that stay as they are claim their spot first, wherever they are in the day.
        val taken = originals.filter { ExerciseLibrary.alternativeFor(it) == null }.map { it.id }.toMutableSet()
        return exercises.zip(originals).map { (pe, from) ->
            val alt = ExerciseLibrary.alternativeFor(from) ?: return@map from to pe
            val to = if (alt.id !in taken) alt else replacementFor(from, alt, taken) ?: alt
            taken += to.id
            to to adapt(pe, from, to)
        }
    }

    private fun replacementFor(from: Exercise, usual: Exercise, taken: Set<String>): Exercise? =
        ExerciseLibrary.builtIn
            .filter {
                it.muscle == from.muscle && it.id !in taken &&
                    (it.equipment == Equipment.DUMBBELL || it.equipment == Equipment.BODYWEIGHT)
            }
            .sortedBy { if (it.track == usual.track) 0 else 1 }
            .firstOrNull()

    /** Target to use for one set when swapping into [to] from a move tracked differently. */
    fun targetFor(to: Exercise): Int = if (to.track == TrackType.TIME) DEFAULT_SECONDS else DEFAULT_REPS
}
