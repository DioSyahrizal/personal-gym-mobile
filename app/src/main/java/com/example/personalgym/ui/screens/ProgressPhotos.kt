@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.personalgym.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.personalgym.AppViewModel
import com.example.personalgym.data.PhotoPose
import com.example.personalgym.data.PhotoStore
import com.example.personalgym.data.ProgressPhoto
import com.example.personalgym.ui.components.AppCard
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Goals-tab card: latest photo per pose, add, and a link to the full gallery. */
@Composable
fun ProgressPhotosCard(vm: AppViewModel) {
    var adding by remember { mutableStateOf(false) }
    var gallery by rememberSaveable { mutableStateOf(false) }
    val dayFmt = remember { SimpleDateFormat("d MMM", Locale.getDefault()) }

    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Progress photos", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                FilledTonalButton(onClick = { adding = true }) { Text("Add") }
            }
            if (vm.photos.isEmpty()) {
                Text(
                    "Take front, side and back photos every few weeks to see changes the scale misses. " +
                        "They stay on this phone only — not in backups or the cloud.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            } else {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PhotoPose.entries.forEach { pose ->
                        val latest = vm.photos.lastOrNull { it.pose == pose }
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier.fillMaxWidth().aspectRatio(3f / 4f).clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                    .clickable { gallery = true },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (latest != null) PhotoImage(latest, 400, Modifier.fillMaxSize())
                                else Text("No ${pose.label.lowercase()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                pose.label + (latest?.let { " · " + dayFmt.format(Date(it.date)) } ?: ""),
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
                TextButton(onClick = { gallery = true }, contentPadding = PaddingValues(0.dp)) {
                    Text("All photos & compare (${vm.photos.size})")
                }
            }
        }
    }

    if (adding) AddPhotoDialog(vm, onDismiss = { adding = false })
    if (gallery) {
        Dialog(onDismissRequest = { gallery = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            PhotoGallery(vm, onBack = { gallery = false }, onAdd = { adding = true })
        }
    }
}

@Composable
private fun AddPhotoDialog(vm: AppViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pose by rememberSaveable { mutableStateOf(nextPose(vm.photos)) }
    var busy by remember { mutableStateOf(false) }
    var cameraUri by remember { mutableStateOf<android.net.Uri?>(null) }

    fun save(uri: android.net.Uri) {
        busy = true
        scope.launch {
            runCatching { vm.addPhoto(uri, pose) }
                .onSuccess { onDismiss() }
                .onFailure { Toast.makeText(context, it.message ?: "Couldn't save that photo.", Toast.LENGTH_LONG).show() }
            busy = false
        }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = cameraUri
        if (ok && uri != null) save(uri)
    }
    // System photo picker: no storage permission needed.
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(::save) }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Add progress photo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Which angle?", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PhotoPose.entries.forEach { p -> FilterChip(selected = pose == p, onClick = { pose = p }, label = { Text(p.label) }) }
                }
                Text(
                    "Tip: same spot, same light, same time of day makes comparisons honest.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = { PhotoStore.cameraTarget(context).also { cameraUri = it; camera.launch(it) } },
                    enabled = !busy, modifier = Modifier.fillMaxWidth(),
                ) { Text(if (busy) "Saving…" else "Take photo") }
                OutlinedButton(
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    enabled = !busy, modifier = Modifier.fillMaxWidth(),
                ) { Text("Choose from gallery") }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") } },
    )
}

/** Suggests the angle you haven't taken yet today, front first. */
private fun nextPose(photos: List<ProgressPhoto>): PhotoPose {
    val today = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
    val doneToday = photos.filter { SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(it.date)) == today }.map { it.pose }.toSet()
    return PhotoPose.entries.firstOrNull { it !in doneToday } ?: PhotoPose.FRONT
}

