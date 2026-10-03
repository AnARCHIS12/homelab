package com.homelab.app.data.repository

import android.content.Context
import com.homelab.app.R
import com.homelab.app.data.remote.TlsClientSelector
import com.homelab.app.util.ServiceType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

data class TrueNasSystemInfo(
    val hostname: String?,
    val version: String?,
    val product: String?,
    val uptimeSeconds: Long?,
    val physicalMemoryBytes: Double?
)

data class TrueNasPool(
    val id: String,
    val name: String,
    val status: String,
    val usedBytes: Double,
    val totalBytes: Double,
    val freeBytes: Double,
    val healthy: Boolean
) {
    val usedFraction: Float
        get() = if (totalBytes > 0.0) (usedBytes / totalBytes).coerceIn(0.0, 1.0).toFloat() else 0f
}

data class TrueNasDisk(
    val id: String,
    val name: String,
    val model: String?,
    val serial: String?,
    val sizeBytes: Double?,
    val status: String?
)

data class TrueNasAlert(
    val id: String,
    val level: String,
    val title: String,
    val message: String,
    val timestamp: String?
)

data class TrueNasServiceStatus(
    val name: String,
    val state: String,
    val enabled: Boolean
) {
    val isRunning: Boolean
        get() = state.equals("RUNNING", ignoreCase = true) ||
            state.equals("running", ignoreCase = true) ||
            state.equals("started", ignoreCase = true) ||
            state.equals("active", ignoreCase = true)
}

data class TrueNasShareSummary(
    val smb: Int,
    val nfs: Int,
    val iscsi: Int
)

data class TrueNasWorkloadSummary(
    val apps: Int,
    val runningApps: Int,
    val virtualMachines: Int,
    val runningVirtualMachines: Int
)

data class TrueNasDashboardSnapshot(
    val system: TrueNasSystemInfo,
    val pools: List<TrueNasPool>,
    val disks: List<TrueNasDisk>,
    val alerts: List<TrueNasAlert>,
    val services: List<TrueNasServiceStatus>,
    val shares: TrueNasShareSummary,
    val workloads: TrueNasWorkloadSummary
) {
    val healthyPoolCount: Int get() = pools.count { it.healthy }
    val runningServiceCount: Int get() = services.count { it.isRunning }
    val totalStorageBytes: Double get() = pools.sumOf { it.totalBytes }
    val usedStorageBytes: Double get() = pools.sumOf { it.usedBytes }
}

