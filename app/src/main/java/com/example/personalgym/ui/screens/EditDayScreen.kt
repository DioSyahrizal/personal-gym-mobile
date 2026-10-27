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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.personalgym.AppViewModel
import com.example.personalgym.data.ExerciseLibrary
import com.example.personalgym.data.PlanDay
import com.example.personalgym.data.PlanExercise
import com.example.personalgym.data.TrackType
import com.example.personalgym.ui.components.AppCard
import com.example.personalgym.ui.components.ExerciseThumb

@Composable
fun EditDayScreen(vm: AppViewModel, day: PlanDay, onDone: () -> Unit) {
    val items = remember(day) { mutableStateListOf<PlanExercise>().apply { addAll(day.exercises) } }
    val changed = items.toList() != day.exercises
    var showPicker by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }

    fun close() {
        if (changed) confirmDiscard = true else onDone()
    }
    BackHandler { close() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Edit ${DAY_SHORT[day.dayOfWeek - 1]} · ${if (day.isRest) "Rest Day" else day.title}", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = ::close) { Icon(Icons.Filled.Close, "Cancel") } },
                actions = {
                    Button(
                        onClick = { vm.saveDayEdit(day.dayOfWeek, items.toList()); onDone() },
                        enabled = changed,
                        modifier = Modifier.padding(end = 8.dp),
                    ) { Text("Save") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    "Change sets and reps, reorder with the arrows, or add and remove exercises. " +
                        "Your changes apply to every week of this plan.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (items.isEmpty()) {
                item {
                    AppCard(Modifier.fillMaxWidth()) {
                        Text(
                            "No exercises — this will be a rest day. Add exercises to train on this day.",
                            Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            itemsIndexed(items, key = { i, e -> "$i-${e.exerciseId}" }) { i, pe ->
                EditRow(
                    vm = vm, pe = pe,
                    canMoveUp = i > 0, canMoveDown = i < items.lastIndex,
                    onChange = { items[i] = it },
                    onMoveUp = { items.add(i - 1, items.removeAt(i)) },
                    onMoveDown = { items.add(i + 1, items.removeAt(i)) },
                    onRemove = { items.removeAt(i) },
                )
            }
            item {
                OutlinedButton(onClick = { showPicker = true }, Modifier.fillMaxWidth().height(50.dp)) {
                    Icon(Icons.Filled.Add, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Add exercise")
                }
            }
            if (day.edited) {
                item {
                    TextButton(onClick = { confirmReset = true }, Modifier.fillMaxWidth()) {
                        Text("Reset this day to the original plan", color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    }

    if (showPicker) {
        Dialog(onDismissRequest = { showPicker = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                ExercisePicker(
                    onPick = { ex ->
                        items += PlanExercise(ex.id, 3, if (ex.track == TrackType.TIME) 30 else 10)
                        showPicker = false
                    },
                    header = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Add exercise", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                            IconButton(onClick = { showPicker = false }) { Icon(Icons.Filled.Close, "Close") }
                        }
                        Spacer(Modifier.height(8.dp))
                    },
                )
            }
        }
    }
    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard changes?") },
            text = { Text("Your edits to this day won't be saved.") },
            confirmButton = { TextButton(onClick = { confirmDiscard = false; onDone() }) { Text("Discard", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing") } },
        )
    }
    if (confirmReset) {
        val original = vm.defaultDay(day.dayOfWeek)
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset this day?") },
            text = {
                Text(
                    if (original.isRest) "It goes back to a rest day."
                    else "It goes back to ${original.title} with ${original.exercises.size} exercises.",
                )
            },
            confirmButton = { TextButton(onClick = { confirmReset = false; vm.resetDay(day.dayOfWeek); onDone() }) { Text("Reset") } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun EditRow(
    vm: AppViewModel,
    pe: PlanExercise,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onChange: (PlanExercise) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    val ex = ExerciseLibrary.get(pe.exerciseId)
    val timed = ex.track == TrackType.TIME
    val performed = vm.resolve(pe.exerciseId)
    AppCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ExerciseThumb(ex, 48.dp, animate = false)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(ex.name, style = MaterialTheme.typography.titleSmall)
                        if (performed.id != ex.id) {
                            Text(
                                "Done as ${performed.name} (dumbbell-only)",
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Stepper(
                        label = "Sets", value = "${pe.sets}",
                        onMinus = { onChange(pe.copy(sets = (pe.sets - 1).coerceAtLeast(1))) },
                        onPlus = { onChange(pe.copy(sets = (pe.sets + 1).coerceAtMost(10))) },
                    )
                    if (timed) {
                        val step = if (pe.target < 60) 5 else 15
                        Stepper(
                            label = "Time", value = formatSeconds(pe.target),
                            onMinus = { onChange(pe.copy(target = (pe.target - step).coerceAtLeast(5))) },
                            onPlus = { onChange(pe.copy(target = (pe.target + step).coerceAtMost(3600))) },
                        )
                    } else {
                        Stepper(
                            label = "Reps", value = "${pe.target}",
                            onMinus = { onChange(pe.copy(target = (pe.target - 1).coerceAtLeast(1))) },
                            onPlus = { onChange(pe.copy(target = (pe.target + 1).coerceAtMost(100))) },
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onMoveUp, enabled = canMoveUp) { Icon(Icons.Filled.KeyboardArrowUp, "Move up") }
                IconButton(onClick = onRemove) { Icon(Icons.Filled.Delete, "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                IconButton(onClick = onMoveDown, enabled = canMoveDown) { Icon(Icons.Filled.KeyboardArrowDown, "Move down") }
            }
        }
    }
}

@Composable
private fun Stepper(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepButton("−", onMinus)
            Text(
                value, Modifier.widthIn(min = 44.dp), textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
            )
            StepButton("+", onPlus)
        }
    }
}

@Composable
private fun StepButton(symbol: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
    }
}
