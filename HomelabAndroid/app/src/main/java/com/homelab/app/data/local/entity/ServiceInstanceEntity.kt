package com.homelab.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "service_instances")
data class ServiceInstanceEntity(
    @PrimaryKey val id: String,
    val type: String,
    val label: String,
    val url: String,
    // Sensitive fields: Deprecated and cleared in SQLite database, managed via SecureCredentialsStore
    @ColumnInfo(defaultValue = "''")
    val token: String = "",
    val proxmoxCsrfToken: String? = null,
    val proxmoxOtp: String? = null,
    val username: String? = null,
    val apiKey: String? = null,
    val piholePassword: String? = null,
    val piholeAuthMode: String? = null,
    val fallbackUrl: String? = null,
    @ColumnInfo(defaultValue = "0")
    val allowSelfSigned: Boolean = false,
    val password: String? = null,
    @ColumnInfo(defaultValue = "0")
    val allowHttp: Boolean = false,
    val customCertFingerprint: String? = null,
    val customCertificatePem: String? = null
)
