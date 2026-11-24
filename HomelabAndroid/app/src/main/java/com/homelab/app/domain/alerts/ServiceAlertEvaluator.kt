package com.homelab.app.domain.alerts

/** A reachability change worth notifying, for one service instance. */
sealed interface ServiceAlert {
    val instanceId: String

    data class Down(override val instanceId: String) : ServiceAlert
    data class Recovered(override val instanceId: String) : ServiceAlert
}

/** Outcome of one background check: alerts to send and states to persist. */
data class ServiceAlertEvaluation(
    val alerts: List<ServiceAlert>,
    val statesToStore: Map<String, Boolean>,
    val skippedAsDeviceOffline: Boolean
)

/**
 * Pure transition logic for background alerts.
 *
 * - The first observation of an instance only records its state: no alert, so enabling
 *   alerts never floods the shade with services that were already stopped.
 * - Instances whose check gave no answer (null) keep their previous state.
 * - When at least two instances are checked and every one of them is unreachable, the
 *   phone itself is most likely off the home network (no VPN, captive portal): nothing
 *   is notified and nothing is stored, so the next real check compares against the last
 *   trustworthy state.
 */
object ServiceAlertEvaluator {

    fun evaluate(
        previous: Map<String, Boolean>,
        current: Map<String, Boolean?>
    ): ServiceAlertEvaluation {
        val known = current.mapNotNull { (id, reachable) -> reachable?.let { id to it } }.toMap()

        if (known.size >= 2 && known.values.none { it }) {
            return ServiceAlertEvaluation(emptyList(), emptyMap(), skippedAsDeviceOffline = true)
        }

        val alerts = known.mapNotNull { (id, reachable) ->
            when (previous[id]) {
                true -> if (!reachable) ServiceAlert.Down(id) else null
                false -> if (reachable) ServiceAlert.Recovered(id) else null
                null -> null
            }
        }.sortedBy { it.instanceId }

        return ServiceAlertEvaluation(alerts, known, skippedAsDeviceOffline = false)
    }
}
