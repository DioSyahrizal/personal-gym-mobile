package com.example.personalgym.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlateMathTest {
    private val kg = PlateMath.plates(WeightUnit.KG)
    private val lb = PlateMath.plates(WeightUnit.LB)

    @Test
    fun `exact kg loads, heaviest plates first`() {
        val r = PlateMath.calculate(100.0, 20.0, kg)
        assertEquals(listOf(25.0, 15.0), r.perSide)
        assertEquals(100.0, r.loaded, 0.0)
        assertTrue(r.exact)
        assertEquals(listOf(25.0, 5.0, 2.5), PlateMath.calculate(85.0, 20.0, kg).perSide)
    }

    @Test
    fun `standard lb loads`() {
        assertEquals(listOf(45.0), PlateMath.calculate(135.0, 45.0, lb).perSide)
        assertEquals(listOf(45.0, 45.0), PlateMath.calculate(225.0, 45.0, lb).perSide)
        assertEquals(listOf(45.0, 2.5), PlateMath.calculate(140.0, 45.0, lb).perSide)
    }

    @Test
    fun `uneven target gets the closest load without going over`() {
        val r = PlateMath.calculate(61.0, 20.0, kg)
        assertEquals(listOf(20.0), r.perSide)
        assertEquals(60.0, r.loaded, 0.0)
        assertEquals(1.0, r.remainder, 1e-9)
        assertFalse(r.exact)
    }

    @Test
    fun `small plates add up with no floating point drift`() {
        val r = PlateMath.calculate(22.5, 20.0, kg)
        assertEquals(listOf(1.25), r.perSide)
        assertTrue(r.exact)
    }

    @Test
    fun `target at or below the bar needs no plates`() {
        assertTrue(PlateMath.calculate(20.0, 20.0, kg).let { it.perSide.isEmpty() && it.exact })
        val light = PlateMath.calculate(15.0, 20.0, kg)
        assertTrue(light.perSide.isEmpty())
        assertEquals(-5.0, light.remainder, 0.0)
    }
}
