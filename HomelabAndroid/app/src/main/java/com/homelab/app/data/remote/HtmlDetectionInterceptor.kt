package com.homelab.app.data.remote

import com.homelab.app.util.Logger
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HtmlDetectionInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (request.header(SKIP_HTML_DETECTION_HEADER) == "true") {
            return response
        }

        // 401 Unauthorized and 403 Forbidden should be handled by standard authentication error flows
        // (even if the web server/reverse proxy returns an HTML body or text/html header).
        if (response.code == 401 || response.code == 403) {
            return response
        }

        val contentType = response.header("Content-Type")?.lowercase()
        val isHtmlContentType = contentType?.contains("text/html") == true ||
            contentType?.contains("application/xhtml+xml") == true

        val snippet = runCatching { response.peekBody(2048).string() }.getOrNull()
        val looksLikeHtml = snippet
            ?.trimStart()
            ?.startsWith("<!doctype html", ignoreCase = true) == true ||
            snippet
                ?.trimStart()
                ?.startsWith("<html", ignoreCase = true) == true

        val isJsonContentType = contentType?.contains("application/json") == true

        if (isHtmlContentType || (looksLikeHtml && !isJsonContentType)) {
            val tag = extractServiceTag(request.url.host)
            val sanitizedUrl = com.homelab.app.data.security.LogSanitizer.sanitizeUrl(request.url)
            Logger.w(tag, "HTML response detected for ${request.method} $sanitizedUrl")
            response.close()
            throw HtmlResponseException(
                url = sanitizedUrl,
                statusCode = response.code,
                contentType = contentType,
                snippet = snippet
            )
        }

        return response
    }

    private fun extractServiceTag(host: String): String {
        val parts = host.split(".")
        if (parts.size <= 1 || parts[0].all { it.isDigit() || it == ':' }) return "Network"
        return parts[0].replaceFirstChar { it.uppercase() }
    }

    companion object {
        const val SKIP_HTML_DETECTION_HEADER = "X-Homelab-Skip-Html-Detection"
    }
}
