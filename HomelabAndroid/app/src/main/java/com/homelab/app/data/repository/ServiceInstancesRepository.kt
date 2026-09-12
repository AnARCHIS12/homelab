package com.homelab.app.data.repository

import com.homelab.app.data.local.SettingsManager
import com.homelab.app.data.local.dao.ServiceInstanceDao
import com.homelab.app.data.local.entity.ServiceInstanceEntity
import com.homelab.app.data.security.InstanceCredentials
import com.homelab.app.data.security.SecureCredentialsStore
import com.homelab.app.data.security.ServiceUrlNormalizer
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
        entities.map { it.toDomain() }
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
        normalizeStoredInstancesIfNeeded()
        repairAllPreferredInstances()
    }

    suspend fun getInstance(id: String): ServiceInstance? = dao.getById(id)?.toDomain()

    suspend fun getAllInstances(): List<ServiceInstance> {
        return dao.getAll().map { it.toDomain() }
    }

    suspend fun getInstances(type: ServiceType): List<ServiceInstance> {
        return dao.getByType(type.name).map { it.toDomain() }
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

        // 2. Save instance to Room database with full persistence fallback
        dao.upsert(normalized.toEntity())

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
        entities.forEach { entity ->
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
            }
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

private fun ServiceInstanceEntity.toDomain(credentials: InstanceCredentials? = null): ServiceInstance {
    val effectiveToken = credentials?.token?.takeIf { it.isNotBlank() } ?: token
    val effectivePassword = credentials?.password?.takeIf { it.isNotBlank() } ?: password
    val effectiveApiKey = credentials?.apiKey?.takeIf { it.isNotBlank() } ?: apiKey
    val effectiveProxmoxCsrf = credentials?.proxmoxCsrfToken?.takeIf { it.isNotBlank() } ?: proxmoxCsrfToken
    val effectiveProxmoxOtp = credentials?.proxmoxOtp?.takeIf { it.isNotBlank() } ?: proxmoxOtp
    val effectivePiholePassword = credentials?.piholePassword?.takeIf { it.isNotBlank() } ?: piholePassword
    return ServiceInstance(
        id = id,
        type = ServiceType.fromStoredName(type),
        label = label,
        url = url,
        token = effectiveToken,
        proxmoxCsrfToken = effectiveProxmoxCsrf,
        proxmoxOtp = effectiveProxmoxOtp,
        username = username,
        apiKey = effectiveApiKey,
        piholePassword = effectivePiholePassword,
        piholeAuthMode = piholeAuthMode?.let(PiHoleAuthMode::valueOf),
        fallbackUrl = fallbackUrl,
        allowSelfSigned = allowSelfSigned,
        password = effectivePassword,
        allowHttp = allowHttp,
        customCertFingerprint = customCertFingerprint,
        customCertificatePem = customCertificatePem
    )
}

private fun ServiceInstance.toEntity(): ServiceInstanceEntity {
    return ServiceInstanceEntity(
        id = id,
        type = type.name,
        label = label.ifBlank { type.displayName },
        url = url,
        token = token,
        proxmoxCsrfToken = proxmoxCsrfToken,
        proxmoxOtp = proxmoxOtp,
        username = username,
        apiKey = apiKey,
        piholePassword = piholePassword,
        piholeAuthMode = piholeAuthMode?.name,
        fallbackUrl = fallbackUrl,
        allowSelfSigned = allowSelfSigned,
        password = password,
        allowHttp = allowHttp,
        customCertFingerprint = customCertFingerprint,
        customCertificatePem = customCertificatePem
    )
}

private fun normalizeUrl(raw: String, type: ServiceType? = null, allowHttp: Boolean = false): String {
    return ServiceUrlNormalizer.normalizeUrl(raw, type, allowHttp)
}

private fun normalizeOptionalUrl(raw: String?, type: ServiceType? = null, allowHttp: Boolean = false): String? {
    return ServiceUrlNormalizer.normalizeOptionalUrl(raw, type, allowHttp)
}

private fun normalizeInstance(instance: ServiceInstance): ServiceInstance {
    val normalizedUrl = normalizeUrl(instance.url, instance.type, instance.allowHttp)
    val normalizedFallback = normalizeOptionalUrl(instance.fallbackUrl, instance.type, instance.allowHttp)
    if (normalizedUrl == instance.url && normalizedFallback == instance.fallbackUrl) {
        return instance
    }
    return instance.copy(url = normalizedUrl, fallbackUrl = normalizedFallback)
}
