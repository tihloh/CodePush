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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qrdatasender.app.AppViewModel
import com.qrdatasender.app.model.*

private enum class Tab { SCAN, SERVERS }

@Composable
fun QrDataSenderApp(vm: AppViewModel) {
    val servers by vm.servers.collectAsStateWithLifecycle()
    val selectedId by vm.selectedId.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(Tab.SCAN) }
    var editing by remember { mutableStateOf<ServerConfig?>(null) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == Tab.SCAN,
                    onClick = { tab = Tab.SCAN },
                    icon = { Icon(Icons.Default.QrCodeScanner, null) },
                    label = { Text("Scan") }
                )
                NavigationBarItem(
                    selected = tab == Tab.SERVERS,
                    onClick = { tab = Tab.SERVERS },
                    icon = { Icon(Icons.Default.Dns, null) },
                    label = { Text("Servers") }
                )
            }
        }
    ) { padding ->
        when (tab) {
            Tab.SCAN -> ScanScreen(vm, servers, selectedId, Modifier.padding(padding), onManage = { tab = Tab.SERVERS })
            Tab.SERVERS -> ServerList(servers, selectedId, vm::select, { editing = it }, vm::delete, Modifier.padding(padding))
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
    var menu by remember { mutableStateOf(false) }
    val selected = servers.firstOrNull { it.id == selectedId }

    Column(modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            Text("QR Data Sender", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Scan once. Route anywhere.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xFF071613))) {
            if (cameraGranted) {
                ScannerPreview(Modifier.fillMaxSize(), enabled = state !is SendState.Sending, onCode = vm::onScanned)
                Box(Modifier.align(Alignment.Center).size(230.dp).border(3.dp, Color.White.copy(alpha = .9f), RoundedCornerShape(24.dp)))
                Surface(Modifier.align(Alignment.TopCenter).padding(14.dp), color = Color.Black.copy(alpha = .58f), shape = RoundedCornerShape(50)) {
                    Text("Align any QR or barcode inside the frame", Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = Color.White)
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
        is SendState.Success -> listOf(Icons.Default.CheckCircle, "Sent · HTTP ${state.status}", state.message, Color(0xFF087F5B))
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
    servers: List<ServerConfig>, selectedId: String?, select: (String?) -> Unit,
    edit: (ServerConfig) -> Unit, delete: (String) -> Unit, modifier: Modifier
) {
    var deleteTarget by remember { mutableStateOf<ServerConfig?>(null) }
    Box(modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(20.dp, 18.dp, 20.dp, 100.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("Servers", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
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

@Composable private fun EmptyServers() {
    Column(Modifier.fillMaxWidth().padding(vertical = 80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Dns, null, Modifier.size(54.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp)); Text("No servers yet", style = MaterialTheme.typography.titleMedium)
        Text("Add a destination to start routing scans", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
