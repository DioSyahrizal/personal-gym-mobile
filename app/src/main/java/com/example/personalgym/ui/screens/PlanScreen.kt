@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.personalgym.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.personalgym.AppViewModel
import com.example.personalgym.data.ExerciseLibrary
import com.example.personalgym.data.Level
import com.example.personalgym.data.PlanDay
import com.example.personalgym.data.PlanGoal
import com.example.personalgym.data.Plans
import com.example.personalgym.data.Stats
import com.example.personalgym.data.TrackType
import com.example.personalgym.ui.Nav
import com.example.personalgym.ui.components.AppCard
import com.example.personalgym.ui.components.EquipmentPill
import com.example.personalgym.ui.components.ExerciseThumb
import com.example.personalgym.ui.components.Pill
import com.example.personalgym.ui.components.SectionTitle

@Composable
fun PlanScreen(vm: AppViewModel, nav: Nav) {
    val today = Stats.dayOfWeek()
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Weekly Plan", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Pick your goal, days and level — the week is planned for you.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
        }
        item {
            val profile = vm.profile
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PlanGoal.entries.forEach { g ->
                    val selected = g == profile.goal
                    Card(
                        onClick = { vm.updateProfile(profile.copy(goal = g)) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                        ),
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(if (g == PlanGoal.FAT_LOSS) "🔥" else "🏋️", style = MaterialTheme.typography.headlineSmall)
                            Text(g.label, style = MaterialTheme.typography.titleMedium)
                            Text(
                                Plans.base(g, profile.daysPerWeek).name,
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        item {
            val profile = vm.profile
            OptionRow("Days per week") {
                Plans.dayOptions.forEach { d ->
                    FilterChip(
                        selected = profile.daysPerWeek == d,
                        onClick = { vm.updateProfile(profile.copy(daysPerWeek = d)) },
                        label = { Text("$d days") },
                    )
                }
            }
            OptionRow("Level") {
                Level.entries.forEach { l ->
                    FilterChip(
                        selected = profile.level == l,
                        onClick = { vm.updateProfile(profile.copy(level = l)) },
                        label = { Text(l.label) },
                    )
                }
            }
        }
        item {
            Text(
                vm.activePlan.description,
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
        item {
            AppCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Dumbbell-only mode", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "No machines? Machine, cable & barbell moves are swapped for dumbbell alternatives.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Switch(checked = vm.dumbbellOnly, onCheckedChange = vm::updateDumbbellOnly)
                }
            }
        }
        item { SectionTitle("Schedule") }
        items(vm.activePlan.days) { d ->
            AppCard(Modifier.fillMaxWidth().clickable { nav.openDay(d) }) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.width(48.dp)) {
                        Text(
                            DAY_SHORT[d.dayOfWeek - 1], style = MaterialTheme.typography.titleMedium,
                            color = if (d.dayOfWeek == today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                        if (d.dayOfWeek == today) Text("today", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(d.title, style = MaterialTheme.typography.titleSmall)
                        Text(
                            if (d.isRest) d.focus else "${d.focus} · ${d.exercises.size} exercises · ~${estimateMinutes(vm, d)} min",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (!d.isRest) {
                        IconButton(onClick = { if (vm.active == null) nav.startDay(d) else nav.openSession() }) {
                            Icon(Icons.Filled.PlayArrow, "Start", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlanDayScreen(vm: AppViewModel, day: PlanDay, nav: Nav, onBack: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("${DAY_SHORT[day.dayOfWeek - 1]} · ${day.title}") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            if (!day.isRest) {
                Button(
                    onClick = { if (vm.active == null) nav.startDay(day) else nav.openSession() },
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp).height(52.dp),
                ) {
                    Icon(Icons.Filled.PlayArrow, null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (vm.active == null) "Start this workout" else "Resume current workout")
                }
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(day.focus, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!day.isRest) {
                    Text(
                        "Rest ${vm.activePlan.restSeconds}s between sets · ~${estimateMinutes(vm, day)} min",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (day.isRest) {
                item {
                    AppCard(Modifier.fillMaxWidth()) {
                        Text(
                            "Rest day. Light walking, mobility work and 7–9 hours of sleep help you come back stronger.",
                            Modifier.padding(16.dp),
                        )
                    }
                }
            }
            items(day.exercises) { pe ->
                val original = ExerciseLibrary.get(pe.exerciseId)
                val ex = vm.resolve(pe.exerciseId)
                val alt = ExerciseLibrary.alternativeFor(original)
                AppCard(Modifier.fillMaxWidth().clickable { nav.openExercise(ex.id) }) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ExerciseThumb(ex, 76.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(ex.name, style = MaterialTheme.typography.titleSmall)
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    if (ex.track == TrackType.TIME) "${pe.sets} × ${formatSeconds(pe.target)}" else "${pe.sets} sets × ${pe.target} reps",
                                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Pill(ex.muscle.label)
                                    EquipmentPill(ex.equipment)
                                }
                            }
                        }
                        if (ex.id != original.id) {
                            Spacer(Modifier.height(8.dp))
                            SwapNote("Swapped from ${original.name} (dumbbell-only mode)")
                        } else if (alt != null) {
                            Spacer(Modifier.height(8.dp))
                            SwapNote("No ${original.equipment.label.lowercase()}? Do ${alt.name}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionRow(label: String, chips: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label, Modifier.width(110.dp),
            style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { chips() }
    }
}

@Composable
fun SwapNote(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Refresh, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(end = 6.dp).height(16.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
    }
}

fun formatSeconds(sec: Int): String = when {
    sec >= 60 && sec % 60 == 0 -> "${sec / 60} min"
    sec >= 60 -> "${sec / 60}:${"%02d".format(sec % 60)}"
    else -> "${sec}s"
}
