package com.example.personalgym.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.personalgym.AppViewModel
import com.example.personalgym.data.Exercise
import com.example.personalgym.data.Stats
import com.example.personalgym.data.TrackType
import com.example.personalgym.data.formatNumber
import com.example.personalgym.ui.components.AppCard
import com.example.personalgym.ui.components.LineChart
import com.example.personalgym.ui.components.SectionTitle
import com.example.personalgym.ui.components.StatTile
import com.example.personalgym.ui.theme.Coral
import com.example.personalgym.ui.theme.Sky
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class Metric(val label: String) { E1RM("Est. 1RM"), TOP("Top weight"), VOLUME("Volume"), REPS("Total reps"), TIME("Total time") }

/** "Your progress" block on the exercise detail page; shows nothing until the exercise has been logged. */
@Composable
fun ExerciseProgress(vm: AppViewModel, ex: Exercise) {
    val history = remember(vm.sessions, ex.id) { Stats.exerciseHistory(vm.sessions, ex.id) }
    if (history.isEmpty()) return
    val unit = vm.weightUnit
    val timed = ex.track == TrackType.TIME
    val metrics = when {
        ex.usesWeight -> listOf(Metric.E1RM, Metric.TOP, Metric.VOLUME)
        timed -> listOf(Metric.TIME)
        else -> listOf(Metric.REPS)
    }
    var metric by rememberSaveable(ex.id) { mutableStateOf(metrics.first()) }
    val dayFmt = remember { SimpleDateFormat("d MMM", Locale.getDefault()) }

    fun valueOf(s: Stats.ExerciseSession, m: Metric): Double = when (m) {
        Metric.E1RM -> unit.fromKg(s.bestE1rm)
        Metric.TOP -> unit.fromKg(s.bestWeight)
        Metric.VOLUME -> unit.fromKg(s.volume)
        Metric.REPS, Metric.TIME -> s.totalReps.toDouble()
    }
    fun show(v: Double, m: Metric) = when (m) {
        Metric.REPS -> "${formatNumber(v)} reps"
        Metric.TIME -> formatSeconds(v.toInt())
        else -> "${formatNumber(v)} ${unit.label}"
    }

    SectionTitle("Your progress")
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (ex.usesWeight) {
            val bestE1rm = history.maxOf { it.bestE1rm }
            val bestSet = history.flatMap { it.sets }.maxWith(compareBy({ it.weight }, { it.reps }))
            StatTile("Est. 1-rep max (${unit.label})", unit.format(bestE1rm), Modifier.weight(1f))
            StatTile("Best set", "${unit.format(bestSet.weight)}×${bestSet.reps}", Modifier.weight(1f), accent = Coral)
        } else {
            val best = history.flatMap { it.sets }.maxOf { it.reps }
            StatTile("Best set", if (timed) formatSeconds(best) else "$best reps", Modifier.weight(1f))
        }
        StatTile("Workouts", "${history.size}", Modifier.weight(1f), accent = Sky)
    }

    Spacer(Modifier.height(10.dp))
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            if (metrics.size > 1) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    metrics.forEach { m -> FilterChip(selected = metric == m, onClick = { metric = m }, label = { Text(m.label) }) }
                }
                Spacer(Modifier.height(8.dp))
            }
            val recent = history.takeLast(12)
            if (recent.size < 2) {
                Text(
                    "Log this exercise in another workout to see your trend.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val first = valueOf(recent.first(), metric)
                val last = valueOf(recent.last(), metric)
                val diff = last - first
                Text(show(last, metric), style = MaterialTheme.typography.titleLarge)
                Text(
                    when {
                        diff > 0 -> "▲ ${show(diff, metric)} since ${dayFmt.format(Date(recent.first().date))}"
                        diff < 0 -> "▼ ${show(-diff, metric)} since ${dayFmt.format(Date(recent.first().date))}"
                        else -> "No change since ${dayFmt.format(Date(recent.first().date))}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (diff >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(8.dp))
                LineChart(recent.map { valueOf(it, metric).toFloat() }, color = MaterialTheme.colorScheme.primary)
                Row(Modifier.fillMaxWidth()) {
                    Text(dayFmt.format(Date(recent.first().date)), Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(dayFmt.format(Date(recent.last().date)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    Spacer(Modifier.height(10.dp))
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Recent workouts", style = MaterialTheme.typography.titleSmall)
            history.takeLast(5).asReversed().forEach { s ->
                Row(Modifier.fillMaxWidth()) {
                    Text(dayFmt.format(Date(s.date)), Modifier.padding(end = 12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        s.sets.joinToString("  ") { set ->
                            when {
                                timed -> formatSeconds(set.reps)
                                ex.usesWeight -> "${unit.format(set.weight)}×${set.reps}"
                                else -> "×${set.reps}"
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
