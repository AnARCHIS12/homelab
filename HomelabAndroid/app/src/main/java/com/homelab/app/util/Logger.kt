package com.homelab.app.util

import android.util.Log

/**
 * Centralized logging utility for the Homelab Android application.
 */
object Logger {
    fun d(tag: String, message: String) {
        val sanitized = com.homelab.app.data.security.LogSanitizer.redactString(message)
        LogStore.add(LogLevel.DEBUG, tag, sanitized)
        try { Log.d(tag, sanitized) } catch (_: Throwable) {}
    }

    fun i(tag: String, message: String) {
        val sanitized = com.homelab.app.data.security.LogSanitizer.redactString(message)
        LogStore.add(LogLevel.INFO, tag, sanitized)
        try { Log.i(tag, sanitized) } catch (_: Throwable) {}
    }

    fun w(tag: String, message: String) {
        val sanitized = com.homelab.app.data.security.LogSanitizer.redactString(message)
        LogStore.add(LogLevel.WARN, tag, sanitized)
        try { Log.w(tag, sanitized) } catch (_: Throwable) {}
    }

    fun net(tag: String, message: String) {
        val sanitized = com.homelab.app.data.security.LogSanitizer.redactString(message)
        LogStore.add(LogLevel.NET, tag, sanitized)
        try { Log.i(tag, sanitized) } catch (_: Throwable) {}
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val raw = if (throwable?.message.isNullOrBlank()) message else "$message (${throwable?.message})"
        val sanitized = com.homelab.app.data.security.LogSanitizer.redactString(raw)
        LogStore.add(LogLevel.WARN, tag, sanitized)
        try { Log.e(tag, sanitized, throwable) } catch (_: Throwable) {}
    }

    fun stateTransition(tag: String, stateName: String, state: UiState<*>) {
        val stateString = when (state) {
            is UiState.Idle -> "Idle"
            is UiState.Loading -> "Loading"
            is UiState.Success -> "Success"
            is UiState.Error -> "Error(${state.message})"
            is UiState.Offline -> "Offline"
        }
        val message = "State Transition -> $stateName: $stateString"
        LogStore.add(LogLevel.DEBUG, tag, message)
        Log.d(tag, message)
    }
}
