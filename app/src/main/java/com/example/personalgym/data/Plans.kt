package com.example.personalgym.data

private fun ex(id: String, sets: Int, target: Int) = PlanExercise(id, sets, target)
private fun rest(day: Int, title: String = "Rest Day", focus: String = "Recover, stretch, sleep well") =
    PlanDay(day, title, focus, emptyList())
private fun walk(day: Int) = rest(day, "Rest Day", "Optional: easy 20–30 min walk")

/** Lays out training days on the given weekdays; every other day becomes a rest day. */
private fun week(vararg days: Pair<Int, PlanDay>, restDay: (Int) -> PlanDay = ::rest): List<PlanDay> {
    val byDay = days.toMap()
    return (1..7).map { d -> byDay[d]?.copy(dayOfWeek = d) ?: restDay(d) }
}

// ───────────── Fat loss workouts ─────────────
private val fullBodyCircuit = PlanDay(
    0, "Full Body Circuit", "Total body · fat burn",
    listOf(
        ex("goblet_squat", 3, 15), ex("push_up", 3, 12), ex("db_row", 3, 12),
        ex("kb_swing", 3, 20), ex("mountain_climber", 3, 40), ex("plank", 3, 45),
    ),
)
private val hiitCardio = PlanDay(
    0, "HIIT Cardio", "Intervals · conditioning",
    listOf(
        ex("treadmill_run", 1, 600), ex("jumping_jack", 4, 45), ex("burpee", 4, 10),
        ex("high_knees", 4, 30), ex("rowing_machine", 1, 600),
    ),
)
private val hiitCore = PlanDay(
    0, "HIIT + Core", "Intervals · abs",
    listOf(
        ex("jumping_jack", 4, 45), ex("burpee", 4, 10), ex("high_knees", 4, 30),
        ex("mountain_climber", 3, 40), ex("russian_twist", 3, 20), ex("crunch", 3, 20), ex("plank", 3, 45),
    ),
)
private val lowerBurn = PlanDay(
    0, "Lower Body Burn", "Legs · glutes · core",
    listOf(
        ex("leg_press", 4, 15), ex("walking_lunge", 3, 12), ex("leg_curl", 3, 15),
        ex("hip_thrust", 3, 15), ex("calf_raise_machine", 3, 20), ex("russian_twist", 3, 20),
    ),
)
private val upperCore = PlanDay(
    0, "Upper Body + Core", "Chest · back · arms",
    listOf(
        ex("db_bench", 3, 12), ex("lat_pulldown", 3, 12), ex("machine_shoulder_press", 3, 12),
        ex("cable_row", 3, 12), ex("tricep_pushdown", 3, 15), ex("db_curl", 3, 15), ex("crunch", 3, 20),
    ),
)
private val metabolic = PlanDay(
    0, "Metabolic Finisher", "Full body · max effort",
    listOf(
        ex("db_thruster", 4, 12), ex("burpee", 4, 10), ex("kb_swing", 4, 20),
        ex("mountain_climber", 4, 40), ex("rowing_machine", 1, 600), ex("plank", 3, 60),
    ),
)
private val activeRecovery = PlanDay(0, "Active Recovery", "Light cardio 30 min", listOf(ex("treadmill_run", 1, 1800)))

