package com.homelab.app.data.remote

import com.homelab.app.data.repository.ServiceInstancesRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.Request

@Singleton
class TlsRoutingCallFactory @Inject constructor(
    private val tlsClientSelector: TlsClientSelector,
    private val serviceInstancesRepository: ServiceInstancesRepository
) : Call.Factory {

    override fun newCall(request: Request): Call {
        val instanceId = request.header(INSTANCE_ID_HEADER)
        val instance = instanceId?.let {
            runBlocking { serviceInstancesRepository.getInstance(it) }
        }

        val sanitizedRequest = request.newBuilder()
            .removeHeader(ALLOW_SELF_SIGNED_HEADER)
            .build()

        val client = tlsClientSelector.clientForInstance(instance)
        return client.newCall(sanitizedRequest)
    }

    companion object {
        const val ALLOW_SELF_SIGNED_HEADER = "X-Homelab-Allow-Self-Signed"
        private const val INSTANCE_ID_HEADER = "X-Homelab-Instance-Id"
    }
}
