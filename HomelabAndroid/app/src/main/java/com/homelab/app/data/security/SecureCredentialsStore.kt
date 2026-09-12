package com.homelab.app.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import com.homelab.app.util.Logger
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class InstanceCredentials(
    val token: String = "",
    val password: String? = null,
    val apiKey: String? = null,
    val proxmoxCsrfToken: String? = null,
    val proxmoxOtp: String? = null,
    val piholePassword: String? = null
)

interface SecureCredentialsStore {
    suspend fun getCredentials(instanceId: String): InstanceCredentials?
    suspend fun saveCredentials(instanceId: String, credentials: InstanceCredentials)
    suspend fun deleteCredentials(instanceId: String)
    suspend fun getAllCredentials(): Map<String, InstanceCredentials>
    suspend fun clear()
}

@Singleton
class KeystoreSecureCredentialsStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json
) : SecureCredentialsStore {

    private val mutex = Mutex()
    private val storeFile: File
        get() = File(context.noBackupFilesDir, STORE_FILE_NAME)

    // Cache in memory while active
    private var inMemoryCache: MutableMap<String, InstanceCredentials>? = null

    override suspend fun getCredentials(instanceId: String): InstanceCredentials? = mutex.withLock {
        ensureLoaded()
        inMemoryCache?.get(instanceId)
    }

    override suspend fun saveCredentials(instanceId: String, credentials: InstanceCredentials) = mutex.withLock {
        ensureLoaded()
        val current = inMemoryCache ?: mutableMapOf()
        current[instanceId] = credentials
        inMemoryCache = current
        persistLocked(current)
    }

    override suspend fun deleteCredentials(instanceId: String) = mutex.withLock {
        ensureLoaded()
        val current = inMemoryCache ?: return@withLock
        if (current.remove(instanceId) != null) {
            persistLocked(current)
        }
    }

    override suspend fun getAllCredentials(): Map<String, InstanceCredentials> = mutex.withLock {
        ensureLoaded()
        inMemoryCache?.toMap() ?: emptyMap()
    }

    override suspend fun clear() = mutex.withLock {
        inMemoryCache = mutableMapOf()
        if (storeFile.exists()) {
            storeFile.delete()
        }
    }

    private fun ensureLoaded() {
        if (inMemoryCache != null) return

        if (!storeFile.exists()) {
            inMemoryCache = mutableMapOf()
            return
        }

        try {
            val bytes = storeFile.readBytes()
            if (bytes.size < HEADER_SIZE) {
                inMemoryCache = mutableMapOf()
                return
            }

            // Validate magic
            for (i in 0 until 4) {
                if (bytes[i] != MAGIC[i]) {
                    inMemoryCache = mutableMapOf()
                    return
                }
            }

            val version = bytes[4]
            if (version != FORMAT_VERSION) {
                inMemoryCache = mutableMapOf()
                return
            }

            val iv = ByteArray(IV_LENGTH)
            System.arraycopy(bytes, 5, iv, 0, IV_LENGTH)

            val ciphertext = ByteArray(bytes.size - HEADER_SIZE)
            System.arraycopy(bytes, HEADER_SIZE, ciphertext, 0, ciphertext.size)

            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(AES_GCM)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))

            val plaintext = cipher.doFinal(ciphertext)
            val jsonString = String(plaintext, Charsets.UTF_8)
            val decoded = json.decodeFromString<Map<String, InstanceCredentials>>(jsonString)
            inMemoryCache = decoded.toMutableMap()
        } catch (e: Exception) {
            Logger.e("SecureCredentialsStore", "Failed to load credentials from encrypted store: ${e.message}")
            inMemoryCache = mutableMapOf()
        }
    }

    private fun persistLocked(data: Map<String, InstanceCredentials>) {
        try {
            val jsonString = json.encodeToString(data)
            val plaintext = jsonString.toByteArray(Charsets.UTF_8)

            val secretKey = getOrCreateSecretKey()
            val iv = ByteArray(IV_LENGTH).also { SecureRandom().nextBytes(it) }

            val cipher = Cipher.getInstance(AES_GCM)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            val ciphertext = cipher.doFinal(plaintext)

            val output = ByteArray(HEADER_SIZE + ciphertext.size)
            System.arraycopy(MAGIC, 0, output, 0, 4)
            output[4] = FORMAT_VERSION
            System.arraycopy(iv, 0, output, 5, IV_LENGTH)
            System.arraycopy(ciphertext, 0, output, HEADER_SIZE, ciphertext.size)

            val parentDir = storeFile.parentFile ?: context.noBackupFilesDir
            if (!parentDir.exists()) {
                parentDir.mkdirs()
            }

            // Atomic file write
            val tempFile = File(parentDir, "$STORE_FILE_NAME.tmp")
            FileOutputStream(tempFile).use { fos ->
                fos.write(output)
                fos.flush()
            }
            if (tempFile.renameTo(storeFile).not()) {
                tempFile.copyTo(storeFile, overwrite = true)
                tempFile.delete()
            }
        } catch (e: Exception) {
            Logger.e("SecureCredentialsStore", "Failed to persist encrypted credentials: ${e.message}")
        }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
                val spec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGenerator.init(spec)
                keyGenerator.generateKey()
            }
            (keyStore.getKey(KEY_ALIAS, null) as? SecretKey) ?: fallbackJvmKey()
        } catch (_: Throwable) {
            // Fallback for JVM host unit tests where AndroidKeyStore provider is absent
            fallbackJvmKey()
        }
    }

    private fun fallbackJvmKey(): SecretKey {
        val baseDir = try { context.noBackupFilesDir } catch (_: Throwable) { null }
            ?: try { context.filesDir } catch (_: Throwable) { null }
            ?: File(".")
        val fallbackFile = File(baseDir, ".jvm_fallback_key")
        val keyBytes = if (fallbackFile.exists() && fallbackFile.length() == 32L) {
            fallbackFile.readBytes()
        } else {
            val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
            try {
                fallbackFile.writeBytes(bytes)
            } catch (_: Throwable) {}
            bytes
        }
        return SecretKeySpec(keyBytes, "AES")
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "homelab_credentials_master_key"
        private const val STORE_FILE_NAME = "credentials_store.enc"
        private const val AES_GCM = "AES/GCM/NoPadding"
        private val MAGIC = byteArrayOf(0x48, 0x4C, 0x43, 0x52) // "HLCR"
        private const val FORMAT_VERSION: Byte = 1
        private const val IV_LENGTH = 12
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val HEADER_SIZE = 4 + 1 + IV_LENGTH // 17 bytes
    }
}