// ───────────── Muscle workouts ─────────────
private val fullBodyA = PlanDay(
    0, "Full Body A", "Squat · bench · row",
    listOf(
        ex("back_squat", 3, 8), ex("barbell_bench", 3, 8), ex("barbell_row", 3, 10),
        ex("db_shoulder_press", 3, 10), ex("plank", 3, 45),
    ),
)
private val fullBodyB = PlanDay(
    0, "Full Body B", "Deadlift · incline · pulldown",
    listOf(
        ex("deadlift", 3, 6), ex("incline_db_press", 3, 10), ex("lat_pulldown", 3, 10),
        ex("walking_lunge", 3, 10), ex("db_curl", 3, 12), ex("tricep_pushdown", 3, 12),
    ),
)
private val fullBodyC = PlanDay(
    0, "Full Body C", "Leg press · press · row",
    listOf(
        ex("leg_press", 3, 12), ex("db_bench", 3, 10), ex("cable_row", 3, 10),
        ex("lateral_raise", 3, 15), ex("hip_thrust", 3, 10), ex("crunch", 3, 20),
    ),
)
private val upperA = PlanDay(
    0, "Upper A", "Heavy press · row",
    listOf(
        ex("barbell_bench", 4, 8), ex("barbell_row", 4, 8), ex("machine_shoulder_press", 3, 10),
        ex("lat_pulldown", 3, 10), ex("tricep_pushdown", 3, 12), ex("barbell_curl", 3, 12),
    ),
)
private val lowerA = PlanDay(
    0, "Lower A", "Squat focus",
    listOf(
        ex("back_squat", 4, 8), ex("db_rdl", 3, 10), ex("leg_extension", 3, 12),
        ex("leg_curl", 3, 12), ex("calf_raise_machine", 4, 15),
    ),
)
private val upperB = PlanDay(
    0, "Upper B", "Incline · pull-ups · arms",
    listOf(
        ex("incline_db_press", 4, 10), ex("pull_up", 4, 8), ex("db_shoulder_press", 3, 10),
        ex("cable_row", 3, 10), ex("lateral_raise", 3, 15), ex("hammer_curl", 3, 12), ex("db_overhead_extension", 3, 12),
    ),
)
private val lowerB = PlanDay(
    0, "Lower B", "Deadlift focus",
    listOf(
        ex("deadlift", 4, 5), ex("bulgarian_split_squat", 3, 10), ex("leg_press", 3, 12),
        ex("hip_thrust", 3, 10), ex("db_calf_raise", 3, 15), ex("plank", 3, 60),
    ),
)
private val push = PlanDay(
    0, "Push Day", "Chest · shoulders · triceps",
    listOf(
        ex("barbell_bench", 4, 8), ex("incline_db_press", 3, 10), ex("machine_shoulder_press", 3, 10),
        ex("lateral_raise", 3, 15), ex("cable_crossover", 3, 12), ex("tricep_pushdown", 3, 12),
    ),
)
private val pull = PlanDay(
    0, "Pull Day", "Back · rear delts · biceps",
    listOf(
        ex("deadlift", 4, 6), ex("lat_pulldown", 4, 10), ex("cable_row", 3, 10),
        ex("barbell_row", 3, 10), ex("face_pull", 3, 15), ex("barbell_curl", 3, 10), ex("hammer_curl", 3, 12),
    ),
)
private val legs = PlanDay(
    0, "Leg Day", "Quads · hamstrings · glutes · calves",
    listOf(
        ex("back_squat", 4, 8), ex("leg_press", 3, 12), ex("leg_extension", 3, 12),
        ex("leg_curl", 3, 12), ex("hip_thrust", 3, 10), ex("calf_raise_machine", 4, 15),
    ),
)
private val upperDb = PlanDay(
    0, "Upper Body", "Chest · back · shoulders · arms",
    listOf(
        ex("db_bench", 4, 10), ex("pull_up", 4, 8), ex("db_shoulder_press", 3, 10),
        ex("db_row", 3, 10), ex("pec_deck", 3, 12), ex("db_curl", 3, 12), ex("db_overhead_extension", 3, 12),
    ),
)
private val lowerCore = PlanDay(
    0, "Lower Body + Core", "Hamstrings · glutes · abs",
    listOf(
        ex("db_rdl", 4, 10), ex("bulgarian_split_squat", 3, 10), ex("goblet_squat", 3, 12),
        ex("db_calf_raise", 3, 15), ex("plank", 3, 60), ex("crunch", 3, 20),
    ),
)

object Plans {
    private const val FAT_LOSS_DESC = "Higher reps and short rests keep your heart rate up to burn more calories."
    private const val MUSCLE_DESC = "Moderate reps (6–12) with heavier weight and longer rests. Add weight when you hit the top of the rep range."

