package com.example.personalgym.data

private fun ex(id: String, sets: Int, target: Int) = PlanExercise(id, sets, target)
private fun rest(day: Int, title: String = "Rest Day", focus: String = "Recover, stretch, sleep well") =
    PlanDay(day, title, focus, emptyList())

object Plans {

    val fatLoss = WorkoutPlan(
        id = "fat_loss",
        name = "Fat Loss Shred",
        goal = PlanGoal.FAT_LOSS,
        description = "5 training days of full-body circuits, HIIT and conditioning. " +
            "Higher reps and short rests keep your heart rate up to burn more calories.",
        restSeconds = 45,
        days = listOf(
            PlanDay(
                1, "Full Body Circuit", "Total body · fat burn",
                listOf(
                    ex("goblet_squat", 3, 15), ex("push_up", 3, 12), ex("db_row", 3, 12),
                    ex("kb_swing", 3, 20), ex("mountain_climber", 3, 40), ex("plank", 3, 45),
                ),
            ),
            PlanDay(
                2, "HIIT Cardio", "Intervals · conditioning",
                listOf(
                    ex("treadmill_run", 1, 600), ex("jumping_jack", 4, 45), ex("burpee", 4, 10),
                    ex("high_knees", 4, 30), ex("rowing_machine", 1, 600),
                ),
            ),
            PlanDay(
                3, "Lower Body Burn", "Legs · glutes · core",
                listOf(
                    ex("leg_press", 4, 15), ex("walking_lunge", 3, 12), ex("leg_curl", 3, 15),
                    ex("hip_thrust", 3, 15), ex("calf_raise_machine", 3, 20), ex("russian_twist", 3, 20),
                ),
            ),
            PlanDay(
                4, "Upper Body + Core", "Chest · back · arms",
                listOf(
                    ex("db_bench", 3, 12), ex("lat_pulldown", 3, 12), ex("machine_shoulder_press", 3, 12),
                    ex("cable_row", 3, 12), ex("tricep_pushdown", 3, 15), ex("db_curl", 3, 15), ex("crunch", 3, 20),
                ),
            ),
            PlanDay(
                5, "Metabolic Finisher", "Full body · max effort",
                listOf(
                    ex("db_thruster", 4, 12), ex("burpee", 4, 10), ex("kb_swing", 4, 20),
                    ex("mountain_climber", 4, 40), ex("rowing_machine", 1, 600), ex("plank", 3, 60),
                ),
            ),
            PlanDay(6, "Active Recovery", "Light cardio 30 min", listOf(ex("treadmill_run", 1, 1800))),
            rest(7),
        ),
    )

    val buildMuscle = WorkoutPlan(
        id = "build_muscle",
        name = "Muscle Builder",
        goal = PlanGoal.BUILD_MUSCLE,
        description = "Push / Pull / Legs + Upper / Lower split. Moderate reps (6–12) with heavier weight " +
            "and longer rests. Add weight when you hit the top of the rep range.",
        restSeconds = 90,
        days = listOf(
            PlanDay(
                1, "Push Day", "Chest · shoulders · triceps",
                listOf(
                    ex("barbell_bench", 4, 8), ex("incline_db_press", 3, 10), ex("machine_shoulder_press", 3, 10),
                    ex("lateral_raise", 3, 15), ex("cable_crossover", 3, 12), ex("tricep_pushdown", 3, 12),
                ),
            ),
            PlanDay(
                2, "Pull Day", "Back · rear delts · biceps",
                listOf(
                    ex("deadlift", 4, 6), ex("lat_pulldown", 4, 10), ex("cable_row", 3, 10),
                    ex("barbell_row", 3, 10), ex("face_pull", 3, 15), ex("barbell_curl", 3, 10), ex("hammer_curl", 3, 12),
                ),
            ),
            PlanDay(
                3, "Leg Day", "Quads · hamstrings · glutes · calves",
                listOf(
                    ex("back_squat", 4, 8), ex("leg_press", 3, 12), ex("leg_extension", 3, 12),
                    ex("leg_curl", 3, 12), ex("hip_thrust", 3, 10), ex("calf_raise_machine", 4, 15),
                ),
            ),
            rest(4),
            PlanDay(
                5, "Upper Body", "Chest · back · shoulders · arms",
                listOf(
                    ex("db_bench", 4, 10), ex("pull_up", 4, 8), ex("db_shoulder_press", 3, 10),
                    ex("db_row", 3, 10), ex("pec_deck", 3, 12), ex("db_curl", 3, 12), ex("db_overhead_extension", 3, 12),
                ),
            ),
            PlanDay(
                6, "Lower Body + Core", "Hamstrings · glutes · abs",
                listOf(
                    ex("db_rdl", 4, 10), ex("bulgarian_split_squat", 3, 10), ex("goblet_squat", 3, 12),
                    ex("db_calf_raise", 3, 15), ex("plank", 3, 60), ex("crunch", 3, 20),
                ),
            ),
            rest(7),
        ),
    )

    val all = listOf(fatLoss, buildMuscle)

    fun get(id: String?): WorkoutPlan = all.firstOrNull { it.id == id } ?: buildMuscle
}
