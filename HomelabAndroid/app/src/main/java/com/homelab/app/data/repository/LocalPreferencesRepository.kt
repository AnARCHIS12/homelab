package com.homelab.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.homelab.app.util.ServiceType
import com.homelab.app.util.AppIconOption
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class ThemeMode {
    SYSTEM, LIGHT, DARK;

    companion object {
        fun fromString(value: String?): ThemeMode {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: SYSTEM
        }
    }
}

enum class LanguageMode(val code: String, val shortCode: String, val nativeName: String) {
    ITALIAN("it", "IT", "Italiano"),
    ENGLISH("en", "EN", "English"),
    FRENCH("fr", "FR", "Français"),
    SPANISH("es", "ES", "Español"),
    GERMAN("de", "DE", "Deutsch");

    companion object {
        fun fromCode(code: String?): LanguageMode {
            return entries.find { it.code.equals(code, ignoreCase = true) } ?: ENGLISH
        }
    }
}

@Singleton
class LocalPreferencesRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    val pinSecurityManager: com.homelab.app.data.security.PinSecurityManager
) {
    private val dataStore = context.dataStore

    private val THEME_KEY = stringPreferencesKey("theme_mode")
    private val LANG_KEY = stringPreferencesKey("language_mode")
    private val HIDDEN_SERVICES_KEY = stringPreferencesKey("hidden_services")
    private val SERVICE_ORDER_KEY = stringPreferencesKey("service_order")
    private val BIOMETRIC_KEY = booleanPreferencesKey("biometric_enabled")
    private val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")
    private val BESZEL_SHOW_CPU_KEY = booleanPreferencesKey("beszel_show_cpu")
    private val BESZEL_SHOW_MEMORY_KEY = booleanPreferencesKey("beszel_show_memory")
    private val BESZEL_SHOW_NETWORK_KEY = booleanPreferencesKey("beszel_show_network")
    private val DOCKHAND_AUTO_REFRESH_KEY = booleanPreferencesKey("dockhand_auto_refresh")
    private val DOCKHAND_REFRESH_INTERVAL_KEY = longPreferencesKey("dockhand_refresh_interval")
    private val DOCKHAND_ACTIVITY_LIMIT_KEY = longPreferencesKey("dockhand_activity_limit")
    private val DOCKHAND_SHOW_RAW_ACTIVITY_KEY = booleanPreferencesKey("dockhand_show_raw_activity")
    private val HOME_CYBERPUNK_CARDS_KEY = booleanPreferencesKey("home_cyberpunk_cards")
    private val APP_ICON_KEY = stringPreferencesKey("app_icon")
    private val DISMISSED_UPDATE_VERSION_KEY = stringPreferencesKey("dismissed_update_version")
    private val UPDATE_LAST_CHECKED_AT_KEY = longPreferencesKey("update_last_checked_at")
    private val UPDATE_AVAILABLE_VERSION_KEY = stringPreferencesKey("update_available_version")
    private val UPDATE_AVAILABLE_URL_KEY = stringPreferencesKey("update_available_url")
    private val UPDATE_AVAILABLE_CHANGELOG_KEY = stringPreferencesKey("update_available_changelog")
    private val DISMISSED_POPUP_VERSION_KEY = stringPreferencesKey("dismissed_popup_version")
    private val MEDIA_ARR_TUTORIAL_DISMISSED_KEY = booleanPreferencesKey("media_arr_tutorial_dismissed")
    private val BACKUP_SELECTED_TYPES_KEY = stringPreferencesKey("backup_selected_service_types")
    private val BACKUP_REMEMBER_SELECTION_KEY = booleanPreferencesKey("backup_remember_selection")

    val themeMode: Flow<ThemeMode> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            ThemeMode.fromString(preferences[THEME_KEY])
        }

    val languageMode: Flow<LanguageMode> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            LanguageMode.fromCode(preferences[LANG_KEY])
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[THEME_KEY] = mode.name
        }
    }

    suspend fun setLanguageMode(mode: LanguageMode) {
        dataStore.edit { preferences ->
            preferences[LANG_KEY] = mode.code
        }
    }

    val beszelShowCpu: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences -> preferences[BESZEL_SHOW_CPU_KEY] ?: true }

    val beszelShowMemory: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences -> preferences[BESZEL_SHOW_MEMORY_KEY] ?: true }

    val beszelShowNetwork: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences -> preferences[BESZEL_SHOW_NETWORK_KEY] ?: true }

    suspend fun setBeszelShowCpu(value: Boolean) {
        dataStore.edit { preferences -> preferences[BESZEL_SHOW_CPU_KEY] = value }
    }

    suspend fun setBeszelShowMemory(value: Boolean) {
        dataStore.edit { preferences -> preferences[BESZEL_SHOW_MEMORY_KEY] = value }
    }

    suspend fun setBeszelShowNetwork(value: Boolean) {
        dataStore.edit { preferences -> preferences[BESZEL_SHOW_NETWORK_KEY] = value }
    }

    val dockhandAutoRefreshEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences -> preferences[DOCKHAND_AUTO_REFRESH_KEY] ?: true }

    val dockhandRefreshIntervalSeconds: Flow<Int> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val value = (preferences[DOCKHAND_REFRESH_INTERVAL_KEY] ?: 45L).toInt()
            value.coerceIn(15, 300)
        }

    val dockhandActivityLimit: Flow<Int> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val value = (preferences[DOCKHAND_ACTIVITY_LIMIT_KEY] ?: 25L).toInt()
            value.coerceIn(5, 100)
        }

    val dockhandShowRawActivity: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences -> preferences[DOCKHAND_SHOW_RAW_ACTIVITY_KEY] ?: false }

    suspend fun setDockhandAutoRefreshEnabled(value: Boolean) {
        dataStore.edit { preferences -> preferences[DOCKHAND_AUTO_REFRESH_KEY] = value }
    }

    suspend fun setDockhandRefreshIntervalSeconds(value: Int) {
        dataStore.edit { preferences ->
            preferences[DOCKHAND_REFRESH_INTERVAL_KEY] = value.coerceIn(15, 300).toLong()
        }
    }

    suspend fun setDockhandActivityLimit(value: Int) {
        dataStore.edit { preferences ->
            preferences[DOCKHAND_ACTIVITY_LIMIT_KEY] = value.coerceIn(5, 100).toLong()
        }
    }

    suspend fun setDockhandShowRawActivity(value: Boolean) {
        dataStore.edit { preferences -> preferences[DOCKHAND_SHOW_RAW_ACTIVITY_KEY] = value }
    }

    val hiddenServices: Flow<Set<String>> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val raw = preferences[HIDDEN_SERVICES_KEY] ?: ""
            if (raw.isBlank()) {
                emptySet()
            } else {
                raw.split(",")
                    .mapNotNull(::canonicalServiceKey)
                    .toSet()
            }
        }

    val homeCyberpunkCardsEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences -> preferences[HOME_CYBERPUNK_CARDS_KEY] ?: false }

    val appIcon: Flow<AppIconOption> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            AppIconOption.fromPersistedValue(preferences[APP_ICON_KEY])
        }

    val mediaArrTutorialDismissed: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences -> preferences[MEDIA_ARR_TUTORIAL_DISMISSED_KEY] ?: false }

    val backupSelectedServiceTypes: Flow<Set<ServiceType>> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val raw = preferences[BACKUP_SELECTED_TYPES_KEY].orEmpty()
            if (raw.isBlank()) {
                emptySet()
            } else {
                raw.split(',')
                    .mapNotNull(::parseStoredServiceType)
                    .filter { it != ServiceType.UNKNOWN }
                    .toSet()
            }
        }

    val backupRememberSelectionEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences -> preferences[BACKUP_REMEMBER_SELECTION_KEY] ?: true }

    val serviceOrder: Flow<List<ServiceType>> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val raw = preferences[SERVICE_ORDER_KEY]
                ?.split(",")
                ?.mapNotNull(::parseStoredServiceType)
                .orEmpty()
            normalizeServiceOrder(raw)
        }

    suspend fun toggleServiceVisibility(serviceKey: String) {
        dataStore.edit { preferences ->
            val raw = preferences[HIDDEN_SERVICES_KEY] ?: ""
            val normalizedKey = canonicalServiceKey(serviceKey) ?: return@edit
            val current = if (raw.isBlank()) {
                mutableSetOf()
            } else {
                raw.split(",")
                    .mapNotNull(::canonicalServiceKey)
                    .toMutableSet()
            }
            if (current.contains(normalizedKey)) {
                current.remove(normalizedKey)
            } else {
                current.add(normalizedKey)
            }
            preferences[HIDDEN_SERVICES_KEY] = current.joinToString(",")
        }
    }

    suspend fun moveService(serviceType: ServiceType, offset: Int) {
        dataStore.edit { preferences ->
            val current = normalizeServiceOrder(
                preferences[SERVICE_ORDER_KEY]
                    ?.split(",")
                    ?.mapNotNull(::parseStoredServiceType)
                    .orEmpty()
            ).toMutableList()
            val index = current.indexOf(serviceType)
            if (index == -1) return@edit
            val destination = index + offset
            if (destination !in current.indices) return@edit
            val moved = current.removeAt(index)
            current.add(destination, moved)
            preferences[SERVICE_ORDER_KEY] = current.joinToString(",") { it.name }
        }
    }

    suspend fun moveServiceWithin(
        serviceType: ServiceType,
        offset: Int,
        within: Set<ServiceType>
    ) {
        dataStore.edit { preferences ->
            val current = normalizeServiceOrder(
                preferences[SERVICE_ORDER_KEY]
                    ?.split(",")
                    ?.mapNotNull(::parseStoredServiceType)
                    .orEmpty()
            ).toMutableList()

            if (serviceType !in within) return@edit
            val scopedOrder = current.filter { it in within }
            val scopedIndex = scopedOrder.indexOf(serviceType)
            if (scopedIndex == -1) return@edit
            val scopedDestination = scopedIndex + offset
            if (scopedDestination !in scopedOrder.indices) return@edit

            val targetType = scopedOrder[scopedDestination]
            val fromIndex = current.indexOf(serviceType)
            val toIndex = current.indexOf(targetType)
            if (fromIndex == -1 || toIndex == -1 || fromIndex == toIndex) return@edit

            val moved = current.removeAt(fromIndex)
            current.add(toIndex, moved)
            preferences[SERVICE_ORDER_KEY] = current.joinToString(",") { it.name }
        }
    }

    suspend fun setHomeCyberpunkCardsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[HOME_CYBERPUNK_CARDS_KEY] = enabled
        }
    }

    suspend fun setAppIcon(icon: AppIconOption) {
        dataStore.edit { preferences ->
            preferences[APP_ICON_KEY] = icon.persistedValue
        }
    }

    suspend fun setMediaArrTutorialDismissed(dismissed: Boolean) {
        dataStore.edit { preferences ->
            preferences[MEDIA_ARR_TUTORIAL_DISMISSED_KEY] = dismissed
        }
    }

    suspend fun setBackupSelectedServiceTypes(types: Set<ServiceType>) {
        dataStore.edit { preferences ->
            val normalized = types
                .asSequence()
                .filter { it != ServiceType.UNKNOWN }
                .map { it.name }
                .sorted()
                .toList()
            if (normalized.isEmpty()) {
                preferences.remove(BACKUP_SELECTED_TYPES_KEY)
            } else {
                preferences[BACKUP_SELECTED_TYPES_KEY] = normalized.joinToString(",")
            }
        }
    }

    suspend fun setBackupRememberSelectionEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[BACKUP_REMEMBER_SELECTION_KEY] = enabled
        }
    }

    val dismissedUpdateVersion: Flow<String?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences -> preferences[DISMISSED_UPDATE_VERSION_KEY] }

    suspend fun setDismissedUpdateVersion(version: String?) {
        dataStore.edit { preferences ->
            if (version.isNullOrBlank()) {
                preferences.remove(DISMISSED_UPDATE_VERSION_KEY)
            } else {
                preferences[DISMISSED_UPDATE_VERSION_KEY] = version
            }
        }
    }

    val updateLastCheckedAt: Flow<Long?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences -> preferences[UPDATE_LAST_CHECKED_AT_KEY] }

    suspend fun setUpdateLastCheckedAt(timestampMillis: Long?) {
        dataStore.edit { preferences ->
            if (timestampMillis == null) {
                preferences.remove(UPDATE_LAST_CHECKED_AT_KEY)
            } else {
                preferences[UPDATE_LAST_CHECKED_AT_KEY] = timestampMillis
            }
        }
    }

    val updateAvailableVersion: Flow<String?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences -> preferences[UPDATE_AVAILABLE_VERSION_KEY] }

    val updateAvailableUrl: Flow<String?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences -> preferences[UPDATE_AVAILABLE_URL_KEY] }

    val updateAvailableChangelog: Flow<String?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences -> preferences[UPDATE_AVAILABLE_CHANGELOG_KEY] }

    val dismissedPopupVersion: Flow<String?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences -> preferences[DISMISSED_POPUP_VERSION_KEY] }

    suspend fun setAvailableUpdate(version: String?, url: String?, changelog: String? = null) {
        dataStore.edit { preferences ->
            if (version.isNullOrBlank()) {
                preferences.remove(UPDATE_AVAILABLE_VERSION_KEY)
                preferences.remove(UPDATE_AVAILABLE_URL_KEY)
                preferences.remove(UPDATE_AVAILABLE_CHANGELOG_KEY)
            } else {
                preferences[UPDATE_AVAILABLE_VERSION_KEY] = version
                if (url.isNullOrBlank()) {
                    preferences.remove(UPDATE_AVAILABLE_URL_KEY)
                } else {
                    preferences[UPDATE_AVAILABLE_URL_KEY] = url
                }
                if (changelog.isNullOrBlank()) {
                    preferences.remove(UPDATE_AVAILABLE_CHANGELOG_KEY)
                } else {
                    preferences[UPDATE_AVAILABLE_CHANGELOG_KEY] = changelog
                }
            }
        }
    }

    suspend fun setDismissedPopupVersion(version: String?) {
        dataStore.edit { preferences ->
            if (version.isNullOrBlank()) {
                preferences.remove(DISMISSED_POPUP_VERSION_KEY)
            } else {
                preferences[DISMISSED_POPUP_VERSION_KEY] = version
            }
        }
    }

    // PIN & Biometric

    val appPin: Flow<String?> = pinSecurityManager.isPinSet
        .map { if (it) "SET" else null }

    val isPinSet: Flow<Boolean> = pinSecurityManager.isPinSet

    val pinLockoutRemainingSeconds: Flow<Long> = pinSecurityManager.lockoutRemainingSeconds

    val biometricEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences -> preferences[BIOMETRIC_KEY] ?: false }

    val hasCompletedOnboarding: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences -> preferences[ONBOARDING_COMPLETED_KEY] ?: false }

    suspend fun savePin(pin: String) {
        pinSecurityManager.savePin(pin)
    }

    suspend fun verifyPin(pin: String): com.homelab.app.data.security.PinVerificationResult {
        return pinSecurityManager.verifyPin(pin)
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[BIOMETRIC_KEY] = enabled
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED_KEY] = completed
        }
    }

    suspend fun clearSecurity() {
        pinSecurityManager.clearPin()
        dataStore.edit { preferences ->
            preferences.remove(BIOMETRIC_KEY)
        }
    }

    private fun normalizeServiceOrder(order: List<ServiceType>): List<ServiceType> {
        val visibleTypes = ServiceType.entries.filter { it != ServiceType.UNKNOWN }
        val unique = buildList {
            order.forEach { type ->
                if (type != ServiceType.UNKNOWN && type !in this) add(type)
            }
        }
        return unique + visibleTypes.filterNot(unique::contains)
    }

    private fun parseStoredServiceType(raw: String): ServiceType? {
        val parsed = ServiceType.fromStoredName(raw)
        return parsed.takeUnless { it == ServiceType.UNKNOWN }
    }

    private fun canonicalServiceKey(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        return parseStoredServiceType(trimmed)?.name ?: trimmed
    }
}
