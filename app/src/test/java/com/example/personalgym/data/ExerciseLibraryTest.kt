package com.example.personalgym.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseLibraryTest {

    @Test
    fun `exercise ids are unique`() {
        val ids = ExerciseLibrary.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `every machine, cable, barbell or kettlebell move has a dumbbell or bodyweight alternative`() {
        val needsAlt = setOf(Equipment.MACHINE, Equipment.CABLE, Equipment.BARBELL, Equipment.KETTLEBELL)
        ExerciseLibrary.all.filter { it.equipment in needsAlt }.forEach { ex ->
            val alt = ExerciseLibrary.alternativeFor(ex)
            assertNotNull("${ex.id} has no alternative", alt)
            assertTrue("${ex.id} -> ${alt!!.id} is ${alt.equipment}", alt.equipment == Equipment.DUMBBELL || alt.equipment == Equipment.BODYWEIGHT)
        }
    }

    @Test
    fun `alternatives point at real exercises`() {
        ExerciseLibrary.all.mapNotNull { it.alternativeId }.forEach { assertNotNull(it, ExerciseLibrary.find(it)) }
    }

    @Test
    fun `every exercise has instructions`() {
        ExerciseLibrary.all.forEach {
            assertTrue("${it.id} steps", it.steps.isNotEmpty())
            assertTrue("${it.id} tip", it.tip.isNotBlank())
        }
    }
}
