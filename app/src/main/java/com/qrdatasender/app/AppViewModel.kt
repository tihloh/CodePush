package com.qrdatasender.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.qrdatasender.app.data.ServerStore
import com.qrdatasender.app.model.*
import com.qrdatasender.app.network.RequestEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val store = ServerStore(application)
    private val engine = RequestEngine()
    private val _servers = MutableStateFlow(store.load())
    val servers = _servers.asStateFlow()
    private val _selectedId = MutableStateFlow(store.selectedId())
    val selectedId = _selectedId.asStateFlow()
    private val _sendState = MutableStateFlow<SendState>(SendState.Idle)
    val sendState = _sendState.asStateFlow()
    private val _lastScanned = MutableStateFlow<String?>(null)
    val lastScanned = _lastScanned.asStateFlow()
    private val _showDetails = MutableStateFlow(store.showDetails())
    val showDetails = _showDetails.asStateFlow()
    private val _transmissionDetails = MutableStateFlow<TransmissionDetails?>(null)
    val transmissionDetails = _transmissionDetails.asStateFlow()
    private val _darkTheme = MutableStateFlow(store.darkTheme())
    val darkTheme = _darkTheme.asStateFlow()
    private val _screenTimeoutSeconds = MutableStateFlow(store.screenTimeoutSeconds())
    val screenTimeoutSeconds = _screenTimeoutSeconds.asStateFlow()
    private var busy = false

    fun upsert(server: ServerConfig) {
        _servers.value = _servers.value.toMutableList().apply {
            val index = indexOfFirst { it.id == server.id }
            if (index >= 0) set(index, server) else add(server)
        }
        store.save(_servers.value)
        if (_selectedId.value == null) select(server.id)
    }

    fun delete(id: String) {
        _servers.value = _servers.value.filterNot { it.id == id }
        store.save(_servers.value)
        if (_selectedId.value == id) select(_servers.value.firstOrNull()?.id)
    }

    fun select(id: String?) { _selectedId.value = id; store.select(id) }
    fun clearLast() { _lastScanned.value = null; _sendState.value = SendState.Idle }
    fun setShowDetails(value: Boolean) { _showDetails.value = value; store.setShowDetails(value) }
    fun setDarkTheme(value: Boolean) { _darkTheme.value = value; store.setDarkTheme(value) }
    fun setScreenTimeoutSeconds(value: Int) {
        _screenTimeoutSeconds.value = value
        store.setScreenTimeoutSeconds(value)
    }

    fun onScanned(value: String) {
        if (busy || value.isBlank()) return
        if (_lastScanned.value == value) return
        val server = _servers.value.firstOrNull { it.id == _selectedId.value }
            ?: run { _sendState.value = SendState.Error("Select or add a server first"); return }
        _lastScanned.value = value
        _transmissionDetails.value = null
        busy = true
        viewModelScope.launch {
            _sendState.value = SendState.Sending(server.name)
            try {
                val result = engine.send(server, value)
                if (result.matched && server.responseRule.followUpServerId != null) {
                    val followUp = _servers.value.firstOrNull { it.id == server.responseRule.followUpServerId }
                        ?: error("Follow-up server no longer exists")
                    val next = engine.send(followUp, value)
                    _transmissionDetails.value = TransmissionDetails(value, result.sent + "\n\nFollow-up:\n" + next.sent)
                    _sendState.value = SendState.Success(next.status, "Sent to ${followUp.name}")
                } else {
                    _transmissionDetails.value = TransmissionDetails(value, result.sent)
                    _sendState.value = SendState.Success(result.status, "Request completed")
                }
            } catch (e: Exception) {
                _sendState.value = SendState.Error(e.message ?: "Request failed")
            } finally { busy = false }
        }
    }
}
