package com.homelab.app.data.security

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SecureCredentialsStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var store: KeystoreSecureCredentialsStore
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Before
    fun setUp() {
        val mockContext = mockk<Context>(relaxed = true)
        every { mockContext.noBackupFilesDir } returns tempFolder.root
        store = KeystoreSecureCredentialsStore(mockContext, json)
    }

    @Test
    fun `save and retrieve credentials round-trip`() = runTest {
        val instanceId = "srv-123"
        val creds = InstanceCredentials(
            token = "super-secret-token",
            password = "admin-password",
            apiKey = "api-key-999"
        )

        store.saveCredentials(instanceId, creds)
        val retrieved = store.getCredentials(instanceId)

        assertNotNull(retrieved)
        assertEquals("super-secret-token", retrieved?.token)
        assertEquals("admin-password", retrieved?.password)
        assertEquals("api-key-999", retrieved?.apiKey)
    }

    @Test
    fun `retrieve non-existent instance credentials returns null`() = runTest {
        val retrieved = store.getCredentials("nonexistent")
        assertNull(retrieved)
    }

    @Test
    fun `deleteCredentials deletes specific instance`() = runTest {
        val credsA = InstanceCredentials(token = "tokA")
        val credsB = InstanceCredentials(token = "tokB")

        store.saveCredentials("inst-A", credsA)
        store.saveCredentials("inst-B", credsB)

        store.deleteCredentials("inst-A")

        assertNull(store.getCredentials("inst-A"))
        assertEquals("tokB", store.getCredentials("inst-B")?.token)
    }

    @Test
    fun `getAllCredentials returns all saved instances`() = runTest {
        store.saveCredentials("inst-1", InstanceCredentials(token = "tok1"))
        store.saveCredentials("inst-2", InstanceCredentials(token = "tok2"))

        val all = store.getAllCredentials()
        assertEquals(2, all.size)
        assertEquals("tok1", all["inst-1"]?.token)
        assertEquals("tok2", all["inst-2"]?.token)
    }

    @Test
    fun `clear wipes all stored credentials`() = runTest {
        store.saveCredentials("inst-1", InstanceCredentials(token = "tok1"))
        store.clear()

        val all = store.getAllCredentials()
        assertEquals(0, all.size)
        assertNull(store.getCredentials("inst-1"))
    }
}
