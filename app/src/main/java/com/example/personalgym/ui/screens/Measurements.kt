package com.example.personalgym.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.personalgym.AppViewModel
import com.example.personalgym.data.BodySite
import com.example.personalgym.data.Measurement
import com.example.personalgym.data.WeightUnit
import com.example.personalgym.data.formatNumber
import com.example.personalgym.ui.components.AppCard
import com.example.personalgym.ui.components.LineChart
import com.example.personalgym.ui.components.ValueInputDialog
import com.example.personalgym.ui.components.filterDecimal
import com.example.personalgym.ui.components.parseDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private val CM_RANGE = 10.0..250.0
private val FAT_RANGE = 2.0..70.0

/** Converts a typed value to what is stored (cm or %), or null when out of range. */
private fun accept(site: BodySite, unit: WeightUnit, typed: Double): Double? {
    val stored = unit.measurementToStored(site, typed)
    return stored.takeIf { it in if (site.isPercent) FAT_RANGE else CM_RANGE }
}

private fun rangeText(site: BodySite, unit: WeightUnit): String =
    if (site.isPercent) "Enter between ${formatNumber(FAT_RANGE.start)}% and ${formatNumber(FAT_RANGE.endInclusive)}%"
    else "Enter between ${unit.showLength(CM_RANGE.start)} and ${unit.showLength(CM_RANGE.endInclusive)}"

private fun changeText(site: BodySite, unit: WeightUnit, diffStored: Double): String {
    val shown = unit.measurementFromStored(site, abs(diffStored))
    val sign = if (diffStored > 0) "+" else if (diffStored < 0) "−" else "±"
    return "$sign${formatNumber(shown)}${if (site.isPercent) "%" else " ${unit.lengthLabel}"}"
}

@Composable
fun MeasurementsCard(vm: AppViewModel) {
    val unit = vm.weightUnit
    var logging by remember { mutableStateOf(false) }
    var openSite by remember { mutableStateOf<BodySite?>(null) }
    val bySite = vm.measurements.groupBy { it.site }
    val dayFmt = remember { SimpleDateFormat("d MMM", Locale.getDefault()) }

    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Body measurements", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                FilledTonalButton(onClick = { logging = true }) { Text("Log") }
            }
            if (bySite.isEmpty()) {
                Text(
                    "Track your waist, arms and more. They often show fat loss or muscle gain before the scale does.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            BodySite.entries.forEach { site ->
                val entries = bySite[site] ?: return@forEach
                val latest = entries.last()
                val first = entries.first()
                Row(
                    Modifier.fillMaxWidth().clickable { openSite = site }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(site.label, Modifier.width(84.dp), style = MaterialTheme.typography.bodyMedium)
                    Text(unit.showMeasurement(site, latest.value), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                    if (entries.size > 1) {
                        Text(
                            "${changeText(site, unit, latest.value - first.value)} since ${dayFmt.format(Date(first.date))}",
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    if (logging) LogMeasurementsDialog(vm, onDismiss = { logging = false })
    openSite?.let { SiteHistoryDialog(vm, it, onDismiss = { openSite = null }) }
}

/** One field per site; blank fields are skipped, so you can log just your waist. */
@Composable
private fun LogMeasurementsDialog(vm: AppViewModel, onDismiss: () -> Unit) {
    val unit = vm.weightUnit
    val texts = remember { mutableStateMapOf<BodySite, String>() }
    val parsed = BodySite.entries.associateWith { site ->
        texts[site]?.takeIf { it.isNotBlank() }?.let { t -> parseDecimal(t)?.let { accept(site, unit, it) } }
    }
    val anyInvalid = BodySite.entries.any { !texts[it].isNullOrBlank() && parsed[it] == null }
    val valid = parsed.filterValues { it != null }.mapValues { it.value!! }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log measurements") },
        text = {
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Fill in any you measured today; leave the rest blank.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                BodySite.entries.forEach { site ->
                    val text = texts[site].orEmpty()
                    val bad = text.isNotBlank() && parsed[site] == null
                    OutlinedTextField(
                        value = text,
                        onValueChange = { texts[site] = filterDecimal(it) },
                        label = { Text("${site.label} (${unit.measurementLabel(site)})") },
                        isError = bad,
                        supportingText = if (bad) {
                            { Text(rangeText(site, unit)) }
                        } else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            Button(enabled = valid.isNotEmpty() && !anyInvalid, onClick = { vm.addMeasurements(valid); onDismiss() }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun SiteHistoryDialog(vm: AppViewModel, site: BodySite, onDismiss: () -> Unit) {
    val unit = vm.weightUnit
    val entries = vm.measurements.filter { it.site == site }
    val dateFmt = remember { SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()) }
    var editing by remember { mutableStateOf<Measurement?>(null) }
    var deleting by remember { mutableStateOf<Measurement?>(null) }

    if (entries.isEmpty()) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }
    val newestFirst = entries.asReversed()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(site.label) },
        text = {
            Column {
                if (entries.size >= 2) {
                    LineChart(entries.takeLast(12).map { unit.measurementFromStored(site, it.value).toFloat() }, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                }
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    items(newestFirst, key = { it.date }) { m ->
                        val prev = newestFirst.getOrNull(newestFirst.indexOf(m) + 1)
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(unit.showMeasurement(site, m.value), style = MaterialTheme.typography.titleMedium)
                                Text(
                                    dateFmt.format(Date(m.date)) + (prev?.let { " · ${changeText(site, unit, m.value - it.value)}" } ?: ""),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { editing = m }) { Icon(Icons.Filled.Edit, "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                            IconButton(onClick = { deleting = m }) { Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )

    editing?.let { m ->
        ValueInputDialog(
            title = "Edit ${site.label.lowercase()}",
            subtitle = dateFmt.format(Date(m.date)),
            label = "${site.label} (${unit.measurementLabel(site)})",
            initial = formatNumber(unit.measurementFromStored(site, m.value)),
            errorText = rangeText(site, unit),
            accept = { accept(site, unit, it) },
            onDismiss = { editing = null },
            onConfirm = { vm.updateMeasurement(m.date, site, it); editing = null },
        )
    }
    deleting?.let { m ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete this entry?") },
            text = { Text("${site.label} ${unit.showMeasurement(site, m.value)} on ${dateFmt.format(Date(m.date))} will be removed.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteMeasurement(m.date, site); deleting = null }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
}
