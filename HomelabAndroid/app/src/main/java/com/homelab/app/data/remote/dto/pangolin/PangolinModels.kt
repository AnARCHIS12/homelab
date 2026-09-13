package com.homelab.app.data.remote.dto.pangolin

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import java.util.Locale

object PangolinFlexibleBoolSerializer : KSerializer<Boolean> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("PangolinFlexibleBool", PrimitiveKind.BOOLEAN)

    override fun deserialize(decoder: Decoder): Boolean {
        val jsonDecoder = decoder as? JsonDecoder
        if (jsonDecoder != null) {
            val element = jsonDecoder.decodeJsonElement()
            if (element is JsonNull) return false
            val primitive = element as? JsonPrimitive ?: return false
            return primitive.asFlexibleBool()
        }

        return runCatching { decoder.decodeBoolean() }
            .recoverCatching {
                val raw = decoder.decodeString().trim().lowercase(Locale.ROOT)
                raw == "true" || raw == "1" || raw == "yes"
            }
            .getOrDefault(false)
    }

    override fun serialize(encoder: Encoder, value: Boolean) {
        encoder.encodeBoolean(value)
    }
}

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
object PangolinFlexibleNullableBoolSerializer : KSerializer<Boolean?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("PangolinFlexibleNullableBool", PrimitiveKind.BOOLEAN)

    override fun deserialize(decoder: Decoder): Boolean? {
        val jsonDecoder = decoder as? JsonDecoder
        if (jsonDecoder != null) {
            val element = jsonDecoder.decodeJsonElement()
            if (element is JsonNull) return null
            val primitive = element as? JsonPrimitive ?: return null
            if (primitive.isString && primitive.content.equals("null", ignoreCase = true)) return null
            return primitive.asFlexibleBool()
        }

        return runCatching { decoder.decodeBoolean() }
            .recoverCatching {
                val raw = decoder.decodeString().trim().lowercase(Locale.ROOT)
                if (raw == "null" || raw.isEmpty()) null
                else raw == "true" || raw == "1" || raw == "yes"
            }
            .getOrNull()
    }

    override fun serialize(encoder: Encoder, value: Boolean?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            encoder.encodeBoolean(value)
        }
    }
}

private fun JsonPrimitive?.asFlexibleBool(): Boolean {
    val primitive = this ?: return false
    if (primitive.isString) {
        val raw = primitive.content.trim().lowercase(Locale.ROOT)
        return raw == "true" || raw == "1" || raw == "yes"
    }
    return primitive.booleanOrNull
        ?: primitive.intOrNull?.let { it != 0 }
        ?: primitive.doubleOrNull?.let { it != 0.0 }
        ?: false
}

@Serializable
data class PangolinEnvelope<T>(
    val data: T,
    val pagination: PangolinPagination? = null,
    @Serializable(with = PangolinFlexibleNullableBoolSerializer::class)
    val success: Boolean? = null,
    @Serializable(with = PangolinFlexibleNullableBoolSerializer::class)
    val error: Boolean? = null,
    val message: String? = null,
    val status: Int? = null
)

@Serializable
data class PangolinPagination(
    val total: Int = 0,
    val limit: Int? = null,
    val offset: Int? = null,
    val pageSize: Int? = null,
    val page: Int? = null
)

@Serializable
data class PangolinOrg(
    val orgId: String = "",
    val name: String = "",
    val subnet: String? = null,
    val utilitySubnet: String? = null,
    @Serializable(with = PangolinFlexibleNullableBoolSerializer::class)
    val suspendOrg: Boolean? = null,
    val suspendAt: Long? = null,
    @Serializable(with = PangolinFlexibleNullableBoolSerializer::class)
    val isBillingOrg: Boolean? = null
)

@Serializable
data class PangolinOrgsData(
    val orgs: List<PangolinOrg> = emptyList()
)

typealias PangolinOrgsResponse = PangolinEnvelope<PangolinOrgsData>

@Serializable
data class PangolinSite(
    val siteId: Int = 0,
    val niceId: String = "",
    val name: String = "",
    val subnet: String? = null,
    val megabytesIn: Double? = null,
    val megabytesOut: Double? = null,
    val type: String? = null,
    @Serializable(with = PangolinFlexibleBoolSerializer::class)
    val online: Boolean = false,
    val address: String? = null,
    val newtVersion: String? = null,
    val exitNodeName: String? = null,
    val exitNodeEndpoint: String? = null,
    @Serializable(with = PangolinFlexibleNullableBoolSerializer::class)
    val newtUpdateAvailable: Boolean? = null
)

