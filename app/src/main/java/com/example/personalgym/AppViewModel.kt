package com.example.personalgym

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.example.personalgym.data.Backup
import com.example.personalgym.data.BackupFormat
import com.example.personalgym.data.BodySite
import com.example.personalgym.data.CustomExercise
import com.example.personalgym.data.DayEdits
import com.example.personalgym.data.Exercise
import com.example.personalgym.data.ExerciseLibrary
import com.example.personalgym.data.ExerciseLog
import com.example.personalgym.data.Goal
import com.example.personalgym.data.Measurement
import com.example.personalgym.data.PhotoPose
import com.example.personalgym.data.PhotoStore
import com.example.personalgym.data.PlanDay
import com.example.personalgym.data.PlanExercise
import com.example.personalgym.data.Plans
import com.example.personalgym.data.Profile
import com.example.personalgym.data.ProgressPhoto
import com.example.personalgym.data.SetLog
import com.example.personalgym.data.Stats
import com.example.personalgym.data.Storage
import com.example.personalgym.data.Supersets
import com.example.personalgym.data.ThemeMode
import com.example.personalgym.data.TrackType
import com.example.personalgym.data.WeightUnit
import com.example.personalgym.data.WeightEntry
import com.example.personalgym.data.WorkoutSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A countdown shown at the bottom of the workout screen (rest between sets, or a timed set). */
data class Countdown(val label: String, val endsAt: Long, val totalSec: Int, val onFinish: FinishAction)

