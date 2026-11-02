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
    fun `swapping between reps and timed moves uses a fresh per-set target`() {
        assertEquals(45, Prescription.targetFor(ExerciseLibrary.get("rowing_machine")))
        assertEquals(12, Prescription.targetFor(ExerciseLibrary.get("db_thruster")))
    }
}
