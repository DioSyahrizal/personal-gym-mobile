package com.example.personalgym.data

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomExerciseTest {

    @After
    fun clear() = ExerciseLibrary.setCustom(emptyList())

    @Test
    fun `custom exercises are pickable and found by id`() {
        ExerciseLibrary.setCustom(listOf(CustomExercise("custom_1", "Landmine Press", Muscle.SHOULDERS, Equipment.BARBELL, notes = "Step one\n\n Step two ")))
        val ex = ExerciseLibrary.find("custom_1")!!
        assertTrue(ex.isCustom)
        assertEquals("Landmine Press", ex.name)
        assertEquals(listOf("Step one", "Step two"), ex.steps)
        assertTrue(ex.usesWeight)
        assertTrue(ExerciseLibrary.all.any { it.id == "custom_1" })
    }

    @Test
    fun `archived custom exercises leave the picker but stay findable for history`() {
        ExerciseLibrary.setCustom(listOf(CustomExercise("custom_2", "Old Move", Muscle.CORE, Equipment.BODYWEIGHT, archived = true)))
        assertFalse(ExerciseLibrary.all.any { it.id == "custom_2" })
        assertNotNull(ExerciseLibrary.find("custom_2"))
        assertEquals("Old Move", ExerciseLibrary.get("custom_2").name)
    }

    @Test
    fun `every muscle group has a figure for custom moves`() {
        ExerciseLibrary.setCustom(Muscle.entries.mapIndexed { i, m -> CustomExercise("custom_m$i", "M$i", m, Equipment.DUMBBELL) })
        Muscle.entries.indices.forEach { assertNotNull(ExerciseLibrary.find("custom_m$it")) }
    }

    @Test
    fun `stats work for custom exercises`() {
        ExerciseLibrary.setCustom(listOf(CustomExercise("custom_3", "Cable Fly Low", Muscle.CHEST, Equipment.CABLE)))
        val now = System.currentTimeMillis()
        val s = WorkoutSession(now, now, "W", 90, listOf(ExerciseLog("custom_3", listOf(SetLog(10, 10, 15.0, true)))))
        assertEquals(1, Stats.weeklySets(listOf(s), now)[Muscle.CHEST])
        assertEquals(15.0, Stats.personalRecords(listOf(s)).single().weight, 0.0)
    }
}
