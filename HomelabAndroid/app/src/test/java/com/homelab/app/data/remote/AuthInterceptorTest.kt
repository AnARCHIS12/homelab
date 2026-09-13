package com.homelab.app.data.remote

import com.homelab.app.data.repository.BeszelRepository
import com.homelab.app.data.repository.DockhandRepository
import com.homelab.app.data.repository.MaltrailRepository
import com.homelab.app.data.repository.NginxProxyManagerRepository
import com.homelab.app.data.repository.PangolinRepository
import com.homelab.app.data.repository.ProxmoxRepository
import com.homelab.app.data.repository.ServiceInstancesRepository
import com.homelab.app.domain.model.ServiceInstance
import com.homelab.app.util.GlobalEventBus
import com.homelab.app.util.ServiceType
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthInterceptorTest {

    private fun createInterceptor(
        eventBus: GlobalEventBus = mockk(relaxed = true),
        instancesRepository: ServiceInstancesRepository = mockk(),
        beszelRepository: BeszelRepository = mockk(),
        pangolinRepository: PangolinRepository = mockk()
    ): Triple<AuthInterceptor, GlobalEventBus, ServiceInstancesRepository> {
        val beszelRepo = mockk<dagger.Lazy<BeszelRepository>>()
        every { beszelRepo.get() } returns beszelRepository
        val dockhandRepo = mockk<dagger.Lazy<DockhandRepository>>()
        every { dockhandRepo.get() } returns mockk()
        val maltrailRepo = mockk<dagger.Lazy<MaltrailRepository>>()
        every { maltrailRepo.get() } returns mockk()
        val npmRepo = mockk<dagger.Lazy<NginxProxyManagerRepository>>()
        every { npmRepo.get() } returns mockk()
        val proxmoxRepo = mockk<dagger.Lazy<ProxmoxRepository>>()
        every { proxmoxRepo.get() } returns mockk()
        val pangolinRepo = mockk<dagger.Lazy<PangolinRepository>>()
        every { pangolinRepo.get() } returns pangolinRepository
        return Triple(AuthInterceptor(eventBus, instancesRepository, beszelRepo, dockhandRepo, maltrailRepo, npmRepo, proxmoxRepo, pangolinRepo), eventBus, instancesRepository)
    }

    @Test
    fun `resolves auth from instance id instead of service header`() {
        val eventBus = mockk<GlobalEventBus>(relaxed = true)
        val instancesRepository = mockk<ServiceInstancesRepository>()
        val (interceptor) = createInterceptor(eventBus, instancesRepository)
        val chain = mockk<Interceptor.Chain>()
        val capturedRequest = slot<Request>()
        val request = Request.Builder()
            .url("https://example.com/api/endpoints")
            .header("X-Homelab-Service", "Gitea")
            .header("X-Homelab-Instance-Id", "instance-1")
            .build()

        coEvery { instancesRepository.getInstance("instance-1") } returns ServiceInstance(
            id = "instance-1",
            type = ServiceType.PORTAINER,
            label = "Portainer Lab",
            url = "https://portainer.local",
            apiKey = "real-api-key"
        )
        every { chain.request() } returns request
        every { chain.proceed(capture(capturedRequest)) } answers {
            response(capturedRequest.captured, 200)
        }

        interceptor.intercept(chain)

        assertEquals("real-api-key", capturedRequest.captured.header("X-API-Key"))
        assertNull(capturedRequest.captured.header("Authorization"))
        assertNull(capturedRequest.captured.header("X-Homelab-Service"))
        assertNull(capturedRequest.captured.header("X-Homelab-Instance-Id"))
    }

    @Test
    fun `resolves auth even when service header is missing`() {
        val eventBus = mockk<GlobalEventBus>(relaxed = true)
        val instancesRepository = mockk<ServiceInstancesRepository>()
        val (interceptor) = createInterceptor(eventBus, instancesRepository)
        val chain = mockk<Interceptor.Chain>()
        val capturedRequest = slot<Request>()
        val request = Request.Builder()
            .url("https://example.com/api/endpoints")
            .header("X-Homelab-Instance-Id", "instance-3")
            .build()

        coEvery { instancesRepository.getInstance("instance-3") } returns ServiceInstance(
            id = "instance-3",
            type = ServiceType.PORTAINER,
            label = "Portainer Office",
            url = "https://portainer-office.local",
            apiKey = "office-api-key"
        )
        every { chain.request() } returns request
        every { chain.proceed(capture(capturedRequest)) } answers {
            response(capturedRequest.captured, 200)
        }

        interceptor.intercept(chain)

        assertEquals("office-api-key", capturedRequest.captured.header("X-API-Key"))
        assertNull(capturedRequest.captured.header("Authorization"))
    }

    @Test
    fun `401 emits auth error for affected instance only`() {
        val eventBus = mockk<GlobalEventBus>(relaxed = true)
        val instancesRepository = mockk<ServiceInstancesRepository>()
        val (interceptor) = createInterceptor(eventBus, instancesRepository)
        val chain = mockk<Interceptor.Chain>()
        val request = Request.Builder()
            .url("https://example.com/api/v1/user")
            .header("X-Homelab-Service", "Gitea")
            .header("X-Homelab-Instance-Id", "instance-2")
            .build()

        coEvery { instancesRepository.getInstance("instance-2") } returns ServiceInstance(
            id = "instance-2",
            type = ServiceType.GITEA,
            label = "Main",
            url = "https://gitea.local",
            token = "token-1"
        )
        every { chain.request() } returns request
        every { chain.proceed(any()) } answers {
            response(invocation.args[0] as Request, 401)
        }

        interceptor.intercept(chain)

        verify { eventBus.emitAuthError("instance-2") }
    }

    @Test
    fun `adds jellystat api token header from stored api key`() {
        val eventBus = mockk<GlobalEventBus>(relaxed = true)
        val instancesRepository = mockk<ServiceInstancesRepository>()
        val (interceptor) = createInterceptor(eventBus, instancesRepository)
        val chain = mockk<Interceptor.Chain>()
        val capturedRequest = slot<Request>()
        val request = Request.Builder()
            .url("https://example.com/stats/getViewsByLibraryType?days=30")
            .header("X-Homelab-Service", "Jellystat")
            .header("X-Homelab-Instance-Id", "instance-jelly")
            .build()

        coEvery { instancesRepository.getInstance("instance-jelly") } returns ServiceInstance(
            id = "instance-jelly",
            type = ServiceType.JELLYSTAT,
            label = "Jellystat Main",
            url = "https://jellystat.local",
            apiKey = "jelly-api-key"
        )
        every { chain.request() } returns request
        every { chain.proceed(capture(capturedRequest)) } answers {
            response(capturedRequest.captured, 200)
        }

        interceptor.intercept(chain)

        assertEquals("jelly-api-key", capturedRequest.captured.header("X-API-Token"))
        assertNull(capturedRequest.captured.header("X-Homelab-Service"))
        assertNull(capturedRequest.captured.header("X-Homelab-Instance-Id"))
    }

    @Test
    fun `beszel 403 refreshes token and retries request`() {
        val eventBus = mockk<GlobalEventBus>(relaxed = true)
        val instancesRepository = mockk<ServiceInstancesRepository>()
        val beszelRepository = mockk<BeszelRepository>()
        val (interceptor) = createInterceptor(eventBus, instancesRepository, beszelRepository)
        val chain = mockk<Interceptor.Chain>()
        val proceeded = mutableListOf<Request>()
        val request = Request.Builder()
            .url("https://beszel.local/api/collections/systems/records")
            .header("X-Homelab-Service", "Beszel")
            .header("X-Homelab-Instance-Id", "instance-beszel")
            .build()

        coEvery { instancesRepository.getInstance("instance-beszel") } returns ServiceInstance(
            id = "instance-beszel",
            type = ServiceType.BESZEL,
            label = "Beszel",
            url = "https://beszel.local",
            token = "expired-token",
            username = "admin@example.com",
            password = "secret"
        )
        coEvery { beszelRepository.refreshStoredToken("instance-beszel") } returns "fresh-token"
        every { chain.request() } returns request
        every { chain.proceed(any()) } answers {
            val req = invocation.args[0] as Request
            proceeded += req
            response(req, if (proceeded.size == 1) 403 else 200)
        }

        val response = interceptor.intercept(chain)

        assertEquals(200, response.code)
        assertEquals("Bearer expired-token", proceeded.first().header("Authorization"))
        assertEquals("Bearer fresh-token", proceeded.last().header("Authorization"))
    }

    @Test
    fun `proxmox adds cookie and csrf header without leaking credentials`() {
        val eventBus = mockk<GlobalEventBus>(relaxed = true)
        val instancesRepository = mockk<ServiceInstancesRepository>()
        val (interceptor) = createInterceptor(eventBus, instancesRepository)
        val chain = mockk<Interceptor.Chain>()
        val capturedRequest = slot<Request>()
        val request = Request.Builder()
            .url("https://proxmox.local/api2/json/nodes/pve/qemu/100/status/start")
            .post("{}".toRequestBody("application/json".toMediaType()))
            .header("X-Homelab-Service", "Proxmox")
            .header("X-Homelab-Instance-Id", "instance-proxmox")
            .build()

        coEvery { instancesRepository.getInstance("instance-proxmox") } returns ServiceInstance(
            id = "instance-proxmox",
            type = ServiceType.PROXMOX,
            label = "PVE",
            url = "https://proxmox.local",
            token = "ticket-123",
            proxmoxCsrfToken = "csrf-123",
            username = "root@pam",
            password = "secret"
        )
        every { chain.request() } returns request
        every { chain.proceed(capture(capturedRequest)) } answers {
            response(capturedRequest.captured, 200)
        }

        interceptor.intercept(chain)

        assertEquals("PVEAuthCookie=ticket-123", capturedRequest.captured.header("Cookie"))
        assertEquals("csrf-123", capturedRequest.captured.header("CSRFPreventionToken"))
        assertNull(capturedRequest.captured.header("X-Homelab-Username"))
        assertNull(capturedRequest.captured.header("X-Homelab-Password"))
    }

    @Test
    fun `proxmox api token uses authorization header without cookie`() {
        val eventBus = mockk<GlobalEventBus>(relaxed = true)
        val instancesRepository = mockk<ServiceInstancesRepository>()
        val (interceptor) = createInterceptor(eventBus, instancesRepository)
        val chain = mockk<Interceptor.Chain>()
        val capturedRequest = slot<Request>()
        val request = Request.Builder()
            .url("https://proxmox.local/api2/json/version")
            .header("X-Homelab-Service", "Proxmox")
            .header("X-Homelab-Instance-Id", "instance-proxmox-token")
            .build()

        coEvery { instancesRepository.getInstance("instance-proxmox-token") } returns ServiceInstance(
            id = "instance-proxmox-token",
            type = ServiceType.PROXMOX,
            label = "PVE Token",
            url = "https://proxmox.local",
            apiKey = "root@pam!codex=secret-token"
        )
        every { chain.request() } returns request
        every { chain.proceed(capture(capturedRequest)) } answers {
            response(capturedRequest.captured, 200)
        }

        interceptor.intercept(chain)

        assertEquals("PVEAPIToken=root@pam!codex=secret-token", capturedRequest.captured.header("Authorization"))
        assertNull(capturedRequest.captured.header("Cookie"))
        assertNull(capturedRequest.captured.header("CSRFPreventionToken"))
    }

    @Test
    fun `attaches cookie and csrf token and rewrites path for pangolin session auth`() {
        val eventBus = mockk<GlobalEventBus>(relaxed = true)
        val instancesRepository = mockk<ServiceInstancesRepository>()
        val (interceptor) = createInterceptor(eventBus, instancesRepository)
        val chain = mockk<Interceptor.Chain>()
        val capturedRequest = slot<Request>()
        val request = Request.Builder()
            .url("https://pangolin.local/v1/org/as/sites?pageSize=1")
            .header("X-Homelab-Service", "Pangolin")
            .header("X-Homelab-Instance-Id", "instance-pangolin-cookie")
            .build()

        coEvery { instancesRepository.getInstance("instance-pangolin-cookie") } returns ServiceInstance(
            id = "instance-pangolin-cookie",
            type = ServiceType.PANGOLIN,
            label = "Pangolin",
            url = "https://pangolin.local",
            token = "pangolin_session=s%3Axyz123"
        )
        every { chain.request() } returns request
        every { chain.proceed(capture(capturedRequest)) } answers {
            response(capturedRequest.captured, 200)
        }

        interceptor.intercept(chain)

        assertEquals("pangolin_session=s%3Axyz123", capturedRequest.captured.header("Cookie"))
        assertEquals("x-csrf-protection", capturedRequest.captured.header("x-csrf-token"))
        assertNull(capturedRequest.captured.header("Authorization"))
        assertEquals("/api/v1/org/as/sites", capturedRequest.captured.url.encodedPath)
    }

    @Test
    fun `attaches bearer auth for pangolin api key auth and keeps v1 path`() {
        val eventBus = mockk<GlobalEventBus>(relaxed = true)
        val instancesRepository = mockk<ServiceInstancesRepository>()
        val (interceptor) = createInterceptor(eventBus, instancesRepository)
        val chain = mockk<Interceptor.Chain>()
        val capturedRequest = slot<Request>()
        val request = Request.Builder()
            .url("https://pangolin.local:3003/v1/orgs")
            .header("X-Homelab-Service", "Pangolin")
            .header("X-Homelab-Instance-Id", "instance-pangolin-key")
            .build()

        coEvery { instancesRepository.getInstance("instance-pangolin-key") } returns ServiceInstance(
            id = "instance-pangolin-key",
            type = ServiceType.PANGOLIN,
            label = "Pangolin API",
            url = "https://pangolin.local:3003",
            apiKey = "my-secret-api-key"
        )
        every { chain.request() } returns request
        every { chain.proceed(capture(capturedRequest)) } answers {
            response(capturedRequest.captured, 200)
        }

        interceptor.intercept(chain)

        assertEquals("Bearer my-secret-api-key", capturedRequest.captured.header("Authorization"))
        assertNull(capturedRequest.captured.header("Cookie"))
        assertEquals("/v1/orgs", capturedRequest.captured.url.encodedPath)
    }

    private fun response(request: Request, code: Int): Response {
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(if (code == 200) "OK" else "Unauthorized")
            .body("{}".toResponseBody("application/json".toMediaType()))
            .build()
    }
}
