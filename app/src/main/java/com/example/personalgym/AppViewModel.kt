package com.example.personalgym

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.example.personalgym.data.Backup
import com.example.personalgym.data.BackupFormat
import com.example.personalgym.data.Exercise
import com.example.personalgym.data.ExerciseLibrary
import com.example.personalgym.data.ExerciseLog
import com.example.personalgym.data.Goal
import com.example.personalgym.data.PlanDay
import com.example.personalgym.data.Plans
import com.example.personalgym.data.Profile
import com.example.personalgym.data.SetLog
import com.example.personalgym.data.Stats
import com.example.personalgym.data.Storage
import com.example.personalgym.data.TrackType
import com.example.personalgym.data.WeightEntry
import com.example.personalgym.data.WorkoutSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** A countdown shown at the bottom of the workout screen (rest between sets, or a timed set). */
data class Countdown(val label: String, val endsAt: Long, val totalSec: Int, val onFinish: FinishAction)

sealed interface FinishAction {
    data object None : FinishAction
    /** A timed set finished — mark it done and start resting. */
    data class CompleteSet(val exIndex: Int, val setIndex: Int) : FinishAction
}

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val storage = Storage(app)

    /** Onboarding answers; before onboarding, a guess from the old saved plan. */
    var profile by mutableStateOf(storage.profile ?: Plans.profileFor(storage.activePlanId))
        private set
    var needsOnboarding by mutableStateOf(storage.profile == null)
        private set
    var activePlan by mutableStateOf(Plans.forProfile(profile))
        private set
    var dumbbellOnly by mutableStateOf(storage.dumbbellOnly)
        private set
    var sessions by mutableStateOf(storage.loadSessions())
        private set
    var goals by mutableStateOf(storage.loadGoals())
        private set
    var weights by mutableStateOf(storage.loadWeights())
        private set
    var active by mutableStateOf(storage.loadActive())
        private set
    private var countdownState by mutableStateOf<Countdown?>(null)

    /** Every change is mirrored to the lock-screen notification + alarm. */
    var countdown: Countdown?
        get() = countdownState
        private set(value) {
            countdownState = value
            if (value != null) RestTimerAlerts.schedule(getApplication(), value) else RestTimerAlerts.cancel(getApplication())
        }

    // ---------- Settings ----------

    fun updateProfile(p: Profile) {
        profile = p
        activePlan = Plans.forProfile(p)
        storage.profile = p
        storage.activePlanId = activePlan.id
    }

    fun completeOnboarding(p: Profile, dumbbells: Boolean, bodyWeightKg: Double?) {
        updateProfile(p)
        updateDumbbellOnly(dumbbells)
        bodyWeightKg?.let(::addWeight)
        needsOnboarding = false
    }

    fun redoOnboarding() {
        needsOnboarding = true
    }

    /** Only someone who finished onboarding before can back out of redoing it. */
    val canCancelOnboarding: Boolean get() = storage.profile != null

    fun cancelOnboarding() {
        if (canCancelOnboarding) needsOnboarding = false
    }

    fun updateDumbbellOnly(value: Boolean) {
        dumbbellOnly = value
        storage.dumbbellOnly = value
    }

    /** Exercise to actually perform, honouring the dumbbell-only setting. */
    fun resolve(exerciseId: String): Exercise {
        val ex = ExerciseLibrary.get(exerciseId)
        return if (dumbbellOnly) ExerciseLibrary.alternativeFor(ex) ?: ex else ex
    }

    // ---------- Workout session ----------

    fun startWorkout(day: PlanDay) {
        val logs = day.exercises.map { pe ->
            val ex = resolve(pe.exerciseId)
            val w = Stats.suggestedWeight(sessions, ex.id)
            ExerciseLog(ex.id, List(pe.sets) { SetLog(pe.target, pe.target, w, false) })
        }
        begin(WorkoutSession(System.currentTimeMillis(), System.currentTimeMillis(), day.title, activePlan.restSeconds, logs))
    }

    fun startEmptyWorkout() {
        begin(WorkoutSession(System.currentTimeMillis(), System.currentTimeMillis(), "Custom Workout", 60, emptyList()))
    }

    private fun begin(session: WorkoutSession) {
        countdown = null
        storeActive(session)
    }

    private fun storeActive(s: WorkoutSession?) {
        active = s
        storage.saveActive(s)
    }

    private fun editExercises(block: (MutableList<ExerciseLog>) -> Unit) {
        val s = active ?: return
        val list = s.exercises.toMutableList()
        block(list)
        storeActive(s.copy(exercises = list))
    }

    fun updateSet(exIndex: Int, setIndex: Int, set: SetLog) = editExercises { list ->
        val sets = list[exIndex].sets.toMutableList()
        val oldWeight = sets[setIndex].weight
        sets[setIndex] = set
        // Typing a weight carries it to the later sets that still had the same value.
        if (set.weight != oldWeight) {
            for (i in setIndex + 1 until sets.size) {
                if (!sets[i].done && sets[i].weight == oldWeight) sets[i] = sets[i].copy(weight = set.weight)
            }
        }
        list[exIndex] = list[exIndex].copy(sets = sets)
    }

    fun toggleSetDone(exIndex: Int, setIndex: Int) {
        val s = active ?: return
        val set = s.exercises[exIndex].sets[setIndex]
        updateSet(exIndex, setIndex, set.copy(done = !set.done))
        if (!set.done) startRest() else countdown = null
    }

    fun startTimedSet(exIndex: Int, setIndex: Int) {
        val s = active ?: return
        val log = s.exercises[exIndex]
        val ex = ExerciseLibrary.get(log.exerciseId)
        val secs = log.sets[setIndex].reps.coerceAtLeast(5)
        countdown = Countdown(
            "${ex.name} · set ${setIndex + 1}",
            System.currentTimeMillis() + secs * 1000L, secs,
            FinishAction.CompleteSet(exIndex, setIndex),
        )
    }

    private fun startRest() {
        val secs = active?.restSeconds ?: 60
        countdown = Countdown("Rest", System.currentTimeMillis() + secs * 1000L, secs, FinishAction.None)
    }

    fun addCountdownTime(sec: Int) {
        countdown = countdown?.let { it.copy(endsAt = it.endsAt + sec * 1000L, totalSec = it.totalSec + sec) }
    }

    fun skipCountdown() {
        countdown = null
    }

    /** Called by the UI when the countdown reaches zero. */
    fun onCountdownFinished() {
        val c = countdown ?: return
        countdown = null
        when (val a = c.onFinish) {
            FinishAction.None -> Unit
            is FinishAction.CompleteSet -> {
                val set = active?.exercises?.getOrNull(a.exIndex)?.sets?.getOrNull(a.setIndex) ?: return
                if (!set.done) updateSet(a.exIndex, a.setIndex, set.copy(done = true))
                startRest()
            }
        }
    }

    fun addSet(exIndex: Int) = editExercises { list ->
        val last = list[exIndex].sets.lastOrNull()
        val target = last?.target ?: defaultTarget(list[exIndex].exerciseId)
        list[exIndex] = list[exIndex].copy(
            sets = list[exIndex].sets + SetLog(target, last?.reps ?: target, last?.weight ?: 0.0, false),
        )
    }

    fun removeSet(exIndex: Int, setIndex: Int) = editExercises { list ->
        list[exIndex] = list[exIndex].copy(sets = list[exIndex].sets.filterIndexed { i, _ -> i != setIndex })
    }

    fun addExercise(exerciseId: String) = editExercises { list ->
        val t = defaultTarget(exerciseId)
        val w = Stats.suggestedWeight(sessions, exerciseId)
        list += ExerciseLog(exerciseId, List(3) { SetLog(t, t, w, false) })
    }

    fun removeExercise(exIndex: Int) = editExercises { it.removeAt(exIndex) }

    fun swapExercise(exIndex: Int, newId: String) = editExercises { list ->
        val w = Stats.suggestedWeight(sessions, newId)
        val newEx = ExerciseLibrary.get(newId)
        list[exIndex] = ExerciseLog(newId, list[exIndex].sets.map {
            it.copy(weight = if (newEx.usesWeight) w else 0.0, done = false)
        })
    }

    /** Swap targets: the dumbbell alternative, or the machine version(s) a dumbbell move replaces. */
    fun swapOptions(exerciseId: String): List<Exercise> {
        val ex = ExerciseLibrary.get(exerciseId)
        val alt = ExerciseLibrary.alternativeFor(ex)
        val originals = ExerciseLibrary.all.filter { it.alternativeId == ex.id }
        return listOfNotNull(alt) + originals
    }

    private fun defaultTarget(exerciseId: String) =
        if (ExerciseLibrary.get(exerciseId).track == TrackType.TIME) 30 else 10

    fun finishWorkout(): WorkoutSession? {
        val s = active ?: return null
        val now = System.currentTimeMillis()
        val duration = (now - s.startedAt) / 1000
        val bodyKg = weights.maxByOrNull { it.date }?.kg ?: 70.0
        // MET-based estimate, weighted by how many sets of each exercise were completed.
        val doneSets = s.exercises.flatMap { e -> e.sets.filter { it.done }.map { ExerciseLibrary.get(e.exerciseId).met } }
        val avgMet = if (doneSets.isEmpty()) 3.5 else doneSets.average()
        val calories = (avgMet * bodyKg * duration / 3600.0).roundToInt()
        val cleaned = s.copy(
            durationSec = duration,
            calories = calories,
            exercises = s.exercises.filter { it.sets.isNotEmpty() },
        )
        sessions = sessions + cleaned
        storage.saveSessions(sessions)
        countdown = null
        storeActive(null)
        return cleaned
    }

    fun discardWorkout() {
        countdown = null
        storeActive(null)
    }

    fun deleteSession(id: Long) {
        sessions = sessions.filterNot { it.id == id }
        storage.saveSessions(sessions)
    }

    // ---------- Goals & body weight ----------

    fun addGoal(goal: Goal) {
        goals = goals + goal
        storage.saveGoals(goals)
    }

    fun deleteGoal(id: Long) {
        goals = goals.filterNot { it.id == id }
        storage.saveGoals(goals)
    }

    fun adjustManualGoal(id: Long, delta: Double) {
        goals = goals.map { if (it.id == id) it.copy(manualProgress = (it.manualProgress + delta).coerceAtLeast(0.0)) else it }
        storage.saveGoals(goals)
    }

    fun addWeight(kg: Double) {
        weights = (weights + WeightEntry(System.currentTimeMillis(), kg)).sortedBy { it.date }
        storage.saveWeights(weights)
    }

    val latestWeight: Double? get() = weights.maxByOrNull { it.date }?.kg

    // ---------- Backup & restore ----------

    var lastBackupAt by mutableStateOf(storage.lastBackupAt)
        private set

    fun currentBackup() =
        Backup(activePlan.id, dumbbellOnly, sessions, goals, weights, System.currentTimeMillis(), profile.level)

    /** Writes all data to [uri] (picked by the user). Returns the number of workouts saved. */
    suspend fun exportBackup(uri: Uri): Int {
        val backup = currentBackup()
        val text = BackupFormat.encode(backup)
        withContext(Dispatchers.IO) {
            val out = getApplication<Application>().contentResolver.openOutputStream(uri, "wt")
                ?: throw IllegalStateException("Couldn't open the file for writing.")
            out.use { it.write(text.toByteArray()) }
        }
        lastBackupAt = backup.exportedAt
        storage.lastBackupAt = backup.exportedAt
        return backup.sessions.size
    }

    /** Reads and validates a backup file without changing anything yet. */
    suspend fun readBackup(uri: Uri): Backup = withContext(Dispatchers.IO) {
        val input = getApplication<Application>().contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Couldn't open the file.")
        BackupFormat.decode(input.use { it.readBytes().decodeToString() })
    }

    /** Replaces all saved data with [b]. A workout in progress is left alone. */
    fun restore(b: Backup) {
        sessions = b.sessions.sortedBy { it.startedAt }
        goals = b.goals
        weights = b.weights.sortedBy { it.date }
        storage.saveSessions(sessions)
        storage.saveGoals(goals)
        storage.saveWeights(weights)
        updateProfile(Plans.profileFor(b.activePlanId, b.level ?: profile.level))
        updateDumbbellOnly(b.dumbbellOnly)
        // A restored backup already has the user's choices, so there's nothing to set up.
        needsOnboarding = false
    }
}
