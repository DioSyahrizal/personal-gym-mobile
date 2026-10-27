@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.personalgym.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.personalgym.AppViewModel
import com.example.personalgym.data.Backup
import com.example.personalgym.ui.components.AppCard
import com.example.personalgym.ui.components.SectionTitle
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(vm: AppViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<Backup?>(null) }
    var busy by remember { mutableStateOf(false) }
    val dateFmt = remember { SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()) }

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_LONG).show()

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            runCatching { vm.exportBackup(uri) }
                .onSuccess { toast("Backup saved · ${plural(it, "workout")}") }
                .onFailure { toast("Backup failed: ${it.message}") }
            busy = false
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            runCatching { vm.readBackup(uri) }
                .onSuccess { pending = it }
                .onFailure { toast(it.message ?: "Couldn't read that file.") }
            busy = false
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        ) {
            SectionTitle("Backup & restore")
            AppCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Save your workouts, goals and weight log to a file — keep it in Google Drive, " +
                            "Downloads or send it to yourself. Restore it on a new phone or after reinstalling.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        summary(vm.sessions.size, vm.goals.size, vm.weights.size),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        if (vm.lastBackupAt == 0L) "Never backed up" else "Last backup: ${dateFmt.format(Date(vm.lastBackupAt))}",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (vm.lastBackupAt == 0L) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                    )
                    Button(
                        onClick = {
                            val day = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                            exportLauncher.launch("personal-gym-backup-$day.json")
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Export backup") }
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Restore from backup file") }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Your data is also included in Android's automatic Google backup, if it's turned on in your phone's settings.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )

            SectionTitle("Training")
            AppCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Dumbbell-only mode", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Swap machine, cable & barbell moves for dumbbell alternatives.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Switch(checked = vm.dumbbellOnly, onCheckedChange = vm::updateDumbbellOnly)
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    pending?.let { b ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("Restore this backup?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (b.exportedAt > 0) Text("Saved ${dateFmt.format(Date(b.exportedAt))}", style = MaterialTheme.typography.labelLarge)
                    Text(summary(b.sessions.size, b.goals.size, b.weights.size))
                    Text(
                        "This replaces everything currently in the app " +
                            "(${plural(vm.sessions.size, "workout")}, ${plural(vm.goals.size, "goal")}). Export a backup first if you want to keep it.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    vm.restore(b)
                    pending = null
                    toast("Restored ${plural(b.sessions.size, "workout")}")
                }) { Text("Replace & restore") }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("Cancel") } },
        )
    }
}

private fun plural(n: Int, one: String, many: String = one + "s") = "$n " + if (n == 1) one else many

private fun summary(workouts: Int, goals: Int, weights: Int) =
    "${plural(workouts, "workout")} · ${plural(goals, "goal")} · ${plural(weights, "weight entry", "weight entries")}"
