package com.homelab.app.data.security

import com.homelab.app.domain.model.ServiceInstance
import okhttp3.OkHttpClient
import java.io.ByteArrayInputStream
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * Provides explicit, secure TLS certificate validation.
 *
 * Replaces permissive "trust-all" trust managers and wildcards with:
 * 1. Default system CA validation.
 * 2. Custom CA / server certificate trust via in-memory KeyStore.
 * 3. SHA-256 certificate fingerprint pinning per instance.
 * 4. Strict hostname and IP SAN verification.
 */
object HomelabCertificateTrust {

    /**
     * Gets the default platform X509TrustManager backed by system CAs.
     */
    fun getSystemTrustManager(): X509TrustManager {
        val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        factory.init(null as KeyStore?)
        return factory.trustManagers.filterIsInstance<X509TrustManager>().first()
    }

    /**
     * Computes the SHA-256 fingerprint of an X509Certificate as an uppercase hex string (without colons).
     */
    fun computeSha256Fingerprint(cert: X509Certificate): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(cert.encoded)
        return hash.joinToString("") { "%02X".format(it) }
    }

    /**
     * Parses a list of PEM-encoded X.509 certificates.
     */
    fun parsePemCertificates(pemContent: String): List<X509Certificate> {
        if (pemContent.isBlank()) return emptyList()
        val certFactory = CertificateFactory.getInstance("X.509")
        val certs = mutableListOf<X509Certificate>()
        val certStream = ByteArrayInputStream(pemContent.toByteArray(Charsets.UTF_8))
        while (certStream.available() > 0) {
            try {
                val cert = certFactory.generateCertificate(certStream) as? X509Certificate
                if (cert != null) {
                    certs.add(cert)
                }
            } catch (_: Exception) {
                break
            }
        }
        return certs
    }

    /**
     * Builds a custom TrustManager for an instance.
     *
     * If [trustedCertificatesPem] or [pinnedFingerprints] are supplied, they are explicitly verified.
     * Under NO circumstance will any certificate be trusted without verification.
     */
    fun createTrustManager(
        trustedCertificatesPem: String? = null,
        pinnedFingerprints: Set<String> = emptySet()
    ): X509TrustManager {
        val systemTrustManager = getSystemTrustManager()

        val customCerts = trustedCertificatesPem?.let { parsePemCertificates(it) }.orEmpty()
        val customTrustManager = if (customCerts.isNotEmpty()) {
            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
                load(null, null)
                customCerts.forEachIndexed { index, cert ->
                    setCertificateEntry("custom_cert_$index", cert)
                }
            }
            val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            factory.init(keyStore)
            factory.trustManagers.filterIsInstance<X509TrustManager>().firstOrNull()
        } else {
            null
        }

        val normalizedPins = pinnedFingerprints.map { it.replace(":", "").uppercase().trim() }.filter { it.isNotBlank() }.toSet()

        return object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                systemTrustManager.checkClientTrusted(chain, authType)
            }

            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                if (chain.isNullOrEmpty()) {
                    throw CertificateException("Certificate chain is empty")
                }

                // If fingerprint pinning is configured, verify that at least one certificate
                // in the presented chain matches a pinned fingerprint.
                if (normalizedPins.isNotEmpty()) {
                    val matched = chain.any { cert ->
                        val fp = computeSha256Fingerprint(cert)
                        normalizedPins.contains(fp)
                    }
                    if (!matched) {
                        val presentedFingerprints = chain.map { computeSha256Fingerprint(it) }
                        throw CertificateException(
                            "Certificate pinning failure. None of the presented certificates " +
                                "matched the configured pinned fingerprints. Presented: $presentedFingerprints"
                        )
                    }
                    return
                }

                // Try validating against custom trust store first if configured
                if (customTrustManager != null) {
                    try {
                        customTrustManager.checkServerTrusted(chain, authType)
                        return
                    } catch (e: Exception) {
                        // Fall back to system CA check
                    }
                }

                // Default validation against system CAs
                systemTrustManager.checkServerTrusted(chain, authType)
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> {
                val systemIssuers = systemTrustManager.acceptedIssuers
                val customIssuers = customTrustManager?.acceptedIssuers.orEmpty()
                return systemIssuers + customIssuers
            }
        }
    }

    /**
     * Creates an SSLSocketFactory initialized with the given TrustManager.
     */
    fun createSslSocketFactory(trustManager: X509TrustManager): SSLSocketFactory {
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(trustManager), SecureRandom())
        return sslContext.socketFactory
    }

    /**
     * Configures an OkHttpClient.Builder with explicit certificate trust for a given instance.
     */
    fun configureTlsForInstance(
        builder: OkHttpClient.Builder,
        instance: ServiceInstance?
    ): OkHttpClient.Builder {
        val customPem = instance?.customCertificatePem
        val pins = buildSet {
            instance?.customCertFingerprint?.let { add(it) }
        }

        if (!customPem.isNullOrBlank() || pins.isNotEmpty()) {
            val trustManager = createTrustManager(customPem, pins)
            val sslSocketFactory = createSslSocketFactory(trustManager)
            builder.sslSocketFactory(sslSocketFactory, trustManager)
        }
        // Note: Default OkHttp HostnameVerifier is preserved and NEVER disabled.
        return builder
    }
}
