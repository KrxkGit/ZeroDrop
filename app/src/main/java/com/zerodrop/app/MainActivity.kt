package com.zerodrop.app

import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.ambient.AmbientModeSupport
import androidx.wear.ambient.AmbientModeSupport.AmbientCallbackProvider
import com.zerodrop.app.ui.AmbientUiState
import com.zerodrop.app.ui.LocalAmbientState
import com.zerodrop.app.ui.QrCodeScreen
import com.zerodrop.app.ui.ScoringScreen
import com.zerodrop.app.ui.SetupScreen
import com.zerodrop.app.ui.theme.ZeroDropTheme

/**
 * Main entry point for ZeroDrop on Wear OS.
 *
 * Only attaches [AmbientModeSupport] when the wearable shared library is
 * physically present on the device — probes for the same class that
 * SharedLibraryVersion.PresenceHolder checks internally. This avoids a
 * hard crash during the ambient Fragment's lifecycle on devices that
 * lack com.google.android.wearable.
 */
class MainActivity : FragmentActivity(),
    AmbientCallbackProvider {

    private var ambientController: AmbientModeSupport.AmbientController? = null

    private val isAmbient = mutableStateOf(false)

    override fun getAmbientCallback(): AmbientModeSupport.AmbientCallback {
        return object : AmbientModeSupport.AmbientCallback() {
            override fun onEnterAmbient(ambientDetails: Bundle?) {
                isAmbient.value = true
            }
            override fun onUpdateAmbient() {}
            override fun onExitAmbient() {
                isAmbient.value = false
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ambientController = if (wearableLibraryPresent()) {
            AmbientModeSupport.attach(this)
        } else {
            Log.w(TAG, "Wearable shared library missing — ambient mode disabled")
            null
        }

        enableEdgeToEdge()

        setContent {
            val ambient by isAmbient

            // ── Screen-on management ──
            // FLAG_KEEP_SCREEN_ON keeps the display awake in interactive mode.
            // When ambient is available, the system handles AOD automatically.
            DisposableEffect(Unit) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                onDispose {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            CompositionLocalProvider(
                LocalAmbientState provides AmbientUiState(isAmbient = ambient)
            ) {
                ZeroDropTheme {
                    var screen by remember { mutableStateOf<Screen>(Screen.Setup) }
                    var scoreLimit by remember { mutableIntStateOf(21) }
                    var totalSets by remember { mutableIntStateOf(3) }
                    var initHalf by remember { mutableIntStateOf(-1) }
                    var qrCodeData by remember { mutableStateOf("") }

                    when (screen) {
                        Screen.Setup -> {
                            SetupScreen(
                                onStartMatch = { limit, sets, half ->
                                    scoreLimit = limit
                                    totalSets = sets
                                    initHalf = half
                                    screen = Screen.Scoring
                                },
                                onShowQrCode = { data ->
                                    qrCodeData = data
                                    screen = Screen.QrCode
                                }
                            )
                        }
                        Screen.Scoring -> {
                            val viewModel: GameViewModel = viewModel(
                                factory = GameViewModel.Factory(application)
                            )

                            LaunchedEffect(Unit) {
                                viewModel.startMatch(scoreLimit, totalSets, initHalf)
                            }

                            ScoringScreen(
                                onNewMatch = {
                                    viewModel.onMatchEnded()
                                    screen = Screen.Setup
                                },
                                viewModel = viewModel
                            )
                        }
                        Screen.QrCode -> {
                            QrCodeScreen(
                                matchData = qrCodeData,
                                leftScore = 0,
                                rightScore = 0,
                                onBack = {
                                    qrCodeData = ""
                                    screen = Screen.Setup
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "ZeroDrop"

        /**
         * Mirrors SharedLibraryVersion.isSharedLibPresent() —
         * probes for the wearable-compat controller class before
         * the ambient Fragment ever enters its lifecycle.
         */
        private fun wearableLibraryPresent(): Boolean {
            return try {
                Class.forName("com.google.android.wearable.compat.WearableActivityController")
                true
            } catch (_: ClassNotFoundException) {
                false
            }
        }
    }
}

private enum class Screen {
    Setup,
    Scoring,
    QrCode
}