package com.qrdatasender.app.data

import com.qrdatasender.app.model.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONObject

/** Parses setup data only: never makes HTTP requests or accepts remote server IDs. */
object ServerQrParser {
    private val placeholder = Regex("""\{\{([A-Za-z_][A-Za-z0-9_.-]*)\}\}|\{([A-Za-z_][A-Za-z0-9_.-]*)\}""")
    private val fieldName = Regex("[A-Za-z_][A-Za-z0-9_.-]*")

    /** null means ordinary scan data; a marked but invalid setup QR throws. */
    fun parse(raw: String, existingServers: List<ServerConfig> = emptyList()): ServerConfig? {
        if (!raw.trimStart().startsWith("{")) return null
        val root = try { JSONObject(raw) } catch (error: Exception) {
            require(!raw.contains("codepush.server")) { "Invalid setup QR JSON" }
            return null
        }
        if (root.opt("type") != "codepush.server") return null
        require(raw.toByteArray(Charsets.UTF_8).size <= 12_000) { "Setup QR is too large" }
        root.only("type", "version", "server")
        require(root.opt("version") == 1) { "Unsupported setup QR version; expected 1" }
        val server = root.optJSONObject("server") ?: error("Setup QR must contain a server object")
        server.only("name", "method", "url", "fields", "responseRule")
        val name = server.string("name").trim()
        require(name.isNotEmpty() && name.length <= 100) { "Server name must be 1-100 characters" }
        val method = runCatching { HttpMethod.valueOf(server.string("method").uppercase()) }
            .getOrElse { error("Method must be GET or POST") }
        val url = server.string("url").trim()
        require(url.length <= 2048) { "Server URL is too long" }

        val fields = if (!server.has("fields")) listOf(DynamicField("value", "{scan}")) else {
            val array = server.optJSONArray("fields") ?: error("fields must be an array")
            require(array.length() <= 30) { "Too many dynamic fields" }
            List(array.length()) { index ->
                val field = array.optJSONObject(index) ?: error("Each field must be an object")
                field.only("name", "value")
                val key = field.string("name")
                val value = field.string("value")
                require(fieldName.matches(key) && key != "scan") { "Invalid or reserved field name: $key" }
                require(value.length <= 2048) { "Field value is too long" }
                DynamicField(key, value)
            }
        }
        require(fields.map { it.name }.distinct().size == fields.size) { "Field names must be unique" }
        val knownNames = fields.map { it.name }.toSet() + "scan"
        val checkedUrl = placeholder.replace(url) { match ->
            val key = match.groupValues[1].ifBlank { match.groupValues[2] }
            require(key in knownNames) { "No field supplied for {$key}" }
            "sample"
        }
        require(!checkedUrl.contains('{') && !checkedUrl.contains('}')) { "Invalid URL placeholder" }
        val parsedUrl = checkedUrl.toHttpUrlOrNull() ?: error("Use a valid HTTP or HTTPS URL")
        require(parsedUrl.username.isEmpty() && parsedUrl.password.isEmpty()) { "Credentials in URLs are not supported" }

        val rule = if (!server.has("responseRule")) ResponseRule() else {
            val json = server.optJSONObject("responseRule") ?: error("responseRule must be an object")
            json.only("enabled", "type", "selector", "operator", "expected", "followUpServerName")
            val enabled = if (json.has("enabled")) (json.opt("enabled") as? Boolean
                ?: error("enabled must be true or false")) else true
            val type = runCatching { ResponseType.valueOf(json.string("type", "AUTO").uppercase()) }
                .getOrElse { error("Response type must be AUTO, TEXT, or JSON") }
            val operator = runCatching { MatchOperator.valueOf(json.string("operator", "EQUALS").uppercase()) }
                .getOrElse { error("Operator must be EXISTS, EQUALS, or CONTAINS") }
            val followUpName = json.string("followUpServerName", "")
            val followUp = if (followUpName.isBlank()) null else {
                val matches = existingServers.filter { it.name == followUpName }
                require(matches.size == 1) { "Follow-up server '$followUpName' must match exactly one saved server" }
                matches.single().id
            }
            ResponseRule(enabled, type, json.string("selector", ""), operator, json.string("expected", ""), followUp)
        }
        return ServerConfig(name = name, method = method, url = url, fields = fields, responseRule = rule)
    }

    private fun JSONObject.string(key: String, default: String? = null): String {
        if (!has(key) && default != null) return default
        return opt(key) as? String ?: error("$key must be a string")
    }

    private fun JSONObject.only(vararg allowed: String) {
        keys().forEach { require(it in allowed) { "Unknown setup field: $it" } }
    }
}
