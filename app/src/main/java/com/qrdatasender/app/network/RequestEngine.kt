package com.qrdatasender.app.network

import com.qrdatasender.app.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

data class RequestResult(
    val status: Int,
    val body: String,
    val matched: Boolean = false,
    val sent: String
)

class RequestEngine(private val client: OkHttpClient = OkHttpClient()) {
    suspend fun send(config: ServerConfig, scannedValue: String): RequestResult = withContext(Dispatchers.IO) {
        val values = config.fields
            .filter { it.name.isNotBlank() }
            .associate { it.name.trim() to expandValue(it.value, scannedValue) }
            .toMutableMap()
            .also { it.putIfAbsent("scan", scannedValue) }

        var target = config.url
        val consumed = mutableSetOf<String>()
        val placeholder = Regex("""\{\{([A-Za-z_][A-Za-z0-9_.-]*)\}\}|\{([A-Za-z_][A-Za-z0-9_.-]*)\}""")
        target = placeholder.replace(target) { match ->
            val key = match.groupValues[1].ifBlank { match.groupValues[2] }
            val value = values[key] ?: throw IllegalArgumentException("No value supplied for {$key}")
            consumed += key
            java.net.URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")
        }

        val extras = values.filterKeys { it !in consumed && it !in setOf("scan") }
        val request = when (config.method) {
            HttpMethod.GET -> {
                val url = target.toHttpUrl().newBuilder().apply {
                    extras.forEach { (key, value) -> addQueryParameter(key, value) }
                }.build()
                Request.Builder().url(url).get().build()
            }
            HttpMethod.POST -> {
                val json = JSONObject()
                extras.forEach { (key, value) -> json.put(key, value) }
                Request.Builder().url(target)
                    .post(json.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()
            }
        }
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}: ${body.take(180)}")
            val sent = if (config.method == HttpMethod.GET) {
                "GET ${request.url}"
            } else {
                "POST ${request.url}\n${request.body?.let { bodyToText(it) }.orEmpty()}"
            }
            RequestResult(response.code, body, matches(config.responseRule, body), sent)
        }
    }

    private fun expandValue(template: String, scan: String) = template
        .replace("{scan}", scan).replace("{{scan}}", scan)

    private fun matches(rule: ResponseRule, body: String): Boolean {
        if (!rule.enabled) return false
        val actual = when (rule.type) {
            ResponseType.TEXT -> body
            ResponseType.JSON -> jsonValue(body, rule.selector)
            ResponseType.AUTO -> if (rule.selector.isBlank()) body else runCatching { jsonValue(body, rule.selector) }.getOrDefault(body)
        }
        return when (rule.operator) {
            MatchOperator.EXISTS -> actual.isNotBlank()
            MatchOperator.EQUALS -> actual == rule.expected
            MatchOperator.CONTAINS -> actual.contains(rule.expected, ignoreCase = true)
        }
    }

    private fun jsonValue(body: String, selector: String): String {
        var current: Any = JSONObject(body)
        if (selector.isBlank()) return current.toString()
        selector.split('.').forEach { key -> current = (current as JSONObject).get(key) }
        return if (current == JSONObject.NULL) "" else current.toString()
    }

    private fun bodyToText(body: okhttp3.RequestBody): String {
        val buffer = okio.Buffer()
        body.writeTo(buffer)
        return buffer.readUtf8()
    }
}
