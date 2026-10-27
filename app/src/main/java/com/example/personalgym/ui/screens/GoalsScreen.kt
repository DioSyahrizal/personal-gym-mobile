@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.personalgym.ui.screens

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.personalgym.AppViewModel
import com.example.personalgym.data.Equipment
import com.example.personalgym.data.ExerciseLibrary
import com.example.personalgym.data.Goal
import com.example.personalgym.data.GoalType
import com.example.personalgym.data.Stats
import com.example.personalgym.ui.components.AppCard
import com.example.personalgym.ui.components.ProgressBar
import com.example.personalgym.ui.theme.Coral

@Composable
fun GoalsScreen(vm: AppViewModel) {
    var adding by remember { mutableStateOf(false) }
    var loggingWeight by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text("Goals", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Set targets and watch the progress fill up automatically from your workouts.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
            }
            item {
                AppCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Body weight", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(vm.latestWeight?.let { vm.weightUnit.show(it) } ?: "Not logged yet", style = MaterialTheme.typography.titleLarge)
                        }
                        FilledTonalButton(onClick = { loggingWeight = true }) { Text("Log weight") }
                    }
                }
            }
            if (vm.goals.isEmpty()) {
                item {
                    AppCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🎯", style = MaterialTheme.typography.headlineMedium)
                            Text("No goals yet", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Try \"4 workouts per week\" or \"Bench press 60 ${vm.weightUnit.label}\".",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            items(vm.goals, key = { it.id }) { g -> GoalCard(vm, g) }
        }
        ExtendedFloatingActionButton(
            onClick = { adding = true },
            icon = { Icon(Icons.Filled.Add, null) },
            text = { Text("New goal") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        )
    }

    if (adding) AddGoalDialog(vm, onDismiss = { adding = false })
    if (loggingWeight) {
        NumberInputDialog(
            title = "Log body weight", label = "Weight (${vm.weightUnit.label})", initial = vm.latestWeight?.let { vm.weightUnit.format(it) } ?: "",
            onDismiss = { loggingWeight = false },
            onConfirm = { vm.addWeight(vm.weightUnit.toKg(it)); loggingWeight = false },
        )
    }
}

@Composable
private fun GoalCard(vm: AppViewModel, g: Goal) {
    val p = Stats.goalProgress(g, vm.sessions, vm.weights, vm.weightUnit)
    val done = p.fraction >= 1f
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(g.type.label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(Stats.goalTitle(g, vm.weightUnit), style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    if (done) "✅" else "${(p.fraction * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                IconButton(onClick = { vm.deleteGoal(g.id) }) {
                    Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(8.dp))
            ProgressBar(p.fraction, height = 10.dp, color = if (done) MaterialTheme.colorScheme.primary else Coral)
            Spacer(Modifier.height(6.dp))
            Text(p.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (g.type == GoalType.CUSTOM) {
                Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { vm.adjustManualGoal(g.id, -1.0) }) { Text("−1") }
                    FilledTonalButton(onClick = { vm.adjustManualGoal(g.id, 1.0) }) { Text("+1") }
                }
            }
        }
    }
}

@Composable
private fun AddGoalDialog(vm: AppViewModel, onDismiss: () -> Unit) {
    var type by remember { mutableStateOf(GoalType.WEEKLY_WORKOUTS) }
    var target by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    val liftable = remember { ExerciseLibrary.all.filter { it.usesWeight }.sortedBy { it.name } }
    var exerciseId by remember { mutableStateOf(liftable.first { it.equipment == Equipment.BARBELL || it.equipment == Equipment.DUMBBELL }.id) }
    var typeOpen by remember { mutableStateOf(false) }
    var exOpen by remember { mutableStateOf(false) }

    val targetNum = target.replace(',', '.').toDoubleOrNull()
    val needsWeight = type == GoalType.BODY_WEIGHT && vm.latestWeight == null
    var startWeight by remember { mutableStateOf("") }
    val unit = vm.weightUnit
    val isWeightGoal = type == GoalType.BODY_WEIGHT || type == GoalType.LIFT

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExposedDropdownMenuBox(expanded = typeOpen, onExpandedChange = { typeOpen = it }) {
                    OutlinedTextField(
                        value = type.label, onValueChange = {}, readOnly = true, label = { Text("Goal type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeOpen) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(expanded = typeOpen, onDismissRequest = { typeOpen = false }) {
                        GoalType.entries.forEach { t ->
                            DropdownMenuItem(text = { Text(t.label) }, onClick = { type = t; typeOpen = false })
                        }
                    }
                }
                if (type == GoalType.LIFT) {
                    ExposedDropdownMenuBox(expanded = exOpen, onExpandedChange = { exOpen = it }) {
                        OutlinedTextField(
                            value = ExerciseLibrary.get(exerciseId).name, onValueChange = {}, readOnly = true,
                            label = { Text("Exercise") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(exOpen) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        )
                        ExposedDropdownMenu(expanded = exOpen, onDismissRequest = { exOpen = false }) {
                            liftable.forEach { e ->
                                DropdownMenuItem(text = { Text(e.name) }, onClick = { exerciseId = e.id; exOpen = false })
                            }
                        }
                    }
                }
                if (type == GoalType.CUSTOM) {
                    OutlinedTextField(
                        value = title, onValueChange = { title = it }, label = { Text("What's the goal?") },
                        placeholder = { Text("e.g. Run 50 km this month") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                }
                OutlinedTextField(
                    value = target, onValueChange = { target = it },
                    label = { Text("Target" + if (isWeightGoal) " (${unit.label})" else if (type.unit.isNotEmpty()) " (${type.unit})" else "") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                if (needsWeight) {
                    OutlinedTextField(
                        value = startWeight, onValueChange = { startWeight = it },
                        label = { Text("Current weight (${unit.label})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            val startNum = startWeight.replace(',', '.').toDoubleOrNull()
            Button(
                enabled = targetNum != null && targetNum > 0 && (!needsWeight || startNum != null) &&
                    (type != GoalType.CUSTOM || title.isNotBlank()),
                onClick = {
                    // Weight targets are stored in kg whatever unit they were typed in.
                    val t = (targetNum ?: return@Button).let { if (isWeightGoal) unit.toKg(it) else it }
                    val startKg = startNum?.let(unit::toKg)
                    if (needsWeight && startKg != null) vm.addWeight(startKg)
                    val goal = Goal(
                        id = System.currentTimeMillis(), type = type, title = title.trim(), target = t,
                        start = startKg ?: vm.latestWeight ?: 0.0,
                        exerciseId = if (type == GoalType.LIFT) exerciseId else null,
                    )
                    vm.addGoal(goal.copy(title = Stats.goalTitle(goal, unit)))
                    onDismiss()
                },
            ) { Text("Add goal") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun NumberInputDialog(title: String, label: String, initial: String, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    val num = text.replace(',', '.').toDoubleOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text, onValueChange = { text = it }, label = { Text(label) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true,
            )
        },
        confirmButton = { Button(enabled = num != null && num > 0, onClick = { num?.let(onConfirm) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

