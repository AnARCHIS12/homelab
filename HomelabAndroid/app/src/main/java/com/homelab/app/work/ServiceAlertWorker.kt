package com.homelab.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.homelab.app.R
import com.homelab.app.data.local.dao.ServiceDao
import com.homelab.app.data.local.entity.ServiceStatusEntity
import com.homelab.app.data.repository.LocalPreferencesRepository
import com.homelab.app.data.repository.ServicesRepository
import com.homelab.app.domain.alerts.ServiceAlert
import com.homelab.app.domain.alerts.ServiceAlertEvaluator
import com.homelab.app.domain.model.ServiceInstance
import com.homelab.app.util.AppIconOption
import com.homelab.app.util.Logger
import com.homelab.app.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

/**
 * Periodic reachability check of every configured instance. Notifies only on state
 * changes (reachable to unreachable, and back); see [ServiceAlertEvaluator].
 */
@HiltWorker
class ServiceAlertWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val servicesRepository: ServicesRepository,
    private val serviceDao: ServiceDao,
    private val preferences: LocalPreferencesRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!preferences.backgroundAlertsEnabled.first()) return Result.success()
        return try {
            runCheck()
            Result.success()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Logger.e(TAG, "Background alert check failed", error)
            Result.retry()
        }
    }

    private suspend fun runCheck() {
        servicesRepository.initialize()
        val instances = servicesRepository.allInstances.first()
        val ids = instances.map { it.id }
        if (ids.isEmpty()) {
            serviceDao.clearAll()
            return
        }
        serviceDao.deleteAllExcept(ids)

        servicesRepository.checkAllReachability(force = true)
        val reachability = servicesRepository.reachability.first()
        val current = ids.associateWith { reachability[it] }
        val previous = serviceDao.getAllOnce().associate { it.serviceId to it.isRunning }

        val evaluation = ServiceAlertEvaluator.evaluate(previous, current)
        if (evaluation.skippedAsDeviceOffline) {
            Logger.i(TAG, "All ${ids.size} instances unreachable: device likely off the home network, skipping")
            return
        }
        storeStates(evaluation.statesToStore)
        notify(evaluation.alerts, instances.associateBy { it.id }, preferences.appIcon.first())
    }

    private suspend fun storeStates(states: Map<String, Boolean>) {
        val now = System.currentTimeMillis()
        serviceDao.insertStatuses(
            states.map { (id, running) ->
                ServiceStatusEntity(serviceId = id, isRunning = running, lastUpdated = now, rawJsonData = "")
            }
        )
    }

    private fun notify(
        alerts: List<ServiceAlert>,
        instancesById: Map<String, ServiceInstance>,
        appIcon: AppIconOption
    ) {
        if (alerts.isEmpty()) return
        NotificationHelper.createChannels(applicationContext)
        alerts.forEach { alert ->
            val instance = instancesById[alert.instanceId] ?: return@forEach
            val name = displayName(instance)
            val critical = alert is ServiceAlert.Down
            val titleRes = if (critical) R.string.alerts_service_down_title else R.string.alerts_service_recovered_title
            val textRes = if (critical) R.string.alerts_service_down_text else R.string.alerts_service_recovered_text
            NotificationHelper.notifyServiceAlert(
                context = applicationContext,
                instanceId = instance.id,
                title = applicationContext.getString(titleRes, name),
                text = applicationContext.getString(textRes, instance.type.displayName),
                critical = critical,
                largeIconRes = appIcon.previewDrawableRes
            )
        }
    }

    private fun displayName(instance: ServiceInstance): String =
        instance.label.trim().ifEmpty { instance.type.displayName }

    companion object {
        private const val TAG = "ServiceAlertWorker"
    }
}