    val fatLoss3 = WorkoutPlan(
        "fat_loss_3", "Fat Loss Starter", PlanGoal.FAT_LOSS,
        "3 full-body days: a circuit, intervals and a finisher. $FAT_LOSS_DESC",
        restSeconds = 45, daysPerWeek = 3,
        days = week(1 to fullBodyCircuit, 3 to hiitCore, 5 to metabolic, restDay = ::walk),
    )
    val fatLoss4 = WorkoutPlan(
        "fat_loss_4", "Fat Loss Split", PlanGoal.FAT_LOSS,
        "4 days: lower and upper strength, a full-body circuit and HIIT. $FAT_LOSS_DESC",
        restSeconds = 45, daysPerWeek = 4,
        days = week(1 to lowerBurn, 2 to upperCore, 4 to fullBodyCircuit, 5 to hiitCardio, restDay = ::walk),
    )
    val fatLoss5 = WorkoutPlan(
        "fat_loss", "Fat Loss Shred", PlanGoal.FAT_LOSS,
        "5 training days of circuits, HIIT and conditioning, plus active recovery. $FAT_LOSS_DESC",
        restSeconds = 45, daysPerWeek = 5,
        days = week(
            1 to fullBodyCircuit, 2 to hiitCardio, 3 to lowerBurn, 4 to upperCore, 5 to metabolic, 6 to activeRecovery,
        ),
    )
    val muscle3 = WorkoutPlan(
        "build_muscle_3", "Full Body Builder", PlanGoal.BUILD_MUSCLE,
        "3 full-body days that hit every muscle twice a week or more. Great for beginners. $MUSCLE_DESC",
        restSeconds = 90, daysPerWeek = 3,
        days = week(1 to fullBodyA, 3 to fullBodyB, 5 to fullBodyC),
    )
    val muscle4 = WorkoutPlan(
        "build_muscle_4", "Upper / Lower", PlanGoal.BUILD_MUSCLE,
        "4 days alternating upper and lower body, each trained twice a week. $MUSCLE_DESC",
        restSeconds = 90, daysPerWeek = 4,
        days = week(1 to upperA, 2 to lowerA, 4 to upperB, 5 to lowerB),
    )
    val muscle5 = WorkoutPlan(
        "build_muscle", "Muscle Builder", PlanGoal.BUILD_MUSCLE,
        "Push / Pull / Legs + Upper / Lower over 5 days. $MUSCLE_DESC",
        restSeconds = 90, daysPerWeek = 5,
        days = week(1 to push, 2 to pull, 3 to legs, 5 to upperDb, 6 to lowerCore),
    )

    val all = listOf(fatLoss3, fatLoss4, fatLoss5, muscle3, muscle4, muscle5)
    val dayOptions = listOf(3, 4, 5)

    fun get(id: String?): WorkoutPlan = all.firstOrNull { it.id == id } ?: muscle4

    fun base(goal: PlanGoal, days: Int): WorkoutPlan =
        all.firstOrNull { it.goal == goal && it.daysPerWeek == days } ?: all.first { it.goal == goal }

    /**
     * The plan for a profile. Beginners get one set fewer on big lifts (4+ sets)
     * and 15 s more rest, to build the habit without overdoing volume.
     */
    fun forProfile(p: Profile, edits: DayEdits = emptyMap()): WorkoutPlan {
        val plan = base(p.goal, p.daysPerWeek)
        val leveled = if (p.level == Level.INTERMEDIATE) plan else plan.copy(
            restSeconds = plan.restSeconds + 15,
            days = plan.days.map { d ->
                d.copy(exercises = d.exercises.map { if (it.sets >= 4) it.copy(sets = it.sets - 1) else it })
            },
        )
        return applyEdits(leveled, edits)
    }

    /** User edits replace a day's exercises as-is (the beginner adjustment doesn't touch them). */
    private fun applyEdits(plan: WorkoutPlan, edits: DayEdits): WorkoutPlan {
        if (edits.isEmpty()) return plan
        return plan.copy(
            days = plan.days.map { d ->
                val custom = edits[d.dayOfWeek] ?: return@map d
                when {
                    custom.isEmpty() -> rest(d.dayOfWeek).copy(edited = true)
                    d.isRest -> PlanDay(d.dayOfWeek, "Extra Workout", "Your own session", custom, edited = true)
                    else -> d.copy(exercises = custom, edited = true)
                }
            },
        )
    }

    /** Best guess at a profile for data saved before onboarding existed. */
    fun profileFor(planId: String?, level: Level = Level.INTERMEDIATE): Profile {
        val plan = get(planId)
        return Profile(plan.goal, level, plan.daysPerWeek)
    }
}