@Serializable
data class PangolinSitesData(
    val sites: List<PangolinSite> = emptyList()
)

typealias PangolinSitesResponse = PangolinEnvelope<PangolinSitesData>

@Serializable
data class PangolinSiteResource(
    val siteResourceId: Int = 0,
    val siteId: Int? = null,
    val orgId: String? = null,
    val niceId: String = "",
    val name: String = "",
    val mode: String? = null,
    val protocol: String? = null,
    val proxyPort: Int? = null,
    val destinationPort: Int? = null,
    val destination: String? = null,
    @Serializable(with = PangolinFlexibleBoolSerializer::class)
    val enabled: Boolean = false,
    val alias: String? = null,
    val aliasAddress: String? = null,
    val tcpPortRangeString: String? = null,
    val udpPortRangeString: String? = null,
    @Serializable(with = PangolinFlexibleNullableBoolSerializer::class)
    val disableIcmp: Boolean? = null,
    val authDaemonMode: String? = null,
    val authDaemonPort: Int? = null,
    val siteName: String? = null,
    val siteNiceId: String? = null,
    val siteAddress: String? = null
)

@Serializable
data class PangolinSiteResourcesData(
    val siteResources: List<PangolinSiteResource> = emptyList()
)

typealias PangolinSiteResourcesResponse = PangolinEnvelope<PangolinSiteResourcesData>

@Serializable
data class PangolinTarget(
    val targetId: Int = 0,
    val ip: String = "",
    val port: Int = 0,
    @Serializable(with = PangolinFlexibleBoolSerializer::class)
    val enabled: Boolean = false,
    val healthStatus: String? = null,
    val method: String? = null,
    val resourceId: Int? = null,
    val siteId: Int? = null,
    val siteType: String? = null,
    @Serializable(with = PangolinFlexibleNullableBoolSerializer::class)
    val hcEnabled: Boolean? = null,
    val hcPath: String? = null,
    val hcScheme: String? = null,
    val hcMode: String? = null,
    val hcHostname: String? = null,
    val hcPort: Int? = null,
    val hcInterval: Int? = null,
    val hcUnhealthyInterval: Int? = null,
    val hcTimeout: Int? = null,
    val hcHeaders: List<PangolinHeader>? = null,
    @Serializable(with = PangolinFlexibleNullableBoolSerializer::class)
    val hcFollowRedirects: Boolean? = null,
    val hcMethod: String? = null,
    val hcStatus: String? = null,
    val hcHealth: String? = null,
    val hcTlsServerName: String? = null,
    val path: String? = null,
    val pathMatchType: String? = null,
    val rewritePath: String? = null,
    val rewritePathType: String? = null,
    val priority: Int? = null
)

@Serializable
data class PangolinHeader(
    val name: String = "",
    val value: String = ""
)

@Serializable
data class PangolinResource(
    val resourceId: Int = 0,
    val name: String = "",
    @Serializable(with = PangolinFlexibleBoolSerializer::class)
    val ssl: Boolean = false,
    val fullDomain: String? = null,
    @Serializable(with = PangolinFlexibleBoolSerializer::class)
    val sso: Boolean = false,
    @Serializable(with = PangolinFlexibleBoolSerializer::class)
    val whitelist: Boolean = false,
    @Serializable(with = PangolinFlexibleBoolSerializer::class)
    val http: Boolean = false,
    val protocol: String? = null,
    val proxyPort: Int? = null,
    @Serializable(with = PangolinFlexibleBoolSerializer::class)
    val enabled: Boolean = false,
    val domainId: String? = null,
    val niceId: String = "",
    val targets: List<PangolinTarget> = emptyList()
)

@Serializable
data class PangolinResourcesData(
    val resources: List<PangolinResource> = emptyList()
)

typealias PangolinResourcesResponse = PangolinEnvelope<PangolinResourcesData>

@Serializable
data class PangolinTargetsData(
    val targets: List<PangolinTarget> = emptyList()
)

typealias PangolinTargetsResponse = PangolinEnvelope<PangolinTargetsData>

@Serializable
data class PangolinClientSite(
    val siteId: Int = 0,
    val siteName: String? = null,
    val siteNiceId: String? = null
)

