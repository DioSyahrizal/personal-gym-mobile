package com.example.personalgym.data

import java.util.Calendar

object Stats {
    private const val DAY = 24L * 60 * 60 * 1000
    private const val WEEK = 7 * DAY

    /** Monday 00:00 of the week containing [ms]. */
    fun weekStart(ms: Long): Long {
        val c = Calendar.getInstance().apply {
            timeInMillis = ms
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val dow = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Monday = 0
        c.add(Calendar.DAY_OF_YEAR, -dow)
        return c.timeInMillis
    }

    /** 1 = Monday … 7 = Sunday */
    fun dayOfWeek(ms: Long = System.currentTimeMillis()): Int {
        val c = Calendar.getInstance().apply { timeInMillis = ms }
        return (c.get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1
    }

    fun thisWeek(sessions: List<WorkoutSession>, now: Long = System.currentTimeMillis()): List<WorkoutSession> {
        val start = weekStart(now)
        return sessions.filter { it.startedAt >= start }
    }

    /** Days of the current week (1–7) that have at least one finished workout. */
    fun trainedDaysThisWeek(sessions: List<WorkoutSession>): Set<Int> =
        thisWeek(sessions).map { dayOfWeek(it.startedAt) }.toSet()

    /** Consecutive weeks (ending with this or last week) that contain at least one workout. */
    fun weekStreak(sessions: List<WorkoutSession>, now: Long = System.currentTimeMillis()): Int {
        if (sessions.isEmpty()) return 0
        val weeks = sessions.map { weekStart(it.startedAt) }.toSet()
        var cursor = weekStart(now)
        if (cursor !in weeks) cursor = weekStart(cursor - DAY) // this week not started yet is fine
        var streak = 0
        while (cursor in weeks) {
            streak++
            cursor = weekStart(cursor - DAY)
        }
        return streak
    }

    data class WeekBucket(val start: Long, val workouts: Int, val volume: Double, val minutes: Long)

    fun lastWeeks(sessions: List<WorkoutSession>, count: Int = 8, now: Long = System.currentTimeMillis()): List<WeekBucket> {
        val current = weekStart(now)
        return (count - 1 downTo 0).map { back ->
            // Step back by days so DST changes don't skew the boundary.
            val start = weekStart(current - back * WEEK + DAY)
            val end = start + WEEK
            val inWeek = sessions.filter { it.startedAt in start until end }
            WeekBucket(start, inWeek.size, inWeek.sumOf { it.volume }, inWeek.sumOf { it.durationSec } / 60)
        }
    }

    /** Completed sets per muscle group over the last [days] days. */
    fun muscleSplit(sessions: List<WorkoutSession>, days: Int = 30, now: Long = System.currentTimeMillis()): Map<Muscle, Int> {
        val from = now - days * DAY
        val out = mutableMapOf<Muscle, Int>()
        sessions.filter { it.startedAt >= from }.forEach { s ->
            s.exercises.forEach { e ->
                val m = ExerciseLibrary.find(e.exerciseId)?.muscle ?: return@forEach
                out[m] = (out[m] ?: 0) + e.sets.count { it.done }
            }
        }
        return out.filterValues { it > 0 }.toList().sortedByDescending { it.second }.toMap()
    }

    data class Record(val exercise: Exercise, val weight: Double, val reps: Int, val date: Long)

    fun personalRecords(sessions: List<WorkoutSession>): List<Record> {
        val best = mutableMapOf<String, Record>()
        sessions.forEach { s ->
            s.exercises.forEach { e ->
                val ex = ExerciseLibrary.find(e.exerciseId) ?: return@forEach
                if (!ex.usesWeight) return@forEach
                e.sets.filter { it.done && it.weight > 0 }.forEach { set ->
                    val cur = best[ex.id]
                    if (cur == null || set.weight > cur.weight || (set.weight == cur.weight && set.reps > cur.reps)) {
                        best[ex.id] = Record(ex, set.weight, set.reps, s.startedAt)
                    }
                }
            }
        }
        return best.values.sortedByDescending { it.date }
    }

    fun maxWeight(sessions: List<WorkoutSession>, exerciseId: String): Double =
        sessions.flatMap { s -> s.exercises.filter { it.exerciseId == exerciseId } }
            .flatMap { it.sets }.filter { it.done }.maxOfOrNull { it.weight } ?: 0.0

    /** Weight used last time for an exercise, to prefill the next session. */
    fun lastWeight(sessions: List<WorkoutSession>, exerciseId: String): Double =
        sessions.sortedByDescending { it.startedAt }
            .firstNotNullOfOrNull { s ->
                s.exercises.firstOrNull { it.exerciseId == exerciseId }
                    ?.sets?.lastOrNull { it.done && it.weight > 0 }?.weight
            } ?: 0.0

    /** Completed sets from the most recent finished workout that included this exercise. */
    fun previousSets(sessions: List<WorkoutSession>, exerciseId: String): List<SetLog> =
        sessions.sortedByDescending { it.startedAt }
            .firstNotNullOfOrNull { s ->
                s.exercises.firstOrNull { it.exerciseId == exerciseId }
                    ?.sets?.filter { it.done }?.takeIf { it.isNotEmpty() }
            }.orEmpty()

    data class Overload(val weight: Double?, val message: String)

    /** Smallest realistic plate jump for the equipment, in the user's own unit. */
    private fun increment(ex: Exercise, unit: WeightUnit): Double {
        val big = ex.muscle == Muscle.LEGS || ex.id == "deadlift"
        return when (unit) {
            WeightUnit.KG -> when (ex.equipment) {
                Equipment.DUMBBELL, Equipment.KETTLEBELL -> 2.0
                else -> if (big) 5.0 else 2.5
            }
            WeightUnit.LB -> if (big && ex.equipment != Equipment.DUMBBELL) 10.0 else 5.0
        }
    }

    /**
     * Progressive overload: if every set last time hit its target, go heavier (or do more).
     * Returns null when there is no history or the targets weren't all reached.
     */
    fun overload(sessions: List<WorkoutSession>, exerciseId: String, unit: WeightUnit = WeightUnit.KG): Overload? {
        val ex = ExerciseLibrary.find(exerciseId) ?: return null
        val last = sessions.sortedByDescending { it.startedAt }
            .firstNotNullOfOrNull { s -> s.exercises.firstOrNull { it.exerciseId == exerciseId } } ?: return null
        val done = last.sets.filter { it.done }
        val allHit = done.isNotEmpty() && done.size == last.sets.size && done.all { it.reps >= it.target }
        if (!allHit) return null
        val best = done.first()
        return when {
            ex.usesWeight && best.weight > 0 -> {
                val heaviest = done.maxOf { it.weight }
                // Step up in the displayed unit so suggestions are round numbers (45 → 50 lb, not 44.1 → 48.5).
                val shown = unit.fromKg(heaviest).let { Math.round(it * 10) / 10.0 }
                val next = unit.toKg(shown + increment(ex, unit))
                Overload(next, "You hit all ${done.size} sets at ${unit.show(heaviest)} last time — try ${unit.show(next)}")
            }
            ex.track == TrackType.TIME -> Overload(null, "You completed every set last time — try adding 5–10 seconds")
            else -> Overload(null, "You hit every set last time — try 1–2 more reps per set")
        }
    }

    /** Weight to prefill: the overload suggestion if there is one, otherwise what was used last time. */
    fun suggestedWeight(sessions: List<WorkoutSession>, exerciseId: String, unit: WeightUnit = WeightUnit.KG): Double =
        overload(sessions, exerciseId, unit)?.weight ?: lastWeight(sessions, exerciseId)

    data class GoalProgress(val current: Double, val fraction: Float, val label: String)

    /** Title shown for a goal; generated so weight goals follow the current unit. */
    fun goalTitle(goal: Goal, unit: WeightUnit): String {
        val fmt = ::formatNumber
        return when (goal.type) {
            GoalType.WEEKLY_WORKOUTS -> "Train ${fmt(goal.target)}× per week"
            GoalType.TOTAL_WORKOUTS -> "Complete ${fmt(goal.target)} workouts"
            GoalType.BODY_WEIGHT -> "Reach ${unit.show(goal.target)}"
            GoalType.LIFT -> "${goal.exerciseId?.let { ExerciseLibrary.find(it)?.name } ?: "Lift"}: ${unit.show(goal.target)}"
            GoalType.CUSTOM -> goal.title
        }
    }

    fun goalProgress(goal: Goal, sessions: List<WorkoutSession>, weights: List<WeightEntry>, unit: WeightUnit = WeightUnit.KG): GoalProgress {
        val fmt = ::formatNumber
        return when (goal.type) {
            GoalType.WEEKLY_WORKOUTS -> {
                val c = thisWeek(sessions).size.toDouble()
                GoalProgress(c, (c / goal.target).toFloat(), "${fmt(c)} / ${fmt(goal.target)} this week")
            }
            GoalType.TOTAL_WORKOUTS -> {
                val c = sessions.count { it.startedAt >= goal.createdAt }.toDouble()
                GoalProgress(c, (c / goal.target).toFloat(), "${fmt(c)} / ${fmt(goal.target)} workouts")
            }
            GoalType.BODY_WEIGHT -> {
                val cur = weights.maxByOrNull { it.date }?.kg ?: goal.start
                val total = goal.start - goal.target
                val f = if (total == 0.0) 1f else ((goal.start - cur) / total).toFloat()
                GoalProgress(cur, f, "Now ${unit.show(cur)} → goal ${unit.show(goal.target)}")
            }
            GoalType.LIFT -> {
                val m = goal.exerciseId?.let { maxWeight(sessions, it) } ?: 0.0
                GoalProgress(m, (m / goal.target).toFloat(), "Best ${unit.show(m)} / ${unit.show(goal.target)}")
            }
            GoalType.CUSTOM -> GoalProgress(
                goal.manualProgress, (goal.manualProgress / goal.target).toFloat(),
                "${fmt(goal.manualProgress)} / ${fmt(goal.target)}",
            )
        }.let { it.copy(fraction = it.fraction.coerceIn(0f, 1f)) }
    }
}
