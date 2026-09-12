package com.homelab.app.data.security

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest
import java.security.cert.CertificateException
import java.security.cert.X509Certificate

class HomelabCertificateTrustTest {

    @Test
    fun `computeSha256Fingerprint computes uppercase hex string`() {
        val cert = mockk<X509Certificate>()
        val dummyBytes = byteArrayOf(0x01, 0x02, 0x0A, 0x0F, 0x10, 0xFF.toByte())
        every { cert.encoded } returns dummyBytes

        val md = MessageDigest.getInstance("SHA-256")
        val expected = md.digest(dummyBytes).joinToString("") { "%02X".format(it) }

        val actual = HomelabCertificateTrust.computeSha256Fingerprint(cert)
        assertEquals(expected, actual)
    }

    @Test
    fun `checkServerTrusted succeeds when certificate matches pinned fingerprint`() {
        val cert = mockk<X509Certificate>()
        val dummyBytes = "test-certificate-bytes".toByteArray()
        every { cert.encoded } returns dummyBytes

        val fp = HomelabCertificateTrust.computeSha256Fingerprint(cert)
        val trustManager = HomelabCertificateTrust.createTrustManager(
            pinnedFingerprints = setOf(fp)
        )

        // Should not throw CertificateException
        trustManager.checkServerTrusted(arrayOf(cert), "RSA")
    }

    @Test
    fun `checkServerTrusted throws when certificate does not match pinned fingerprint`() {
        val cert = mockk<X509Certificate>()
        val dummyBytes = "test-certificate-bytes".toByteArray()
        every { cert.encoded } returns dummyBytes

        val trustManager = HomelabCertificateTrust.createTrustManager(
            pinnedFingerprints = setOf("00112233445566778899AABBCCDDEEFF00112233445566778899AABBCCDDEEFF")
        )

        val exception = assertThrows(CertificateException::class.java) {
            trustManager.checkServerTrusted(arrayOf(cert), "RSA")
        }
        assertTrue(exception.message?.contains("Certificate pinning failure") == true)
    }

    @Test
    fun `checkServerTrusted throws when certificate chain is empty`() {
        val trustManager = HomelabCertificateTrust.createTrustManager()

        assertThrows(CertificateException::class.java) {
            trustManager.checkServerTrusted(emptyArray(), "RSA")
        }
    }
}
