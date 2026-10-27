package com.example.personalgym.data

import kotlin.math.roundToLong

/** Weights are always stored in kg; this only changes how they are shown and typed in. */
enum class WeightUnit(val label: String, private val perKg: Double) {
    KG("kg", 1.0),
    LB("lb", 2.20462262);

    fun fromKg(kg: Double): Double = kg * perKg
    fun toKg(value: Double): Double = value / perKg

    /** Number only, in this unit, rounded to one decimal ("44.1", "45"). */
    fun format(kg: Double): String = formatNumber(fromKg(kg))

    /** Number with unit ("44.1 lb"). */
    fun show(kg: Double): String = "${format(kg)} $label"
}

enum class ThemeMode(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark") }

fun formatNumber(v: Double): String {
    val rounded = (v * 10).roundToLong() / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else "%.1f".format(rounded)
}
