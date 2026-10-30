package com.example.personalgym.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Small JSON-in-SharedPreferences store. Plenty for a personal, single-device app. */
class Storage(context: Context) {
    private val prefs = context.getSharedPreferences("personal_gym", Context.MODE_PRIVATE)

    var activePlanId: String?
        get() = prefs.getString("active_plan", null)
        set(v) = prefs.edit().putString("active_plan", v).apply()

    /** Saved onboarding answers, or null if onboarding hasn't been completed yet. */
    var profile: Profile?
        get() {
            if (!prefs.getBoolean("onboarded", false)) return null
            return runCatching {
                Profile(
                    PlanGoal.valueOf(prefs.getString("goal", null)!!),
                    Level.valueOf(prefs.getString("level", null)!!),
                    prefs.getInt("days", 4),
                )
            }.getOrNull()
        }
        set(p) {
            prefs.edit().apply {
                if (p == null) putBoolean("onboarded", false)
                else putBoolean("onboarded", true).putString("goal", p.goal.name)
                    .putString("level", p.level.name).putInt("days", p.daysPerWeek)
            }.apply()
        }

    var dumbbellOnly: Boolean
        get() = prefs.getBoolean("dumbbell_only", false)
        set(v) = prefs.edit().putBoolean("dumbbell_only", v).apply()

    var weightUnit: WeightUnit
        get() = WeightUnit.entries.firstOrNull { it.name == prefs.getString("weight_unit", null) } ?: WeightUnit.KG
        set(v) = prefs.edit().putString("weight_unit", v.name).apply()

    var themeMode: ThemeMode
        get() = ThemeMode.entries.firstOrNull { it.name == prefs.getString("theme_mode", null) } ?: ThemeMode.DARK
        set(v) = prefs.edit().putString("theme_mode", v.name).apply()

    var reminderEnabled: Boolean
        get() = prefs.getBoolean("reminder_on", false)
        set(v) = prefs.edit().putBoolean("reminder_on", v).apply()

    /** Reminder time as minutes after midnight (default 18:00). */
    var reminderMinutes: Int
        get() = prefs.getInt("reminder_min", 18 * 60)
        set(v) = prefs.edit().putInt("reminder_min", v).apply()

    var lastBackupAt: Long
        get() = prefs.getLong("last_backup", 0L)
        set(v) = prefs.edit().putLong("last_backup", v).apply()

    /** Plan id → that plan's edited days. */
    fun loadPlanEdits(): Map<String, DayEdits> =
        prefs.getString("plan_edits", null)?.let { runCatching { planEditsFromJson(JSONObject(it)) }.getOrNull() }.orEmpty()
    fun savePlanEdits(edits: Map<String, DayEdits>) =
        prefs.edit().putString("plan_edits", planEditsToJson(edits).toString()).apply()

    fun loadSessions(): List<WorkoutSession> = readArray("sessions").map { it.toSession() }
    fun saveSessions(list: List<WorkoutSession>) = writeArray("sessions", list.map { it.toJson() })

    fun loadActive(): WorkoutSession? = prefs.getString("active_session", null)?.let {
        runCatching { JSONObject(it).toSession() }.getOrNull()
    }
    fun saveActive(s: WorkoutSession?) =
        prefs.edit().putString("active_session", s?.toJson()?.toString()).apply()

    fun loadGoals(): List<Goal> = readArray("goals").mapNotNull { runCatching { it.toGoal() }.getOrNull() }
    fun saveGoals(list: List<Goal>) = writeArray("goals", list.map { it.toJson() })

    fun loadMeasurements(): List<Measurement> = readArray("measurements").mapNotNull { runCatching { it.toMeasurement() }.getOrNull() }
    fun saveMeasurements(list: List<Measurement>) = writeArray("measurements", list.map { it.toJson() })

    fun loadPhotos(): List<ProgressPhoto> = readArray("photos").mapNotNull {
        runCatching { ProgressPhoto(it.getLong("d"), PhotoPose.valueOf(it.getString("pose")), it.getString("file")) }.getOrNull()
    }
    fun savePhotos(list: List<ProgressPhoto>) =
        writeArray("photos", list.map { JSONObject().put("d", it.date).put("pose", it.pose.name).put("file", it.file) })

    fun loadWeights(): List<WeightEntry> = readArray("weights").map { WeightEntry(it.getLong("d"), it.getDouble("kg")) }
    fun saveWeights(list: List<WeightEntry>) =
        writeArray("weights", list.map { JSONObject().put("d", it.date).put("kg", it.kg) })

    private fun readArray(key: String): List<JSONObject> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getJSONObject(it) }
        }.getOrDefault(emptyList())
    }

    private fun writeArray(key: String, items: List<JSONObject>) {
        val arr = JSONArray()
        items.forEach { arr.put(it) }
        prefs.edit().putString(key, arr.toString()).apply()
    }
}

/** Everything worth keeping, as written to / read from a backup file. */
data class Backup(
    val activePlanId: String?,
    val dumbbellOnly: Boolean,
    val sessions: List<WorkoutSession>,
    val goals: List<Goal>,
    val weights: List<WeightEntry>,
    val exportedAt: Long,
    /** Added after v1 launched; older backups don't have these. */
    val level: Level? = null,
    val planEdits: Map<String, DayEdits> = emptyMap(),
    val measurements: List<Measurement> = emptyList(),
)

object BackupFormat {
    private const val APP = "personal-gym"
    private const val VERSION = 1

