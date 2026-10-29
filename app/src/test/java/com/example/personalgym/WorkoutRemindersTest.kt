package com.example.personalgym

import com.example.personalgym.data.PlanDay
import com.example.personalgym.data.PlanExercise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class WorkoutRemindersTest {
    private val training = PlanDay(3, "Leg Day", "Legs", listOf(PlanExercise("back_squat", 3, 8)))
    private val rest = PlanDay(4, "Rest Day", "Recover", emptyList())

    @Test
    fun `reminds only on an untrained training day with no workout running`() {
        assertTrue(WorkoutReminders.shouldRemind(training, trainedToday = false, workoutInProgress = false))
        assertFalse(WorkoutReminders.shouldRemind(rest, trainedToday = false, workoutInProgress = false))
        assertFalse(WorkoutReminders.shouldRemind(training, trainedToday = true, workoutInProgress = false))
        assertFalse(WorkoutReminders.shouldRemind(training, trainedToday = false, workoutInProgress = true))
    }

    @Test
    fun `next trigger is later today or tomorrow at the chosen time`() {
        val base = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 28, 11, 15, 30); set(Calendar.MILLISECOND, 0) }
        val now = base.timeInMillis

        val later = Calendar.getInstance().apply { timeInMillis = WorkoutReminders.nextTrigger(18 * 60, now) }
        assertEquals(28, later.get(Calendar.DAY_OF_MONTH))
        assertEquals(18, later.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, later.get(Calendar.MINUTE))

        val earlier = Calendar.getInstance().apply { timeInMillis = WorkoutReminders.nextTrigger(7 * 60 + 30, now) }
        assertEquals(29, earlier.get(Calendar.DAY_OF_MONTH))
        assertEquals(7, earlier.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, earlier.get(Calendar.MINUTE))

        // The current minute has already started, so it rolls to tomorrow.
        val sameMinute = Calendar.getInstance().apply { timeInMillis = WorkoutReminders.nextTrigger(11 * 60 + 15, now) }
        assertEquals(29, sameMinute.get(Calendar.DAY_OF_MONTH))
    }
}
