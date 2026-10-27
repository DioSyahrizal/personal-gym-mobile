@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.personalgym.ui.screens

import android.media.AudioManager
import android.media.ToneGenerator
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.personalgym.AppViewModel
import com.example.personalgym.Countdown
import com.example.personalgym.data.ExerciseLibrary
import com.example.personalgym.data.ExerciseLog
import com.example.personalgym.data.SetLog
import com.example.personalgym.data.TrackType
import com.example.personalgym.data.WorkoutSession
import com.example.personalgym.ui.Nav
import com.example.personalgym.ui.components.AppCard
import com.example.personalgym.ui.components.ExerciseThumb
import com.example.personalgym.ui.components.ProgressBar
import kotlinx.coroutines.delay

@Composable
fun SessionScreen(
    vm: AppViewModel,
    session: WorkoutSession,
    nav: Nav,
    onMinimize: () -> Unit,
    onFinished: (WorkoutSession) -> Unit,
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { now = System.currentTimeMillis(); delay(1000) }
    }
    // Keep the screen awake while training.
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    var confirmFinish by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }
    var swapIndex by remember { mutableStateOf<Int?>(null) }
    var menuOpen by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onMinimize) { Icon(Icons.Filled.KeyboardArrowDown, "Minimize") } },
                title = {
                    Column {
                        Text(session.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            formatClock((now - session.startedAt) / 1000),
                            style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                actions = {
                    Button(onClick = { confirmFinish = true }) { Text("Finish") }
                    Box {
                        IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, "More") }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Discard workout") },
                                leadingIcon = { Icon(Icons.Filled.Delete, null) },
                                onClick = { menuOpen = false; confirmDiscard = true },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            vm.countdown?.let { CountdownBar(it, vm) }
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                val f = if (session.totalSets == 0) 0f else session.completedSets.toFloat() / session.totalSets
                Text(
                    "${session.completedSets} of ${session.totalSets} sets",
                    style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                ProgressBar(f)
            }
            itemsIndexed(session.exercises, key = { i, e -> "$i-${e.exerciseId}" }) { i, log ->
                ExerciseLogCard(
                    vm, i, log,
                    onInfo = { nav.openExercise(log.exerciseId) },
                    onSwap = { swapIndex = i },
                )
            }
            item {
                OutlinedButton(onClick = { showPicker = true }, Modifier.fillMaxWidth().height(50.dp)) {
                    Icon(Icons.Filled.Add, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Add exercise")
                }
                Spacer(Modifier.height(80.dp))
            }
        }
    }

    if (confirmFinish) {
        val unfinished = session.totalSets - session.completedSets
        AlertDialog(
            onDismissRequest = { confirmFinish = false },
            title = { Text("Finish workout?") },
            text = {
                Text(
                    if (unfinished > 0) "You still have $unfinished set(s) not checked off. Only completed sets count toward your stats."
                    else "Great job — every set is done!",
                )
            },
            confirmButton = {
                Button(onClick = { confirmFinish = false; vm.finishWorkout()?.let(onFinished) }) { Text("Finish") }
            },
            dismissButton = { TextButton(onClick = { confirmFinish = false }) { Text("Keep going") } },
        )
    }
    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard workout?") },
            text = { Text("This workout won't be saved.") },
            confirmButton = {
                TextButton(onClick = { confirmDiscard = false; vm.discardWorkout(); onMinimize() }) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Cancel") } },
        )
    }
    swapIndex?.let { idx ->
        val current = session.exercises.getOrNull(idx)
        val options = current?.let { vm.swapOptions(it.exerciseId) }.orEmpty()
        AlertDialog(
            onDismissRequest = { swapIndex = null },
            title = { Text("Swap exercise") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (options.isEmpty()) Text("No direct alternative for this move. Use \"Add exercise\" to pick something else.")
                    options.forEach { o ->
                        AppCard(Modifier.fillMaxWidth().clickable { vm.swapExercise(idx, o.id); swapIndex = null }) {
                            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                ExerciseThumb(o, 52.dp)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(o.name, style = MaterialTheme.typography.titleSmall)
                                    Text(o.equipment.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { swapIndex = null }) { Text("Close") } },
        )
    }
    if (showPicker) {
        Dialog(onDismissRequest = { showPicker = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                ExercisePicker(
                    onPick = { vm.addExercise(it.id); showPicker = false },
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
}

@Composable
private fun ExerciseLogCard(vm: AppViewModel, index: Int, log: ExerciseLog, onInfo: () -> Unit, onSwap: () -> Unit) {
    val ex = ExerciseLibrary.get(log.exerciseId)
    val timed = ex.track == TrackType.TIME
    var menu by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val hasSwap = vm.swapOptions(ex.id).isNotEmpty()

    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ExerciseThumb(ex, 64.dp, Modifier.clickable(onClick = onInfo))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f).clickable(onClick = onInfo)) {
                    Text(ex.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${ex.muscle.label} · ${ex.equipment.label} · tap for how-to",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (hasSwap) {
                    IconButton(onClick = onSwap) { Icon(Icons.Filled.Refresh, "Swap", tint = MaterialTheme.colorScheme.secondary) }
                }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "Options") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Remove exercise") }, onClick = { menu = false; vm.removeExercise(index) })
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            // Header row
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                HeaderCell("SET", Modifier.width(40.dp))
                HeaderCell("TARGET", Modifier.weight(1f))
                if (ex.usesWeight) HeaderCell("KG", Modifier.weight(1f))
                HeaderCell(if (timed) "SEC" else "REPS", Modifier.weight(1f))
                Spacer(Modifier.width(if (timed) 80.dp else 44.dp))
            }
            log.sets.forEachIndexed { si, set ->
                SetRow(
                    index = si, set = set, usesWeight = ex.usesWeight, timed = timed,
                    onChange = { vm.updateSet(index, si, it) },
                    onToggle = { focus.clearFocus(); vm.toggleSetDone(index, si) },
                    onPlay = { focus.clearFocus(); vm.startTimedSet(index, si) },
                    onRemove = { vm.removeSet(index, si) },
                )
            }
            TextButton(onClick = { vm.addSet(index) }) {
                Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add set")
            }
        }
    }
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier) {
    Text(
        text, modifier, style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
    )
}

@Composable
private fun SetRow(
    index: Int,
    set: SetLog,
    usesWeight: Boolean,
    timed: Boolean,
    onChange: (SetLog) -> Unit,
    onToggle: () -> Unit,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
) {
    val bg = if (set.done) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "${index + 1}", Modifier.width(40.dp).clickable(onClick = onRemove),
            textAlign = TextAlign.Center, fontWeight = FontWeight.Bold,
        )
        Text(
            if (timed) formatSeconds(set.target) else "${set.target}",
            Modifier.weight(1f), textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (usesWeight) {
            NumberField(
                value = fmtKg(set.weight), decimal = true,
                onValue = { onChange(set.copy(weight = it.toDoubleOrNull() ?: 0.0)) },
                modifier = Modifier.weight(1f),
            )
        }
        NumberField(
            value = set.reps.toString(), decimal = false,
            onValue = { onChange(set.copy(reps = it.toIntOrNull() ?: 0)) },
            modifier = Modifier.weight(1f),
        )
        Row(
            Modifier.width(if (timed) 80.dp else 44.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        ) {
            if (timed && !set.done) {
                SmallRoundButton(onPlay, filled = false) { Icon(Icons.Filled.PlayArrow, "Start timer", Modifier.size(18.dp)) }
            }
            SmallRoundButton(onToggle, filled = set.done) { Icon(Icons.Filled.Check, "Done", Modifier.size(18.dp)) }
        }
    }
}

@Composable
private fun SmallRoundButton(onClick: () -> Unit, filled: Boolean, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides
                if (filled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        ) { content() }
    }
}