    fun encode(b: Backup): String = JSONObject()
        .put("app", APP)
        .put("version", VERSION)
        .put("exportedAt", b.exportedAt)
        .put("activePlanId", b.activePlanId ?: "")
        .put("dumbbellOnly", b.dumbbellOnly)
        .put("level", b.level?.name ?: "")
        .put("planEdits", planEditsToJson(b.planEdits))
        .put("measurements", JSONArray().apply { b.measurements.forEach { put(it.toJson()) } })
        .put("sessions", JSONArray().apply { b.sessions.forEach { put(it.toJson()) } })
        .put("goals", JSONArray().apply { b.goals.forEach { put(it.toJson()) } })
        .put("weights", JSONArray().apply { b.weights.forEach { put(JSONObject().put("d", it.date).put("kg", it.kg)) } })
        .toString(2)

    /** Throws [IllegalArgumentException] with a readable message if the file isn't a valid backup. */
    fun decode(text: String): Backup {
        val o = runCatching { JSONObject(text) }.getOrElse { throw IllegalArgumentException("This file isn't valid JSON.") }
        require(o.optString("app") == APP) { "This isn't a Personal Gym backup file." }
        require(o.optInt("version") <= VERSION) { "This backup was made by a newer version of the app." }
        fun objects(key: String): List<JSONObject> {
            val arr = o.optJSONArray(key) ?: return emptyList()
            return (0 until arr.length()).map { arr.getJSONObject(it) }
        }
        return runCatching {
            Backup(
                activePlanId = o.optString("activePlanId").ifBlank { null },
                dumbbellOnly = o.optBoolean("dumbbellOnly"),
                sessions = objects("sessions").map { it.toSession() },
                goals = objects("goals").map { it.toGoal() },
                weights = objects("weights").map { WeightEntry(it.getLong("d"), it.getDouble("kg")) },
                exportedAt = o.optLong("exportedAt"),
                level = Level.entries.firstOrNull { it.name == o.optString("level") },
                planEdits = o.optJSONObject("planEdits")?.let(::planEditsFromJson).orEmpty(),
                measurements = objects("measurements").mapNotNull { runCatching { it.toMeasurement() }.getOrNull() },
            )
        }.getOrElse { throw IllegalArgumentException("The backup file is damaged and couldn't be read.") }
    }
}

private fun Measurement.toJson(): JSONObject = JSONObject().put("d", date).put("site", site.name).put("v", value)

private fun JSONObject.toMeasurement() = Measurement(getLong("d"), BodySite.valueOf(getString("site")), getDouble("v"))

private fun planEditsToJson(edits: Map<String, DayEdits>): JSONObject = JSONObject().apply {
    edits.forEach { (planId, days) ->
        put(planId, JSONObject().apply {
            days.forEach { (day, list) ->
                put(day.toString(), JSONArray().apply {
                    list.forEach { put(JSONObject().put("id", it.exerciseId).put("sets", it.sets).put("t", it.target)) }
                })
            }
        })
    }
}

private fun planEditsFromJson(o: JSONObject): Map<String, DayEdits> =
    o.keys().asSequence().associateWith { planId ->
        val days = o.getJSONObject(planId)
        days.keys().asSequence().associate { day ->
            val arr = days.getJSONArray(day)
            day.toInt() to (0 until arr.length()).map {
                val e = arr.getJSONObject(it)
                PlanExercise(e.getString("id"), e.getInt("sets"), e.getInt("t"))
            }
        }
    }

private fun WorkoutSession.toJson(): JSONObject = JSONObject()
    .put("id", id).put("start", startedAt).put("title", title).put("rest", restSeconds)
    .put("dur", durationSec).put("cal", calories)
    .put("ex", JSONArray().apply {
        exercises.forEach { e ->
            put(JSONObject().put("id", e.exerciseId).put("sets", JSONArray().apply {
                e.sets.forEach { s ->
                    put(JSONObject().put("t", s.target).put("r", s.reps).put("w", s.weight).put("d", s.done))
                }
            }))
        }
    })

private fun JSONObject.toSession(): WorkoutSession {
    val ex = getJSONArray("ex")
    return WorkoutSession(
        id = getLong("id"),
        startedAt = getLong("start"),
        title = getString("title"),
        restSeconds = optInt("rest", 60),
        durationSec = optLong("dur"),
        calories = optInt("cal"),
        exercises = (0 until ex.length()).map { i ->
            val e = ex.getJSONObject(i)
            val sets = e.getJSONArray("sets")
            ExerciseLog(e.getString("id"), (0 until sets.length()).map { j ->
                val s = sets.getJSONObject(j)
                SetLog(s.getInt("t"), s.getInt("r"), s.getDouble("w"), s.getBoolean("d"))
            })
        },
    )
}

private fun Goal.toJson(): JSONObject = JSONObject()
    .put("id", id).put("type", type.name).put("title", title).put("target", target)
    .put("start", start).put("ex", exerciseId ?: "").put("manual", manualProgress).put("created", createdAt)

private fun JSONObject.toGoal() = Goal(
    id = getLong("id"),
    type = GoalType.valueOf(getString("type")),
    title = getString("title"),
    target = getDouble("target"),
    start = optDouble("start", 0.0),
    exerciseId = optString("ex").ifBlank { null },
    manualProgress = optDouble("manual", 0.0),
    createdAt = optLong("created", System.currentTimeMillis()),
)
