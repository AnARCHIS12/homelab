package com.homelab.app.data.security

import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogSanitizerTest {

    @Test
    fun `sensitive query parameters in URLs are redacted`() {
        val rawUrl = "https://service.local/api?token=secret123&username=admin&apiKey=api999&normal=value"
        val sanitized = LogSanitizer.sanitizeUrl(rawUrl.toHttpUrl())

        assertTrue(sanitized.contains("token=%5BREDACTED%5D") || sanitized.contains("token=[REDACTED]"))
        assertTrue(sanitized.contains("apiKey=%5BREDACTED%5D") || sanitized.contains("apiKey=[REDACTED]"))
        assertTrue(sanitized.contains("normal=value"))
        assertFalse(sanitized.contains("secret123"))
        assertFalse(sanitized.contains("api999"))
    }

    @Test
    fun `sensitive HTTP headers are redacted`() {
        val headers = Headers.Builder()
            .add("Authorization", "Bearer top-secret-token")
            .add("Cookie", "session_id=123456789")
            .add("X-Api-Key", "my-secret-api-key")
            .add("Accept", "application/json")
            .build()

        val sanitized = LogSanitizer.sanitizeHeaders(headers)

        assertEquals("[REDACTED]", sanitized["Authorization"])
        assertEquals("[REDACTED]", sanitized["Cookie"])
        assertEquals("[REDACTED]", sanitized["X-Api-Key"])
        assertEquals("application/json", sanitized["Accept"])
    }

    @Test
    fun `bearer tokens and basic auth strings are redacted from text`() {
        val message = "Request failed: Bearer secret-jwt-token-abcdef123 with Basic dXNlcjpwYXNz"
        val redacted = LogSanitizer.redactString(message)

        assertFalse(redacted.contains("secret-jwt-token-abcdef123"))
        assertFalse(redacted.contains("dXNlcjpwYXNz"))
        assertTrue(redacted.contains("Bearer [REDACTED]"))
        assertTrue(redacted.contains("Basic [REDACTED]"))
    }

    @Test
    fun `URL string sanitization handles both URLs and generic strings`() {
        val url = "https://pihole.local/admin/api.php?auth=supersecret"
        val sanitized = LogSanitizer.sanitizeUrlString(url)

        assertFalse(sanitized.contains("supersecret"))
        assertTrue(sanitized.contains("REDACTED"))
    }
}
