package com.homelab.app.data.repository

import com.homelab.app.data.local.SettingsManager
import com.homelab.app.data.local.dao.ServiceInstanceDao
import com.homelab.app.data.local.entity.ServiceInstanceEntity
import com.homelab.app.data.security.InstanceCredentials
import com.homelab.app.data.security.SecureCredentialsStore
import com.homelab.app.data.security.UrlSecurityValidator
import com.homelab.app.domain.model.PiHoleAuthMode
import com.homelab.app.domain.model.ServiceInstance
import com.homelab.app.util.ServiceType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.net.URI
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServiceInstancesRepository @Inject constructor(
    private val dao: ServiceInstanceDao,
    private val settingsManager: SettingsManager,
    private val secureCredentialsStore: SecureCredentialsStore
) {
    val allInstances: Flow<List<ServiceInstance>> = dao.observeAll().map { entities ->
        val allCreds = secureCredentialsStore.getAllCredentials()
        entities.map { it.toDomain(allCreds[it.id]) }
    }

    val instancesByType: Flow<Map<ServiceType, List<ServiceInstance>>> = allInstances.map { instances ->
        ServiceType.entries
            .filter { it != ServiceType.UNKNOWN }
            .associateWith { type -> instances.filter { it.type == type } }
    }

    val preferredInstanceIdByType: Flow<Map<ServiceType, String?>> = settingsManager.preferredInstanceIds

    val preferredInstancesByType: Flow<Map<ServiceType, ServiceInstance?>> = combine(
        instancesByType,
        preferredInstanceIdByType
    ) { grouped, preferredIds ->
        ServiceType.entries
            .filter { it != ServiceType.UNKNOWN }
            .associateWith { type ->
                val instances = grouped[type].orEmpty()
                val preferredId = preferredIds[type]
                instances.firstOrNull { it.id == preferredId } ?: instances.firstOrNull()
            }
    }

    suspend fun initialize() {
        migrateLegacyDataIfNeeded()
        migrateRoomSecretsToKeystoreStoreIfNeeded()
        normalizeStoredInstancesIfNeeded()
        repairAllPreferredInstances()
    }

    suspend fun getInstance(id: String): ServiceInstance? {
        val entity = dao.getById(id) ?: return null
        val creds = secureCredentialsStore.getCredentials(id)
        return entity.toDomain(creds)
    }

    suspend fun getAllInstances(): List<ServiceInstance> {
        val entities = dao.getAll()
        val allCreds = secureCredentialsStore.getAllCredentials()
        return entities.map { it.toDomain(allCreds[it.id]) }
    }

    suspend fun getInstances(type: ServiceType): List<ServiceInstance> {
        val entities = dao.getByType(type.name)
        val allCreds = secureCredentialsStore.getAllCredentials()
        return entities.map { it.toDomain(allCreds[it.id]) }
    }

    suspend fun getPreferredInstance(type: ServiceType): ServiceInstance? {
        val instances = getInstances(type)
        val preferredId = settingsManager.preferredInstanceId(type).first()
        val preferred = instances.firstOrNull { it.id == preferredId } ?: instances.firstOrNull()
        if (preferred?.id != preferredId) {
            settingsManager.setPreferredInstanceId(type, preferred?.id)
        }
        return preferred
    }

    suspend fun saveInstance(instance: ServiceInstance) {
        val normalized = normalizeInstance(instance)

        // 1. Save sensitive credentials to Android Keystore-backed storage
        secureCredentialsStore.saveCredentials(
            normalized.id,
            InstanceCredentials(
                token = normalized.token,
                password = normalized.password,
                apiKey = normalized.apiKey,
                proxmoxCsrfToken = normalized.proxmoxCsrfToken,
                proxmoxOtp = normalized.proxmoxOtp,
                piholePassword = normalized.piholePassword
            )
        )

        // 2. Save public metadata to Room with secrets stripped/cleared
        dao.upsert(normalized.toMetadataEntity())

        val currentPreferred = settingsManager.preferredInstanceId(normalized.type).first()
        if (currentPreferred.isNullOrBlank()) {
            settingsManager.setPreferredInstanceId(normalized.type, normalized.id)
        }
    }

    suspend fun deleteInstance(id: String) {
        val instance = getInstance(id) ?: return
        dao.deleteById(id)
        secureCredentialsStore.deleteCredentials(id)
        repairPreferredInstance(instance.type)
    }

    suspend fun setPreferredInstance(type: ServiceType, instanceId: String?) {
        val validId = instanceId?.takeIf { candidate ->
            dao.getById(candidate)?.let { entity ->
                ServiceType.fromStoredName(entity.type) == type
            } == true
        }
        settingsManager.setPreferredInstanceId(type, validId)
        if (validId == null) {
            repairPreferredInstance(type)
        }
    }

    suspend fun migrateLegacyDataIfNeeded() {
        if (settingsManager.serviceInstancesMigrated.first()) {
            return
        }

        ServiceType.entries
            .filter { it != ServiceType.UNKNOWN }
            .forEach { type ->
                val existing = getInstances(type)
                val legacy = settingsManager.getLegacyConnection(type)

                if (legacy != null && existing.isEmpty()) {
                    val migrated = normalizeInstance(legacy.migratedInstance(UUID.randomUUID().toString()))
                    saveInstance(migrated)
                    settingsManager.setPreferredInstanceId(type, migrated.id)
                } else if (existing.isNotEmpty()) {
                    val currentPreferred = settingsManager.preferredInstanceId(type).first()
                    if (currentPreferred.isNullOrBlank()) {
                        settingsManager.setPreferredInstanceId(type, existing.first().id)
                    }
                }

                settingsManager.removeLegacyConnection(type)
            }

        settingsManager.setServiceInstancesMigrated(true)
    }

    /**
     * Security migration: extracts any pre-existing plaintext credentials from Room SQLite table
     * into KeystoreSecureCredentialsStore, then clears those columns from Room so no secrets
     * remain in plaintext on disk.
     */
    private suspend fun migrateRoomSecretsToKeystoreStoreIfNeeded() {
        val entities = dao.getAll()
        var updated = false
        val sanitizedEntities = entities.map { entity ->
            val hasPlaintextSecrets = entity.token.isNotBlank() ||
                !entity.password.isNullOrBlank() ||
                !entity.apiKey.isNullOrBlank() ||
                !entity.proxmoxCsrfToken.isNullOrBlank() ||
                !entity.proxmoxOtp.isNullOrBlank() ||
                !entity.piholePassword.isNullOrBlank()

            if (hasPlaintextSecrets) {
                val existingCreds = secureCredentialsStore.getCredentials(entity.id)
                if (existingCreds == null) {
                    secureCredentialsStore.saveCredentials(
                        entity.id,
                        InstanceCredentials(
                            token = entity.token,
                            password = entity.password,
                            apiKey = entity.apiKey,
                            proxmoxCsrfToken = entity.proxmoxCsrfToken,
                            proxmoxOtp = entity.proxmoxOtp,
                            piholePassword = entity.piholePassword
                        )
                    )
                }
                updated = true
                entity.copy(
                    token = "",
                    password = null,
                    apiKey = null,
                    proxmoxCsrfToken = null,
                    proxmoxOtp = null,
                    piholePassword = null
                )
            } else {
                entity
            }
        }

        if (updated) {
            dao.upsertAll(sanitizedEntities)
        }
    }

    private suspend fun normalizeStoredInstancesIfNeeded() {
        val entities = dao.getAll()
        if (entities.isEmpty()) return

        val normalized = entities.map { entity ->
            val serviceType = ServiceType.fromStoredName(entity.type)
            val normalizedType = serviceType.name
            val normalizedUrl = normalizeUrl(entity.url, serviceType, entity.allowHttp)
            val normalizedFallback = normalizeOptionalUrl(entity.fallbackUrl, serviceType, entity.allowHttp)
            if (
                normalizedType == entity.type &&
                normalizedUrl == entity.url &&
                normalizedFallback == entity.fallbackUrl
            ) {
                entity
            } else {
                entity.copy(
                    type = normalizedType,
                    url = normalizedUrl,
                    fallbackUrl = normalizedFallback
                )
            }
        }

        if (normalized != entities) {
            dao.upsertAll(normalized)
        }
    }

    suspend fun repairAllPreferredInstances() {
        ServiceType.entries
            .filter { it != ServiceType.UNKNOWN }
            .forEach { repairPreferredInstance(it) }
    }

    suspend fun repairPreferredInstance(type: ServiceType) {
        val instances = getInstances(type)
        val currentPreferred = settingsManager.preferredInstanceId(type).first()
        val repaired = instances.firstOrNull { it.id == currentPreferred } ?: instances.firstOrNull()
        settingsManager.setPreferredInstanceId(type, repaired?.id)
    }
}