@Singleton
class TrueNasRepository @Inject constructor(
    private val serviceInstancesRepository: ServiceInstancesRepository,
    private val tlsClientSelector: TlsClientSelector,
    private val json: Json,
    @param:ApplicationContext private val context: Context
) {

    suspend fun authenticate(
        url: String,
        apiKey: String,
        fallbackUrl: String? = null,
        allowSelfSigned: Boolean = false
    ) {
        require(apiKey.trim().isNotBlank()) { context.getString(R.string.login_error_api_key_required) }
        val candidates = baseCandidates(url, fallbackUrl)
        require(candidates.isNotEmpty()) { context.getString(R.string.login_error_url_required) }
        var lastError: Exception? = null
        for (base in candidates) {
            try {
                requireSecureTransport(base)
                requestMethod(
                    baseUrl = base,
                    apiKey = apiKey,
                    allowSelfSigned = allowSelfSigned,
                    method = "system.info"
                )
                return
            } catch (error: Exception) {
                lastError = error
            }
        }
        throw lastError ?: IllegalStateException(context.getString(R.string.truenas_auth_failed))
    }

    suspend fun getDashboard(instanceId: String): TrueNasDashboardSnapshot = coroutineScope {
        val instance = serviceInstancesRepository.getInstance(instanceId)
            ?: throw IllegalArgumentException("TrueNAS instance not found.")
        require(instance.type == ServiceType.TRUENAS) { "Instance is not TrueNAS." }
        val apiKey = instance.apiKey?.trim().orEmpty()
        require(apiKey.isNotBlank()) { context.getString(R.string.login_error_api_key_required) }
        val bases = baseCandidates(instance.url, instance.fallbackUrl)

        val system = parseSystemInfo(fetchFirst(bases, apiKey, instance.allowSelfSigned, "system.info"))
        val pools = runCatching {
            parsePools(
                fetchFirst(
                    bases,
                    apiKey,
                    instance.allowSelfSigned,
                    "pool.query",
                    listOf(
                        JsonArray(emptyList()),
                        buildJsonObject {
                            put("extra", buildJsonObject { put("is_upgraded", JsonPrimitive(false)) })
                        }
                    )
                )
            )
        }.getOrDefault(emptyList())
        val diskParams = listOf(
            JsonArray(emptyList()),
            buildJsonObject {
                put("extra", buildJsonObject { put("pools", JsonPrimitive(true)) })
            }
        )
        val disks = runCatching {
            parseDisks(fetchFirst(bases, apiKey, instance.allowSelfSigned, "disk.query", diskParams))
        }.getOrDefault(emptyList())
        val alerts = runCatching {
            parseAlerts(fetchFirst(bases, apiKey, instance.allowSelfSigned, "alert.list"))
        }.getOrDefault(emptyList())
        val services = runCatching {
            parseServices(fetchFirst(bases, apiKey, instance.allowSelfSigned, "service.query"))
        }.getOrDefault(emptyList())
        val iscsiObject = fetchFirstOptional(bases, apiKey, instance.allowSelfSigned, "iscsi.target.query")
            ?: fetchFirstOptional(bases, apiKey, instance.allowSelfSigned, "sharing.iscsi.target.query")
        val appObject = fetchFirstOptional(bases, apiKey, instance.allowSelfSigned, "app.query")
        val vmObject = fetchFirstOptional(bases, apiKey, instance.allowSelfSigned, "vm.query")
        val shares = TrueNasShareSummary(
            smb = runCatching { countItems(fetchFirst(bases, apiKey, instance.allowSelfSigned, "sharing.smb.query")) }.getOrDefault(0),
            nfs = runCatching { countItems(fetchFirst(bases, apiKey, instance.allowSelfSigned, "sharing.nfs.query")) }.getOrDefault(0),
            iscsi = iscsiObject?.let(::countItems) ?: 0
        )
        val workloads = TrueNasWorkloadSummary(
            apps = appObject?.let(::countItems) ?: 0,
            runningApps = appObject?.let(::countRunningItems) ?: 0,
            virtualMachines = vmObject?.let(::countItems) ?: 0,
            runningVirtualMachines = vmObject?.let(::countRunningItems) ?: 0
        )

        TrueNasDashboardSnapshot(
            system = system,
            pools = pools,
            disks = disks,
            alerts = alerts,
            services = services,
            shares = shares,
            workloads = workloads
        )
    }

    suspend fun getSummary(instanceId: String): TrueNasDashboardSnapshot = getDashboard(instanceId)

    private suspend fun fetchFirst(
        bases: List<String>,
        apiKey: String,
        allowSelfSigned: Boolean,
        method: String,
        params: List<JsonElement> = emptyList()
    ): JsonElement {
        var lastError: Exception? = null
        for (base in bases) {
            try {
                requireSecureTransport(base)
                return requestMethod(base, apiKey, allowSelfSigned, method, params)
            } catch (error: Exception) {
                lastError = error
            }
        }
        throw lastError ?: IllegalStateException(context.getString(R.string.truenas_request_failed))
    }

    private suspend fun fetchFirstOptional(
        bases: List<String>,
        apiKey: String,
        allowSelfSigned: Boolean,
        method: String,
        params: List<JsonElement> = emptyList()
    ): JsonElement? {
        return runCatching { fetchFirst(bases, apiKey, allowSelfSigned, method, params) }.getOrNull()
    }

    private suspend fun requestMethod(
        baseUrl: String,
        apiKey: String,
        allowSelfSigned: Boolean,
        method: String,
        params: List<JsonElement> = emptyList()
    ): JsonElement = withContext(Dispatchers.IO) {
        val client = tlsClientSelector.forAllowSelfSigned(allowSelfSigned, baseUrl)
            .newBuilder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .writeTimeout(12, TimeUnit.SECONDS)
            .callTimeout(16, TimeUnit.SECONDS)
            .build()

        val attempts = listOf(
            WebSocketAttempt("/api/current", ProtocolMode.JSON_RPC),
            WebSocketAttempt("/api/v25.04", ProtocolMode.JSON_RPC),
            WebSocketAttempt("/websocket", ProtocolMode.DDP)
        )
        var lastError: Exception? = null
        for (attempt in attempts) {
            try {
                val url = webSocketUrl(baseUrl, attempt.endpoint)
                return@withContext when (attempt.mode) {
                    ProtocolMode.JSON_RPC -> requestJsonRpc(client, url, apiKey, method, params)
                    ProtocolMode.DDP -> requestDdp(client, url, apiKey, method, params)
                }
            } catch (error: Exception) {
                lastError = error
            }
        }
        throw lastError ?: IllegalStateException(context.getString(R.string.truenas_request_failed))
    }

    private suspend fun requestJsonRpc(
        client: OkHttpClient,
        url: String,
        apiKey: String,
        method: String,
        params: List<JsonElement>
    ): JsonElement {
        val session = WebSocketSession(client, url, json)
        return session.use { active ->
            val auth = active.jsonRpcCall("auth.login_with_api_key", listOf(JsonPrimitive(apiKey.trim())))
            val authAccepted = (auth as? JsonPrimitive)?.booleanOrNull
            if (authAccepted == false) {
                throw IllegalStateException(context.getString(R.string.truenas_invalid_api_key))
            }
            active.jsonRpcCall(method, params)
        }
    }

    private suspend fun requestDdp(
        client: OkHttpClient,
        url: String,
        apiKey: String,
        method: String,
        params: List<JsonElement>
    ): JsonElement {
        val session = WebSocketSession(client, url, json)
        return session.use { active ->
            active.sendDdpConnect()
            active.ddpCall("auth.login_with_api_key", listOf(JsonPrimitive(apiKey.trim())))
            active.ddpCall(method, params)
        }
    }

    private fun parseSystemInfo(element: JsonElement): TrueNasSystemInfo {
        val obj = element.firstObjectOrNull()
        return TrueNasSystemInfo(
            hostname = obj?.string("hostname", "host_name", "name"),
            version = obj?.string("version", "version_short", "buildtime", "system_version"),
            product = obj?.string("system_product", "product", "model", "system_product_name", "system_manufacturer"),
            uptimeSeconds = obj?.long("uptime_seconds", "uptime", "uptime_s"),
            physicalMemoryBytes = obj?.double("physmem", "physical_memory", "memory")
        )
    }

    private fun parsePools(element: JsonElement): List<TrueNasPool> {
        return element.objectArray().mapIndexedNotNull { index, obj ->
            val name = obj.string("name", "pool_name", "id").ifBlank { return@mapIndexedNotNull null }
            val topology = obj["topology"] as? JsonObject
            val total = obj.optionalDouble("size", "total", "total_bytes", "raw_capacity")
                ?: topology?.optionalDouble("size", "total", "raw_capacity")
                ?: 0.0
            val free = obj.optionalDouble("free", "available", "avail", "free_bytes")
                ?: topology?.optionalDouble("free", "available", "avail")
                ?: 0.0
            val allocated = obj.optionalDouble("allocated", "used", "used_bytes")
                ?: topology?.optionalDouble("allocated", "used")
                ?: (total - free).coerceAtLeast(0.0)
            val status = obj.string("status", "health", "state").ifBlank { "UNKNOWN" }
            TrueNasPool(
                id = obj.string("id", "guid", "name").ifBlank { name },
                name = name,
                status = status,
                usedBytes = allocated,
                totalBytes = total,
                freeBytes = free,
                healthy = status.equals("ONLINE", ignoreCase = true) ||
                    status.equals("HEALTHY", ignoreCase = true) ||
                    obj.boolean("healthy", "is_healthy")
            )
        }
    }

    private fun parseDisks(element: JsonElement): List<TrueNasDisk> {
        return element.objectArray().mapIndexedNotNull { index, obj ->
            val name = obj.string("name", "devname", "identifier", "id").ifBlank { "disk-${index + 1}" }
            TrueNasDisk(
                id = obj.string("identifier", "id", "name").ifBlank { name },
                name = name,
                model = obj.string("model", "descr").ifBlank { null },
                serial = obj.string("serial", "serial_number").ifBlank { null },
                sizeBytes = obj.double("size", "capacity", "bytes").takeIf { it > 0.0 },
                status = obj.string("status", "state").ifBlank { null }
            )
        }
    }

    private fun parseAlerts(element: JsonElement): List<TrueNasAlert> {
        return element.objectArray().mapIndexed { index, obj ->
            val text = obj.string("text", "message", "formatted", "klass")
            TrueNasAlert(
                id = obj.string("uuid", "id").ifBlank { "alert-$index" },
                level = obj.string("level", "severity").ifBlank { "INFO" },
                title = obj.string("klass", "title").ifBlank { obj.string("level", "severity").ifBlank { "Alert" } },
                message = text.ifBlank { obj.toString() },
                timestamp = obj.string("datetime", "timestamp", "created").ifBlank { null }
            )
        }
    }

    private fun parseServices(element: JsonElement): List<TrueNasServiceStatus> {
        return element.objectArray().mapIndexedNotNull { index, obj ->
            val name = obj.string("service", "name", "id").ifBlank { return@mapIndexedNotNull null }
            TrueNasServiceStatus(
                name = name,
                state = obj.string("state", "status").ifBlank { "UNKNOWN" },
                enabled = obj.boolean("enable", "enabled")
            )
        }.sortedWith(
            compareByDescending<TrueNasServiceStatus> { it.isRunning }
                .thenByDescending { it.enabled }
                .thenBy { it.name.lowercase() }
        )
    }

    private fun countItems(element: JsonElement): Int = element.collectionArray().size

    private fun countRunningItems(element: JsonElement): Int {
        return element.collectionArray().count { obj ->
            val state = obj.string("state", "status")
                .ifBlank { obj.stringAt("metadata", "state") }
                .ifBlank { obj.stringAt("status", "state") }
            state.equals("RUNNING", ignoreCase = true) ||
                state.equals("active", ignoreCase = true) ||
                state.contains("running", ignoreCase = true) ||
                obj.boolean("active", "running")
        }
    }

    private fun baseCandidates(url: String, fallbackUrl: String?): List<String> {
        return listOf(url, fallbackUrl.orEmpty())
            .mapNotNull { raw -> raw.trim().takeIf { it.isNotBlank() }?.let(::cleanUrl) }
            .distinct()
    }

    private fun requireSecureTransport(raw: String) {
        require(usesSecureApiTransport(raw)) { context.getString(R.string.truenas_secure_transport_required) }
    }

    private fun cleanUrl(raw: String): String {
        val trimmed = raw.trim().trimEnd('/')
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("ws://", ignoreCase = true) ||
            trimmed.startsWith("wss://", ignoreCase = true)
        ) {
            return trimmed
        }
        return "https://$trimmed"
    }

    private fun webSocketUrl(rawBase: String, endpoint: String): String {
        val cleaned = cleanUrl(rawBase)
        val parseable = when {
            cleaned.startsWith("wss://", ignoreCase = true) -> "https://" + cleaned.substringAfter("://")
            cleaned.startsWith("ws://", ignoreCase = true) -> "http://" + cleaned.substringAfter("://")
            else -> cleaned
        }
        val httpUrl = parseable.toHttpUrl()
        val path = if (endpoint.startsWith("/")) endpoint else "/$endpoint"
        val rebuilt = httpUrl.newBuilder()
            .encodedPath(path)
            .encodedQuery(null)
            .build()
            .toString()
        return when {
            rebuilt.startsWith("https://", ignoreCase = true) -> "wss://" + rebuilt.substringAfter("://")
            rebuilt.startsWith("http://", ignoreCase = true) -> "ws://" + rebuilt.substringAfter("://")
            else -> rebuilt
        }
    }

    private fun JsonElement.firstObjectOrNull(): JsonObject? {
        return when (this) {
            is JsonObject -> this
            is JsonArray -> this.firstOrNull() as? JsonObject
            else -> null
        }
    }

    private fun JsonElement.objectArray(): List<JsonObject> {
        return when (this) {
            is JsonArray -> mapNotNull { it as? JsonObject }
            is JsonObject -> {
                val direct = listOf("rows", "data", "items", "results", "result", "response")
                    .firstNotNullOfOrNull { key -> this[key] as? JsonArray }
                direct?.mapNotNull { it as? JsonObject } ?: listOf(this)
            }
            else -> emptyList()
        }
    }

    private fun JsonElement.collectionArray(): List<JsonObject> {
        return when (this) {
            is JsonArray -> mapNotNull { it as? JsonObject }
            is JsonObject -> {
                val direct = listOf("rows", "data", "items", "results", "result", "response")
                    .firstNotNullOfOrNull { key -> this[key] as? JsonArray }
                direct?.mapNotNull { it as? JsonObject } ?: emptyList()
            }
            else -> emptyList()
        }
    }

    private fun JsonObject.string(vararg keys: String): String {
        return keys.firstNotNullOfOrNull { key ->
            (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
        }.orEmpty()
    }

    private fun JsonObject.double(vararg keys: String): Double {
        return optionalDouble(*keys) ?: 0.0
    }

    private fun JsonObject.optionalDouble(vararg keys: String): Double? {
        return keys.firstNotNullOfOrNull { key ->
            (this[key] as? JsonPrimitive)?.doubleOrNull
        }
    }

    private fun JsonObject.long(vararg keys: String): Long? {
        return keys.firstNotNullOfOrNull { key ->
            (this[key] as? JsonPrimitive)?.longOrNull
        }
    }

    private fun JsonObject.boolean(vararg keys: String): Boolean {
        return keys.firstNotNullOfOrNull { key ->
            (this[key] as? JsonPrimitive)?.booleanOrNull
        } ?: false
    }

    private fun JsonObject.stringAt(vararg path: String): String {
        var current: JsonElement = this
        for (key in path) {
            current = (current as? JsonObject)?.get(key) ?: return ""
        }
        return (current as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }.orEmpty()
    }

    private fun protocolError(obj: JsonObject): String? {
        val error = obj["error"] ?: return null
        if (error is JsonObject) {
            return error.string("reason", "message", "error").ifBlank { error.toString() }
        }
        return (error as? JsonPrimitive)?.contentOrNull
    }

    private suspend fun <T> WebSocketSession.use(block: suspend (WebSocketSession) -> T): T {
        try {
            open()
            return block(this)
        } finally {
            close()
        }
    }

    private inner class WebSocketSession(
        private val client: OkHttpClient,
        private val url: String,
        private val json: Json
    ) {
        private val messages = Channel<JsonElement>(Channel.UNLIMITED)
        private var webSocket: WebSocket? = null

        fun open() {
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .build()
            webSocket = client.newWebSocket(
                request,
                object : WebSocketListener() {
                    override fun onMessage(webSocket: WebSocket, text: String) {
                        runCatching { json.parseToJsonElement(text) }
                            .onSuccess { messages.trySend(it) }
                    }

                    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                        onMessage(webSocket, bytes.utf8())
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        messages.close(t)
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        messages.close()
                    }
                }
            )
        }

        suspend fun jsonRpcCall(method: String, params: List<JsonElement>): JsonElement {
            val id = UUID.randomUUID().toString()
            val payload = buildJsonObject {
                put("jsonrpc", JsonPrimitive("2.0"))
                put("id", JsonPrimitive(id))
                put("method", JsonPrimitive(method))
                put("params", JsonArray(params))
            }.toString()
            check(webSocket?.send(payload) == true) { context.getString(R.string.truenas_request_failed) }
            return withTimeout(12_000L) {
                while (true) {
                    val obj = messages.receive() as? JsonObject ?: continue
                    if (obj["id"]?.jsonPrimitive?.contentOrNull != id) continue
                    protocolError(obj)?.let { throw IllegalStateException(it) }
                    return@withTimeout obj["result"] ?: JsonNull
                }
                JsonNull
            }
        }

        suspend fun sendDdpConnect() {
            val payload = buildJsonObject {
                put("msg", JsonPrimitive("connect"))
                put("version", JsonPrimitive("1"))
                put("support", buildJsonArray { add(JsonPrimitive("1")) })
            }.toString()
            check(webSocket?.send(payload) == true) { context.getString(R.string.truenas_request_failed) }
            withTimeout(12_000L) {
                while (true) {
                    val obj = messages.receive() as? JsonObject ?: continue
                    when (obj["msg"]?.jsonPrimitive?.contentOrNull) {
                        "connected" -> return@withTimeout
                        "failed" -> throw IllegalStateException(context.getString(R.string.truenas_request_failed))
                    }
                }
            }
        }

        suspend fun ddpCall(method: String, params: List<JsonElement>): JsonElement {
            val id = UUID.randomUUID().toString()
            val payload = buildJsonObject {
                put("msg", JsonPrimitive("method"))
                put("id", JsonPrimitive(id))
                put("method", JsonPrimitive(method))
                put("params", JsonArray(params))
            }.toString()
            check(webSocket?.send(payload) == true) { context.getString(R.string.truenas_request_failed) }
            return withTimeout(12_000L) {
                while (true) {
                    val obj = messages.receive() as? JsonObject ?: continue
                    if (obj["id"]?.jsonPrimitive?.contentOrNull != id) continue
                    if (obj["msg"]?.jsonPrimitive?.contentOrNull != "result") continue
                    protocolError(obj)?.let { throw IllegalStateException(it) }
                    return@withTimeout obj["result"] ?: JsonNull
                }
                JsonNull
            }
        }

        fun close() {
            webSocket?.close(1000, null)
            messages.close()
        }
    }

    private data class WebSocketAttempt(val endpoint: String, val mode: ProtocolMode)
    private enum class ProtocolMode { JSON_RPC, DDP }

    companion object {
        fun usesSecureApiTransport(raw: String): Boolean {
            val trimmed = raw.trim()
            if (trimmed.isBlank()) return false
            val normalized = if ("://" in trimmed) trimmed else "https://$trimmed"
            val scheme = normalized.substringBefore("://", "").lowercase()
            return scheme == "https" || scheme == "wss"
        }
    }
}