@Composable
private fun NumberField(value: String, decimal: Boolean, onValue: (String) -> Unit, modifier: Modifier) {
    // Keep the raw text locally so typing "12." isn't immediately reformatted.
    var text by remember { mutableStateOf(if (value == "0") "" else value) }
    LaunchedEffect(value) {
        if ((text.toDoubleOrNull() ?: 0.0) != (value.toDoubleOrNull() ?: 0.0)) text = if (value == "0") "" else value
    }
    BasicTextField(
        value = text,
        onValueChange = { new ->
            val filtered = new.filter { it.isDigit() || (decimal && (it == '.' || it == ',')) }.replace(',', '.').take(6)
            text = filtered
            onValue(filtered)
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        textStyle = TextStyle(
            color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(vertical = 8.dp),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.Center) {
                if (text.isEmpty()) Text("0", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
                inner()
            }
        },
    )
}

@Composable
private fun CountdownBar(c: Countdown, vm: AppViewModel) {
    var remainingMs by remember(c) { mutableLongStateOf(c.endsAt - System.currentTimeMillis()) }
    LaunchedEffect(c) {
        while (true) {
            remainingMs = c.endsAt - System.currentTimeMillis()
            if (remainingMs <= 0) break
            delay(200)
        }
        beep()
        vm.onCountdownFinished()
    }
    val secs = ((remainingMs + 999) / 1000).coerceAtLeast(0)
    val isRest = c.label == "Rest"
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, tonalElevation = 4.dp) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (isRest) "Rest — next set soon" else c.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isRest) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                    )
                    Text(formatClock(secs), style = MaterialTheme.typography.headlineMedium)
                }
                OutlinedButton(onClick = { vm.addCountdownTime(15) }) { Text("+15s") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { vm.skipCountdown() }) { Text("Skip") }
            }
            Spacer(Modifier.height(8.dp))
            ProgressBar(
                (remainingMs.toFloat() / (c.totalSec * 1000f)).coerceIn(0f, 1f),
                color = if (isRest) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                height = 6.dp,
            )
        }
    }
}

private fun beep() {
    runCatching {
        val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
        tg.startTone(ToneGenerator.TONE_PROP_BEEP2, 400)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ tg.release() }, 600)
    }
}

fun formatClock(totalSec: Long): String {
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

@Composable
fun WorkoutSummaryDialog(s: WorkoutSession, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Workout complete! 🎉") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(s.title, style = MaterialTheme.typography.titleMedium)
                SummaryRow("Duration", formatClock(s.durationSec))
                SummaryRow("Sets completed", "${s.completedSets}")
                SummaryRow("Volume lifted", "${fmtKg(s.volume)} kg")
                SummaryRow("Est. calories", "${s.calories} kcal")
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Nice!") } },
    )
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Bold)
    }
}
