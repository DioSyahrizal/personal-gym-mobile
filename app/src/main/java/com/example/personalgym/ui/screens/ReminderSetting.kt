@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.personalgym.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.personalgym.AppViewModel
import com.example.personalgym.ui.components.AppCard
import java.util.Calendar

@Composable
fun ReminderSetting(vm: AppViewModel) {
    val context = LocalContext.current
    var picking by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }
    val is24h = DateFormat.is24HourFormat(context)
    val timeText = remember(vm.reminderMinutes, is24h) {
        val c = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, vm.reminderMinutes / 60)
            set(Calendar.MINUTE, vm.reminderMinutes % 60)
        }
        DateFormat.getTimeFormat(context).format(c.time)
    }
    // Reminders are notifications, so turning them on asks for that permission if needed.
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionDenied = !granted
        vm.updateReminder(granted)
    }
    fun enable() {
        val needsAsk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsAsk) permission.launch(Manifest.permission.POST_NOTIFICATIONS) else vm.updateReminder(true)
    }

    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Workout reminders", style = MaterialTheme.typography.titleSmall)
                    Text(
                        if (vm.reminderEnabled) "On training days at $timeText. Skipped on rest days and once you've trained."
                        else "Get a nudge on training days.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Switch(checked = vm.reminderEnabled, onCheckedChange = { on -> if (on) enable() else vm.updateReminder(false) })
            }
            if (vm.reminderEnabled) {
                OutlinedButton(onClick = { picking = true }, Modifier.padding(top = 8.dp)) { Text("Time: $timeText") }
            }
            if (permissionDenied && !vm.reminderEnabled) {
                Text(
                    "Notifications are blocked for Personal Gym. Allow them in your phone's app settings to get reminders.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }

    if (picking) {
        val state = rememberTimePickerState(vm.reminderMinutes / 60, vm.reminderMinutes % 60, is24h)
        AlertDialog(
            onDismissRequest = { picking = false },
            title = { Text("Reminder time") },
            text = { TimePicker(state) },
            confirmButton = {
                TextButton(onClick = { vm.updateReminder(true, state.hour * 60 + state.minute); picking = false }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text("Cancel") } },
        )
    }
}
