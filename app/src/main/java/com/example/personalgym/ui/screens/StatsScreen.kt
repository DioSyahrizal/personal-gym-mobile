package com.example.personalgym.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.personalgym.AppViewModel
import com.example.personalgym.data.Stats
import com.example.personalgym.data.WorkoutSession
import com.example.personalgym.ui.Nav
import com.example.personalgym.ui.components.AppCard
import com.example.personalgym.ui.components.BarChart
import com.example.personalgym.ui.components.ExerciseThumb
import com.example.personalgym.ui.components.LineChart
import com.example.personalgym.ui.components.ProgressBar
import com.example.personalgym.ui.components.SectionTitle
import com.example.personalgym.ui.components.StatTile
import com.example.personalgym.ui.theme.Coral
import com.example.personalgym.ui.theme.Sky
import com.example.personalgym.ui.theme.Violet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class ChartMode(val label: String) { WORKOUTS("Workouts"), VOLUME("Volume"), MINUTES("Minutes") }

@Composable
fun StatsScreen(vm: AppViewModel, nav: Nav) {
    val sessions = vm.sessions
    var mode by rememberSaveable { mutableStateOf(ChartMode.WORKOUTS) }
    var toDelete by remember { mutableStateOf<WorkoutSession?>(null) }
    val weeks = Stats.lastWeeks(sessions)
    val weekFmt = SimpleDateFormat("d/M", Locale.getDefault())

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Text("Progress", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("Total workouts", "${sessions.size}", Modifier.weight(1f))
            StatTile("Hours trained", "%.1f".format(sessions.sumOf { it.durationSec } / 3600.0), Modifier.weight(1f), accent = Sky)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("Total volume (kg)", compact(sessions.sumOf { it.volume }), Modifier.weight(1f), accent = Coral)
            StatTile("Calories burned", compact(sessions.sumOf { it.calories }.toDouble()), Modifier.weight(1f), accent = Violet)
        }

        SectionTitle("Last 8 weeks")
        AppCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ChartMode.entries.forEach { m ->
                        FilterChip(selected = mode == m, onClick = { mode = m }, label = { Text(m.label) })
                    }
                }
                Spacer(Modifier.height(10.dp))
                BarChart(
                    values = weeks.map {
                        when (mode) {
                            ChartMode.WORKOUTS -> it.workouts.toFloat()
                            ChartMode.VOLUME -> it.volume.toFloat()
                            ChartMode.MINUTES -> it.minutes.toFloat()
                        }
                    },
                    labels = weeks.map { weekFmt.format(Date(it.start)) },
                    color = when (mode) {
                        ChartMode.WORKOUTS -> MaterialTheme.colorScheme.primary
                        ChartMode.VOLUME -> Coral
                        ChartMode.MINUTES -> Sky
                    },
                    valueLabel = { if (it == 0f) "" else compact(it.toDouble()) },
                )
            }
        }

        val split = Stats.muscleSplit(sessions)
        SectionTitle("Muscles trained (30 days)")
        AppCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (split.isEmpty()) {
                    Text("Finish a workout to see which muscles you train most.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val max = split.values.maxOrNull() ?: 1
                split.forEach { (muscle, sets) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(muscle.label, Modifier.width(84.dp), style = MaterialTheme.typography.bodyMedium)
                        ProgressBar(sets.toFloat() / max, Modifier.weight(1f), color = Violet)
                        Text("$sets sets", Modifier.width(64.dp).padding(start = 8.dp), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        SectionTitle("Body weight")
        AppCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                val recent = vm.weights.takeLast(12)
                if (recent.size < 2) {
                    Text(
                        "Log your weight on the Goals tab (at least twice) to see the trend.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    val diff = recent.last().kg - recent.first().kg
                    Text(
                        "${fmtKg(recent.last().kg)} kg  (${if (diff >= 0) "+" else ""}${"%.1f".format(diff)} kg)",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    LineChart(recent.map { it.kg.toFloat() })
                }
            }
        }

        val prs = Stats.personalRecords(sessions)
        if (prs.isNotEmpty()) {
            SectionTitle("Personal records 🏆")
            prs.take(8).forEach { r ->
                AppCard(Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { nav.openExercise(r.exercise.id) }) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        ExerciseThumb(r.exercise, 48.dp, animate = false)
                        Spacer(Modifier.width(12.dp))
                        Text(r.exercise.name, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                        Text("${fmtKg(r.weight)} kg × ${r.reps}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }

        SectionTitle("History")
        if (sessions.isEmpty()) {
            Text("No workouts yet — your history will show up here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val dateFmt = SimpleDateFormat("EEE, d MMM · HH:mm", Locale.getDefault())
        sessions.sortedByDescending { it.startedAt }.take(30).forEach { s ->
            AppCard(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(s.title, style = MaterialTheme.typography.titleSmall)
                        Text(dateFmt.format(Date(s.startedAt)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "${formatClock(s.durationSec)} · ${s.completedSets} sets · ${compact(s.volume)} kg · ${s.calories} kcal",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    IconButton(onClick = { toDelete = s }) {
                        Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    toDelete?.let { s ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Delete workout?") },
            text = { Text("\"${s.title}\" will be removed from your history and stats.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteSession(s.id); toDelete = null }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancel") } },
        )
    }
}

private fun compact(v: Double): String = when {
    v >= 1_000_000 -> "%.1fM".format(v / 1_000_000)
    v >= 10_000 -> "%.0fk".format(v / 1000)
    v >= 1_000 -> "%.1fk".format(v / 1000)
    else -> fmtKg(Math.round(v * 10) / 10.0)
}
