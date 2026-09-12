package com.homelab.app.ui.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homelab.app.data.repository.LocalPreferencesRepository
import com.homelab.app.data.security.PinVerificationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val preferencesRepository: LocalPreferencesRepository,
    private val servicesRepository: com.homelab.app.data.repository.ServicesRepository
) : ViewModel() {

    val isPinSet: StateFlow<Boolean> = preferencesRepository.isPinSet
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val biometricEnabled: StateFlow<Boolean> = preferencesRepository.biometricEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val lockoutRemainingSeconds: StateFlow<Long> = preferencesRepository.pinLockoutRemainingSeconds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    fun savePin(pin: String) {
        viewModelScope.launch {
            preferencesRepository.savePin(pin)
        }
    }

    fun verifyPin(pin: String): Boolean {
        return runBlocking {
            preferencesRepository.verifyPin(pin) is PinVerificationResult.Success
        }
    }

    suspend fun verifyPinWithResult(pin: String): PinVerificationResult {
        return preferencesRepository.verifyPin(pin)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setBiometricEnabled(enabled)
        }
    }

    fun clearSecurity() {
        viewModelScope.launch {
            preferencesRepository.clearSecurity()
        }
    }

    fun checkTailscale() {
        servicesRepository.checkTailscale()
    }
}
