@file:OptIn(ExperimentalLayoutApi::class)

package com.example.personalgym.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.personalgym.AppViewModel
import com.example.personalgym.data.CustomExercise
import com.example.personalgym.data.Equipment
import com.example.personalgym.data.ExerciseLibrary
import com.example.personalgym.data.Muscle
import com.example.personalgym.data.TrackType

/** Create a custom exercise, or edit [existing]. [onSaved] gets the saved exercise. */
@Composable
fun CustomExerciseDialog(
    vm: AppViewModel,
    onDismiss: () -> Unit,
    existing: CustomExercise? = null,
    prefillName: String = "",
    onSaved: (CustomExercise) -> Unit = {},
) {
    var name by rememberSaveable { mutableStateOf(existing?.name ?: prefillName) }
    var muscle by rememberSaveable { mutableStateOf(existing?.muscle ?: Muscle.CHEST) }
    var equipment by rememberSaveable { mutableStateOf(existing?.equipment ?: Equipment.DUMBBELL) }
    var track by rememberSaveable { mutableStateOf(existing?.track ?: TrackType.REPS) }
    var notes by rememberSaveable { mutableStateOf(existing?.notes ?: "") }
    val trimmed = name.trim()
    val duplicate = trimmed.isNotEmpty() && ExerciseLibrary.all.any { it.name.equals(trimmed, ignoreCase = true) && it.id != existing?.id }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New exercise" else "Edit exercise") },
        text = {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it.take(40) },
                    label = { Text("Name") }, singleLine = true,
                    isError = duplicate,
                    supportingText = if (duplicate) {
                        { Text("There's already an exercise with this name") }
                    } else null,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Muscle group", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Muscle.entries.forEach { m -> FilterChip(selected = muscle == m, onClick = { muscle = m }, label = { Text(m.label) }) }
                }
                Text("Equipment", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Equipment.entries.forEach { e -> FilterChip(selected = equipment == e, onClick = { equipment = e }, label = { Text(e.label) }) }
                }
                Text("Track by", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = track == TrackType.REPS, onClick = { track = TrackType.REPS }, label = { Text("Reps") })
                    FilterChip(selected = track == TrackType.TIME, onClick = { track = TrackType.TIME }, label = { Text("Time") })
                }
                OutlinedTextField(
                    value = notes, onValueChange = { notes = it.take(500) },
                    label = { Text("How to do it (optional)") },
                    placeholder = { Text("One step per line") },
                    minLines = 3, modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Custom moves use a general figure for their muscle group.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(
                enabled = trimmed.isNotEmpty() && !duplicate,
                onClick = {
                    val saved = CustomExercise(
                        id = existing?.id ?: "custom_${System.currentTimeMillis()}",
                        name = trimmed, muscle = muscle, equipment = equipment, track = track, notes = notes.trim(),
                    )
                    vm.saveCustomExercise(saved)
                    onSaved(saved)
                    onDismiss()
                },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
