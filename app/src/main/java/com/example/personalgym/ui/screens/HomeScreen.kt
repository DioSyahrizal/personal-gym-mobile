package com.example.personalgym.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.personalgym.AppViewModel
import com.example.personalgym.data.Stats
import com.example.personalgym.ui.Nav
import com.example.personalgym.ui.Tab
import com.example.personalgym.ui.components.AppCard
import com.example.personalgym.ui.components.ExerciseThumb
import com.example.personalgym.ui.components.Pill
import com.example.personalgym.ui.components.ProgressBar
import com.example.personalgym.ui.components.SectionTitle
import com.example.personalgym.ui.components.StatTile
import com.example.personalgym.ui.theme.Coral
import com.example.personalgym.ui.theme.Sky
import java.util.Calendar

val DAY_SHORT = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@Composable
fun HomeScreen(vm: AppViewModel, nav: Nav) {
    val today = Stats.dayOfWeek()
    val plan = vm.activePlan
    val todayPlan = plan.days.first { it.dayOfWeek == today }
    val trained = Stats.trainedDaysThisWeek(vm.sessions)
    val week = Stats.thisWeek(vm.sessions)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(greeting(), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Let's train 💪", style = MaterialTheme.typography.headlineMedium)
            }
            IconButton(onClick = nav.openSettings) {
                Icon(Icons.Filled.Settings, "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        vm.active?.let { s ->
            Spacer(Modifier.height(16.dp))
            AppCard(Modifier.fillMaxWidth().clickable { nav.openSession() }) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Workout in progress", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(s.title, style = MaterialTheme.typography.titleMedium)
                        Text("${s.completedSets}/${s.totalSets} sets done", style = MaterialTheme.typography.bodySmall)
                    }
                    Button(onClick = nav.openSession) { Text("Resume") }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        TodayCard(vm, nav)

        SectionTitle("This week")
        AppCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                plan.days.forEach { d ->
                    val isToday = d.dayOfWeek == today
                    val done = d.dayOfWeek in trained
                    Column(
                        Modifier.clip(RoundedCornerShape(12.dp)).clickable { nav.openDay(d) }.padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            DAY_SHORT[d.dayOfWeek - 1], style = MaterialTheme.typography.labelSmall,
                            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(6.dp))
                        Box(
                            Modifier.size(36.dp).clip(CircleShape).background(
                                when {
                                    done -> MaterialTheme.colorScheme.primary
                                    d.isRest -> MaterialTheme.colorScheme.surfaceContainerHigh
                                    else -> MaterialTheme.colorScheme.surfaceContainerHighest
                                },
                            ),
                            contentAlignment = Alignment.Center,
                        ) {
                            when {
                                done -> Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                                d.isRest -> Text("–", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                else -> Text(
                                    d.title.take(1), fontWeight = FontWeight.Bold,
                                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("Workouts this week", "${week.size}", Modifier.weight(1f))
            StatTile("Week streak", "${Stats.weekStreak(vm.sessions)} 🔥", Modifier.weight(1f), accent = Coral)
            StatTile("Minutes this week", "${week.sumOf { it.durationSec } / 60}", Modifier.weight(1f), accent = Sky)
        }

        if (vm.goals.isNotEmpty()) {
            SectionTitle("Goals") {
                Text(
                    "See all", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.clickable { nav.goTo(Tab.GOALS) },
                )
            }
            vm.goals.take(2).forEach { g ->
                val p = Stats.goalProgress(g, vm.sessions, vm.weights)
                AppCard(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(g.title, style = MaterialTheme.typography.titleSmall)
                        Text(p.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        ProgressBar(p.fraction)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { vm.startEmptyWorkout(); nav.openSession() }, Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Add, null)
            Spacer(Modifier.width(6.dp))
            Text("Start an empty workout")
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TodayCard(vm: AppViewModel, nav: Nav) {
    val day = vm.activePlan.days.first { it.dayOfWeek == Stats.dayOfWeek() }
    val doneToday = Stats.dayOfWeek() in Stats.trainedDaysThisWeek(vm.sessions)
    AppCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .background(
                    Brush.linearGradient(
                        listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.surfaceContainer),
                    ),
                )
                .padding(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pill("TODAY · ${vm.activePlan.goal.label.uppercase()}", color = MaterialTheme.colorScheme.primary, textColor = MaterialTheme.colorScheme.onPrimary)
                if (doneToday) {
                    Spacer(Modifier.width(8.dp))
                    Pill("Completed ✓")
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(day.title, style = MaterialTheme.typography.headlineSmall)
            Text(day.focus, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (day.isRest) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Recovery is when muscles grow. Go for a walk, stretch, and get good sleep.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Spacer(Modifier.height(14.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    day.exercises.forEach { pe ->
                        val ex = vm.resolve(pe.exerciseId)
                        ExerciseThumb(ex, 64.dp, Modifier.clickable { nav.openExercise(ex.id) })
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "${day.exercises.size} exercises · ${day.exercises.sumOf { it.sets }} sets · ~${estimateMinutes(vm, day)} min",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { if (vm.active == null) nav.startDay(day) else nav.openSession() },
                        Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(),
                    ) {
                        Icon(Icons.Filled.PlayArrow, null)
                        Spacer(Modifier.width(4.dp))
                        Text(if (vm.active == null) "Start workout" else "Resume workout")
                    }
                    OutlinedButton(onClick = { nav.openDay(day) }) { Text("Details") }
                }
            }
        }
    }
}

fun estimateMinutes(vm: AppViewModel, day: com.example.personalgym.data.PlanDay): Int {
    val secs = day.exercises.sumOf { pe ->
        val ex = vm.resolve(pe.exerciseId)
        val work = if (ex.track == com.example.personalgym.data.TrackType.TIME) pe.target else pe.target * 3
        pe.sets * (work + vm.activePlan.restSeconds)
    }
    return (secs / 60).coerceAtLeast(5)
}

private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 4..11 -> "Good morning"
    in 12..17 -> "Good afternoon"
    else -> "Good evening"
}
