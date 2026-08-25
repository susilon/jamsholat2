package com.jamsholat2.android.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.google.android.gms.location.LocationServices
import com.jamsholat2.android.data.AppConfig
import com.jamsholat2.android.data.InfoTextItem
import com.jamsholat2.android.data.ScrollingData
import com.jamsholat2.android.ui.components.aquaFocusBorder
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsDialog(
    config: AppConfig,
    onDismiss: () -> Unit,
    onSave: (AppConfig) -> Unit,
    onUseLocation: (Double, Double) -> Unit
) {
    var locale by remember { mutableStateOf(config.locale) }
    var madhab by remember { mutableStateOf(config.madhab) }
    var calculation by remember { mutableStateOf(config.calculation) }
    var latlng by remember { mutableStateOf(config.latlngdata) }
    var fajrAngle by remember { mutableStateOf(config.fajrAngle.toString()) }
    var ishaAngle by remember { mutableStateOf(config.ishaAngle.toString()) }
    var imsak by remember { mutableStateOf(config.imsak.toString()) }
    var beepVolume by remember { mutableStateOf(config.beep.beepVolume) }
    var volumeSlider by remember { mutableStateOf((config.beep.beepVolume * 100).toInt().toFloat()) }
    var selectedTab by remember { mutableStateOf(0) }
    var newVideoName by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var locationStatus by remember { mutableStateOf("") }

    val tabs = listOf("General", "About", "Credits", "Video List")

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(decorFitsSystemWindows = false),
        title = { Text("General Settings") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().imePadding()) {
                // TV-friendly tab bar with aqua focus border and clear selected state
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tabs.forEachIndexed { idx, title ->
                        val interaction = remember(idx) { MutableInteractionSource() }
                        val isSelected = selectedTab == idx
                        val tabModifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .aquaFocusBorder(interaction, RoundedCornerShape(10.dp))
                        if (isSelected) {
                            Button(
                                onClick = { selectedTab = idx },
                                modifier = tabModifier,
                                interactionSource = interaction,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                colors = ButtonDefaults.buttonColors()
                            ) {
                                Text(title, maxLines = 1, fontSize = 12.sp)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { selectedTab = idx },
                                modifier = tabModifier,
                                interactionSource = interaction,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                            ) {
                                Text(title, maxLines = 1, fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                }
                Text(
                    "← → pindah tab  •  ↓ ke konten",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(Modifier.height(12.dp))
                when (selectedTab) {
                    0 -> {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            OutlinedTextField(value = locale, onValueChange = { locale = it }, label = { Text("Locale (id, en)") }, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(8.dp))
                            // Madhab dropdown
                            var expanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                                OutlinedTextField(
                                    value = if (madhab == "hanafi") "Hanafi" else "Syafii",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Mazhab") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    DropdownMenuItem(text = { Text("Syafii") }, onClick = { madhab = "syafii"; expanded = false })
                                    DropdownMenuItem(text = { Text("Hanafi") }, onClick = { madhab = "hanafi"; expanded = false })
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            // Calculation method
                            var calcExpanded by remember { mutableStateOf(false) }
                            val methods = listOf("Egyptian","Muslim World League","Karachi","UmmAlQura","Singapore","North America","Dubai","Qatar","Kuwait","MoonsightingCommittee")
                            ExposedDropdownMenuBox(expanded = calcExpanded, onExpandedChange = { calcExpanded = !calcExpanded }) {
                                OutlinedTextField(
                                    value = methods.getOrNull(calculation) ?: methods[0],
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Metode") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(calcExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(expanded = calcExpanded, onDismissRequest = { calcExpanded = false }) {
                                    methods.forEachIndexed { idx, name ->
                                        DropdownMenuItem(text = { Text(name) }, onClick = { calculation = idx; calcExpanded = false })
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = latlng,
                                onValueChange = { latlng = it },
                                label = { Text("Lat, Long") },
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    IconButton(onClick = {
                                        scope.launch {
                                            try {
                                                val fused = LocationServices.getFusedLocationProviderClient(context)
                                                // Request permission is handled outside; just try
                                                fused.lastLocation.addOnSuccessListener { loc ->
                                                    if (loc != null) {
                                                        latlng = "${loc.latitude}, ${loc.longitude}"
                                                        locationStatus = "Location updated"
                                                        onUseLocation(loc.latitude, loc.longitude)
                                                    } else {
                                                        locationStatus = "No location available"
                                                    }
                                                }.addOnFailureListener {
                                                    locationStatus = "Failed: ${it.message}"
                                                }
                                            } catch (e: Exception) {
                                                locationStatus = "Error: ${e.message}"
                                            }
                                        }
                                    }) { Icon(Icons.Default.MyLocation, contentDescription = "Use location") }
                                }
                            )
                            if (locationStatus.isNotEmpty()) Text(locationStatus, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = fajrAngle, onValueChange = { fajrAngle = it }, label = { Text("Sudut Subuh") }, modifier = Modifier.weight(1f))
                                OutlinedTextField(value = ishaAngle, onValueChange = { ishaAngle = it }, label = { Text("Sudut Isya") }, modifier = Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(value = imsak, onValueChange = { imsak = it }, label = { Text("Imsak (menit sebelum subuh)") }, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(8.dp))
                            Text("Beep Volume: ${(volumeSlider).toInt()}%")
                            Slider(value = volumeSlider, onValueChange = { volumeSlider = it; beepVolume = it/100f }, valueRange = 0f..100f)
                            Text("For Reference: https://github.com/batoulapps/adhan-js/blob/master/METHODS.md", style = MaterialTheme.typography.bodySmall)
                            Text("Jam komputer GMT: ${java.util.TimeZone.getDefault().rawOffset/3600000}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    1 -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text("Jam Sholat - Beta", style = MaterialTheme.typography.headlineSmall)
                            Text("Yuk kita bikin petunjuk waktu sholat dengan mudah.")
                            Text("jamsholat.susilon.com")
                            Spacer(Modifier.height(8.dp))
                            Text("Author : susilonurcahyo@gmail.com")
                        }
                    }
                    2 -> {
                        Column {
                            Text("Adhan js\nBootstrap 4\njQuery\nMoment js\nMoment-Hijri\nThe Roboto Light Fonts\nCKEditor 4")
                            Spacer(Modifier.height(8.dp))
                            Text("Video Credits:", style = MaterialTheme.typography.titleSmall)
                            Text("Tawaf around the Kaaba - Hajj and Umrah Youtube Channel")
                        }
                    }
                    3 -> {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            Text("Background Video List", style = MaterialTheme.typography.titleMedium)
                            Text("Copy file video MP4 ke folder videos untuk menambahkan video", style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(8.dp))
                            config.videolist.forEach { vid ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(vid, modifier = Modifier.weight(1f))
                                    TextButton(onClick = {
                                        val newList = config.videolist.filterNot { it == vid }
                                        onSave(config.copy(videolist = newList))
                                    }) { Text("Hapus") }
                                }
                                Divider()
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(value = newVideoName, onValueChange = { newVideoName = it }, label = { Text("Nama file video") }, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = {
                                if (newVideoName.isNotBlank()) {
                                    val newList = config.videolist + newVideoName.trim()
                                    onSave(config.copy(videolist = newList))
                                    newVideoName = ""
                                }
                            }) { Text("Tambah") }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val updated = config.copy(
                    locale = locale,
                    madhab = madhab,
                    calculation = calculation,
                    latlngdata = latlng,
                    fajrAngle = fajrAngle.toDoubleOrNull() ?: config.fajrAngle,
                    ishaAngle = ishaAngle.toDoubleOrNull() ?: config.ishaAngle,
                    imsak = imsak.toIntOrNull() ?: config.imsak,
                    beep = config.beep.copy(beepVolume = volumeSlider/100f)
                )
                onSave(updated)
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun LabelEditDialog(
    title: String,
    initialValue: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var value by remember { mutableStateOf(initialValue) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(decorFitsSystemWindows = false),
        title = { Text("Setting Label $title") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
            ) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Label $title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    maxLines = 1,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
                    )
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(value); onDismiss() }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun PrayerEditDialog(
    label: String,
    initialLabel: String,
    initialIqomah: Int,
    initialAdjustment: Int,
    initialDuration: Int,
    onDismiss: () -> Unit,
    onSave: (String, Int, Int, Int) -> Unit
) {
    var lab by remember { mutableStateOf(initialLabel) }
    var iqomah by remember { mutableStateOf(initialIqomah.toString()) }
    var adj by remember { mutableStateOf(initialAdjustment.toString()) }
    var duration by remember { mutableStateOf(initialDuration.toString()) }
    val isTerbit = label == "Terbit"
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(decorFitsSystemWindows = false),
        title = { Text("Setting Waktu $label") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .imePadding()
            ) {
                OutlinedTextField(
                    value = lab,
                    onValueChange = { lab = it },
                    label = { Text("Label $label") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    maxLines = 1,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide(); focusManager.clearFocus() })
                )
                if (!isTerbit) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = iqomah,
                        onValueChange = { iqomah = it },
                        label = { Text("Waktu Iqomah (menit, setelah adzan)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = adj,
                    onValueChange = { adj = it },
                    label = { Text("Adjustment Waktu (menit)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it },
                    label = { Text("Durasi Sholat (menit, layar off)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide(); focusManager.clearFocus() })
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(lab, iqomah.toIntOrNull() ?: initialIqomah, adj.toIntOrNull() ?: initialAdjustment, duration.toIntOrNull() ?: initialDuration)
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ScrollingTextDialog(
    initialValue: String,
    initialValueOnPray: String,
    initialSpeed: Int,
    onDismiss: () -> Unit,
    onSave: (String, String, Int) -> Unit
) {
    var value by remember { mutableStateOf(initialValue) }
    var valueOnPray by remember { mutableStateOf(initialValueOnPray) }
    var speed by remember { mutableStateOf(initialSpeed.toFloat()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(decorFitsSystemWindows = false),
        title = { Text("Setting Scrolling Text") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).imePadding()) {
                OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text("Scrolling Text") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = valueOnPray, onValueChange = { valueOnPray = it }, label = { Text("Scrolling Text Saat Sholat") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                Spacer(Modifier.height(8.dp))
                Text("Speed: ${speed.toInt()}")
                Slider(value = speed, onValueChange = { speed = it }, valueRange = 1f..9f, steps = 7)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(value, valueOnPray, speed.toInt()); onDismiss() }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun InfoTextDialog(
    items: List<InfoTextItem>,
    onDismiss: () -> Unit,
    onSave: (List<InfoTextItem>) -> Unit
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var editingItems by remember { mutableStateOf(items.toMutableList()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(decorFitsSystemWindows = false),
        title = { Text("Setting Info") },
        text = {
            if (selectedIndex == null) {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(editingItems.size) { idx ->
                        val item = editingItems[idx]
                        val badge = if (item.enable) "Active" else "Not Active"
                        ListItem(
                            headlineContent = { Text(item.title) },
                            supportingContent = {
                                if (idx == 7) Text("(hanya muncul saat dzuhur di hari jumat)", style = MaterialTheme.typography.bodySmall)
                                else Text(badge, style = MaterialTheme.typography.bodySmall)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { selectedIndex = idx }) { Text("Edit") }
                            if (idx != 7) {
                                TextButton(onClick = {
                                    val updated = item.copy(enable = !item.enable)
                                    editingItems = editingItems.toMutableList().also { it[idx] = updated }
                                }) { Text(if (item.enable) "Disable" else "Enable") }
                            }
                        }
                        Divider()
                    }
                }
            } else {
                val idx = selectedIndex!!
                var title by remember(idx) { mutableStateOf(editingItems[idx].title) }
                var content by remember(idx) { mutableStateOf(editingItems[idx].content) }
                var duration by remember(idx) { mutableStateOf(editingItems[idx].duration.toString()) }
                var enable by remember(idx) { mutableStateOf(editingItems[idx].enable) }

                Column(modifier = Modifier.verticalScroll(rememberScrollState()).imePadding()) {
                    if (idx == 7) Text("Info Khusus Saat Khotbah Jum'at", style = MaterialTheme.typography.titleSmall)
                    else {
                        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Judul Info") }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = duration, onValueChange = { duration = it }, label = { Text("Durasi (detik)") }, modifier = Modifier.weight(1f))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Active")
                                Switch(checked = enable, onCheckedChange = { enable = it })
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Content (HTML allowed)") }, modifier = Modifier.fillMaxWidth().height(150.dp), minLines = 4)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { selectedIndex = null }) { Text("Cancel") }
                        Button(onClick = {
                            val updated = editingItems[idx].copy(title = title, content = content, duration = duration.toIntOrNull() ?: editingItems[idx].duration, enable = enable)
                            editingItems = editingItems.toMutableList().also { it[idx] = updated }
                            selectedIndex = null
                        }) { Text("Save Info") }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(editingItems)
                onDismiss()
            }) { Text("Save All") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
