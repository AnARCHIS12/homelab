package com.homelab.app.util

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.homelab.app.MainActivity
import com.homelab.app.R
import com.homelab.app.domain.model.ServiceInstance
import com.homelab.app.ui.theme.localIconRes

/** Dashboard a launcher shortcut opens: the service type and its instance. */
data class ShortcutTarget(val type: ServiceType, val instanceId: String)

/**
 * Launcher shortcuts (long press on the app icon) to the first services of the home list:
 * configured, visible, in the order chosen by the user, each on its preferred instance.
 */
object AppShortcuts {
    const val ACTION_OPEN_SERVICE = "com.homelab.app.action.OPEN_SERVICE"
    const val EXTRA_SERVICE_TYPE = "com.homelab.app.extra.SERVICE_TYPE"
    const val EXTRA_INSTANCE_ID = "com.homelab.app.extra.INSTANCE_ID"
    private const val MAX_SHORTCUTS = 4
    private const val TAG = "AppShortcuts"

    fun select(
        order: List<ServiceType>,
        hiddenTypeNames: Set<String>,
        preferredInstances: Map<ServiceType, ServiceInstance?>,
        max: Int = MAX_SHORTCUTS
    ): List<ServiceInstance> = order
        .asSequence()
        .filter { it != ServiceType.UNKNOWN && it.name !in hiddenTypeNames }
        .mapNotNull { preferredInstances[it] }
        .take(max)
        .toList()

    fun publish(context: Context, instances: List<ServiceInstance>) {
        val limit = ShortcutManagerCompat.getMaxShortcutCountPerActivity(context).coerceAtLeast(1)
        val shortcuts = instances.take(limit).mapIndexed { rank, instance -> build(context, instance, rank) }
        val published = runCatching { ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts) }
        published.exceptionOrNull()?.let { Logger.e(TAG, "Launcher shortcuts could not be published", it) }
    }

    fun parse(intent: Intent?): ShortcutTarget? {
        if (intent?.action != ACTION_OPEN_SERVICE) return null
        val type = ServiceType.fromStoredName(intent.getStringExtra(EXTRA_SERVICE_TYPE))
        val instanceId = intent.getStringExtra(EXTRA_INSTANCE_ID)?.takeIf { it.isNotBlank() }
        if (type == ServiceType.UNKNOWN || instanceId == null) return null
        return ShortcutTarget(type, instanceId)
    }

    private fun build(context: Context, instance: ServiceInstance, rank: Int): ShortcutInfoCompat {
        val label = instance.label.trim().ifEmpty { instance.type.displayName }
        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_SERVICE
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_SERVICE_TYPE, instance.type.name)
            putExtra(EXTRA_INSTANCE_ID, instance.id)
        }
        val iconRes = instance.type.localIconRes.takeIf { it != 0 } ?: R.mipmap.ic_launcher_default
        return ShortcutInfoCompat.Builder(context, "service_${instance.id}")
            .setShortLabel(label)
            .setLongLabel(context.getString(R.string.shortcut_open_service, label))
            .setIcon(IconCompat.createWithResource(context, iconRes))
            .setIntent(intent)
            .setRank(rank)
            .build()
    }
}
