package com.homelab.app.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.homelab.app.MainActivity
import com.homelab.app.R

object NotificationHelper {
    const val CHANNEL_CRITICAL_ALERTS = "critical_alerts"
    const val CHANNEL_GENERAL_INFO = "general_info"
    private const val TAG = "NotificationHelper"

    fun createChannels(context: Context) {
        val criticalChannel = NotificationChannel(
            CHANNEL_CRITICAL_ALERTS,
            context.getString(R.string.notification_channel_critical_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_critical_desc)
        }

        val generalChannel = NotificationChannel(
            CHANNEL_GENERAL_INFO,
            context.getString(R.string.notification_channel_general_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_general_desc)
        }

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(criticalChannel)
        notificationManager.createNotificationChannel(generalChannel)
    }

    fun canPostNotifications(context: Context): Boolean {
        val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return permissionGranted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /**
     * Posts one notification per instance; a later alert for the same instance replaces the
     * previous one, so a recovery visibly supersedes the matching outage.
     */
    fun notifyServiceAlert(
        context: Context,
        instanceId: String,
        title: String,
        text: String,
        critical: Boolean,
        @DrawableRes largeIconRes: Int
    ) {
        if (!canPostNotifications(context)) {
            Logger.w(TAG, "Notification skipped: permission missing or notifications disabled")
            return
        }
        val channel = if (critical) CHANNEL_CRITICAL_ALERTS else CHANNEL_GENERAL_INFO
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification_homelab)
            .setLargeIcon(loadLargeIcon(context, largeIconRes))
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(if (critical) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context))
            .build()
        try {
            NotificationManagerCompat.from(context).notify(instanceId.hashCode(), notification)
        } catch (error: SecurityException) {
            Logger.e(TAG, "Notification permission revoked while posting", error)
        }
    }

    /** Decodes the launcher logo chosen in the settings, scaled to the notification size. */
    private fun loadLargeIcon(context: Context, @DrawableRes res: Int): Bitmap? {
        val size = context.resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width)
        val decoded = BitmapFactory.decodeResource(context.resources, res)
        if (decoded == null) {
            Logger.w(TAG, "Large icon could not be decoded, notification shown without it")
            return null
        }
        return Bitmap.createScaledBitmap(decoded, size, size, true)
    }

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
