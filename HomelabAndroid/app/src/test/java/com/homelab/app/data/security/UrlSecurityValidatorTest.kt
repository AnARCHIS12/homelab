package com.homelab.app.data.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlSecurityValidatorTest {

    @Test
    fun `valid HTTPS URLs are accepted and stripped of trailing slashes`() {
        val result = UrlSecurityValidator.validateAndNormalizeUrl("https://homelab.local:8443/")
        assertEquals("https://homelab.local:8443", result)
    }

    @Test
    fun `scheme defaults to HTTPS if omitted`() {
        val result = UrlSecurityValidator.validateAndNormalizeUrl("myhomelab.local:8080")
        assertEquals("https://myhomelab.local:8080", result)
    }

    @Test
    fun `HTTP URL is rejected when allowHttp is false`() {
        val exception = assertThrows(UrlSecurityValidator.InsecureHttpBlockedException::class.java) {
            UrlSecurityValidator.validateAndNormalizeUrl("http://insecure-service.local", allowHttp = false)
        }
        assertTrue(exception.message?.contains("Unencrypted HTTP traffic is blocked") == true)
    }

    @Test
    fun `HTTP URL is accepted when allowHttp is true`() {
        val result = UrlSecurityValidator.validateAndNormalizeUrl("http://insecure-service.local:8080", allowHttp = true)
        assertEquals("http://insecure-service.local:8080", result)
    }

    @Test
    fun `dangerous schemes are rejected`() {
        val dangerous = listOf(
            "javascript:alert(1)",
            "file:///etc/passwd",
            "content://com.android.providers/telephony",
            "data:text/html;base64,PHNjcmlwdD4=",
            "ftp://ftp.example.com/file"
        )
        for (url in dangerous) {
            assertThrows(UrlSecurityValidator.InvalidUrlException::class.java) {
                UrlSecurityValidator.validateAndNormalizeUrl(url)
            }
        }
    }

    @Test
    fun `trailing punctuation characters are cleaned`() {
        val cleaned = UrlSecurityValidator.validateAndNormalizeUrl("https://my-server.local);")
        assertEquals("https://my-server.local", cleaned)
    }

    @Test
    fun `blank URL throws InvalidUrlException`() {
        assertThrows(UrlSecurityValidator.InvalidUrlException::class.java) {
            UrlSecurityValidator.validateAndNormalizeUrl("   ")
        }
    }
}
