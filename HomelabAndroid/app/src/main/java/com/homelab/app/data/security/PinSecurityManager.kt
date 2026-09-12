package com.homelab.app.data.security

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

sealed class PinVerificationResult {
    data object Success : PinVerificationResult()
    data class Failed(val attemptsCount: Int, val lockoutSeconds: Long = 0) : PinVerificationResult()
    data class LockedOut(val remainingSeconds: Long) : PinVerificationResult()
}

@Singleton
class PinSecurityManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object Keys {
        val LEGACY_PIN = stringPreferencesKey("app_pin")
        val PIN_HASH = stringPreferencesKey("pin_kdf_hash")
        val PIN_SALT = stringPreferencesKey("pin_kdf_salt")
        val FAILED_ATTEMPTS = intPreferencesKey("pin_failed_attempts")
        val LOCKOUT_UNTIL = longPreferencesKey("pin_lockout_until")
    }

    val isPinSet: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.PIN_HASH] != null || prefs[Keys.LEGACY_PIN] != null
    }

    val lockoutRemainingSeconds: Flow<Long> = dataStore.data.map { prefs ->
        val lockoutUntil = prefs[Keys.LOCKOUT_UNTIL] ?: 0L
        val now = System.currentTimeMillis()
        if (lockoutUntil > now) (lockoutUntil - now + 999) / 1000 else 0L
    }

    suspend fun savePin(pin: String) {
        val salt = ByteArray(SALT_LENGTH_BYTES).also { SecureRandom().nextBytes(it) }
        val hash = deriveHash(pin, salt)

        val base64Encoder = Base64.getEncoder()
        val saltBase64 = base64Encoder.encodeToString(salt)
        val hashBase64 = base64Encoder.encodeToString(hash)

        dataStore.edit { prefs ->
            prefs[Keys.PIN_HASH] = hashBase64
            prefs[Keys.PIN_SALT] = saltBase64
            prefs.remove(Keys.LEGACY_PIN) // Purge legacy plaintext PIN immediately
            prefs[Keys.FAILED_ATTEMPTS] = 0
            prefs[Keys.LOCKOUT_UNTIL] = 0L
        }
    }

    suspend fun verifyPin(candidatePin: String): PinVerificationResult {
        val prefs = dataStore.data.first()
        val now = System.currentTimeMillis()

        // 1. Check if currently locked out
        val lockoutUntil = prefs[Keys.LOCKOUT_UNTIL] ?: 0L
        if (lockoutUntil > now) {
            val remainingSeconds = (lockoutUntil - now + 999) / 1000
            return PinVerificationResult.LockedOut(remainingSeconds)
        }

        // 2. Check for legacy migration if needed
        var hashBase64 = prefs[Keys.PIN_HASH]
        var saltBase64 = prefs[Keys.PIN_SALT]
        val legacyPin = prefs[Keys.LEGACY_PIN]

        if (hashBase64 == null && legacyPin != null) {
            savePin(legacyPin)
            val updated = dataStore.data.first()
            hashBase64 = updated[Keys.PIN_HASH]
            saltBase64 = updated[Keys.PIN_SALT]
        }

        if (hashBase64 == null || saltBase64 == null) {
            return PinVerificationResult.Failed(0)
        }

        val base64Decoder = Base64.getDecoder()
        val storedSalt = try {
            base64Decoder.decode(saltBase64)
        } catch (_: Exception) {
            return PinVerificationResult.Failed(0)
        }
        val storedHash = try {
            base64Decoder.decode(hashBase64)
        } catch (_: Exception) {
            return PinVerificationResult.Failed(0)
        }

        val candidateHash = deriveHash(candidatePin, storedSalt)

        // Constant-time comparison to prevent timing attacks
        val isCorrect = MessageDigest.isEqual(storedHash, candidateHash)

        return if (isCorrect) {
            dataStore.edit { editPrefs ->
                editPrefs[Keys.FAILED_ATTEMPTS] = 0
                editPrefs[Keys.LOCKOUT_UNTIL] = 0L
            }
            PinVerificationResult.Success
        } else {
            val failedCount = (prefs[Keys.FAILED_ATTEMPTS] ?: 0) + 1
            val lockoutDurationMs = calculateLockoutDurationMs(failedCount)
            val newLockoutUntil = if (lockoutDurationMs > 0) now + lockoutDurationMs else 0L

            dataStore.edit { editPrefs ->
                editPrefs[Keys.FAILED_ATTEMPTS] = failedCount
                editPrefs[Keys.LOCKOUT_UNTIL] = newLockoutUntil
            }

            val lockoutSeconds = lockoutDurationMs / 1000
            if (lockoutSeconds > 0) {
                PinVerificationResult.LockedOut(lockoutSeconds)
            } else {
                PinVerificationResult.Failed(failedCount)
            }
        }
    }

    suspend fun clearPin() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.PIN_HASH)
            prefs.remove(Keys.PIN_SALT)
            prefs.remove(Keys.LEGACY_PIN)
            prefs.remove(Keys.FAILED_ATTEMPTS)
            prefs.remove(Keys.LOCKOUT_UNTIL)
        }
    }

    private fun calculateLockoutDurationMs(failedAttempts: Int): Long {
        return when {
            failedAttempts >= 10 -> 300_000L // 5 minutes
            failedAttempts >= 5 -> 30_000L   // 30 seconds
            failedAttempts >= 3 -> 5_000L    // 5 seconds
            else -> 0L
        }
    }

    private fun deriveHash(pin: String, salt: ByteArray): ByteArray {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        return factory.generateSecret(spec).encoded
    }

    companion object {
        private const val PBKDF2_ITERATIONS = 100_000
        private const val KEY_LENGTH_BITS = 256
        private const val SALT_LENGTH_BYTES = 16
    }
}
