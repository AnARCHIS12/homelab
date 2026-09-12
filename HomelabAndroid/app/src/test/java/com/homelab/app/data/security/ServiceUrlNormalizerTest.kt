package com.homelab.app.data.security

import com.homelab.app.util.ServiceType
import org.junit.Assert.assertEquals
import org.junit.Test

class ServiceUrlNormalizerTest {

    @Test
    fun normalizeUrl_stripsPiholeAdminPaths() {
        assertEquals("http://pi.hole", ServiceUrlNormalizer.normalizeUrl("http://pi.hole/admin", ServiceType.PIHOLE, allowHttp = true))
        assertEquals("http://pi.hole", ServiceUrlNormalizer.normalizeUrl("http://pi.hole/admin/", ServiceType.PIHOLE, allowHttp = true))
        assertEquals("http://pi.hole", ServiceUrlNormalizer.normalizeUrl("http://pi.hole/admin/index.php", ServiceType.PIHOLE, allowHttp = true))
        assertEquals("https://home.lan/pihole", ServiceUrlNormalizer.normalizeUrl("https://home.lan/pihole/admin", ServiceType.PIHOLE))
    }

    @Test
    fun normalizeUrl_stripsPlexWebPaths() {
        assertEquals("http://192.168.1.10:32400", ServiceUrlNormalizer.normalizeUrl("http://192.168.1.10:32400/web", ServiceType.PLEX, allowHttp = true))
        assertEquals("http://192.168.1.10:32400", ServiceUrlNormalizer.normalizeUrl("http://192.168.1.10:32400/web/index.html", ServiceType.PLEX, allowHttp = true))
    }

    @Test
    fun normalizeUrl_stripsUptimeKumaPaths() {
        assertEquals("http://kuma:3001", ServiceUrlNormalizer.normalizeUrl("http://kuma:3001/dashboard", ServiceType.UPTIME_KUMA, allowHttp = true))
        assertEquals("http://kuma:3001", ServiceUrlNormalizer.normalizeUrl("http://kuma:3001/dashboard/1", ServiceType.UPTIME_KUMA, allowHttp = true))
        assertEquals("http://kuma:3001", ServiceUrlNormalizer.normalizeUrl("http://kuma:3001/metrics", ServiceType.UPTIME_KUMA, allowHttp = true))
        assertEquals("http://kuma:3001", ServiceUrlNormalizer.normalizeUrl("http://kuma:3001/status/service", ServiceType.UPTIME_KUMA, allowHttp = true))
    }

    @Test
    fun normalizeUrl_stripsPangolinApiPaths() {
        assertEquals("https://pangolin.lan", ServiceUrlNormalizer.normalizeUrl("https://pangolin.lan/api/v1", ServiceType.PANGOLIN))
        assertEquals("https://pangolin.lan", ServiceUrlNormalizer.normalizeUrl("https://pangolin.lan/api", ServiceType.PANGOLIN))
        assertEquals("https://pangolin.lan", ServiceUrlNormalizer.normalizeUrl("https://pangolin.lan/v1", ServiceType.PANGOLIN))
    }

    @Test
    fun normalizeUrl_stripsPatchmonApiPaths() {
        assertEquals("http://patchmon:3000", ServiceUrlNormalizer.normalizeUrl("http://patchmon:3000/api/v1/api", ServiceType.PATCHMON, allowHttp = true))
        assertEquals("http://patchmon:3000", ServiceUrlNormalizer.normalizeUrl("http://patchmon:3000/api/v1", ServiceType.PATCHMON, allowHttp = true))
    }

    @Test
    fun normalizeUrl_stripsAdGuardHomePaths() {
        assertEquals("http://adguard:3000", ServiceUrlNormalizer.normalizeUrl("http://adguard:3000/control", ServiceType.ADGUARD_HOME, allowHttp = true))
        assertEquals("http://adguard:3000", ServiceUrlNormalizer.normalizeUrl("http://adguard:3000/control/status", ServiceType.ADGUARD_HOME, allowHttp = true))
    }

    @Test
    fun normalizeUrl_stripsProxmoxPaths() {
        assertEquals("https://pve.lan:8006", ServiceUrlNormalizer.normalizeUrl("https://pve.lan:8006/api2/json", ServiceType.PROXMOX))
    }
}
