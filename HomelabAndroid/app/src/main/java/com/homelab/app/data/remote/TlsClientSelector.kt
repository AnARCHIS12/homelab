package com.homelab.app.data.remote

import com.homelab.app.data.repository.ServiceInstancesRepository
import com.homelab.app.data.security.HomelabCertificateTrust
import com.homelab.app.domain.model.ServiceInstance
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@Singleton
class TlsClientSelector @Inject constructor(
    private val serviceInstancesRepository: ServiceInstancesRepository,
    private val secureClient: OkHttpClient
) {
    private val clientCache = ConcurrentHashMap<String, OkHttpClient>()

    fun clientForInstance(instance: ServiceInstance?): OkHttpClient {
        if (instance == null) return secureClient
        val cacheKey = "${instance.id}_${instance.customCertFingerprint}_${instance.customCertificatePem?.hashCode()}_${instance.allowHttp}"
        return clientCache.computeIfAbsent(cacheKey) {
            val builder = secureClient.newBuilder()
            HomelabCertificateTrust.configureTlsForInstance(builder, instance)
            if (!instance.allowHttp) {
                builder.addInterceptor { chain ->
                    val request = chain.request()
                    if (!request.isHttps) {
                        throw SecurityException("Cleartext HTTP traffic is blocked for instance ${instance.label}")
                    }
                    chain.proceed(request)
                }
            }
            builder.build()
        }
    }

    fun forAllowSelfSigned(allowSelfSigned: Boolean, requestUrl: String? = null): OkHttpClient {
        // The old implementation returned a blind trust-all client here. Keep the
        // parameter for repository compatibility, but always use normal certificate
        // validation. The URL is used only to apply the per-connection HTTP policy.
        val url = requestUrl?.toHttpUrlOrNull() ?: return secureClient
        val transientInstance = ServiceInstance(
            id = "login-${url.host}-${url.port}",
            type = com.homelab.app.util.ServiceType.UNKNOWN,
            label = "Transient connection",
            url = url.toString(),
            allowHttp = url.scheme == "http"
        )
        return clientForInstance(transientInstance)
    }

    suspend fun forInstance(instanceId: String): OkHttpClient {
        val instance = serviceInstancesRepository.getInstance(instanceId)
        return clientForInstance(instance)
    }
}
