@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.personalgym.ui.screens

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.runtime.LaunchedEffect
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
import com.example.personalgym.data.WeightEntry
import com.example.personalgym.data.WeightUnit
import com.example.personalgym.data.formatNumber
import com.example.personalgym.ui.components.AppCard
import com.example.personalgym.ui.components.ProgressBar
import com.example.personalgym.ui.components.ValueInputDialog
import com.example.personalgym.ui.theme.Coral
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GoalsScreen(vm: AppViewModel) {
    var adding by remember { mutableStateOf(false) }
    var loggingWeight by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }

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
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Body weight", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(vm.latestWeight?.let { vm.weightUnit.show(it) } ?: "Not logged yet", style = MaterialTheme.typography.titleLarge)
                            }
                            FilledTonalButton(onClick = { loggingWeight = true }) { Text("Log weight") }
                        }
                        if (vm.weights.isNotEmpty()) {
                            TextButton(onClick = { showHistory = true }, contentPadding = PaddingValues(0.dp)) {
                                Text("View history (${vm.weights.size})")
                            }
                        }
                    }
                }
            }
            item { MeasurementsCard(vm) }
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
        WeightInputDialog(
            title = "Log body weight", unit = vm.weightUnit, initialKg = vm.latestWeight,
            onDismiss = { loggingWeight = false },
            onConfirm = { vm.addWeight(it); loggingWeight = false },
        )
    }
    if (showHistory) WeightHistoryDialog(vm, onDismiss = { showHistory = false })
}

@Composable
private fun WeightHistoryDialog(vm: AppViewModel, onDismiss: () -> Unit) {
    val unit = vm.weightUnit
    val entries = vm.weights.sortedByDescending { it.date }
    val dateFmt = remember { SimpleDateFormat("EEE, d MMM yyyy · HH:mm", Locale.getDefault()) }
    var editing by remember { mutableStateOf<WeightEntry?>(null) }
    var deleting by remember { mutableStateOf<WeightEntry?>(null) }

    // Closes itself once the last entry is deleted.
    if (entries.isEmpty()) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Weight history") },
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                items(entries, key = { it.date }) { e ->
                    val i = entries.indexOf(e)
                    val change = entries.getOrNull(i + 1)?.let { unit.fromKg(e.kg - it.kg) }
                    Row(
                        Modifier.fillMaxWidth().clickable { editing = e }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(unit.show(e.kg), style = MaterialTheme.typography.titleMedium)
                            Text(
                                dateFmt.format(Date(e.date)) +
                                    (change?.let { " · ${if (it >= 0) "+" else "−"}${formatNumber(kotlin.math.abs(it))} ${unit.label}" } ?: ""),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { editing = e }) {
                            Icon(Icons.Filled.Edit, "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { deleting = e }) {
                            Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )

    editing?.let { e ->
        WeightInputDialog(
            title = "Edit weight", subtitle = dateFmt.format(Date(e.date)), unit = unit, initialKg = e.kg,
            onDismiss = { editing = null },
            onConfirm = { vm.updateWeight(e.date, it); editing = null },
        )
    }
    deleting?.let { e ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete this entry?") },
            text = { Text("${unit.show(e.kg)} on ${dateFmt.format(Date(e.date))} will be removed from your weight history.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteWeight(e.date); deleting = null }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
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

/** Body-weight entry dialog. The value is typed in [unit]; [onConfirm] receives kg. */
@Composable
fun WeightInputDialog(
    title: String,
    unit: WeightUnit,
    initialKg: Double?,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
    subtitle: String? = null,
) = ValueInputDialog(
    title = title,
    subtitle = subtitle,
    label = "Weight (${unit.label})",
    initial = initialKg?.let(unit::format) ?: "",
    errorText = "Enter between ${unit.show(MIN_BODY_KG)} and ${unit.show(MAX_BODY_KG)}",
    accept = { unit.toKg(it).takeIf { kg -> kg in MIN_BODY_KG..MAX_BODY_KG } },
    onDismiss = onDismiss,
    onConfirm = onConfirm,
)

private const val MIN_BODY_KG = 25.0
private const val MAX_BODY_KG = 350.0

