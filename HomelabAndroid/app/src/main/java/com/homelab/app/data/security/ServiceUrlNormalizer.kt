package com.homelab.app.data.security

import com.homelab.app.util.ServiceType
import java.net.URI

/**
 * Normalizes service URLs by stripping commonly copy-pasted web UI and API subpaths.
 * Preserves custom reverse proxy base paths while stripping trailing UI/API segments.
 */
object ServiceUrlNormalizer {

    fun normalizeUrl(raw: String, type: ServiceType? = null, allowHttp: Boolean = false): String {
        val clean = try {
            UrlSecurityValidator.validateAndNormalizeUrl(raw, allowHttp)
        } catch (_: Exception) {
            fallbackNormalize(raw)
        }
        return stripKnownServicePath(clean, type)
    }

    fun normalizeOptionalUrl(raw: String?, type: ServiceType? = null, allowHttp: Boolean = false): String? {
        if (raw.isNullOrBlank()) return null
        val normalized = normalizeUrl(raw, type, allowHttp)
        return normalized.ifBlank { null }
    }

    private fun fallbackNormalize(raw: String): String {
        var clean = raw.trim()
        clean = clean.trimEnd { it == ')' || it == ']' || it == '}' || it == ',' || it == ';' }
        if (!clean.startsWith("http://", ignoreCase = true) && !clean.startsWith("https://", ignoreCase = true)) {
            clean = "https://$clean"
        }
        return clean.replace(Regex("/+$"), "")
    }

    fun stripKnownServicePath(raw: String, type: ServiceType?): String {
        if (type == null) return raw
        return runCatching {
            val uri = URI(raw)
            val rawPath = uri.rawPath.orEmpty().trimEnd('/')
            if (rawPath.isEmpty() || rawPath == "/") return@runCatching raw

            val newPath = stripPathForType(rawPath, type)
            if (newPath == rawPath) return@runCatching raw

            val finalPath = if (newPath.isBlank() || newPath == "/") null else newPath
            URI(uri.scheme, uri.userInfo, uri.host, uri.port, finalPath, null, null).toString()
        }.getOrDefault(raw)
    }

    private fun stripPathForType(path: String, type: ServiceType): String {
        val suffixesToStrip = when (type) {
            ServiceType.PIHOLE -> listOf(
                "/admin/index.php",
                "/admin/api.php",
                "/admin",
                "/api/auth",
                "/api"
            )
            ServiceType.PLEX -> listOf(
                "/web/index.html",
                "/web"
            )
            ServiceType.UPTIME_KUMA -> listOf(
                "/metrics",
                "/dashboard",
                "/status"
            )
            ServiceType.PANGOLIN -> listOf(
                "/api/v1",
                "/api",
                "/v1"
            )
            ServiceType.UNIFI_NETWORK -> listOf(
                "/proxy/network/integration/v1",
                "/v1"
            )
            ServiceType.PATCHMON -> listOf(
                "/api/v1/api",
                "/api/v1",
                "/api"
            )
            ServiceType.JELLYSTAT -> listOf(
                "/stats/getViewsByLibraryType",
                "/stats",
                "/api"
            )
            ServiceType.PORTAINER -> listOf(
                "/api/system/version",
                "/api/status",
                "/api"
            )
            ServiceType.ADGUARD_HOME -> listOf(
                "/control/status",
                "/control"
            )
            ServiceType.TECHNITIUM -> listOf(
                "/admin",
                "/api"
            )
            ServiceType.CRAFTY_CONTROLLER -> listOf(
                "/api/v2",
                "/api"
            )
            ServiceType.PROXMOX -> listOf(
                "/api2/json",
                "/api2"
            )
            ServiceType.PTERODACTYL,
            ServiceType.CALAGOPUS -> listOf(
                "/api/client",
                "/api"
            )
            ServiceType.RADARR,
            ServiceType.SONARR -> listOf(
                "/api/v3",
                "/api"
            )
            ServiceType.LIDARR,
            ServiceType.PROWLARR,
            ServiceType.JELLYSEERR -> listOf(
                "/api/v1",
                "/api"
            )
            ServiceType.QBITTORRENT -> listOf(
                "/api/v2",
                "/api"
            )
            ServiceType.GLUETUN,
            ServiceType.FLARESOLVERR -> listOf(
                "/v1"
            )
            ServiceType.BESZEL,
            ServiceType.GITEA,
            ServiceType.HEALTHCHECKS,
            ServiceType.LINUX_UPDATE,
            ServiceType.DOCKHAND,
            ServiceType.DOCKMON,
            ServiceType.NGINX_PROXY_MANAGER -> listOf(
                "/api"
            )
            else -> emptyList()
        }

        // Check if path ends with any of the suffixes
        for (suffix in suffixesToStrip) {
            if (path.equals(suffix, ignoreCase = true)) {
                return ""
            }
            if (path.endsWith(suffix, ignoreCase = true)) {
                return path.substring(0, path.length - suffix.length).trimEnd('/')
            }
        }

        // Special case for Plex paths with sub-routes under /web (e.g. /web/index.html#!/...)
        if (type == ServiceType.PLEX && path.contains("/web")) {
            val idx = path.indexOf("/web")
            return path.substring(0, idx).trimEnd('/')
        }

        // Special case for Uptime Kuma dashboard sub-routes (e.g. /dashboard/1 or /status/demo)
        if (type == ServiceType.UPTIME_KUMA) {
            val match = Regex("(/dashboard.*|/status.*)").find(path)
            if (match != null) {
                return path.substring(0, match.range.first).trimEnd('/')
            }
        }

        return path
    }
}
