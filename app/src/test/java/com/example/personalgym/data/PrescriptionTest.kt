package com.example.personalgym.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrescriptionTest {
    private fun adapt(id: String, sets: Int, target: Int): PlanExercise {
        val from = ExerciseLibrary.get(id)
        val to = ExerciseLibrary.alternativeFor(from)!!
        return Prescription.adapt(PlanExercise(id, sets, target), from, to)
    }

    @Test
    fun `10 minutes of rowing becomes thruster sets, not 600 reps`() {
        assertEquals(PlanExercise("db_thruster", 4, 12), adapt("rowing_machine", 1, 600))
    }

    @Test
    fun `a long treadmill block becomes high-knee intervals, not 30 minutes of high knees`() {
        assertEquals(PlanExercise("high_knees", 6, 45), adapt("treadmill_run", 1, 1800))
    }

    @Test
    fun `same kind of move keeps its sets and reps`() {
        assertEquals(PlanExercise("goblet_squat", 4, 15), adapt("leg_press", 4, 15))
    }

    @Test
    fun `no dumbbell-only swap in any plan produces an absurd prescription`() {
        Plans.all.flatMap { it.days }.flatMap { it.exercises }.forEach { pe ->
            val from = ExerciseLibrary.get(pe.exerciseId)
            val to = ExerciseLibrary.alternativeFor(from) ?: return@forEach
            val rx = Prescription.adapt(pe, from, to)
            val limit = if (to.track == TrackType.TIME) 120 else 30
            assertTrue("${from.id} ${pe.sets}x${pe.target} -> ${to.id} ${rx.sets}x${rx.target}", rx.target <= limit && rx.sets in 1..6)
        }
    }

    @Test
    fun `dumbbell-only days never repeat an exercise`() {
        Plans.all.flatMap { it.days }.filter { !it.isRest }.forEach { day ->
            val ids = Prescription.dumbbellDay(day.exercises).map { it.first.id }
            assertEquals("${day.title}: $ids", ids.size, ids.toSet().size)
        }
    }

    @Test
    fun `metabolic finisher replaces the rower with a cardio move that isn't already in the day`() {
        val friday = Plans.fatLoss3.days.first { it.title == "Metabolic Finisher" }
        val done = Prescription.dumbbellDay(friday.exercises)
        val rower = done[friday.exercises.indexOfFirst { it.exerciseId == "rowing_machine" }].first
        assertEquals(Muscle.CARDIO, rower.muscle)
        assertTrue(rower.id !in setOf("db_thruster", "db_swing", "burpee", "mountain_climber"))
        assertTrue(rower.equipment == Equipment.DUMBBELL || rower.equipment == Equipment.BODYWEIGHT)
    }

    @Test
    fun `a day without duplicates keeps the usual alternatives`() {
        val day = listOf(PlanExercise("leg_press", 4, 15), PlanExercise("leg_curl", 3, 12))
        assertEquals(listOf("goblet_squat", "db_rdl"), Prescription.dumbbellDay(day).map { it.first.id })
    }

    @Test
    fun `swapping between reps and timed moves uses a fresh per-set target`() {
        assertEquals(45, Prescription.targetFor(ExerciseLibrary.get("rowing_machine")))
        assertEquals(12, Prescription.targetFor(ExerciseLibrary.get("db_thruster")))
    }
}
