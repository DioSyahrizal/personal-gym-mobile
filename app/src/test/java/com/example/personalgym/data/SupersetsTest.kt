package com.example.personalgym.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SupersetsTest {
    private fun ex(link: Boolean) = ExerciseLog("db_curl", emptyList(), supersetNext = link)

    @Test
    fun `no rest inside a superset, rest after its last exercise`() {
        val list = listOf(ex(true), ex(false), ex(false))
        assertFalse(Supersets.restAfter(list, 0))
        assertTrue(Supersets.restAfter(list, 1))
        assertTrue(Supersets.restAfter(list, 2))
        assertEquals(1, Supersets.nextInGroup(list, 0))
        assertNull(Supersets.nextInGroup(list, 1))
    }

    @Test
    fun `a link on the last exercise is ignored`() {
        val list = listOf(ex(false), ex(true))
        assertTrue(Supersets.restAfter(list, 1))
        assertEquals(listOf<String?>(null, null), Supersets.labels(list))
    }

    @Test
    fun `labels name each group and position`() {
        // A1 A2 | solo | B1 B2 B3 (a circuit)
        val list = listOf(ex(true), ex(false), ex(false), ex(true), ex(true), ex(false))
        assertEquals(listOf("A1", "A2", null, "B1", "B2", "B3"), Supersets.labels(list))
    }
}
