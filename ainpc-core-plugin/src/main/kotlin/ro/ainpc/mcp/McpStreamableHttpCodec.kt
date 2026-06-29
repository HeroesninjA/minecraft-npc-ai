package ro.ainpc.mcp

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser

object McpStreamableHttpCodec {
    private val gson = Gson()
    fun jsonRpcRequest(id: String, method: String, paramsJson: String = "{}"): String =
        """{"jsonrpc":"2.0","id":"$id","method":"$method","params":$paramsJson}"""

    fun initializedNotification(): String =
        """{"jsonrpc":"2.0","method":"notifications/initialized","params":{}}"""

    fun initializeRequest(id: String = "1"): String =
        jsonRpcRequest(
            id,
            "initialize",
            """{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"ainpc-core-plugin","version":"1.0.0"}}"""
        )

    fun toolCallRequest(id: String, toolName: String, argumentsJson: String): String =
        jsonRpcRequest(
            id,
            "tools/call",
            """{"name":${quote(toolName)},"arguments":${safeObjectJson(argumentsJson)}}"""
        )

    fun extractJsonPayload(responseBody: String): String {
        val dataLines = responseBody.lineSequence()
            .filter { it.startsWith("data:") }
            .map { it.removePrefix("data:").trim() }
            .filter { it.isNotBlank() }
            .toList()
        if (dataLines.isEmpty()) {
            return responseBody.trim()
        }
        dataLines.asReversed().forEach { candidate ->
            if (isJsonPayload(candidate)) {
                return candidate
            }
        }
        return dataLines.last()
    }

    fun firstTextContent(jsonRpcPayload: String): String {
        val root = parseObject(jsonRpcPayload) ?: return "{}"
        val result = root.getAsJsonObject("result") ?: return "{}"
        val content = result.getAsJsonArray("content") ?: return "{}"
        if (content.size() == 0) return "{}"
        val first = content[0].asJsonObject
        val text = first.get("text")
        if (text != null && text.isJsonPrimitive && text.asJsonPrimitive.isString) {
            return text.asString
        }
        val data = first.get("data")
        if (data != null && data.isJsonPrimitive) {
            if (data.asJsonPrimitive.isString) return data.asString
            return data.asJsonPrimitive.toString()
        }
        return first.toString()
    }

    fun errorMessage(jsonRpcPayload: String): String? {
        val root = parseObject(jsonRpcPayload) ?: return null
        val error = root.getAsJsonObject("error") ?: return null
        error.get("message")?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString?.let { return it }
        error.get("detail")?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString?.let { return it }
        return error.toString()
    }

    fun parseObject(json: String): JsonObject? = runCatching { JsonParser.parseString(json).asJsonObject }.getOrNull()

    private fun safeObjectJson(json: String): String {
        val parsed = JsonParser.parseString(json.ifBlank { "{}" })
        return if (parsed is JsonObject) parsed.toString() else "{}"
    }

    private fun isJsonPayload(candidate: String): Boolean = parseObject(candidate) != null

    private fun quote(value: String): String = gson.toJson(value)
}
