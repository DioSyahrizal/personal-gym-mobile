package com.example.personalgym.data

/** Supersets are runs of exercises linked by [ExerciseLog.supersetNext]; a dangling last link is ignored. */
object Supersets {

    /** True when [index] is linked to the exercise after it. */
    fun linksNext(exercises: List<ExerciseLog>, index: Int): Boolean =
        exercises.getOrNull(index)?.supersetNext == true && index < exercises.lastIndex

    /** Rest only after the last exercise of a superset (or after any exercise that isn't in one). */
    fun restAfter(exercises: List<ExerciseLog>, index: Int): Boolean = !linksNext(exercises, index)

    /** The exercise to do next within the superset, or null at the end of the group. */
    fun nextInGroup(exercises: List<ExerciseLog>, index: Int): Int? = if (linksNext(exercises, index)) index + 1 else null

    /** Label per exercise: "A1", "A2", "B1"… for superset members, null for standalone exercises. */
    fun labels(exercises: List<ExerciseLog>): List<String?> {
        val out = MutableList<String?>(exercises.size) { null }
        var group = 0
        var i = 0
        while (i < exercises.size) {
            var end = i
            while (linksNext(exercises, end)) end++
            if (end > i) {
                val letter = 'A' + group++
                for (k in i..end) out[k] = "$letter${k - i + 1}"
            }
            i = end + 1
        }
        return out
    }
}
