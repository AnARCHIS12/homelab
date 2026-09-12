package com.homelab.app.ui.security

import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.core.content.ContextCompat
import com.homelab.app.R
import com.homelab.app.util.BiometricHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class SetupStep { WELCOME, ASK, CREATE, CONFIRM }

@Composable
fun PinSetupScreen(
    onComplete: () -> Unit,
    onSavePin: (String) -> Unit,
    onEnableBiometric: (Boolean) -> Unit
) {
    var step by remember { mutableStateOf(SetupStep.WELCOME) }
    var firstPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = (context as? android.app.Activity)?.window
        window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
    val scope = rememberCoroutineScope()
    val canUseBiometric = remember { BiometricHelper.canAuthenticate(context) }
    val mismatchText = stringResource(R.string.security_pin_mismatch)
    val palette = rememberSecurityScreenPalette()

    Scaffold { padding ->
        AnimatedContent(
            targetState = step,
            modifier = Modifier
                .padding(padding)
                .background(palette.backgroundBrush),
            transitionSpec = {
                slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
            },
            label = "setup_step"
        ) { currentStep ->
            when (currentStep) {
                SetupStep.WELCOME -> {
                    WelcomeScreen(
                        palette = palette,
                        onNext = { step = SetupStep.ASK }
                    )
                }
                
                SetupStep.ASK -> {
                    AskSetupScreen(
                        palette = palette,
                        onYes = { step = SetupStep.CREATE },
                        onNo = { onComplete() }
                    )
                }

                SetupStep.CREATE -> {
                    PinEntryScreen(
                        title = stringResource(R.string.security_setup_pin),
                        subtitle = stringResource(R.string.security_setup_pin_desc),
                        onPinComplete = { pin ->
                            firstPin = pin
                            step = SetupStep.CONFIRM
                        }
                    )
                }

                SetupStep.CONFIRM -> {
                    PinEntryScreen(
                        title = stringResource(R.string.security_confirm_pin),
                        subtitle = stringResource(R.string.security_confirm_pin_desc),
                        errorMessage = errorMessage,
                        onPinComplete = { pin ->
                            if (pin == firstPin) {
                                onSavePin(pin)
                                if (canUseBiometric) {
                                    promptSystemBiometric(
                                        context = context as AppCompatActivity,
                                        onResult = { success ->
                                            onEnableBiometric(success)
                                            // Delay slightly to let the prompt fully dismiss before navigating away
                                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                                onComplete()
                                            }, 200)
                                        }
                                    )
                                } else {
                                    onComplete()
                                }
                            } else {
                                errorMessage = mismatchText
                                scope.launch {
                                    delay(1500)
                                    errorMessage = null
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeScreen(
    palette: SecurityScreenPalette,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.backgroundBrush)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.3f))

        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = palette.iconFill,
            border = androidx.compose.foundation.BorderStroke(1.dp, palette.iconStroke),
            shadowElevation = 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = stringResource(R.string.onboarding_welcome),
                    modifier = Modifier.size(64.dp),
                    tint = palette.accent
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.onboarding_welcome),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = palette.primaryText,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.onboarding_welcome_desc),
            style = MaterialTheme.typography.bodyLarge,
            color = palette.secondaryText,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = Color.White),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
        ) {
            Text(
                text = stringResource(R.string.onboarding_welcome_button),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun AskSetupScreen(
    palette: SecurityScreenPalette,
    onYes: () -> Unit,
    onNo: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.backgroundBrush)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.3f))

        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = palette.iconFill,
            border = androidx.compose.foundation.BorderStroke(1.dp, palette.iconStroke),
            shadowElevation = 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = stringResource(R.string.security_title),
                    modifier = Modifier.size(64.dp),
                    tint = palette.accent
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.security_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = palette.primaryText,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.onboarding_ask_pin),
            style = MaterialTheme.typography.bodyLarge,
            color = palette.secondaryText,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onYes,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = Color.White),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
            ) {
                Text(
                    text = stringResource(R.string.onboarding_ask_pin_yes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            TextButton(
                onClick = onNo,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.onboarding_ask_pin_no),
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.secondaryText
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

private fun promptSystemBiometric(context: AppCompatActivity, onResult: (Boolean) -> Unit) {
    val executor = ContextCompat.getMainExecutor(context)
    val biometricPrompt = BiometricPrompt(context, executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onResult(false)
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onResult(true)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // Do not immediately fail out on a simple mismatch, let the system UI handle retries
            }
        })

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle(context.getString(R.string.security_enable_biometric))
        .setSubtitle(context.getString(R.string.security_biometric_reason))
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)
        .setConfirmationRequired(false)
        .setNegativeButtonText(context.getString(R.string.security_skip))
        .build()

    biometricPrompt.authenticate(promptInfo)
}