sealed interface FinishAction {
    data object None : FinishAction
    /** A timed set finished — mark it done and start resting. */
    data class CompleteSet(val exIndex: Int, val setIndex: Int) : FinishAction
}

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val storage = Storage(app)

    /** Registered with [ExerciseLibrary] first, so history and plans can look custom moves up by id. */
    var customExercises by mutableStateOf(storage.loadCustomExercises().also(ExerciseLibrary::setCustom))
        private set

    /** Onboarding answers; before onboarding, a guess from the old saved plan. */
    var profile by mutableStateOf(storage.profile ?: Plans.profileFor(storage.activePlanId))
        private set
    var needsOnboarding by mutableStateOf(storage.profile == null)
        private set
    private var planEdits by mutableStateOf(storage.loadPlanEdits())
    var activePlan by mutableStateOf(planFor(profile))
        private set

    private fun planFor(p: Profile) = Plans.forProfile(p, planEdits[Plans.base(p.goal, p.daysPerWeek).id].orEmpty())
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
        activePlan = planFor(p)
        storage.profile = p
        storage.activePlanId = activePlan.id
    }

    // ---------- Plan editing ----------

    /** Replaces one day of the active plan with [exercises] (empty = make it a rest day). */
    fun saveDayEdit(dayOfWeek: Int, exercises: List<PlanExercise>) {
        val planId = activePlan.id
        val days = planEdits[planId].orEmpty() + (dayOfWeek to exercises)
        storePlanEdits(planEdits + (planId to days))
    }

    /** Puts a day back to the built-in plan. */
    fun resetDay(dayOfWeek: Int) {
        val planId = activePlan.id
        val days = planEdits[planId].orEmpty() - dayOfWeek
        storePlanEdits(if (days.isEmpty()) planEdits - planId else planEdits + (planId to days))
    }

    /** The built-in version of a day, for showing what "reset" goes back to. */
    fun defaultDay(dayOfWeek: Int): PlanDay = Plans.forProfile(profile).days.first { it.dayOfWeek == dayOfWeek }

    private fun storePlanEdits(edits: Map<String, DayEdits>) {
        planEdits = edits
        storage.savePlanEdits(edits)
        activePlan = planFor(profile)
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

    var weightUnit by mutableStateOf(storage.weightUnit)
        private set
    var themeMode by mutableStateOf(storage.themeMode)
        private set

    fun updateWeightUnit(value: WeightUnit) {
        weightUnit = value
        storage.weightUnit = value
    }

    fun updateThemeMode(value: ThemeMode) {
        themeMode = value
        storage.themeMode = value
    }

    var reminderEnabled by mutableStateOf(storage.reminderEnabled)
        private set
    var reminderMinutes by mutableStateOf(storage.reminderMinutes)
        private set

    fun updateReminder(enabled: Boolean, minutes: Int = reminderMinutes) {
        reminderEnabled = enabled
        reminderMinutes = minutes
        storage.reminderEnabled = enabled
        storage.reminderMinutes = minutes
        WorkoutReminders.schedule(getApplication())
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
            val w = Stats.suggestedWeight(sessions, ex.id, weightUnit)
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

    /** Set while you move between superset exercises instead of resting ("Now: Lateral Raise"). */
    var supersetHint by mutableStateOf<String?>(null)
        private set

    fun toggleSetDone(exIndex: Int, setIndex: Int) {
        val s = active ?: return
        val set = s.exercises[exIndex].sets[setIndex]
        updateSet(exIndex, setIndex, set.copy(done = !set.done))
        if (!set.done) afterSet(exIndex) else { countdown = null; supersetHint = null }
    }

    /** Rest after a set, unless the exercise is linked to the next one in a superset. */
    private fun afterSet(exIndex: Int) {
        val exercises = active?.exercises ?: return
        val next = Supersets.nextInGroup(exercises, exIndex)
        if (next == null) {
            startRest()
        } else {
            countdown = null
            supersetHint = ExerciseLibrary.get(exercises[next].exerciseId).name
        }
    }

    fun dismissSupersetHint() {
        supersetHint = null
    }

    /** Links (or unlinks) an exercise with the one after it. */
    fun toggleSuperset(exIndex: Int) = editExercises { list ->
        if (exIndex < list.lastIndex) list[exIndex] = list[exIndex].copy(supersetNext = !list[exIndex].supersetNext)
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
        supersetHint = null
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
                afterSet(a.exIndex)
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
        val w = Stats.suggestedWeight(sessions, exerciseId, weightUnit)
        list += ExerciseLog(exerciseId, List(3) { SetLog(t, t, w, false) })
    }

    fun removeExercise(exIndex: Int) = editExercises { list ->
        // The exercise before it keeps its link only if the removed one passed it on.
        val passedOn = list[exIndex].supersetNext
        list.removeAt(exIndex)
        if (exIndex > 0 && !passedOn) list[exIndex - 1] = list[exIndex - 1].copy(supersetNext = false)
    }

    fun swapExercise(exIndex: Int, newId: String) = editExercises { list ->
        val w = Stats.suggestedWeight(sessions, newId, weightUnit)
        val newEx = ExerciseLibrary.get(newId)
        list[exIndex] = list[exIndex].copy(exerciseId = newId, sets = list[exIndex].sets.map {
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
        val calories = Stats.estimateCalories(s.exercises, duration, latestWeight ?: 70.0)
        val cleaned = s.copy(
            durationSec = duration,
            calories = calories,
            exercises = s.exercises.filter { it.sets.isNotEmpty() },
        )
        sessions = sessions + cleaned
        storage.saveSessions(sessions)
        countdown = null
        supersetHint = null
        storeActive(null)
        return cleaned
    }

    fun discardWorkout() {
        countdown = null
        supersetHint = null
        storeActive(null)
    }

    /** Saves corrections to a finished workout; empty exercises are dropped and calories re-estimated. */
    fun updateSession(edited: WorkoutSession) {
        val exercises = edited.exercises.filter { it.sets.isNotEmpty() }
        val fixed = edited.copy(
            exercises = exercises,
            calories = Stats.estimateCalories(exercises, edited.durationSec, latestWeight ?: 70.0),
        )
        sessions = sessions.map { if (it.id == fixed.id) fixed else it }
        storage.saveSessions(sessions)
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

    /** Entries are identified by their timestamp; editing keeps the original date. */
    fun updateWeight(date: Long, kg: Double) {
        weights = weights.map { if (it.date == date) it.copy(kg = kg) else it }
        storage.saveWeights(weights)
    }

    fun deleteWeight(date: Long) {
        weights = weights.filterNot { it.date == date }
        storage.saveWeights(weights)
    }

    val latestWeight: Double? get() = weights.maxByOrNull { it.date }?.kg

    // ---------- Custom exercises ----------

    fun saveCustomExercise(ex: CustomExercise) {
        val exists = customExercises.any { it.id == ex.id }
        storeCustomExercises(if (exists) customExercises.map { if (it.id == ex.id) ex else it } else customExercises + ex)
    }

    /** Hidden from pickers but kept, so past workouts and plan edits still show its name. */
    fun archiveCustomExercise(id: String) =
        storeCustomExercises(customExercises.map { if (it.id == id) it.copy(archived = true) else it })

    private fun storeCustomExercises(list: List<CustomExercise>) {
        customExercises = list
        ExerciseLibrary.setCustom(list)
        storage.saveCustomExercises(list)
    }

    // ---------- Progress photos (on this phone only; not in backups) ----------

    var photos by mutableStateOf(storage.loadPhotos())
        private set

    suspend fun addPhoto(source: Uri, pose: PhotoPose) {
        val photo = PhotoStore.import(getApplication(), source, pose)
        photos = (photos + photo).sortedBy { it.date }
        storage.savePhotos(photos)
    }

    fun deletePhoto(photo: ProgressPhoto) {
        PhotoStore.delete(getApplication(), photo.file)
        photos = photos - photo
        storage.savePhotos(photos)
    }

    // ---------- Body measurements ----------

    var measurements by mutableStateOf(storage.loadMeasurements())
        private set

    /** Logs several sites at once; they share one timestamp so they read as one check-in. */
    fun addMeasurements(values: Map<BodySite, Double>) {
        if (values.isEmpty()) return
        val now = System.currentTimeMillis()
        storeMeasurements(measurements + values.map { (site, v) -> Measurement(now, site, v) })
    }

    fun updateMeasurement(date: Long, site: BodySite, value: Double) =
        storeMeasurements(measurements.map { if (it.date == date && it.site == site) it.copy(value = value) else it })

    fun deleteMeasurement(date: Long, site: BodySite) =
        storeMeasurements(measurements.filterNot { it.date == date && it.site == site })

    private fun storeMeasurements(list: List<Measurement>) {
        measurements = list.sortedBy { it.date }
        storage.saveMeasurements(measurements)
    }

    // ---------- Backup & restore ----------

    var lastBackupAt by mutableStateOf(storage.lastBackupAt)
        private set

    fun currentBackup() =
        Backup(activePlan.id, dumbbellOnly, sessions, goals, weights, System.currentTimeMillis(), profile.level, planEdits, measurements, customExercises)

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
        storeCustomExercises(b.customExercises) // before sessions, which may refer to them
        sessions = b.sessions.sortedBy { it.startedAt }
        goals = b.goals
        weights = b.weights.sortedBy { it.date }
        storage.saveSessions(sessions)
        storage.saveGoals(goals)
        storage.saveWeights(weights)
        planEdits = b.planEdits
        storeMeasurements(b.measurements)
        storage.savePlanEdits(b.planEdits)
        updateProfile(Plans.profileFor(b.activePlanId, b.level ?: profile.level))
        updateDumbbellOnly(b.dumbbellOnly)
        // A restored backup already has the user's choices, so there's nothing to set up.
        needsOnboarding = false
    }
}
