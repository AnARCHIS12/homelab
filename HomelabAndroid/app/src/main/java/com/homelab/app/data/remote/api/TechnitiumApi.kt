package com.homelab.app.data.remote.api

import kotlinx.serialization.json.JsonObject
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface TechnitiumApi {

    @FormUrlEncoded
    @POST("api/user/login")
    suspend fun login(
        @Field("user") user: String,
        @Field("pass") password: String,
        @Field("totp") totp: String? = null,
        @Field("includeInfo") includeInfo: Boolean = true,
        @Header("X-Homelab-Service") service: String = "Technitium",
        @Header("X-Homelab-Bypass") bypass: String = "true",
        @Header("X-Homelab-Allow-Self-Signed") allowSelfSigned: String = "false"
    ): JsonObject

    @GET("api/user/session/get")
    suspend fun getSession(
        @Header("Authorization") authorization: String,
        @Header("X-Homelab-Service") service: String = "Technitium",
        @Header("X-Homelab-Instance-Id") instanceId: String
    ): JsonObject

    @GET("api/dashboard/stats/get")
    suspend fun getDashboardStats(
        @Header("Authorization") authorization: String,
        @Query("type") type: String,
        @Query("utc") utc: Boolean = true,
        @Header("X-Homelab-Service") service: String = "Technitium",
        @Header("X-Homelab-Instance-Id") instanceId: String
    ): JsonObject

    @GET("api/dashboard/stats/getTop")
    suspend fun getTopStats(
        @Header("Authorization") authorization: String,
        @Query("type") type: String,
        @Query("statsType") statsType: String,
        @Query("limit") limit: Int = 20,
        @Header("X-Homelab-Service") service: String = "Technitium",
        @Header("X-Homelab-Instance-Id") instanceId: String
    ): JsonObject

    @GET("api/settings/get")
    suspend fun getSettings(
        @Header("Authorization") authorization: String,
        @Header("X-Homelab-Service") service: String = "Technitium",
        @Header("X-Homelab-Instance-Id") instanceId: String
    ): JsonObject

    @GET("api/settings/set")
    suspend fun setSettings(
        @QueryMap params: Map<String, String>,
        @Header("Authorization") authorization: String,
        @Header("X-Homelab-Service") service: String = "Technitium",
        @Header("X-Homelab-Instance-Id") instanceId: String
    ): JsonObject

    @GET("api/settings/forceUpdateBlockLists")
    suspend fun forceUpdateBlockLists(
        @Header("Authorization") authorization: String,
        @Header("X-Homelab-Service") service: String = "Technitium",
        @Header("X-Homelab-Instance-Id") instanceId: String
    ): JsonObject

    @GET("api/settings/temporaryDisableBlocking")
    suspend fun temporaryDisableBlocking(
        @Header("Authorization") authorization: String,
        @Query("minutes") minutes: Int,
        @Header("X-Homelab-Service") service: String = "Technitium",
        @Header("X-Homelab-Instance-Id") instanceId: String
    ): JsonObject

    @GET("api/zones/list")
    suspend fun listZones(
        @Header("Authorization") authorization: String,
        @Query("pageNumber") pageNumber: Int = 1,
        @Query("zonesPerPage") zonesPerPage: Int = 1,
        @Header("X-Homelab-Service") service: String = "Technitium",
        @Header("X-Homelab-Instance-Id") instanceId: String
    ): JsonObject

    @GET("api/cache/list")
    suspend fun listCache(
        @Header("Authorization") authorization: String,
        @Query("domain") domain: String = "",
        @Header("X-Homelab-Service") service: String = "Technitium",
        @Header("X-Homelab-Instance-Id") instanceId: String
    ): JsonObject

    @GET("api/blocked/list")
    suspend fun listBlockedZones(
        @Header("Authorization") authorization: String,
        @Query("domain") domain: String = "",
        @Header("X-Homelab-Service") service: String = "Technitium",
        @Header("X-Homelab-Instance-Id") instanceId: String
    ): JsonObject

    @GET("api/blocked/add")
    suspend fun addBlockedZone(
        @Header("Authorization") authorization: String,
        @Query("domain") domain: String,
        @Header("X-Homelab-Service") service: String = "Technitium",
        @Header("X-Homelab-Instance-Id") instanceId: String
    ): JsonObject

    @GET("api/blocked/delete")
    suspend fun deleteBlockedZone(
        @Header("Authorization") authorization: String,
        @Query("domain") domain: String,
        @Header("X-Homelab-Service") service: String = "Technitium",
        @Header("X-Homelab-Instance-Id") instanceId: String
    ): JsonObject

    @GET("api/logs/list")
    suspend fun listLogs(
        @Header("Authorization") authorization: String,
        @Header("X-Homelab-Service") service: String = "Technitium",
        @Header("X-Homelab-Instance-Id") instanceId: String
    ): JsonObject
}