@Serializable
data class PangolinClient(
    val clientId: Int = 0,
    val orgId: String = "",
    val name: String = "",
    val subnet: String? = null,
    val megabytesIn: Double? = null,
    val megabytesOut: Double? = null,
    val type: String? = null,
    @Serializable(with = PangolinFlexibleBoolSerializer::class)
    val online: Boolean = false,
    val olmVersion: String? = null,
    val niceId: String = "",
    val approvalState: String? = null,
    @Serializable(with = PangolinFlexibleBoolSerializer::class)
    val archived: Boolean = false,
    @Serializable(with = PangolinFlexibleBoolSerializer::class)
    val blocked: Boolean = false,
    val sites: List<PangolinClientSite> = emptyList(),
    @Serializable(with = PangolinFlexibleNullableBoolSerializer::class)
    val olmUpdateAvailable: Boolean? = null
)

@Serializable
data class PangolinClientsData(
    val clients: List<PangolinClient> = emptyList()
)

typealias PangolinClientsResponse = PangolinEnvelope<PangolinClientsData>

@Serializable
data class PangolinUserDevice(
    val clientId: Int = 0,
    val orgId: String = "",
    val name: String = "",
    val subnet: String? = null,
    val megabytesIn: Double? = null,
    val megabytesOut: Double? = null,
    val orgName: String? = null,
    val type: String? = null,
    @Serializable(with = PangolinFlexibleBoolSerializer::class)
    val online: Boolean = false,
    val olmVersion: String? = null,
    val userId: String? = null,
    val username: String? = null,
    val userEmail: String? = null,
    val niceId: String = "",
    val agent: String? = null,
    val approvalState: String? = null,
    @Serializable(with = PangolinFlexibleBoolSerializer::class)
    val olmArchived: Boolean = false,
    @Serializable(with = PangolinFlexibleBoolSerializer::class)
    val archived: Boolean = false,
    @Serializable(with = PangolinFlexibleBoolSerializer::class)
    val blocked: Boolean = false,
    val deviceModel: String? = null,
    val fingerprintPlatform: String? = null,
    val fingerprintOsVersion: String? = null,
    val fingerprintKernelVersion: String? = null,
    val fingerprintArch: String? = null,
    val fingerprintSerialNumber: String? = null,
    val fingerprintUsername: String? = null,
    val fingerprintHostname: String? = null,
    @Serializable(with = PangolinFlexibleNullableBoolSerializer::class)
    val olmUpdateAvailable: Boolean? = null
)

@Serializable
data class PangolinUserDevicesData(
    val devices: List<PangolinUserDevice> = emptyList()
)

typealias PangolinUserDevicesResponse = PangolinEnvelope<PangolinUserDevicesData>

@Serializable
data class PangolinSiteResourceUser(
    val userId: String = ""
)

@Serializable
data class PangolinSiteResourceUsersData(
    val users: List<PangolinSiteResourceUser> = emptyList()
)

typealias PangolinSiteResourceUsersResponse = PangolinEnvelope<PangolinSiteResourceUsersData>

@Serializable
data class PangolinSiteResourceRole(
    val roleId: Int = 0
)

@Serializable
data class PangolinSiteResourceRolesData(
    val roles: List<PangolinSiteResourceRole> = emptyList()
)

typealias PangolinSiteResourceRolesResponse = PangolinEnvelope<PangolinSiteResourceRolesData>

@Serializable
data class PangolinSiteResourceClient(
    val clientId: Int = 0
)

@Serializable
data class PangolinSiteResourceClientsData(
    val clients: List<PangolinSiteResourceClient> = emptyList()
)

typealias PangolinSiteResourceClientsResponse = PangolinEnvelope<PangolinSiteResourceClientsData>

@Serializable
data class PangolinDomain(
    val domainId: String = "",
    val baseDomain: String = "",
    @Serializable(with = PangolinFlexibleBoolSerializer::class)
    val verified: Boolean = false,
    val type: String? = null,
    @Serializable(with = PangolinFlexibleBoolSerializer::class)
    val failed: Boolean = false,
    val tries: Int? = null,
    @Serializable(with = PangolinFlexibleNullableBoolSerializer::class)
    val configManaged: Boolean? = null,
    val certResolver: String? = null,
    @Serializable(with = PangolinFlexibleNullableBoolSerializer::class)
    val preferWildcardCert: Boolean? = null,
    val errorMessage: String? = null
)

@Serializable
data class PangolinDomainsData(
    val domains: List<PangolinDomain> = emptyList()
)

typealias PangolinDomainsResponse = PangolinEnvelope<PangolinDomainsData>