private fun ServiceInstanceEntity.toDomain(credentials: InstanceCredentials?): ServiceInstance {
    return ServiceInstance(
        id = id,
        type = ServiceType.fromStoredName(type),
        label = label,
        url = url,
        token = credentials?.token ?: token,
        proxmoxCsrfToken = credentials?.proxmoxCsrfToken ?: proxmoxCsrfToken,
        proxmoxOtp = credentials?.proxmoxOtp ?: proxmoxOtp,
        username = username,
        apiKey = credentials?.apiKey ?: apiKey,
        piholePassword = credentials?.piholePassword ?: piholePassword,
        piholeAuthMode = piholeAuthMode?.let(PiHoleAuthMode::valueOf),
        fallbackUrl = fallbackUrl,
        allowSelfSigned = allowSelfSigned,
        password = credentials?.password ?: password,
        allowHttp = allowHttp,
        customCertFingerprint = customCertFingerprint,
        customCertificatePem = customCertificatePem
    )
}

private fun ServiceInstance.toMetadataEntity(): ServiceInstanceEntity {
    return ServiceInstanceEntity(
        id = id,
        type = type.name,
        label = label.ifBlank { type.displayName },
        url = url,
        // Strip sensitive credentials from Room entity:
        token = "",
        proxmoxCsrfToken = null,
        proxmoxOtp = null,
        username = username,
        apiKey = null,
        piholePassword = null,
        piholeAuthMode = piholeAuthMode?.name,
        fallbackUrl = fallbackUrl,
        allowSelfSigned = allowSelfSigned,
        password = null,
        allowHttp = allowHttp,
        customCertFingerprint = customCertFingerprint,
        customCertificatePem = customCertificatePem
    )
}

