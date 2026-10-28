package com.example.personalgym.data

import kotlin.math.roundToLong

/**
 * Weights are always stored in kg and lengths in cm; this only changes how they are shown and typed in.
 * Metric pairs kg with cm, imperial pairs lb with inches.
 */
enum class WeightUnit(val label: String, private val perKg: Double, val lengthLabel: String, private val perCm: Double) {
    KG("kg", 1.0, "cm", 1.0),
    LB("lb", 2.20462262, "in", 1 / 2.54);

    val settingLabel: String get() = "$label · $lengthLabel"

    fun lengthFromCm(cm: Double): Double = cm * perCm
    fun lengthToCm(value: Double): Double = value / perCm
    fun showLength(cm: Double): String = "${formatNumber(lengthFromCm(cm))} $lengthLabel"

    /** Display value of a measurement in this unit system. */
    fun showMeasurement(site: BodySite, value: Double): String =
        if (site.isPercent) "${formatNumber(value)}%" else showLength(value)
    fun measurementLabel(site: BodySite): String = if (site.isPercent) "%" else lengthLabel
    fun measurementFromStored(site: BodySite, value: Double): Double = if (site.isPercent) value else lengthFromCm(value)
    fun measurementToStored(site: BodySite, value: Double): Double = if (site.isPercent) value else lengthToCm(value)

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
