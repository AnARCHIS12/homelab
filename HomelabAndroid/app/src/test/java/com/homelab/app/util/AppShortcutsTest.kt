package com.homelab.app.util

import com.homelab.app.domain.model.ServiceInstance
import org.junit.Assert.assertEquals
import org.junit.Test

class AppShortcutsTest {

    private fun instance(type: ServiceType, id: String = type.name.lowercase()) =
        ServiceInstance(id = id, type = type, label = type.displayName, url = "https://example.org")

    @Test
    fun `keeps the home order and skips unconfigured services`() {
        val result = AppShortcuts.select(
            order = listOf(ServiceType.PROXMOX, ServiceType.PIHOLE, ServiceType.PORTAINER),
            hiddenTypeNames = emptySet(),
            preferredInstances = mapOf(
                ServiceType.PORTAINER to instance(ServiceType.PORTAINER),
                ServiceType.PROXMOX to instance(ServiceType.PROXMOX),
                ServiceType.PIHOLE to null
            )
        )

        assertEquals(listOf("proxmox", "portainer"), result.map { it.id })
    }

    @Test
    fun `hidden services get no shortcut`() {
        val result = AppShortcuts.select(
            order = listOf(ServiceType.PROXMOX, ServiceType.PORTAINER),
            hiddenTypeNames = setOf(ServiceType.PROXMOX.name),
            preferredInstances = mapOf(
                ServiceType.PROXMOX to instance(ServiceType.PROXMOX),
                ServiceType.PORTAINER to instance(ServiceType.PORTAINER)
            )
        )

        assertEquals(listOf("portainer"), result.map { it.id })
    }

    @Test
    fun `uses the preferred instance of each service`() {
        val result = AppShortcuts.select(
            order = listOf(ServiceType.PORTAINER),
            hiddenTypeNames = emptySet(),
            preferredInstances = mapOf(ServiceType.PORTAINER to instance(ServiceType.PORTAINER, id = "second"))
        )

        assertEquals(listOf("second"), result.map { it.id })
    }

    @Test
    fun `is limited to four shortcuts`() {
        val types = listOf(
            ServiceType.PORTAINER, ServiceType.PIHOLE, ServiceType.PROXMOX,
            ServiceType.BESZEL, ServiceType.GITEA, ServiceType.PLEX
        )
        val result = AppShortcuts.select(
            order = types,
            hiddenTypeNames = emptySet(),
            preferredInstances = types.associateWith { instance(it) }
        )

        assertEquals(types.take(4).map { it.name.lowercase() }, result.map { it.id })
    }
}
