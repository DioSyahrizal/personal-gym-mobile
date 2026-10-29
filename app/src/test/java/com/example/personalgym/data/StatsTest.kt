package com.example.personalgym.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatsTest {
    private val day = 24L * 60 * 60 * 1000
    private val now = System.currentTimeMillis()

    private fun session(daysAgo: Int, vararg exercises: ExerciseLog, durationSec: Long = 3600) =
        WorkoutSession(now - daysAgo * day, now - daysAgo * day, "W", 90, exercises.toList(), durationSec)

    private fun log(id: String, sets: Int, target: Int, reps: Int, kg: Double, done: Boolean = true) =
        ExerciseLog(id, List(sets) { SetLog(target, reps, kg, done) })

    @Test
    fun `estimated one rep max`() {
        assertEquals(100.0, Stats.e1rm(100.0, 1), 1e-9)
        assertEquals(133.333, Stats.e1rm(100.0, 10), 1e-3)
        // High-rep sets are counted as 12.
        assertEquals(Stats.e1rm(100.0, 12), Stats.e1rm(100.0, 20), 1e-9)
        assertEquals(0.0, Stats.e1rm(0.0, 8), 0.0)
    }

    @Test
    fun `overload suggests the next plate when every set hit its target`() {
        val history = listOf(session(2, log("barbell_bench", 3, 8, 8, 50.0)))
        val o = Stats.overload(history, "barbell_bench")!!
        assertEquals(52.5, o.weight!!, 1e-9)
        assertEquals(52.5, Stats.suggestedWeight(history, "barbell_bench"), 1e-9)
    }

    @Test
    fun `overload steps in round pounds for lb users`() {
        val history = listOf(session(2, log("barbell_bench", 3, 8, 8, WeightUnit.LB.toKg(135.0))))
        val next = Stats.overload(history, "barbell_bench", WeightUnit.LB)!!.weight!!
        assertEquals("140", WeightUnit.LB.format(next))
        val squat = listOf(session(2, log("back_squat", 3, 8, 8, WeightUnit.LB.toKg(185.0))))
        assertEquals("195", WeightUnit.LB.format(Stats.overload(squat, "back_squat", WeightUnit.LB)!!.weight!!))
    }

    @Test
    fun `no overload when a target was missed, and the last weight is reused`() {
        val history = listOf(
            session(2, ExerciseLog("barbell_bench", listOf(SetLog(8, 8, 50.0, true), SetLog(8, 6, 50.0, true)))),
        )
        assertNull(Stats.overload(history, "barbell_bench"))
        assertEquals(50.0, Stats.suggestedWeight(history, "barbell_bench"), 1e-9)
    }

    @Test
    fun `bodyweight and timed moves get a non-weight suggestion`() {
        val push = Stats.overload(listOf(session(1, log("push_up", 3, 12, 12, 0.0))), "push_up")!!
        assertNull(push.weight)
        val plank = Stats.overload(listOf(session(1, log("plank", 3, 45, 45, 0.0))), "plank")!!
        assertNull(plank.weight)
    }

    @Test
    fun `calories use the MET of completed sets`() {
        assertEquals(245, Stats.estimateCalories(emptyList(), 3600, 70.0)) // resting MET 3.5
        val bench = listOf(log("barbell_bench", 3, 8, 8, 50.0)) // MET 5.0
        assertEquals(350, Stats.estimateCalories(bench, 3600, 70.0))
        val notDone = listOf(log("barbell_bench", 3, 8, 8, 50.0, done = false))
        assertEquals(245, Stats.estimateCalories(notDone, 3600, 70.0))
    }

    @Test
    fun `exercise history is oldest first with best set per workout`() {
        val history = listOf(
            session(1, log("barbell_bench", 3, 8, 8, 55.0)),
            session(8, log("barbell_bench", 3, 8, 8, 50.0)),
            session(4, log("push_up", 3, 12, 12, 0.0)),
        )
        val bench = Stats.exerciseHistory(history, "barbell_bench")
        assertEquals(listOf(50.0, 55.0), bench.map { it.bestWeight })
        assertEquals(3 * 8 * 55.0, bench.last().volume, 1e-9)
    }

    @Test
    fun `weekly sets count this week only and leave out cardio`() {
        val thisWeekStart = Stats.weekStart(now)
        val today = WorkoutSession(now, now, "W", 90, listOf(log("barbell_bench", 3, 8, 8, 50.0), log("burpee", 4, 10, 10, 0.0)))
        val lastWeek = WorkoutSession(thisWeekStart - day, thisWeekStart - day, "W", 90, listOf(log("barbell_bench", 5, 8, 8, 50.0)))
        val weekly = Stats.weeklySets(listOf(today, lastWeek), now)
        assertEquals(3, weekly[Muscle.CHEST])
        assertEquals(0, weekly[Muscle.LEGS])
        assertNull(weekly[Muscle.CARDIO])
    }

    @Test
    fun `personal record is the heaviest completed set`() {
        val history = listOf(
            session(3, log("barbell_bench", 3, 8, 8, 50.0)),
            session(1, ExerciseLog("barbell_bench", listOf(SetLog(8, 5, 60.0, true), SetLog(8, 8, 70.0, false)))),
        )
        val pr = Stats.personalRecords(history).single()
        assertEquals(60.0, pr.weight, 0.0)
        assertEquals(5, pr.reps)
    }

    @Test
    fun `body weight goal progress works for losing and gaining`() {
        val lose = Goal(1, GoalType.BODY_WEIGHT, "", target = 70.0, start = 80.0)
        assertEquals(0.5f, Stats.goalProgress(lose, emptyList(), listOf(WeightEntry(now, 75.0))).fraction, 1e-6f)
        val gain = Goal(2, GoalType.BODY_WEIGHT, "", target = 70.0, start = 60.0)
        assertEquals(0.25f, Stats.goalProgress(gain, emptyList(), listOf(WeightEntry(now, 62.5))).fraction, 1e-6f)
    }

    @Test
    fun `lift goal progress uses the best completed set and is capped at 100 percent`() {
        val history = listOf(session(1, log("back_squat", 3, 8, 8, 120.0)))
        val goal = Goal(1, GoalType.LIFT, "", target = 100.0, exerciseId = "back_squat")
        assertEquals(1f, Stats.goalProgress(goal, history, emptyList()).fraction, 0f)
    }

    @Test
    fun `week streak counts consecutive weeks with a workout`() {
        val start = Stats.weekStart(now)
        val sessions = listOf(0L, 7L, 14L, 28L).map { back ->
            val t = start - back * day + day // a day into that week
            WorkoutSession(t, t, "W", 90, emptyList())
        }
        assertEquals(3, Stats.weekStreak(sessions, start + day))
        assertEquals(0, Stats.weekStreak(emptyList(), now))
    }
}
