package com.qrdatasender.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qrdatasender.app.AppViewModel
import com.qrdatasender.app.model.*

private enum class Tab { SCAN, SERVERS, SETTINGS, ABOUT }

@Composable
fun QrDataSenderApp(vm: AppViewModel) {
    val servers by vm.servers.collectAsStateWithLifecycle()
    val selectedId by vm.selectedId.collectAsStateWithLifecycle()
    val darkTheme by vm.darkTheme.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(Tab.SCAN) }
    var editing by remember { mutableStateOf<ServerConfig?>(null) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == Tab.SERVERS,
                    onClick = { tab = Tab.SERVERS },
                    icon = { Icon(Icons.Default.Dns, null) },
                    label = { Text("Servers") }
                )
                NavigationBarItem(
                    selected = tab == Tab.SCAN,
                    onClick = { tab = Tab.SCAN },
                    icon = {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shadowElevation = 6.dp,
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.QrCodeScanner, "Scan", Modifier.size(29.dp))
                            }
                        }
                    },
                    label = { Text("Scan", fontWeight = FontWeight.SemiBold) }
                )
                NavigationBarItem(
                    selected = tab == Tab.SETTINGS,
                    onClick = { tab = Tab.SETTINGS },
                    icon = { Icon(Icons.Default.Settings, null) },
                    label = { Text("Settings") }
                )
                NavigationBarItem(
                    selected = tab == Tab.ABOUT,
                    onClick = { tab = Tab.ABOUT },
                    icon = { Icon(Icons.Default.Info, null) },
                    label = { Text("About") }
                )
            }
        }
    ) { padding ->
        when (tab) {
            Tab.SCAN -> ScanScreen(vm, servers, selectedId, darkTheme, vm::setDarkTheme, Modifier.padding(padding), onManage = { tab = Tab.SERVERS })
            Tab.SERVERS -> ServerList(servers, selectedId, darkTheme, vm::setDarkTheme, vm::select, { editing = it }, vm::delete, Modifier.padding(padding))
            Tab.SETTINGS -> SettingsScreen(vm, darkTheme, Modifier.padding(padding))
            Tab.ABOUT -> AboutScreen(darkTheme, vm::setDarkTheme, Modifier.padding(padding))
        }
    }
    editing?.let { server ->
        ServerEditor(server, servers, onDismiss = { editing = null }, onSave = { vm.upsert(it); editing = null })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScanScreen(
    vm: AppViewModel,
    servers: List<ServerConfig>,
    selectedId: String?,
    darkTheme: Boolean,
    setDarkTheme: (Boolean) -> Unit,
    modifier: Modifier,
    onManage: () -> Unit
) {
    val context = LocalContext.current
    var cameraGranted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { cameraGranted = it }
    val state by vm.sendState.collectAsStateWithLifecycle()
    val last by vm.lastScanned.collectAsStateWithLifecycle()
    val showDetails by vm.showDetails.collectAsStateWithLifecycle()
    val details by vm.transmissionDetails.collectAsStateWithLifecycle()
    val screenTimeout by vm.screenTimeoutSeconds.collectAsStateWithLifecycle()
    var menu by remember { mutableStateOf(false) }
    val selected = servers.firstOrNull { it.id == selectedId }

    Column(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 10.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("CodePush", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Scan once. Route anywhere.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ThemeButton(darkTheme, setDarkTheme)
        }
        Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xFF071613))) {
            if (cameraGranted) {
                ScannerPreview(
                    modifier = Modifier.fillMaxSize(),
                    enabled = state !is SendState.Sending,
                    activityKey = last,
                    timeoutSeconds = screenTimeout,
                    onCode = vm::onScanned
                )
                Box(Modifier.align(Alignment.Center).size(230.dp).border(3.dp, Color.White.copy(alpha = .9f), RoundedCornerShape(24.dp)))
                Surface(Modifier.align(Alignment.TopCenter).padding(10.dp), color = Color.Black.copy(alpha = .58f), shape = RoundedCornerShape(50)) {
                    Text("Align code inside frame", Modifier.padding(horizontal = 12.dp, vertical = 5.dp), color = Color.White, style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Column(Modifier.align(Alignment.Center).padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(44.dp))
                    Spacer(Modifier.height(12.dp)); Text("Camera access is needed to scan codes", color = Color.White)
                    Spacer(Modifier.height(16.dp)); Button(onClick = { permission.launch(Manifest.permission.CAMERA) }) { Text("Allow camera") }
                }
            }
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ExposedDropdownMenuBox(expanded = menu, onExpandedChange = { menu = it }) {
                OutlinedTextField(
                    value = selected?.name ?: "No server selected", onValueChange = {}, readOnly = true,
                    label = { Text("Send scans to") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(menu) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(menu, { menu = false }) {
                    servers.forEach { DropdownMenuItem({ Text(it.name) }, onClick = { vm.select(it.id); menu = false }) }
                    if (servers.isEmpty()) DropdownMenuItem({ Text("Add a server") }, onClick = { menu = false; onManage() })
                }
            }
            StatusCard(state, last, vm::clearLast, showDetails, vm::setShowDetails, details)
        }
    }
}

@Composable
private fun SettingsScreen(vm: AppViewModel, darkTheme: Boolean, modifier: Modifier) {
    val screenTimeout by vm.screenTimeoutSeconds.collectAsStateWithLifecycle()
    Column(
        modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (darkTheme) Icons.Default.DarkMode else Icons.Default.LightMode, null)
                    Text(if (darkTheme) "Dark theme" else "Light theme", Modifier.weight(1f).padding(horizontal = 12.dp))
                    Switch(checked = darkTheme, onCheckedChange = vm::setDarkTheme)
                }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Scanner", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Let Android sleep when no new code is scanned.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ScreenTimeoutSelector(screenTimeout, vm::setScreenTimeoutSeconds)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScreenTimeoutSelector(current: Int, selected: (Int) -> Unit) {
    val options = listOf(30 to "30 seconds", 60 to "1 minute", 120 to "2 minutes", 300 to "5 minutes", 0 to "Always awake")
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = options.firstOrNull { it.first == current }?.second ?: "1 minute",
            onValueChange = {},
            readOnly = true,
            label = { Text("Allow screen sleep after") },
            leadingIcon = { Icon(Icons.Default.Bedtime, null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (seconds, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = { selected(seconds); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun StatusCard(
    state: SendState,
    last: String?,
    clear: () -> Unit,
    showDetails: Boolean,
    setShowDetails: (Boolean) -> Unit,
    details: TransmissionDetails?
) {
    val (icon, title, detail, color) = when (state) {
        SendState.Idle -> listOf(Icons.Default.CenterFocusStrong, "Ready to scan", last ?: "No code scanned yet", MaterialTheme.colorScheme.secondary)
        is SendState.Sending -> listOf(Icons.Default.CloudUpload, "Sending", "To ${state.serverName}", MaterialTheme.colorScheme.primary)
        is SendState.Success -> listOf(Icons.Default.CheckCircle, "Success", "Request sent", Color(0xFF087F5B))
        is SendState.Error -> listOf(Icons.Default.Error, "Could not send", state.message, MaterialTheme.colorScheme.error)
    }
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                @Suppress("UNCHECKED_CAST") Icon(icon as androidx.compose.ui.graphics.vector.ImageVector, null, tint = color as Color)
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(title as String, fontWeight = FontWeight.SemiBold)
                    Text(detail as String, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                }
                if (last != null) TextButton(onClick = clear) { Text("Reset") }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Show scan details", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Switch(checked = showDetails, onCheckedChange = setShowDetails)
            }
            if (showDetails && details != null) {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text("Scanned", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(details.scanned, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                Text("Sent to server", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(details.sent, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ServerList(
    servers: List<ServerConfig>, selectedId: String?, darkTheme: Boolean, setDarkTheme: (Boolean) -> Unit, select: (String?) -> Unit,
    edit: (ServerConfig) -> Unit, delete: (String) -> Unit, modifier: Modifier
) {
    var deleteTarget by remember { mutableStateOf<ServerConfig?>(null) }
    Box(modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(20.dp, 18.dp, 20.dp, 100.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Servers", Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    ThemeButton(darkTheme, setDarkTheme)
                }
                Text("Saved destinations and response workflows", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
            }
            if (servers.isEmpty()) item { EmptyServers() }
            items(servers, key = { it.id }) { server ->
                Card(onClick = { select(server.id) }, colors = CardDefaults.cardColors(containerColor = if (selectedId == server.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surface) {
                            Text(server.method.name, Modifier.padding(9.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(server.name, fontWeight = FontWeight.SemiBold)
                            Text(server.url, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton({ edit(server) }) { Icon(Icons.Default.Edit, "Edit") }
                        IconButton({ deleteTarget = server }) { Icon(Icons.Default.DeleteOutline, "Delete") }
                    }
                }
            }
        }
        FloatingActionButton({ edit(ServerConfig()) }, Modifier.align(Alignment.BottomEnd).padding(24.dp)) { Icon(Icons.Default.Add, "Add server") }
    }
    deleteTarget?.let { target ->
        AlertDialog(onDismissRequest = { deleteTarget = null }, title = { Text("Delete ${target.name}?") }, text = { Text("This saved server will be removed from this device.") },
            confirmButton = { TextButton({ delete(target.id); deleteTarget = null }) { Text("Delete") } }, dismissButton = { TextButton({ deleteTarget = null }) { Text("Cancel") } })
    }
}

@Composable
private fun ThemeButton(darkTheme: Boolean, setDarkTheme: (Boolean) -> Unit) {
    IconButton(onClick = { setDarkTheme(!darkTheme) }) {
        Icon(if (darkTheme) Icons.Default.LightMode else Icons.Default.DarkMode, if (darkTheme) "Use light theme" else "Use dark theme")
    }
}

@Composable
private fun AboutScreen(darkTheme: Boolean, setDarkTheme: (Boolean) -> Unit, modifier: Modifier) {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("About", Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            ThemeButton(darkTheme, setDarkTheme)
        }
        Spacer(Modifier.weight(.7f))
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primaryContainer) {
            Icon(Icons.Default.QrCodeScanner, null, Modifier.padding(22.dp).size(54.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.height(20.dp))
        Text("CodePush", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Version 1.5.0", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(28.dp))
        Text("Developed by", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Tihloh", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text("Christian Borsal Bustamante", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = { uriHandler.openUri("https://github.com/tihloh") }) {
            Icon(Icons.Default.OpenInNew, null)
            Spacer(Modifier.width(8.dp))
            Text("github.com/tihloh")
        }
        Spacer(Modifier.weight(1f))
        Text("Scan once. Route anywhere.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun EmptyServers() {
    Column(Modifier.fillMaxWidth().padding(vertical = 80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Dns, null, Modifier.size(54.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp)); Text("No servers yet", style = MaterialTheme.typography.titleMedium)
        Text("Add a destination to start routing scans", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
