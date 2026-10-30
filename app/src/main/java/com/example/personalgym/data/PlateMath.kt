package com.example.personalgym.data

import kotlin.math.abs

/** Which plates to load on each side of a barbell. All values are in the chosen unit, not kg. */
object PlateMath {

    data class Result(
        /** Plates for ONE side, heaviest first. */
        val perSide: List<Double>,
        /** Bar plus both sides. */
        val loaded: Double,
        /** Target minus loaded: >0 means short of the target, <0 means the target is below the bar. */
        val remainder: Double,
    ) {
        val exact: Boolean get() = abs(remainder) < 1e-6
    }

    fun bars(unit: WeightUnit): List<Double> = when (unit) {
        WeightUnit.KG -> listOf(20.0, 15.0, 10.0)
        WeightUnit.LB -> listOf(45.0, 35.0, 25.0)
    }

    fun plates(unit: WeightUnit): List<Double> = when (unit) {
        WeightUnit.KG -> listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)
        WeightUnit.LB -> listOf(45.0, 35.0, 25.0, 10.0, 5.0, 2.5)
    }

    /** Greedy fill with the given plate sizes; gets as close to [target] as possible without going over. */
    fun calculate(target: Double, bar: Double, plates: List<Double>): Result {
        if (target <= bar) return Result(emptyList(), bar, target - bar)
        var perSideLeft = (target - bar) / 2
        val perSide = mutableListOf<Double>()
        for (plate in plates.sortedDescending()) {
            while (perSideLeft + 1e-9 >= plate) {
                perSide += plate
                perSideLeft -= plate
            }
        }
        val loaded = bar + perSide.sum() * 2
        return Result(perSide, loaded, target - loaded)
    }
}
