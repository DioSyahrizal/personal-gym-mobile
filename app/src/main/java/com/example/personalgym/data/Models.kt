package com.example.personalgym.data

import com.example.personalgym.ui.illustration.Illustration

enum class Muscle(val label: String) {
    CHEST("Chest"),
    BACK("Back"),
    LEGS("Legs"),
    SHOULDERS("Shoulders"),
    ARMS("Arms"),
    CORE("Core"),
    CARDIO("Cardio"),
}

enum class Equipment(val label: String) {
    BODYWEIGHT("Bodyweight"),
    DUMBBELL("Dumbbell"),
    BARBELL("Barbell"),
    MACHINE("Machine"),
    CABLE("Cable"),
    KETTLEBELL("Kettlebell"),
}

/** Reps-based sets log reps (+ weight); time-based sets log seconds. */
enum class TrackType { REPS, TIME }

data class Exercise(
    val id: String,
    val name: String,
    val muscle: Muscle,
    val equipment: Equipment,
    val illustration: Illustration,
    val steps: List<String>,
    val tip: String,
    /** Dumbbell (or bodyweight) substitute when this move needs a machine, cable, barbell or kettlebell. */
    val alternativeId: String? = null,
    val track: TrackType = TrackType.REPS,
    /** Metabolic equivalent, used for a rough calorie estimate. */
    val met: Double = 5.0,
    /** Created by the user (see [CustomExercise]); uses a generic figure for its muscle group. */
    val isCustom: Boolean = false,
) {
    val usesWeight: Boolean get() = track == TrackType.REPS && equipment != Equipment.BODYWEIGHT
}

enum class PlanGoal(val label: String) { FAT_LOSS("Fat Loss"), BUILD_MUSCLE("Build Muscle") }

enum class Level(val label: String, val blurb: String) {
    BEGINNER("Beginner", "New to the gym or coming back after a long break"),
    INTERMEDIATE("Intermediate", "Training regularly for 6+ months"),
}

/** Answers from onboarding; together they pick the weekly plan. */
data class Profile(
    val goal: PlanGoal = PlanGoal.BUILD_MUSCLE,
    val level: Level = Level.INTERMEDIATE,
    val daysPerWeek: Int = 4,
)

data class PlanExercise(
    val exerciseId: String,
    val sets: Int,
    /** Reps for REPS exercises, seconds for TIME exercises. */
    val target: Int,
)

data class PlanDay(
    val dayOfWeek: Int, // 1 = Monday … 7 = Sunday
    val title: String,
    val focus: String,
    val exercises: List<PlanExercise>,
    /** True when the user changed this day from the built-in plan. */
    val edited: Boolean = false,
) {
    val isRest: Boolean get() = exercises.isEmpty()
}

/** User changes to a plan: day of week → that day's exercise list. */
typealias DayEdits = Map<Int, List<PlanExercise>>

data class WorkoutPlan(
    val id: String,
    val name: String,
    val goal: PlanGoal,
    val description: String,
    val restSeconds: Int,
    val daysPerWeek: Int,
    val days: List<PlanDay>,
)

// ---- Logged data ----

data class SetLog(
    val target: Int,
    val reps: Int,
    val weight: Double,
    val done: Boolean,
)

data class ExerciseLog(
    val exerciseId: String,
    val sets: List<SetLog>,
    /** Superset with the exercise after this one: no rest between them, rest after the group. */
    val supersetNext: Boolean = false,
)

data class WorkoutSession(
    val id: Long,
    val startedAt: Long,
    val title: String,
    val restSeconds: Int,
    val exercises: List<ExerciseLog>,
    val durationSec: Long = 0,
    val calories: Int = 0,
) {
    val completedSets: Int get() = exercises.sumOf { e -> e.sets.count { it.done } }
    val totalSets: Int get() = exercises.sumOf { it.sets.size }
    val volume: Double
        get() = exercises.sumOf { e -> e.sets.filter { it.done }.sumOf { it.reps * it.weight } }
}

enum class GoalType(val label: String, val unit: String) {
    WEEKLY_WORKOUTS("Workouts per week", "workouts"),
    TOTAL_WORKOUTS("Total workouts", "workouts"),
    BODY_WEIGHT("Reach body weight", "kg"),
    LIFT("Lift a weight", "kg"),
    CUSTOM("Custom goal", ""),
}

data class Goal(
    val id: Long,
    val type: GoalType,
    val title: String,
    val target: Double,
    /** Body weight when the goal was created (for BODY_WEIGHT progress). */
    val start: Double = 0.0,
    val exerciseId: String? = null,
    val manualProgress: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
)

data class WeightEntry(val date: Long, val kg: Double)

enum class BodySite(val label: String, val isPercent: Boolean = false) {
    WAIST("Waist"),
    CHEST("Chest"),
    HIPS("Hips"),
    ARM("Arm"),
    THIGH("Thigh"),
    NECK("Neck"),
    BODY_FAT("Body fat", isPercent = true),
}

/**
 * A user-made exercise. Ids are "custom_<timestamp>" and, like built-in ids, are kept forever:
 * deleting one only [archived]s it so past workouts still show its name.
 */
data class CustomExercise(
    val id: String,
    val name: String,
    val muscle: Muscle,
    val equipment: Equipment,
    val track: TrackType = TrackType.REPS,
    val notes: String = "",
    val archived: Boolean = false,
)

enum class PhotoPose(val label: String) { FRONT("Front"), SIDE("Side"), BACK("Back") }

/** [file] is a name inside the app's private photos folder. */
data class ProgressPhoto(val date: Long, val pose: PhotoPose, val file: String)

/** [value] is in cm, or percent for [BodySite.BODY_FAT]. */
data class Measurement(val date: Long, val site: BodySite, val value: Double)
