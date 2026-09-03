package com.qrdatasender.app.data

import android.content.Context
import com.qrdatasender.app.model.*
import org.json.JSONArray
import org.json.JSONObject

class ServerStore(context: Context) {
    private val prefs = context.getSharedPreferences("server_store", Context.MODE_PRIVATE)

    fun load(): List<ServerConfig> = runCatching {
        val data = JSONArray(prefs.getString("servers", "[]"))
        List(data.length()) { index -> data.getJSONObject(index).toServer() }
    }.getOrDefault(emptyList())

    fun save(servers: List<ServerConfig>) {
        val array = JSONArray()
        servers.forEach { array.put(it.toJson()) }
        prefs.edit().putString("servers", array.toString()).apply()
    }

    fun selectedId(): String? = prefs.getString("selected_server", null)
    fun select(id: String?) = prefs.edit().putString("selected_server", id).apply()
    fun showDetails(): Boolean = prefs.getBoolean("show_details", false)
    fun setShowDetails(value: Boolean) = prefs.edit().putBoolean("show_details", value).apply()
    fun darkTheme(): Boolean = prefs.getBoolean("dark_theme", false)
    fun setDarkTheme(value: Boolean) = prefs.edit().putBoolean("dark_theme", value).apply()
    fun screenTimeoutSeconds(): Int = prefs.getInt("screen_timeout_seconds", 60)
    fun setScreenTimeoutSeconds(value: Int) = prefs.edit().putInt("screen_timeout_seconds", value).apply()

    private fun ServerConfig.toJson() = JSONObject().apply {
        put("id", id); put("name", name); put("method", method.name); put("url", url)
        put("fields", JSONArray().apply { fields.forEach { put(JSONObject().put("name", it.name).put("value", it.value)) } })
        put("rule", JSONObject().apply {
            put("enabled", responseRule.enabled); put("type", responseRule.type.name)
            put("selector", responseRule.selector); put("operator", responseRule.operator.name)
            put("expected", responseRule.expected); put("followUpServerId", responseRule.followUpServerId)
        })
    }

    private fun JSONObject.toServer(): ServerConfig {
        val fieldArray = optJSONArray("fields") ?: JSONArray()
        val rule = optJSONObject("rule") ?: JSONObject()
        return ServerConfig(
            id = getString("id"), name = optString("name"),
            method = runCatching { HttpMethod.valueOf(optString("method")) }.getOrDefault(HttpMethod.GET),
            url = optString("url"),
            fields = List(fieldArray.length()) { i ->
                fieldArray.getJSONObject(i).let { DynamicField(it.optString("name"), it.optString("value")) }
            },
            responseRule = ResponseRule(
                enabled = rule.optBoolean("enabled"),
                type = runCatching { ResponseType.valueOf(rule.optString("type")) }.getOrDefault(ResponseType.AUTO),
                selector = rule.optString("selector"),
                operator = runCatching { MatchOperator.valueOf(rule.optString("operator")) }.getOrDefault(MatchOperator.EQUALS),
                expected = rule.optString("expected"),
                followUpServerId = rule.optString("followUpServerId").takeIf { it.isNotBlank() && it != "null" }
            )
        )
    }
}
