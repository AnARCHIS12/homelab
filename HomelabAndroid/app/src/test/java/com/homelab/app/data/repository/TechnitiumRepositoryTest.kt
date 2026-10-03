package com.homelab.app.data.repository

import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TechnitiumRepositoryTest {

    @Test
    fun `login sends credentials in form body and never in URL`() {
        val request = buildTechnitiumLoginRequest(
            baseUrl = "https://dns.example.test",
            username = "admin",
            password = "secret-password",
            totp = "123456"
        )
        val body = Buffer().also { request.body?.writeTo(it) }.readUtf8()

        assertEquals("POST", request.method)
        assertEquals("https://dns.example.test/api/user/login", request.url.toString())
        assertFalse(request.url.queryParameterNames.contains("pass"))
        assertFalse(request.url.queryParameterNames.contains("totp"))
        assertTrue(body.contains("pass=secret-password"))
        assertTrue(body.contains("totp=123456"))
    }
}
