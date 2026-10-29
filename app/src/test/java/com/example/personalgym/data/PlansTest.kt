package com.example.personalgym.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlansTest {

    @Test
    fun `every goal has a plan for every day option with that many training days`() {
        PlanGoal.entries.forEach { goal ->
            Plans.dayOptions.forEach { days ->
                val plan = Plans.base(goal, days)
                assertEquals(goal, plan.goal)
                assertEquals(days, plan.daysPerWeek)
                assertEquals(7, plan.days.size)
                assertEquals((1..7).toList(), plan.days.map { it.dayOfWeek })
                // Fat-loss 5-day plan adds an active-recovery cardio day on top of 5 sessions.
                val training = plan.days.count { !it.isRest && it.exercises.size > 1 }
                assertEquals("${plan.id} training days", days, training)
            }
        }
    }

    @Test
    fun `every plan exercise exists in the library`() {
        Plans.all.flatMap { it.days }.flatMap { it.exercises }.forEach {
            assertNotNull("unknown exercise ${it.exerciseId}", ExerciseLibrary.find(it.exerciseId))
        }
    }

    @Test
    fun `original plan ids still exist because saved data refers to them`() {
        assertEquals("fat_loss", Plans.get("fat_loss").id)
        assertEquals("build_muscle", Plans.get("build_muscle").id)
    }

    @Test
    fun `beginners get one set fewer on big lifts and 15 seconds more rest`() {
        val inter = Plans.forProfile(Profile(PlanGoal.BUILD_MUSCLE, Level.INTERMEDIATE, 4))
        val beginner = Plans.forProfile(Profile(PlanGoal.BUILD_MUSCLE, Level.BEGINNER, 4))
        assertEquals(inter.restSeconds + 15, beginner.restSeconds)
        inter.days.zip(beginner.days).forEach { (i, b) ->
            i.exercises.zip(b.exercises).forEach { (ie, be) ->
                assertEquals(if (ie.sets >= 4) ie.sets - 1 else ie.sets, be.sets)
                assertEquals(ie.target, be.target)
            }
        }
    }

    @Test
    fun `day edits replace that day and are not shrunk by the beginner rule`() {
        val custom = listOf(PlanExercise("db_curl", 5, 12))
        val plan = Plans.forProfile(Profile(PlanGoal.BUILD_MUSCLE, Level.BEGINNER, 4), mapOf(1 to custom))
        val monday = plan.days.first { it.dayOfWeek == 1 }
        assertEquals(custom, monday.exercises)
        assertTrue(monday.edited)
        assertFalse(plan.days.first { it.dayOfWeek == 2 }.edited)
    }

    @Test
    fun `empty edit makes a rest day and an edit on a rest day makes an extra workout`() {
        val profile = Profile(PlanGoal.BUILD_MUSCLE, Level.INTERMEDIATE, 4)
        val restDay = Plans.forProfile(profile).days.first { it.isRest }.dayOfWeek
        val plan = Plans.forProfile(profile, mapOf(1 to emptyList(), restDay to listOf(PlanExercise("plank", 3, 30))))
        assertTrue(plan.days.first { it.dayOfWeek == 1 }.isRest)
        val extra = plan.days.first { it.dayOfWeek == restDay }
        assertEquals("Extra Workout", extra.title)
        assertTrue(extra.edited)
    }

    @Test
    fun `profile guess for old saved plan ids`() {
        assertEquals(Profile(PlanGoal.FAT_LOSS, Level.INTERMEDIATE, 5), Plans.profileFor("fat_loss"))
        assertEquals(Profile(PlanGoal.BUILD_MUSCLE, Level.BEGINNER, 3), Plans.profileFor("build_muscle_3", Level.BEGINNER))
    }
}
