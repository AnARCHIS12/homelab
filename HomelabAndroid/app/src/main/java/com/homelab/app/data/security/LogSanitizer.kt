package com.homelab.app.data.security

import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Sanitizes URLs, HTTP headers, and log messages to guarantee that no secrets,
 * tokens, passwords, session cookies, or API keys appear in application logs or exceptions.
 */
object LogSanitizer {

    private val SENSITIVE_PARAM_NAMES = setOf(
        "token", "api_key", "apikey", "key", "password", "secret", "auth",
        "ticket", "session", "code", "otp", "csrf", "pveauthtoken", "sid"
    )

    private val SENSITIVE_HEADERS = setOf(
        "authorization", "cookie", "set-cookie", "x-api-key", "x-api-token",
        "x-plex-token", "x-ftl-sid", "csrfpreventiontoken", "x-api-secret",
        "x-homelab-password", "x-homelab-username"
    )

    private val PATTERNS_TO_REDACT = listOf(
        Regex("(?i)(bearer\\s+)[a-zA-Z0-9_.-]+"),
        Regex("(?i)(basic\\s+)[a-zA-Z0-9+/=]+"),
        Regex("(?i)(pveauthcookie=)[^;\\s]+"),
        Regex("(?i)(token=)[^;\\s&]+"),
        Regex("(?i)(password=)[^;\\s&]+"),
        Regex("(?i)(api[_-]?key=)[^;\\s&]+"),
        Regex("(?i)(secret=)[^;\\s&]+")
    )

    /**
     * Sanitizes an OkHttp HttpUrl by redacting sensitive query parameter values.
     */
    fun sanitizeUrl(url: HttpUrl): String {
        if (url.querySize == 0) return url.toString()

        val builder = url.newBuilder()
        for (i in 0 until url.querySize) {
            val name = url.queryParameterName(i)
            if (isSensitiveParam(name)) {
                builder.setQueryParameter(name, "[REDACTED]")
            }
        }
        return builder.build().toString()
    }

    /**
     * Sanitizes an URL string.
     */
    fun sanitizeUrlString(url: String): String {
        return try {
            val parsed = url.toHttpUrlOrNull()
            if (parsed != null) sanitizeUrl(parsed) else redactString(url)
        } catch (_: Exception) {
            redactString(url)
        }
    }

    /**
     * Sanitizes headers by replacing values of sensitive headers with [REDACTED].
     */
    fun sanitizeHeaders(headers: Headers): Map<String, String> {
        val result = mutableMapOf<String, String>()
        for (name in headers.names()) {
            if (SENSITIVE_HEADERS.contains(name.lowercase())) {
                result[name] = "[REDACTED]"
            } else {
                result[name] = headers[name].orEmpty()
            }
        }
        return result
    }

    /**
     * Redacts known sensitive patterns inside arbitrary log strings.
     */
    fun redactString(message: String): String {
        var sanitized = message
        for (pattern in PATTERNS_TO_REDACT) {
            sanitized = pattern.replace(sanitized) { matchResult ->
                val prefix = matchResult.groups[1]?.value.orEmpty()
                "$prefix[REDACTED]"
            }
        }
        return sanitized
    }

    private fun isSensitiveParam(name: String): Boolean {
        val lowered = name.lowercase()
        return SENSITIVE_PARAM_NAMES.any { lowered.contains(it) }
    }
}
