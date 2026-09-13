package com.homelab.app.data.remote.dto.pangolin

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Test

class PangolinModelsTest {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    @Test
    fun `decodes resources with integer booleans`() {
        val payload = """
            {
              "data": {
                "resources": [
                  {
                    "resourceId": 42,
                    "name": "Dashboard",
                    "ssl": 1,
                    "fullDomain": "dash.example.com",
                    "passwordId": null,
                    "sso": 0,
                    "pincodeId": null,
                    "whitelist": 0,
                    "http": 1,
                    "protocol": "http",
                    "proxyPort": 8080,
                    "enabled": 1,
                    "domainId": "dom-1",
                    "niceId": "res-nice"
                  }
                ]
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<PangolinResourcesResponse>(payload)
        val resource = response.data.resources.first()

        assertEquals(42, resource.resourceId)
        assertEquals("Dashboard", resource.name)
        assertTrue(resource.ssl)
        assertFalse(resource.sso)
        assertFalse(resource.whitelist)
        assertTrue(resource.http)
        assertTrue(resource.enabled)
        assertEquals("res-nice", resource.niceId)
    }

    @Test
    fun `decodes site resources with missing siteId siteName siteNiceId`() {
        val payload = """
            {
              "data": {
                "siteResources": [
                  {
                    "siteResourceId": 101,
                    "niceId": "sr-nice",
                    "name": "Local Service",
                    "mode": "host",
                    "protocol": "tcp",
                    "destination": "192.168.1.50",
                    "enabled": 1
                  }
                ]
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<PangolinSiteResourcesResponse>(payload)
        val siteResource = response.data.siteResources.first()

        assertEquals(101, siteResource.siteResourceId)
        assertEquals("Local Service", siteResource.name)
        assertNull(siteResource.siteId)
        assertNull(siteResource.siteName)
        assertNull(siteResource.siteNiceId)
        assertTrue(siteResource.enabled)
    }

    @Test
    fun `decodes sites and clients with flexible booleans`() {
        val sitePayload = """
            {
              "data": {
                "sites": [
                  {
                    "siteId": 1,
                    "niceId": "site-1",
                    "name": "Home",
                    "online": 1,
                    "newtUpdateAvailable": 0
                  }
                ]
              }
            }
        """.trimIndent()

        val siteResp = json.decodeFromString<PangolinSitesResponse>(sitePayload)
        val site = siteResp.data.sites.first()
        assertTrue(site.online)
        assertEquals(false, site.newtUpdateAvailable)

        val clientPayload = """
            {
              "data": {
                "clients": [
                  {
                    "clientId": 5,
                    "orgId": "org-1",
                    "name": "Phone",
                    "niceId": "cli-1",
                    "online": "true",
                    "archived": 0,
                    "blocked": "0"
                  }
                ]
              }
            }
        """.trimIndent()

        val clientResp = json.decodeFromString<PangolinClientsResponse>(clientPayload)
        val client = clientResp.data.clients.first()
        assertTrue(client.online)
        assertFalse(client.archived)
        assertFalse(client.blocked)
    }
}