@Composable
private fun PhotoGallery(vm: AppViewModel, onBack: () -> Unit, onAdd: () -> Unit) {
    var pose by rememberSaveable { mutableStateOf<PhotoPose?>(null) }
    var viewing by remember { mutableStateOf<ProgressPhoto?>(null) }
    var comparing by remember { mutableStateOf(false) }
    val selected = remember { mutableStateListOf<ProgressPhoto>() }
    var showCompare by remember { mutableStateOf(false) }
    val dayFmt = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }
    val shown = vm.photos.filter { pose == null || it.pose == pose }.asReversed()

    BackHandler {
        when {
            comparing -> { comparing = false; selected.clear() }
            else -> onBack()
        }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text(if (comparing) "Pick 2 photos (${selected.size}/2)" else "Progress photos") },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                    actions = {
                        if (comparing) {
                            TextButton(onClick = { comparing = false; selected.clear() }) { Text("Cancel") }
                        } else if (vm.photos.size >= 2) {
                            TextButton(onClick = { comparing = true }) { Text("Compare") }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
            },
            bottomBar = {
                if (comparing) {
                    Button(
                        onClick = { showCompare = true },
                        enabled = selected.size == 2,
                        modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
                    ) { Text("Compare side by side") }
                }
            },
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = pose == null, onClick = { pose = null }, label = { Text("All") })
                    PhotoPose.entries.forEach { p -> FilterChip(selected = pose == p, onClick = { pose = if (pose == p) null else p }, label = { Text(p.label) }) }
                }
                if (shown.isEmpty()) {
                    Column(Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No photos yet", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = onAdd) { Text("Add a photo") }
                    }
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(shown, key = { it.file }) { photo ->
                        val isSelected = photo in selected
                        Column {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(3f / 4f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .then(if (isSelected) Modifier.border(BorderStroke(3.dp, MaterialTheme.colorScheme.primary), RoundedCornerShape(10.dp)) else Modifier)
                                    .clickable {
                                        if (comparing) {
                                            if (isSelected) selected.remove(photo) else if (selected.size < 2) selected.add(photo)
                                        } else viewing = photo
                                    },
                            ) {
                                PhotoImage(photo, 360, Modifier.fillMaxSize())
                                if (isSelected) {
                                    Box(
                                        Modifier.align(Alignment.TopEnd).padding(6.dp).clip(RoundedCornerShape(50))
                                            .background(MaterialTheme.colorScheme.primary).padding(2.dp),
                                    ) { Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.onPrimary) }
                                }
                            }
                            Text(
                                "${photo.pose.label} · ${dayFmt.format(Date(photo.date))}",
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp), maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }

    viewing?.let { p -> PhotoViewer(vm, p, onDismiss = { viewing = null }) }
    if (showCompare && selected.size == 2) {
        val (a, b) = selected.sortedBy { it.date }
        CompareDialog(a, b, onDismiss = { showCompare = false })
    }
}

@Composable
private fun PhotoViewer(vm: AppViewModel, photo: ProgressPhoto, onDismiss: () -> Unit) {
    var confirmDelete by remember { mutableStateOf(false) }
    val dateFmt = remember { SimpleDateFormat("EEEE, d MMM yyyy", Locale.getDefault()) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Color.Black) {
            Box(Modifier.fillMaxSize()) {
                PhotoImage(photo, 1600, Modifier.fillMaxSize(), ContentScale.Fit)
                Row(
                    Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.5f)).padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Close", tint = Color.White) }
                    Column(Modifier.weight(1f)) {
                        Text(photo.pose.label, color = Color.White, style = MaterialTheme.typography.titleMedium)
                        Text(dateFmt.format(Date(photo.date)), color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, "Delete photo", tint = Color.White) }
                }
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this photo?") },
            text = { Text("It's only stored on this phone, so it can't be recovered.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; vm.deletePhoto(photo); onDismiss() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun CompareDialog(before: ProgressPhoto, after: ProgressPhoto, onDismiss: () -> Unit) {
    val dayFmt = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }
    val days = TimeUnit.MILLISECONDS.toDays(after.date - before.date)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Color.Black) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Close", tint = Color.White) }
                    Text(
                        if (days == 0L) "Same day" else "$days days apart",
                        color = Color.White, style = MaterialTheme.typography.titleMedium,
                    )
                }
                Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Before" to before, "After" to after).forEach { (label, p) ->
                        Column(Modifier.weight(1f).fillMaxSize()) {
                            PhotoImage(p, 1000, Modifier.fillMaxWidth().weight(1f), ContentScale.Fit)
                            Text(
                                "$label · ${p.pose.label}\n${dayFmt.format(Date(p.date))}",
                                color = Color.White, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoImage(photo: ProgressPhoto, targetPx: Int, modifier: Modifier, scale: ContentScale = ContentScale.Crop) {
    val context = LocalContext.current
    val bitmap by produceState<android.graphics.Bitmap?>(null, photo.file, targetPx) {
        value = PhotoStore.load(context, photo.file, targetPx)
    }
    Box(modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest), contentAlignment = Alignment.Center) {
        bitmap?.let { Image(it.asImageBitmap(), contentDescription = "${photo.pose.label} photo", modifier = Modifier.fillMaxSize(), contentScale = scale) }
    }
}
