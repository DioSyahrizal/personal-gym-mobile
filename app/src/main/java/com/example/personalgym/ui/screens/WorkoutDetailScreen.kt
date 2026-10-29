@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.personalgym.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.personalgym.AppViewModel
import com.example.personalgym.data.ExerciseLibrary
import com.example.personalgym.data.ExerciseLog
import com.example.personalgym.data.SetLog
import com.example.personalgym.data.TrackType
import com.example.personalgym.data.WeightUnit
import com.example.personalgym.data.WorkoutSession
import com.example.personalgym.data.formatCompact
import com.example.personalgym.ui.components.AppCard
import com.example.personalgym.ui.components.ExerciseThumb
import com.example.personalgym.ui.components.StatTile
import com.example.personalgym.ui.theme.Coral
import com.example.personalgym.ui.theme.Sky
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** A finished workout from History: read-only by default, with an edit mode for fixing logged sets. */
@Composable
fun WorkoutDetailScreen(vm: AppViewModel, sessionId: Long, onBack: () -> Unit, onOpenExercise: (String) -> Unit) {
    val saved = vm.sessions.firstOrNull { it.id == sessionId }
    if (saved == null) {
        // Deleted (or restored away) while open.
        LaunchedEffect(Unit) { onBack() }
        return
    }
    var draft by remember(sessionId) { mutableStateOf<WorkoutSession?>(null) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val editing = draft != null
    val shown = draft ?: saved
    val unit = vm.weightUnit
    val dateFmt = remember { SimpleDateFormat("EEEE, d MMM yyyy · HH:mm", Locale.getDefault()) }

    fun close() {
        when {
            editing && draft != saved -> confirmDiscard = true
            editing -> draft = null
            else -> onBack()
        }
    }
    fun change(block: (MutableList<ExerciseLog>) -> Unit) {
        val d = draft ?: return
        draft = d.copy(exercises = d.exercises.toMutableList().also(block))
    }
    BackHandler { close() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (editing) "Edit workout" else shown.title, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = ::close) {
                        Icon(if (editing) Icons.Filled.Close else Icons.AutoMirrored.Filled.ArrowBack, if (editing) "Cancel" else "Back")
                    }
                },
                actions = {
                    if (editing) {
                        Button(
                            onClick = { draft?.let(vm::updateSession); draft = null },
                            enabled = draft != saved,
                            modifier = Modifier.padding(end = 8.dp),
                        ) { Text("Save") }
                    } else {
                        IconButton(onClick = { draft = saved }) { Icon(Icons.Filled.Edit, "Edit workout") }
                        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, "Delete workout") }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(dateFmt.format(Date(shown.startedAt)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Duration", formatClock(shown.durationSec), Modifier.weight(1f))
                    StatTile("Sets", "${shown.completedSets}", Modifier.weight(1f), accent = Sky)
                    StatTile("Volume (${unit.label})", formatCompact(unit.fromKg(shown.volume)), Modifier.weight(1f), accent = Coral)
                }
                if (editing) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Fix weights and reps, tick or untick sets, or remove what you didn't do. Records, charts and suggestions update when you save.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            itemsIndexed(shown.exercises, key = { i, e -> "$i-${e.exerciseId}" }) { i, log ->
                LoggedExerciseCard(
                    log = log, unit = unit, editing = editing,
                    onOpen = { if (!editing) onOpenExercise(log.exerciseId) },
                    onSetChange = { si, set -> change { it[i] = it[i].copy(sets = it[i].sets.toMutableList().also { s -> s[si] = set }) } },
                    onRemoveSet = { si -> change { it[i] = it[i].copy(sets = it[i].sets.filterIndexed { j, _ -> j != si }) } },
                    onAddSet = {
                        change {
                            val last = it[i].sets.lastOrNull()
                            val t = last?.target ?: 10
                            it[i] = it[i].copy(sets = it[i].sets + SetLog(t, last?.reps ?: t, last?.weight ?: 0.0, true))
                        }
                    },
                    onRemoveExercise = { change { it.removeAt(i) } },
                )
            }
            if (shown.exercises.isEmpty()) {
                item {
                    Text(
                        "No exercises left. Saving keeps the workout with nothing logged; use Delete to remove it entirely.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard changes?") },
            text = { Text("Your corrections to this workout won't be saved.") },
            confirmButton = { TextButton(onClick = { confirmDiscard = false; draft = null }) { Text("Discard", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing") } },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete workout?") },
            text = { Text("\"${saved.title}\" will be removed from your history and stats.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; vm.deleteSession(saved.id); onBack() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun LoggedExerciseCard(
    log: ExerciseLog,
    unit: WeightUnit,
    editing: Boolean,
    onOpen: () -> Unit,
    onSetChange: (Int, SetLog) -> Unit,
    onRemoveSet: (Int) -> Unit,
    onAddSet: () -> Unit,
    onRemoveExercise: () -> Unit,
) {
    val ex = ExerciseLibrary.get(log.exerciseId)
    val timed = ex.track == TrackType.TIME
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ExerciseThumb(ex, 48.dp, Modifier.clickable(onClick = onOpen), animate = false)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f).clickable(onClick = onOpen)) {
                    Text(ex.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${log.sets.count { it.done }} of ${log.sets.size} sets done",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (editing) {
                    IconButton(onClick = onRemoveExercise) { Icon(Icons.Filled.Delete, "Remove exercise", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                ColumnHeader("SET", Modifier.width(36.dp))
                if (ex.usesWeight) ColumnHeader(unit.label.uppercase(), Modifier.weight(1f))
                ColumnHeader(if (timed) "SEC" else "REPS", Modifier.weight(1f))
                ColumnHeader("DONE", Modifier.width(if (editing) 84.dp else 44.dp))
            }
            log.sets.forEachIndexed { si, set ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (set.done) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer)
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${si + 1}", Modifier.width(36.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                    if (ex.usesWeight) {
                        if (editing) {
                            NumberField(unit.format(set.weight), decimal = true, onValue = { onSetChange(si, set.copy(weight = unit.toKg(it.toDoubleOrNull() ?: 0.0))) }, modifier = Modifier.weight(1f))
                        } else {
                            Text(unit.format(set.weight), Modifier.weight(1f), textAlign = TextAlign.Center)
                        }
                    }
                    if (editing) {
                        NumberField(set.reps.toString(), decimal = false, onValue = { onSetChange(si, set.copy(reps = it.toIntOrNull() ?: 0)) }, modifier = Modifier.weight(1f))
                    } else {
                        Text(if (timed) formatSeconds(set.reps) else "${set.reps}", Modifier.weight(1f), textAlign = TextAlign.Center)
                    }
                    Row(Modifier.width(if (editing) 84.dp else 44.dp), horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)) {
                        DoneMark(set.done, enabled = editing) { onSetChange(si, set.copy(done = !set.done)) }
                        if (editing) {
                            IconButton(onClick = { onRemoveSet(si) }, Modifier.size(36.dp)) {
                                Icon(Icons.Filled.Close, "Remove set", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            if (editing) {
                TextButton(onClick = onAddSet) {
                    Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add set")
                }
            }
        }
    }
}

@Composable
private fun ColumnHeader(text: String, modifier: Modifier) {
    Text(text, modifier, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
}

@Composable
private fun DoneMark(done: Boolean, enabled: Boolean, onToggle: () -> Unit) {
    Box(
        Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest)
            .then(if (enabled) Modifier.clickable(onClick = onToggle) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (done) Icon(Icons.Filled.Check, "Done", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimary)
    }
}
