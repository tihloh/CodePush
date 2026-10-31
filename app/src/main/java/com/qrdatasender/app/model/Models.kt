package com.qrdatasender.app.model

import java.util.UUID

enum class HttpMethod { GET, POST }
enum class ResponseType { AUTO, TEXT, JSON }
enum class MatchOperator { EXISTS, EQUALS, CONTAINS }

data class DynamicField(
    val name: String = "",
    val value: String = ""
)

data class ResponseRule(
    val enabled: Boolean = false,
    val type: ResponseType = ResponseType.AUTO,
    val selector: String = "",
    val operator: MatchOperator = MatchOperator.EQUALS,
    val expected: String = "",
    val followUpServerId: String? = null
)

data class ServerConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val method: HttpMethod = HttpMethod.GET,
    val url: String = "",
    val fields: List<DynamicField> = listOf(DynamicField("value", "{scan}")),
    val responseRule: ResponseRule = ResponseRule()
)

sealed interface SendState {
    data object Idle : SendState
    data class Sending(val serverName: String) : SendState
    data class Success(val status: Int, val message: String) : SendState
    data class Error(val message: String) : SendState
}

data class TransmissionDetails(
    val scanned: String,
    val sent: String,
    val status: Int? = null,
    val response: String = ""
)
