@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.personalgym.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.remember
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.personalgym.AppViewModel
import com.example.personalgym.data.Equipment
import com.example.personalgym.data.Exercise
import com.example.personalgym.data.ExerciseLibrary
import com.example.personalgym.data.Muscle
import com.example.personalgym.data.Stats
import com.example.personalgym.ui.Nav
import com.example.personalgym.ui.components.AppCard
import com.example.personalgym.ui.components.EquipmentPill
import com.example.personalgym.ui.components.ExerciseThumb
import com.example.personalgym.ui.components.Pill
import com.example.personalgym.ui.components.SectionTitle
import com.example.personalgym.ui.illustration.ExerciseFigure
import com.example.personalgym.ui.theme.figureColors

@Composable
fun ExercisePicker(
    onPick: (Exercise) -> Unit,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit = {},
    /** When given, the picker offers "Create custom exercise" and treats a new one as picked. */
    vm: AppViewModel? = null,
) {
    var creating by remember { mutableStateOf(false) }
    var muscle by rememberSaveable { mutableStateOf<Muscle?>(null) }
    var dumbbellOnly by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val list = ExerciseLibrary.all.filter { e ->
        (muscle == null || e.muscle == muscle) &&
            (!dumbbellOnly || e.equipment == Equipment.DUMBBELL || e.equipment == Equipment.BODYWEIGHT) &&
            (query.isBlank() || e.name.contains(query, ignoreCase = true))
    }
    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            header()
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search exercises") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = dumbbellOnly, onClick = { dumbbellOnly = !dumbbellOnly },
                    label = { Text("No machine") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer),
                )
                FilterChip(selected = muscle == null, onClick = { muscle = null }, label = { Text("All") })
                Muscle.entries.forEach { m ->
                    FilterChip(selected = muscle == m, onClick = { muscle = if (muscle == m) null else m }, label = { Text(m.label) })
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${list.size} exercises", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f),
                )
                if (vm != null) {
                    TextButton(onClick = { creating = true }) {
                        Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (list.isEmpty() && query.isNotBlank()) "Create \"${query.trim()}\"" else "Create custom")
                    }
                }
            }
        }
        items(list, key = { it.id }) { ex ->
            AppCard(Modifier.fillMaxWidth().clickable { onPick(ex) }) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    ExerciseThumb(ex, 64.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(ex.name, style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Pill(ex.muscle.label)
                            EquipmentPill(ex.equipment)
                            if (ex.isCustom) CustomPill()
                        }
                        ExerciseLibrary.alternativeFor(ex)?.let {
                            Spacer(Modifier.height(4.dp))
                            Text("Alt: ${it.name}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
    if (creating && vm != null) {
        CustomExerciseDialog(
            vm, onDismiss = { creating = false }, prefillName = query.trim(),
            onSaved = { onPick(ExerciseLibrary.get(it.id)) },
        )
    }
}

@Composable
private fun CustomPill() = Pill("Custom", color = MaterialTheme.colorScheme.primaryContainer, textColor = MaterialTheme.colorScheme.onPrimaryContainer)

@Composable
fun LibraryScreen(vm: AppViewModel, nav: Nav) {
    ExercisePicker(
        vm = vm,
        onPick = { nav.openExercise(it.id) },
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        header = {
            Text("Exercises", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Tap any move to see how to do it.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
        },
    )
}

@Composable
fun ExerciseDetailScreen(vm: AppViewModel, exerciseId: String, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val ex = ExerciseLibrary.get(exerciseId)
    val alt = ExerciseLibrary.alternativeFor(ex)
    val replaces = ExerciseLibrary.all.filter { it.alternativeId == ex.id }
    val custom = vm.customExercises.firstOrNull { it.id == ex.id }
    var editing by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(ex.name, maxLines = 1) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    if (custom != null && !custom.archived) {
                        IconButton(onClick = { editing = true }) { Icon(Icons.Filled.Edit, "Edit exercise") }
                        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, "Delete exercise") }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.15f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer),
            ) {
                ExerciseFigure(ex.illustration, figureColors(), Modifier.fillMaxSize().padding(20.dp))
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Pill(ex.muscle.label)
                EquipmentPill(ex.equipment)
                if (ex.track == com.example.personalgym.data.TrackType.TIME) Pill("Timed")
                if (ex.isCustom) CustomPill()
            }
            ExerciseProgress(vm, ex)

            SectionTitle("How to do it")
            if (ex.steps.isEmpty()) {
                Text(
                    "No notes yet. Tap ✏️ to add your own steps.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ex.steps.forEachIndexed { i, step ->
                Row(Modifier.padding(vertical = 5.dp)) {
                    Box(
                        Modifier.size(26.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${i + 1}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(step, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 3.dp))
                }
            }
            if (ex.tip.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                AppCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp)) {
                        Text("💡", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(10.dp))
                        Text(ex.tip, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (alt != null) {
                SectionTitle("No ${ex.equipment.label.lowercase()}? Dumbbell alternative")
                AltCard(alt) { onOpen(alt.id) }
            }
            if (replaces.isNotEmpty()) {
                SectionTitle("Dumbbell substitute for")
                replaces.forEach { r ->
                    AltCard(r) { onOpen(r.id) }
                    Spacer(Modifier.height(8.dp))
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    if (editing && custom != null) CustomExerciseDialog(vm, onDismiss = { editing = false }, existing = custom)
    if (confirmDelete && custom != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${custom.name}?") },
            text = { Text("It disappears from the exercise list. Past workouts that used it keep showing it.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; vm.archiveCustomExercise(custom.id); onBack() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun AltCard(ex: Exercise, onClick: () -> Unit) {
    AppCard(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            ExerciseThumb(ex, 64.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(ex.name, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                EquipmentPill(ex.equipment)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
        }
    }
}

