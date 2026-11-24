package com.homelab.app.domain.alerts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceAlertEvaluatorTest {

    @Test
    fun `first observation stores state without alerting`() {
        val result = ServiceAlertEvaluator.evaluate(
            previous = emptyMap(),
            current = mapOf("a" to false, "b" to true)
        )

        assertEquals(emptyList<ServiceAlert>(), result.alerts)
        assertEquals(mapOf("a" to false, "b" to true), result.statesToStore)
        assertFalse(result.skippedAsDeviceOffline)
    }

    @Test
    fun `reachable to unreachable raises a down alert`() {
        val result = ServiceAlertEvaluator.evaluate(
            previous = mapOf("a" to true, "b" to true),
            current = mapOf("a" to false, "b" to true)
        )

        assertEquals(listOf(ServiceAlert.Down("a")), result.alerts)
        assertEquals(mapOf("a" to false, "b" to true), result.statesToStore)
    }

    @Test
    fun `unreachable to reachable raises a recovered alert`() {
        val result = ServiceAlertEvaluator.evaluate(
            previous = mapOf("a" to false, "b" to true),
            current = mapOf("a" to true, "b" to true)
        )

        assertEquals(listOf(ServiceAlert.Recovered("a")), result.alerts)
    }

    @Test
    fun `unchanged states raise nothing`() {
        val result = ServiceAlertEvaluator.evaluate(
            previous = mapOf("a" to false, "b" to true),
            current = mapOf("a" to false, "b" to true)
        )

        assertEquals(emptyList<ServiceAlert>(), result.alerts)
    }

    @Test
    fun `unknown result keeps previous state and is not stored`() {
        val result = ServiceAlertEvaluator.evaluate(
            previous = mapOf("a" to true, "b" to true),
            current = mapOf("a" to null, "b" to true)
        )

        assertEquals(emptyList<ServiceAlert>(), result.alerts)
        assertEquals(mapOf("b" to true), result.statesToStore)
    }

    @Test
    fun `all instances unreachable is treated as device offline`() {
        val result = ServiceAlertEvaluator.evaluate(
            previous = mapOf("a" to true, "b" to true, "c" to false),
            current = mapOf("a" to false, "b" to false, "c" to false)
        )

        assertTrue(result.skippedAsDeviceOffline)
        assertEquals(emptyList<ServiceAlert>(), result.alerts)
        assertEquals(emptyMap<String, Boolean>(), result.statesToStore)
    }

    @Test
    fun `single unreachable instance still alerts`() {
        val result = ServiceAlertEvaluator.evaluate(
            previous = mapOf("a" to true),
            current = mapOf("a" to false)
        )

        assertFalse(result.skippedAsDeviceOffline)
        assertEquals(listOf(ServiceAlert.Down("a")), result.alerts)
    }

    @Test
    fun `alerts are ordered by instance id`() {
        val result = ServiceAlertEvaluator.evaluate(
            previous = mapOf("z" to true, "m" to false, "a" to true),
            current = mapOf("z" to false, "m" to true, "a" to false)
        )

        assertEquals(
            listOf(ServiceAlert.Down("a"), ServiceAlert.Recovered("m"), ServiceAlert.Down("z")),
            result.alerts
        )
    }
}
