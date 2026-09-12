package com.homelab.app.data.security

/**
 * In-memory test fake for SecureCredentialsStore.
 */
class FakeSecureCredentialsStore : SecureCredentialsStore {
    private val store = mutableMapOf<String, InstanceCredentials>()

    override suspend fun getCredentials(instanceId: String): InstanceCredentials? {
        return store[instanceId]
    }

    override suspend fun saveCredentials(instanceId: String, credentials: InstanceCredentials) {
        store[instanceId] = credentials
    }

    override suspend fun deleteCredentials(instanceId: String) {
        store.remove(instanceId)
    }

    override suspend fun getAllCredentials(): Map<String, InstanceCredentials> {
        return store.toMap()
    }

    override suspend fun clear() {
        store.clear()
    }
}
