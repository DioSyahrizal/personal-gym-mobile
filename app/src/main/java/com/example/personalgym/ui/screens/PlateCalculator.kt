@file:OptIn(ExperimentalLayoutApi::class)

package com.example.personalgym.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.personalgym.data.PlateMath
import com.example.personalgym.data.WeightUnit
import com.example.personalgym.data.formatNumber
import com.example.personalgym.ui.components.filterDecimal
import com.example.personalgym.ui.components.parseDecimal

/** Shows which plates go on each side of the bar for a target weight. */
@Composable
fun PlateCalculatorDialog(unit: WeightUnit, initialKg: Double, onDismiss: () -> Unit) {
    val start = initialKg.takeIf { it > 0 }?.let { formatNumber(unit.fromKg(it)) } ?: ""
    var targetText by remember { mutableStateOf(start) }
    var bar by remember { mutableStateOf(PlateMath.bars(unit).first()) }
    val target = parseDecimal(targetText)
    val result = target?.let { PlateMath.calculate(it, bar, PlateMath.plates(unit)) }
    val u = unit.label

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Plate calculator") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = targetText, onValueChange = { targetText = filterDecimal(it) },
                    label = { Text("Total weight ($u)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Bar", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PlateMath.bars(unit).forEach { b ->
                        FilterChip(selected = bar == b, onClick = { bar = b }, label = { Text("${formatNumber(b)} $u") })
                    }
                }
                when {
                    result == null -> Text(
                        "Enter the total weight including the bar.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    result.perSide.isEmpty() -> Text(
                        if (result.exact) "Just the empty bar." else "That's lighter than the ${formatNumber(bar)} $u bar.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    else -> {
                        BarDiagram(result.perSide, unit)
                        Text("Each side: " + result.perSide.joinToString(" + ") { formatNumber(it) } + " $u", style = MaterialTheme.typography.titleSmall)
                        if (!result.exact) {
                            Text(
                                "Closest you can load is ${formatNumber(result.loaded)} $u (${formatNumber(result.remainder)} $u short).",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

/** One side of the bar: sleeve with plates drawn heaviest-first, height scaled by plate size. */
@Composable
private fun BarDiagram(perSide: List<Double>, unit: WeightUnit) {
    val biggest = PlateMath.plates(unit).first()
    Row(Modifier.fillMaxWidth().height(96.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(28.dp).height(8.dp).background(MaterialTheme.colorScheme.outline))
        Box(Modifier.width(6.dp).height(28.dp).background(MaterialTheme.colorScheme.outline))
        perSide.forEach { plate ->
            val h = (36 + 60 * plate / biggest).dp
            Box(
                Modifier.padding(horizontal = 1.dp).width(if (plate >= biggest / 2) 16.dp else 11.dp).height(h)
                    .clip(RoundedCornerShape(3.dp)).background(plateColor(plate, unit)),
                contentAlignment = Alignment.Center,
            ) {}
        }
        Box(Modifier.weight(1f).height(8.dp).background(MaterialTheme.colorScheme.outline))
        Spacer(Modifier.width(4.dp))
    }
}

/** Roughly the competition colours, so the picture matches what's on the rack. */
private fun plateColor(plate: Double, unit: WeightUnit): Color {
    val kg = if (unit == WeightUnit.KG) plate else unit.toKg(plate)
    return when {
        kg >= 24 -> Color(0xFFE53935)
        kg >= 19 -> Color(0xFF1E88E5)
        kg >= 14 -> Color(0xFFFDD835)
        kg >= 9 -> Color(0xFF43A047)
        kg >= 4.5 -> Color(0xFFECEFF1)
        else -> Color(0xFF90A4AE)
    }
}
