package com.example.personalgym.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class UnitsTest {

    @Test
    fun `weight typed in lb reads back unchanged`() {
        listOf(135.0, 45.0, 2.5, 225.0).forEach { lb ->
            assertEquals(formatNumber(lb), WeightUnit.LB.format(WeightUnit.LB.toKg(lb)))
        }
    }

    @Test
    fun `kg and lb display`() {
        assertEquals("61.2 kg", WeightUnit.KG.show(61.23))
        assertEquals("135 lb", WeightUnit.LB.show(61.235))
        assertEquals("74.8 kg", WeightUnit.KG.show(WeightUnit.LB.toKg(165.0)))
    }

    @Test
    fun `lengths use cm or inches`() {
        assertEquals("85 cm", WeightUnit.KG.showLength(85.0))
        assertEquals("33.5 in", WeightUnit.LB.showLength(85.0))
        assertEquals(85.0, WeightUnit.LB.lengthToCm(WeightUnit.LB.lengthFromCm(85.0)), 1e-9)
    }

    @Test
    fun `body fat percent is never converted`() {
        assertEquals("18.5%", WeightUnit.LB.showMeasurement(BodySite.BODY_FAT, 18.5))
        assertEquals(18.5, WeightUnit.LB.measurementToStored(BodySite.BODY_FAT, 18.5), 0.0)
        assertEquals("%", WeightUnit.LB.measurementLabel(BodySite.BODY_FAT))
    }

    @Test
    fun `formatNumber drops trailing zero and rounds to one decimal`() {
        assertEquals("12", formatNumber(12.0))
        assertEquals("12.3", formatNumber(12.25))
        assertEquals("0", formatNumber(0.04))
        assertEquals("-0.7", formatNumber(-0.7))
    }

    @Test
    fun `numbers use a dot even when the phone language uses commas`() {
        val saved = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("id-ID"))
            assertEquals("61.2", formatNumber(61.2))
            assertEquals("1.5k", formatCompact(1500.0))
        } finally {
            Locale.setDefault(saved)
        }
    }

    @Test
    fun `compact format for big totals`() {
        assertEquals("950", formatCompact(950.0))
        assertEquals("3.2k", formatCompact(3240.0))
        assertEquals("12k", formatCompact(12_400.0))
        assertEquals("1.5M", formatCompact(1_500_000.0))
    }
}