private fun normalizeUrl(raw: String, type: ServiceType? = null, allowHttp: Boolean = false): String {
    return try {
        val clean = UrlSecurityValidator.validateAndNormalizeUrl(raw, allowHttp)
        if (type == ServiceType.UNIFI_NETWORK) stripKnownUnifiApiPath(clean) else clean
    } catch (_: Exception) {
        var clean = raw.trim()
        clean = clean.trimEnd { it == ')' || it == ']' || it == '}' || it == ',' || it == ';' }
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            clean = "https://$clean"
        }
        clean.replace(Regex("/+$"), "")
    }
}

private fun normalizeOptionalUrl(raw: String?, type: ServiceType? = null, allowHttp: Boolean = false): String? {
    if (raw.isNullOrBlank()) return null
    val normalized = normalizeUrl(raw, type, allowHttp)
    return normalized.ifBlank { null }
}

private fun normalizeInstance(instance: ServiceInstance): ServiceInstance {
    val normalizedUrl = normalizeUrl(instance.url, instance.type, instance.allowHttp)
    val normalizedFallback = normalizeOptionalUrl(instance.fallbackUrl, instance.type, instance.allowHttp)
    if (normalizedUrl == instance.url && normalizedFallback == instance.fallbackUrl) {
        return instance
    }
    return instance.copy(url = normalizedUrl, fallbackUrl = normalizedFallback)
}

private fun stripKnownUnifiApiPath(raw: String): String {
    return runCatching {
        val uri = URI(raw)
        val path = uri.rawPath.orEmpty()
        if (!isKnownUnifiApiPath(path)) return@runCatching raw
        URI(uri.scheme, uri.userInfo, uri.host, uri.port, null, null, null).toString()
    }.getOrDefault(raw)
}

private fun isKnownUnifiApiPath(path: String): Boolean {
    val normalized = path.trimEnd('/')
    return normalized == "/proxy/network/integration/v1" ||
        normalized.startsWith("/proxy/network/integration/v1/") ||
        normalized == "/v1" ||
        normalized.startsWith("/v1/")
}
