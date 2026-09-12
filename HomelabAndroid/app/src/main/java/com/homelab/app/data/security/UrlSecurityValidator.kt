package com.homelab.app.data.security

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.net.URI

/**
 * Validates and sanitizes URLs used to communicate with homelab services.
 *
 * Enforces:
 * - Rejection of dangerous schemes (javascript:, file:, content:, data:, ftp:, etc.).
 * - Enforcement of HTTPS by default.
 * - HTTP cleartext is ONLY permitted when explicitly enabled per instance.
 * - Sanitization of hostnames, IP addresses, ports, and trailing characters.
 * - Protection against path traversal and malicious URL structures.
 */
object UrlSecurityValidator {

    private val DISALLOWED_SCHEMES = setOf(
        "javascript", "file", "content", "data", "ftp", "intent", "blob", "about", "market"
    )

    class InvalidUrlException(message: String) : IllegalArgumentException(message)
    class InsecureHttpBlockedException(message: String) : SecurityException(message)

    fun validateUrl(raw: String, allowHttp: Boolean = false): String = validateAndNormalizeUrl(raw, allowHttp)

    /**
     * Validates and normalizes an input URL string.
     *
     * @param raw The raw URL string provided by the user or stored.
     * @param allowHttp Whether HTTP cleartext is explicitly allowed for this instance.
     * @return The sanitized, valid URL string without trailing slashes.
     * @throws InvalidUrlException if the URL is invalid or uses a forbidden scheme.
     * @throws InsecureHttpBlockedException if HTTP is used but [allowHttp] is false.
     */
    fun validateAndNormalizeUrl(raw: String, allowHttp: Boolean = false): String {
        var clean = raw.trim()
        clean = clean.trimEnd { it == ')' || it == ']' || it == '}' || it == ',' || it == ';' }

        if (clean.isBlank()) {
            throw InvalidUrlException("URL cannot be blank")
        }

        // Check for disallowed dangerous schemes early
        val colonIndex = clean.indexOf(':')
        if (colonIndex > 0) {
            val schemeCandidate = clean.substring(0, colonIndex).lowercase()
            if (schemeCandidate in DISALLOWED_SCHEMES) {
                throw InvalidUrlException("Dangerous scheme '$schemeCandidate' is not permitted")
            }
        }

        // Default to https:// if no scheme is specified
        if (!clean.startsWith("http://", ignoreCase = true) && !clean.startsWith("https://", ignoreCase = true)) {
            clean = "https://$clean"
        }

        val httpUrl = clean.toHttpUrlOrNull()
            ?: throw InvalidUrlException("Malformed URL: $clean")

        val scheme = httpUrl.scheme.lowercase()
        if (scheme != "http" && scheme != "https") {
            throw InvalidUrlException("Unsupported URL scheme: $scheme. Only HTTPS (or HTTP if enabled) is allowed.")
        }

        if (scheme == "http" && !allowHttp) {
            throw InsecureHttpBlockedException(
                "Unencrypted HTTP traffic is blocked by default. You must explicitly allow HTTP for this instance if required."
            )
        }

        val host = httpUrl.host.trim()
        if (host.isBlank()) {
            throw InvalidUrlException("URL must contain a valid hostname or IP address")
        }

        // Ensure URI parse validation succeeds
        val uri = try {
            URI(httpUrl.toString())
        } catch (e: Exception) {
            throw InvalidUrlException("Malformed URI structure: ${e.message}")
        }

        // Sanitize path (prevent path traversal like /../)
        val path = uri.path?.replace(Regex("/{2,}"), "/") ?: "/"
        if (path.contains("/..") || path.contains("../")) {
            throw InvalidUrlException("Path traversal sequences are not permitted in URLs")
        }

        // Reconstruct clean URL without trailing slashes (unless it's just root /)
        val builder = httpUrl.newBuilder()
        val normalized = builder.build().toString().replace(Regex("/+$"), "")
        return normalized
    }

    /**
     * Checks whether an URL can be safely parsed and complies with security rules.
     */
    fun isValidUrl(raw: String, allowHttp: Boolean = false): Boolean {
        return try {
            validateAndNormalizeUrl(raw, allowHttp)
            true
        } catch (_: Exception) {
            false
        }
    }
}
