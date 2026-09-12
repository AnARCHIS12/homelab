package com.homelab.app.data.remote

import com.homelab.app.data.repository.ServiceInstancesRepository
import com.homelab.app.data.security.HomelabCertificateTrust
import com.homelab.app.domain.model.ServiceInstance
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.OkHttpClient

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

    fun forAllowSelfSigned(allowSelfSigned: Boolean): OkHttpClient {
        // Blind trust is permanently removed.
        return secureClient
    }

    suspend fun forInstance(instanceId: String): OkHttpClient {
        val instance = serviceInstancesRepository.getInstance(instanceId)
        return clientForInstance(instance)
    }
}
