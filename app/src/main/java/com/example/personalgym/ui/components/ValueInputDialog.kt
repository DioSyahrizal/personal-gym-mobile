package com.example.personalgym.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

/** Typed text with only digits and one kind of decimal separator kept; `,` is read as `.`. */
fun parseDecimal(text: String): Double? = text.replace(',', '.').toDoubleOrNull()

fun filterDecimal(text: String): String = text.filter { it.isDigit() || it == '.' || it == ',' }.take(6)

/**
 * Single-number dialog. The pre-filled value starts selected so typing replaces it.
 * [accept] turns the typed number into the value to save, or null when it is out of range.
 */
@Composable
fun ValueInputDialog(
    title: String,
    label: String,
    initial: String,
    errorText: String,
    accept: (Double) -> Double?,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
    subtitle: String? = null,
) {
    var field by remember { mutableStateOf(TextFieldValue(initial, TextRange(0, initial.length))) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val value = parseDecimal(field.text)?.let(accept)
    val showError = field.text.isNotBlank() && value == null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                subtitle?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                }
                OutlinedTextField(
                    value = field,
                    onValueChange = { field = it.copy(text = filterDecimal(it.text)) },
                    label = { Text(label) },
                    isError = showError,
                    supportingText = if (showError) {
                        { Text(errorText) }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.focusRequester(focus),
                )
            }
        },
        confirmButton = { Button(enabled = value != null, onClick = { value?.let(onConfirm) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
