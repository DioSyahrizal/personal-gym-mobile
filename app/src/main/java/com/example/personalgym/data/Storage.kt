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

    var dumbbellOnly: Boolean
        get() = prefs.getBoolean("dumbbell_only", false)
        set(v) = prefs.edit().putBoolean("dumbbell_only", v).apply()

    var lastBackupAt: Long
        get() = prefs.getLong("last_backup", 0L)
        set(v) = prefs.edit().putLong("last_backup", v).apply()

    fun loadSessions(): List<WorkoutSession> = readArray("sessions").map { it.toSession() }
    fun saveSessions(list: List<WorkoutSession>) = writeArray("sessions", list.map { it.toJson() })

    fun loadActive(): WorkoutSession? = prefs.getString("active_session", null)?.let {
        runCatching { JSONObject(it).toSession() }.getOrNull()
    }
    fun saveActive(s: WorkoutSession?) =
        prefs.edit().putString("active_session", s?.toJson()?.toString()).apply()

    fun loadGoals(): List<Goal> = readArray("goals").mapNotNull { runCatching { it.toGoal() }.getOrNull() }
    fun saveGoals(list: List<Goal>) = writeArray("goals", list.map { it.toJson() })

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
            )
        }.getOrElse { throw IllegalArgumentException("The backup file is damaged and couldn't be read.") }
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
