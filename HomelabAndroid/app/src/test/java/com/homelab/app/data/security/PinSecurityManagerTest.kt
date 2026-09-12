package com.homelab.app.data.security

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class PinSecurityManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var dataStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>
    private lateinit var pinSecurityManager: PinSecurityManager

    @Before
    fun setUp() {
        dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tempFolder.newFile("test_pin.preferences_pb") }
        )
        pinSecurityManager = PinSecurityManager(dataStore)
    }

    @Test
    fun `isPinSet is false initially`() = testScope.runTest {
        assertFalse(pinSecurityManager.isPinSet.first())
    }

    @Test
    fun `savePin sets isPinSet to true and secures PIN`() = testScope.runTest {
        pinSecurityManager.savePin("1234")
        assertTrue(pinSecurityManager.isPinSet.first())

        // Verify that raw PIN is NOT stored in DataStore
        val prefs = dataStore.data.first()
        val legacyRawPin = prefs[stringPreferencesKey("app_pin")]
        assertNull(legacyRawPin)

        val hash = prefs[stringPreferencesKey("pin_kdf_hash")]
        assertTrue(!hash.isNullOrBlank())
    }

    @Test
    fun `verifyPin returns Success for correct PIN`() = testScope.runTest {
        pinSecurityManager.savePin("4321")
        val result = pinSecurityManager.verifyPin("4321")

        assertEquals(PinVerificationResult.Success, result)
        assertEquals(0L, pinSecurityManager.lockoutRemainingSeconds.first())
    }

    @Test
    fun `verifyPin returns Failed for incorrect PIN`() = testScope.runTest {
        pinSecurityManager.savePin("4321")
        val result = pinSecurityManager.verifyPin("0000")

        assertTrue(result is PinVerificationResult.Failed)
        assertEquals(1, (result as PinVerificationResult.Failed).attemptsCount)
    }

    @Test
    fun `progressive lockout triggers after repeated failed attempts`() = testScope.runTest {
        pinSecurityManager.savePin("9999")

        // First 2 failed attempts do not trigger lockout
        repeat(2) {
            val res = pinSecurityManager.verifyPin("1111")
            assertTrue(res is PinVerificationResult.Failed)
        }

        // 3rd failed attempt triggers progressive lockout (5 seconds)
        val lockedResult = pinSecurityManager.verifyPin("1111")
        assertTrue(lockedResult is PinVerificationResult.LockedOut)
        assertEquals(5L, (lockedResult as PinVerificationResult.LockedOut).remainingSeconds)
    }

    @Test
    fun `removePin clears PIN and sets isPinSet to false`() = testScope.runTest {
        pinSecurityManager.savePin("1234")
        assertTrue(pinSecurityManager.isPinSet.first())

        pinSecurityManager.clearPin()
        assertFalse(pinSecurityManager.isPinSet.first())
    }

    @Test
    fun `legacy plaintext PIN is automatically migrated on verification`() = testScope.runTest {
        // Pre-populate legacy raw PIN in DataStore
        val legacyKey = stringPreferencesKey("app_pin")
        dataStore.edit { prefs ->
            prefs[legacyKey] = "8888"
        }

        assertTrue(pinSecurityManager.isPinSet.first())

        // Verification should succeed and migrate the legacy PIN
        val result = pinSecurityManager.verifyPin("8888")
        assertEquals(PinVerificationResult.Success, result)

        // Ensure legacy raw PIN has been deleted and KDF hash created
        val prefs = dataStore.data.first()
        assertNull(prefs[legacyKey])
        assertTrue(!prefs[stringPreferencesKey("pin_kdf_hash")].isNullOrBlank())
    }
}
