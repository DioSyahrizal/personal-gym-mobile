package com.example.personalgym.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BackupFormatTest {

    private val backup = Backup(
        activePlanId = "build_muscle_4",
        dumbbellOnly = true,
        sessions = listOf(
            WorkoutSession(
                id = 1, startedAt = 1_000, title = "Upper A", restSeconds = 90,
                exercises = listOf(ExerciseLog("barbell_bench", listOf(SetLog(8, 8, 57.5, true), SetLog(8, 6, 57.5, false)))),
                durationSec = 2700, calories = 300,
            ),
        ),
        goals = listOf(
            Goal(5, GoalType.LIFT, "Squat 100", 100.0, start = 0.0, exerciseId = "back_squat", createdAt = 42),
            Goal(6, GoalType.CUSTOM, "Run", 50.0, manualProgress = 3.0, createdAt = 43),
        ),
        weights = listOf(WeightEntry(10, 74.2)),
        exportedAt = 99,
        level = Level.BEGINNER,
        planEdits = mapOf("build_muscle_4" to mapOf(1 to listOf(PlanExercise("db_curl", 4, 12)), 3 to emptyList())),
        measurements = listOf(Measurement(20, BodySite.WAIST, 82.5), Measurement(20, BodySite.BODY_FAT, 18.0)),
    )

    @Test
    fun `round trip keeps everything`() {
        assertEquals(backup, BackupFormat.decode(BackupFormat.encode(backup)))
    }

    @Test
    fun `backups from before newer fields still restore`() {
        val v1 = """{"app":"personal-gym","version":1,"exportedAt":5,"activePlanId":"fat_loss","dumbbellOnly":false,
            "sessions":[],"goals":[],"weights":[{"d":1,"kg":80}]}"""
        val b = BackupFormat.decode(v1)
        assertEquals("fat_loss", b.activePlanId)
        assertNull(b.level)
        assertTrue(b.planEdits.isEmpty())
        assertTrue(b.measurements.isEmpty())
        assertEquals(listOf(WeightEntry(1, 80.0)), b.weights)
    }

    @Test
    fun `rejects files that are not backups with a readable message`() {
        expectError("This file isn't valid JSON.") { BackupFormat.decode("not json") }
        expectError("This isn't a Personal Gym backup file.") { BackupFormat.decode("""{"hello":"world"}""") }
        expectError("This backup was made by a newer version of the app.") {
            BackupFormat.decode("""{"app":"personal-gym","version":99}""")
        }
        expectError("The backup file is damaged and couldn't be read.") {
            BackupFormat.decode("""{"app":"personal-gym","version":1,"sessions":[{"id":"x"}]}""")
        }
    }

    private fun expectError(message: String, block: () -> Unit) {
        try {
            block()
            fail("expected: $message")
        } catch (e: IllegalArgumentException) {
            assertEquals(message, e.message)
        }
    }
}
