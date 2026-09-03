package com.qrdatasender.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.qrdatasender.app.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerEditor(
    initial: ServerConfig,
    allServers: List<ServerConfig>,
    onDismiss: () -> Unit,
    onSave: (ServerConfig) -> Unit
) {
    var name by remember { mutableStateOf(initial.name) }
    var method by remember { mutableStateOf(initial.method) }
    var url by remember { mutableStateOf(initial.url) }
    var fields by remember { mutableStateOf(initial.fields.ifEmpty { listOf(DynamicField()) }) }
    var rule by remember { mutableStateOf(initial.responseRule) }
    var showErrors by remember { mutableStateOf(false) }
    val valid = name.isNotBlank() && (url.startsWith("http://") || url.startsWith("https://"))

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column {
                TopAppBar(
                    title = { Text(if (initial.name.isBlank()) "New server" else "Edit server") },
                    navigationIcon = { IconButton(onDismiss) { Icon(Icons.Default.Close, "Close") } },
                    actions = {
                        TextButton(onClick = {
                            showErrors = true
                            if (valid) onSave(initial.copy(name = name.trim(), method = method, url = url.trim(), fields = fields.filter { it.name.isNotBlank() }, responseRule = rule))
                        }) { Text("Save", fontWeight = FontWeight.Bold) }
                    }
                )
                LazyColumn(
                    Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 10.dp, 20.dp, 48.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text("Destination", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Server name") }, singleLine = true,
                            isError = showErrors && name.isBlank(), supportingText = if (showErrors && name.isBlank()) {{ Text("Required") }} else null)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HttpMethod.entries.forEach { option ->
                                FilterChip(selected = method == option, onClick = { method = option }, label = { Text(option.name) })
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(url, { url = it }, Modifier.fillMaxWidth(), label = { Text("Full URL") },
                            placeholder = { Text("https://api.example.com/items/{value}") }, minLines = 2,
                            isError = showErrors && !valid,
                            supportingText = { Text(if (showErrors && !valid) "Enter a full http:// or https:// URL" else "Use {variable} or {{variable}} placeholders") })
                    }
                    item {
                        HorizontalDivider(); Spacer(Modifier.height(16.dp))
                        Text("Dynamic data", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Matched names fill URL placeholders. Other fields become query parameters for GET or JSON fields for POST. Use {scan} in a value for scanned data.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    items(fields.size) { index ->
                        val field = fields[index]
                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(field.name, { value -> fields = fields.toMutableList().also { it[index] = field.copy(name = value) } }, Modifier.weight(.42f), label = { Text("Variable") }, singleLine = true)
                            OutlinedTextField(field.value, { value -> fields = fields.toMutableList().also { it[index] = field.copy(value = value) } }, Modifier.weight(.58f), label = { Text("Value") }, singleLine = true)
                            IconButton(onClick = { fields = fields.toMutableList().also { it.removeAt(index) } }) { Icon(Icons.Default.Close, "Remove field") }
                        }
                    }
                    item {
                        OutlinedButton(onClick = { fields = fields + DynamicField() }) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Add field") }
                    }
                    item {
                        HorizontalDivider(); Spacer(Modifier.height(16.dp))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Response check", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text("Inspect the response and optionally call another server", style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(rule.enabled, { rule = rule.copy(enabled = it) })
                        }
                    }
                    if (rule.enabled) {
                        item {
                            EnumDropdown("Response type", rule.type, ResponseType.entries) { rule = rule.copy(type = it) }
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(rule.selector, { rule = rule.copy(selector = it) }, Modifier.fillMaxWidth(), label = { Text("JSON path (optional)") }, placeholder = { Text("data.status") }, singleLine = true,
                                supportingText = { Text("Leave blank to check the entire response") })
                            Spacer(Modifier.height(12.dp))
                            EnumDropdown("Condition", rule.operator, MatchOperator.entries) { rule = rule.copy(operator = it) }
                            if (rule.operator != MatchOperator.EXISTS) {
                                Spacer(Modifier.height(12.dp))
                                OutlinedTextField(rule.expected, { rule = rule.copy(expected = it) }, Modifier.fillMaxWidth(), label = { Text("Expected value") }, singleLine = true)
                            }
                            Spacer(Modifier.height(12.dp))
                            ServerDropdown(
                                label = "When matched, send to",
                                current = rule.followUpServerId,
                                servers = allServers.filter { it.id != initial.id },
                                onSelected = { rule = rule.copy(followUpServerId = it) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T : Enum<T>> EnumDropdown(label: String, current: T, options: List<T>, selected: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded, { expanded = it }) {
        OutlinedTextField(current.name.lowercase().replaceFirstChar { it.uppercase() }, {}, readOnly = true, label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.fillMaxWidth().menuAnchor())
        ExposedDropdownMenu(expanded, { expanded = false }) {
            options.forEach { option -> DropdownMenuItem({ Text(option.name.lowercase().replaceFirstChar { it.uppercase() }) }, { selected(option); expanded = false }) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServerDropdown(label: String, current: String?, servers: List<ServerConfig>, onSelected: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val display = servers.firstOrNull { it.id == current }?.name ?: "No follow-up request"
    ExposedDropdownMenuBox(expanded, { expanded = it }) {
        OutlinedTextField(display, {}, readOnly = true, label = { Text(label) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.fillMaxWidth().menuAnchor())
        ExposedDropdownMenu(expanded, { expanded = false }) {
            DropdownMenuItem({ Text("No follow-up request") }, { onSelected(null); expanded = false })
            servers.forEach { server -> DropdownMenuItem({ Text(server.name) }, { onSelected(server.id); expanded = false }) }
        }
    }
}
