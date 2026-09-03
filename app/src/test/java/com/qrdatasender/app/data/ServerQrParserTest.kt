package com.qrdatasender.app.data

import com.qrdatasender.app.model.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ServerQrParserTest {
    private fun setup(): JSONObject = JSONObject("""{
        "type":"codepush.server","version":1,
        "server":{"name":"Budget","method":"POST","url":"https://example.com/scan",
        "fields":[{"name":"codevalue","value":"{scan}"},{"name":"user_id","value":"42"}]}
    }""")

    private fun rejected(root: JSONObject) {
        assertThrows(Exception::class.java) { ServerQrParser.parse(root.toString()) }
    }

    @Test fun readsServerAndUsesFreshLocalId() {
        val a = ServerQrParser.parse(setup().toString())!!
        val b = ServerQrParser.parse(setup().toString())!!
        assertEquals("Budget", a.name)
        assertEquals(HttpMethod.POST, a.method)
        assertEquals(listOf(DynamicField("codevalue", "{scan}"), DynamicField("user_id", "42")), a.fields)
        assertFalse(a.responseRule.enabled)
        assertNotEquals(a.id, b.id)
    }

    @Test fun ordinaryScansIncludingJsonRemainOrdinary() {
        listOf("ABC123", "https://example.com/scan", "{\"code\":\"123\"}", "{broken").forEach {
            assertNull(ServerQrParser.parse(it))
        }
    }

    @Test fun supportsGetAndBothPlaceholderStyles() {
        val root = setup()
        root.getJSONObject("server").put("method", "get")
            .put("url", "http://192.168.0.2/{user_id}?code={{codevalue}}&raw={scan}")
        assertEquals(HttpMethod.GET, ServerQrParser.parse(root.toString())!!.method)
    }

    @Test fun suppliesDefaultScanFieldWhenFieldsOmitted() {
        val root = setup()
        root.getJSONObject("server").remove("fields")
        assertEquals(listOf(DynamicField("value", "{scan}")), ServerQrParser.parse(root.toString())!!.fields)
    }

    @Test fun explicitlyEmptyFieldsArePreserved() {
        val root = setup()
        root.getJSONObject("server").put("fields", JSONArray()).put("url", "https://example.com/{scan}")
        assertTrue(ServerQrParser.parse(root.toString())!!.fields.isEmpty())
    }

    @Test fun rejectsBadVersionAndMissingServer() {
        rejected(setup().put("version", 2))
        rejected(setup().put("version", "1"))
        rejected(setup().also { it.remove("server") })
    }

    @Test fun rejectsBadMethodAndUrl() {
        for ((key, value) in listOf("method" to "DELETE", "url" to "file:///etc/passwd", "url" to "https://user:pass@example.com", "name" to "")) {
            rejected(setup().also { it.getJSONObject("server").put(key, value) })
        }
    }

    @Test fun rejectsInvalidFieldsAndMissingPlaceholder() {
        rejected(setup().also { it.getJSONObject("server").put("url", "https://example.com/{missing}") })
        for (fields in listOf(
            "[{\"name\":\"scan\",\"value\":\"x\"}]",
            "[{\"name\":\"bad key\",\"value\":\"x\"}]",
            "[{\"name\":\"id\",\"value\":42}]",
            "[{\"name\":\"id\",\"value\":\"1\"},{\"name\":\"id\",\"value\":\"2\"}]"
        )) rejected(setup().also { it.getJSONObject("server").put("fields", JSONArray(fields)) })
    }

    @Test fun rejectsMalformedMarkedJsonInsteadOfTreatingAsScan() {
        assertThrows(Exception::class.java) { ServerQrParser.parse("{\"type\":\"codepush.server\",broken") }
    }

    @Test fun rejectsRemoteIdsAndUnknownSettings() {
        rejected(setup().also { it.getJSONObject("server").put("id", "existing-id") })
        rejected(setup().put("autoSave", true))
    }

    @Test fun parsesResponseRuleAndResolvesExistingFollowUp() {
        val saved = ServerConfig(name = "Notify")
        val root = setup()
        root.getJSONObject("server").put("responseRule", JSONObject("""{
            "enabled":true,"type":"JSON","selector":"success","operator":"EQUALS",
            "expected":"true","followUpServerName":"Notify"}
        """))
        val rule = ServerQrParser.parse(root.toString(), listOf(saved))!!.responseRule
        assertTrue(rule.enabled)
        assertEquals(ResponseType.JSON, rule.type)
        assertEquals("success", rule.selector)
        assertEquals(saved.id, rule.followUpServerId)
        rejected(root)
        assertThrows(Exception::class.java) { ServerQrParser.parse(root.toString(), listOf(saved, saved.copy(id = "another"))) }
    }

    @Test fun rejectsInvalidResponseRuleTypes() {
        for (rule in listOf("{\"enabled\":\"true\"}", "{\"type\":\"XML\"}", "{\"operator\":\"RUN\"}")) {
            rejected(setup().also { it.getJSONObject("server").put("responseRule", JSONObject(rule)) })
        }
    }
}
